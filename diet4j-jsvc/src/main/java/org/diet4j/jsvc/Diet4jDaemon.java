//
// The rights holder(s) license this file to you under the
// Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License. You
// may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
// For information about copyright ownership, see the NOTICE
// file distributed with this work.
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
//

package org.diet4j.jsvc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.LogManager;
import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.diet4j.cmdline.CmdlineParameter;
import org.diet4j.cmdline.CmdlineParameters;
import org.diet4j.core.Module;
import org.diet4j.core.ModuleActivationException;
import org.diet4j.core.ModuleDeactivationException;
import org.diet4j.core.ModuleMeta;
import org.diet4j.core.ModuleNotFoundException;
import org.diet4j.core.ModuleRegistry;
import org.diet4j.core.ModuleRequirement;
import org.diet4j.core.ModuleResolutionException;
import org.diet4j.core.ModuleRunException;
import org.diet4j.core.ModuleSettings;
import org.diet4j.core.NoRunMethodException;
import org.diet4j.core.ScanningDirectoriesModuleRegistry;

/**
 * An implementation of the jsvc Daemon interface that enables jsvc to
 * invoke the diet4j framework and start a diet4j module.
 */
public class Diet4jDaemon
    implements Daemon
{
    @Override
    public void init(
            DaemonContext dc )
        throws
            DaemonInitException
    {
        CmdlineParameters parameters = new CmdlineParameters(
            new CmdlineParameter.Value( "directory",    true ),
            new CmdlineParameter.Value( "directories",  false ),
            new CmdlineParameter.Value( "runclass",     false ),
            new CmdlineParameter.Value( "runmethod",    false ),
            new CmdlineParameter.Value( "config",       false ),
            new CmdlineParameter.Flag(  "verbose",      true ),
            new CmdlineParameter.Value( "logConfigDir", true ),
            new CmdlineParameter.Value( "logConfig",    true )
        );

        String [] remaining = parameters.parse( dc.getArguments() );

        // logging

        List<String> logConfigDirs = parameters.getManyValued(   "logConfigDir" );
        String       logConfigFile = parameters.getSingleValued( "logConfig" );
        int          verbosity     = parameters.getFlagCount(    "verbose" );

        if( logConfigFile != null ) {
            if( verbosity > 0 ) {
                fatal( "Specify --verbose or --logConfig, not both" );
            }
            if( !logConfigDirs.isEmpty() ) {
                fatal( "specify --logConfig or --logConfigDir, not both" );
            }
        } else {
            if( logConfigDirs.isEmpty() ) {
                logConfigDirs.add( "/etc/diet4j" );
            }

            String localLogConfigName;
            if( verbosity > 0 ) {
                localLogConfigName = String.format( "log-default-v%d-java.properties", verbosity );
            } else {
                localLogConfigName = "log-default-java.properties";
            }

            for( String logConfigDir : logConfigDirs ) {
                String candidate = logConfigDir + "/" + localLogConfigName;
                if( new File( candidate ).canRead() ) {
                    logConfigFile = candidate;
                }
            }
        }
        if( logConfigFile != null ) {
            try {
                LogManager.getLogManager().readConfiguration( new FileInputStream( logConfigFile ));

            } catch( IOException ex ) {
                throw new DaemonInitException( "Failed to read log configuration file:", ex );
            }
        }

        // modules

        String [] moduleNames;
        // in case of command-line, we recognize only one to-be-activated root module,
        // otherwise the invocation syntax becomes awkward
        if( remaining.length >= 1 ) {
            moduleNames = new String[] { remaining[0] };
        } else {
            moduleNames = null;
        }
        if( remaining.length > 1 ) {
            theRunArguments = new String[ remaining.length - 1 ];
            System.arraycopy( remaining, 1, theRunArguments, 0 , theRunArguments.length );
        } else {
            theRunArguments = null;
        }

        theRunClassName  = parameters.getSingleValued( "runclass" );
        theRunMethodName = parameters.getSingleValued( "runmethod" );

        ArrayList<String> directories = new ArrayList<>();
        if( parameters.get( "directory" ) != null ) {
            for( String dir : parameters.getManyValued( "directory" )) {
                directories.add( dir );
            }
        }
        if( parameters.get( "directories" ) != null ) {
            for( String dir : parameters.getSingleValued( "directories" ).split( "[:;]+" )) {
                directories.add( dir );
            }
        }

        Map<ModuleRequirement,Map<String,String>> rawModuleSettings = new HashMap<>();

        String config = parameters.getSingleValued( "config" );
        if( config != null ) {
            Properties configProps = new Properties();

            // read properties from config file, or if directory, properties from all files in directory (non-recursive)
            File configFile = new File( config );
            if( configFile.exists() ) {
                if( configFile.isFile() ) {
                    try ( FileInputStream configStream = new FileInputStream( configFile ) ) {
                        configProps.load( configStream );

                    } catch( IOException ex ) {
                        fatal( "Cannot read config file: " + configFile.getAbsolutePath(), ex );
                    }
                } else if( configFile.isDirectory() ) {
                    for( File subConfigFile : configFile.listFiles() ) {
                        if( subConfigFile.isFile() ) {
                            Properties subConfigProps = new Properties();
                            try ( FileInputStream configStream = new FileInputStream( subConfigFile ) ) {
                                subConfigProps.load( configStream );

                            } catch( IOException ex ) {
                                fatal( "Cannot read config file: " + subConfigFile.getAbsolutePath(), ex );
                            }
                            configProps.putAll( subConfigProps ); // maybe it would merge itself. but the docs don't say
                        }
                    }
                } else {
                    fatal( "Config file is neither file nor directory: " + config );
                }
            } else {
                fatal( "Config file does not exist: " + config );
            }


            if( configProps.containsKey( "diet4j!directory" )) {
                if( !directories.isEmpty() ) {
                    fatal( "Specified both as argument and in config file: directory" );
                }
                for( String dir : configProps.getProperty( "diet4j!directories" ).split( "[:;]+" )) {
                    directories.add( dir );
                }
                // FIXME: no spaces or commas in file names
            }
            if( configProps.containsKey( "diet4j!module" )) {
                if( moduleNames != null && moduleNames.length > 0 ) {
                    fatal( "Specified both as argument and in config file: module" );
                }
                moduleNames = configProps.getProperty( "diet4j!module" ).split( "[,\\s]+" );
                // FIXME: no spaces or commas in file names
            }
            if( configProps.containsKey( "diet4j!runclass" )) {
                if( theRunClassName != null ) {
                    fatal( "Specified both as argument and in config file: runclass" );
                }
                theRunClassName = configProps.getProperty( "diet4j!runclass" );
            }
            if( configProps.containsKey( "diet4j!runmethod" )) {
                if( theRunMethodName != null ) {
                    fatal( "Specified both as argument and in config file: runmethod" );
                }
                theRunMethodName = configProps.getProperty( "diet4j!runmethod" );
            }
            if( configProps.containsKey( "diet4j!runarg" )) {
                if( theRunArguments != null && theRunArguments.length > 0 ) {
                    fatal( "Specified both as argument and in config file: arg" );
                }
                theRunArguments = configProps.getProperty( "diet4j!runarg" ).split( "[,\\s]+" );
                // FIXME: this currently does not allow quotes to be used to keep white space or such
            }
            for( Object key : configProps.keySet() ) {
                String realKey = (String) key;
                int    excl    = realKey.indexOf( '!' );
                if( excl > 0 ) {
                    String n1 = realKey.substring( 0, excl );
                    String n2 = realKey.substring( excl + 1 );

                    if( !"diet4j".equals( n1 )) {
                        try {
                            ModuleRequirement req1 = ModuleRequirement.parse( n1 );
                            Map<String,String> forThisModule = rawModuleSettings.get( req1 );
                            if( forThisModule == null ) {
                                forThisModule = new HashMap<>();
                                rawModuleSettings.put( req1, forThisModule );
                            }
                            forThisModule.put( n2, configProps.getProperty( realKey ));

                        } catch( ParseException ex ) {
                            fatal( "Failed to parse String into ModuleRequirement: " + n1 );
                        }
                    }
                }
            }
        }

        if( moduleNames == null || moduleNames.length == 0 ) {
            fatal( "No root module given" );
            return; // won't happen, but make IDE happy
        }

        theModuleRequirements = new ModuleRequirement[ moduleNames.length ];
        for( int i=0 ; i<moduleNames.length ; ++i ) {
            try {
                theModuleRequirements[i] = ModuleRequirement.parse( moduleNames[i] );
            } catch( ParseException ex ) {
                fatal( ex.getLocalizedMessage() );
            }
        }

        HashSet<File> fileDirs = new HashSet<>();
        if( directories != null ) {
            for( String dir : directories ) {
                try {
                    if( !fileDirs.add( new File( dir ).getCanonicalFile() )) {
                        fatal( "Directory (indirectly?) specified more than once in moduledirectory parameter: " + dir );
                    }
                } catch( IOException ex ) {
                    fatal( "Failed to read directory " + dir, ex );
                }
            }
        }
        theDirectories = new File[ fileDirs.size() ];
        fileDirs.toArray( theDirectories );

        if( theRunArguments == null ) {
            theRunArguments = new String[0];
        }

        Map<ModuleRequirement,ModuleSettings> moduleSettings = new HashMap<>();
        for( Map.Entry<ModuleRequirement,Map<String,String>> e : rawModuleSettings.entrySet() ) {
            if( !e.getValue().isEmpty() ) {
                moduleSettings.put( e.getKey(), ModuleSettings.create( e.getValue() ));
            }
        }

        // create ModuleRegistry
        theModuleRegistry = ScanningDirectoriesModuleRegistry.create( theDirectories, moduleSettings );

        // find and resolve modules
        theModuleMetas = new ModuleMeta[ theModuleRequirements.length ];
        theModules     = new Module[     theModuleRequirements.length ]; // while we are at it

        for( int i=0 ; i<theModuleMetas.length ; ++i ) {
            try {
                theModuleMetas[i] = theModuleRegistry.determineSingleResolutionCandidate( theModuleRequirements[i] );

            } catch( Throwable ex ) {
                fatal( "Cannot find module " + theModuleRequirements[i] + " in registry " + theModuleRegistry, ex );
            }
        }
    }

    @Override
    public void start()
        throws
            ModuleResolutionException,
            ModuleNotFoundException,
            ModuleActivationException,
            ClassNotFoundException,
            ModuleRunException,
            NoRunMethodException,
            InvocationTargetException,
            DaemonInitException
    {
        for( int i=0 ; i<theModuleMetas.length ; ++i ) {
            try {
                theModules[i] = theModuleRegistry.resolve( theModuleMetas[i] );
                theModules[i].activateRecursively();

            } catch( Throwable ex ) {
                fatal( "Activation of module " + theModuleMetas[i] + " failed", ex );
            }
        }

        try {
            if( theRunClassName != null || theModules[0].getModuleMeta().getRunClassName() != null ) {
                theModules[0].run( theRunClassName, theRunMethodName, theRunArguments );
            }

        } catch( Throwable ex ) {
            Throwable rootCause = ex;
            while( rootCause.getCause() != null ) {
                rootCause = rootCause.getCause();
            }
            fatal( "Run of module " + theModules[0].getModuleMeta() + " failed. Root cause: " + rootCause.getMessage(), ex );
        }
    }

    @Override
    @SuppressWarnings("null")
    public void stop()
        throws
            ModuleDeactivationException,
            DaemonInitException
    {
        Throwable thrown = null;
        Module    failed = null;
        for( int i=theModules.length-1 ; i>=0 ; --i ) {
            if( theModules[i] == null ) {
                continue;
            }
            try {
                theModules[i].deactivateRecursively();

            } catch( Throwable ex ) {
                failed = theModules[i];
                thrown = ex;
            }
        }
        if( failed != null ) {
            Throwable rootCause = thrown;
            while( rootCause.getCause() != null ) {
                rootCause = rootCause.getCause();
            }
            fatal( "Deactivation of module " + failed.getModuleMeta() + " failed. Root cause: " + rootCause.getMessage(), thrown );
        }
    }

    @Override
    public void destroy()
    {
        // no op
    }

    /**
     * Something fatal has happened.
     *
     * @param msg the message
     * @throws DaemonInitException tell the daemon about it, always thrown
     */
    protected void fatal(
            String msg )
        throws
            DaemonInitException
    {
        fatal( msg, null );
    }

    /**
     * Something fatal has happened that had a cause.
     *
     * @param msg the message
     * @param cause the cause
     * @throws DaemonInitException tell the daemon about it, always thrown
     */
    protected void fatal(
            String    msg,
            Throwable cause )
        throws
            DaemonInitException
    {
        if( theModuleMetas != null ) {
            // try to clean up modules already initialized
            for( int i=theModuleMetas.length-1 ; i>=0 ; --i ) {
                try {
                    if( theModules[i] != null ) {
                        theModules[i].deactivateRecursively();
                    }

                } catch( Throwable ex ) {
                    // ignore
                }
            }
        }

        // jsvc truncates the stack trace that it dumps, so we need to do it ourselves.
        System.err.print( "FATAL: " );
        System.err.println( msg != null ? msg : "no message" );

        if( cause != null ) {

            for( Throwable current = cause; current != null; current = current.getCause() ) {
                if( current != cause ) {
                    System.err.print( "Caused by: " );
                }
                System.err.println( current.getMessage() != null ? current.getMessage() : current.getClass().getName() );
                for( StackTraceElement e : current.getStackTrace() ) {
                    System.err.println( String.format(
                            "    at %s.%s(%s:%d)",
                               e.getClassName(),
                               e.getMethodName(),
                               e.getFileName(),
                               e.getLineNumber() ) );
                }
            }
        }
        throw new DaemonInitException( msg, cause );
    }

    /**
     * The ModuleRegistry.
     */
    protected ModuleRegistry theModuleRegistry;

    /**
     * The ModuleMetas.
     */
    protected ModuleMeta [] theModuleMetas;

    /**
     * All Modules once they have been resolved.
     */
    protected Module [] theModules;

    /**
     * The paths to the Module JAR files.
     */
    protected static File [] theDirectories;

    /**
     * The name of the run class in the root Module, if specified on the command-line.
     */
    protected static String theRunClassName;

    /**
     * The name of the run method in the run class, if specified on the command-line.
     */
    protected static String theRunMethodName;

    /**
     * The ModuleRequirements to activate.
     */
    protected static ModuleRequirement [] theModuleRequirements;

    /**
     * The arguments to the run
     */
    protected static String [] theRunArguments;
}
