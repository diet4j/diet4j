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

package org.diet4j.cmdline;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.diet4j.cmdline.CmdlineParameters.Parameter;
import org.diet4j.core.Module;
import org.diet4j.core.ModuleMeta;
import org.diet4j.core.ModuleRegistry;
import org.diet4j.core.ModuleRequirement;
import org.diet4j.core.ModuleSettings;
import org.diet4j.core.ScanningDirectoriesModuleRegistry;
import org.diet4j.core.Version;

/**
 * <p>Acts as the main() program in a diet4j-based application.
 *    Based on passed-in parameters, it instantiates a ModuleRegistry, figures
 *    out what Modules are available, resolves and activates the top-most Module and
 *    all its mandatory dependencies, and runs it.</p>
 */
public abstract class CmdlineBootLoader
{
    /**
     * Private constructor to keep this from being instantiated.
     */
    protected CmdlineBootLoader()
    {
        // no op
    }

    /**
     * The main program from the Java perspective: just a shell that catches exceptions.
     *
     * @param args arguments provided by the user
     */
    public static void main(
            String [] args )
    {
        try {
            parseArguments( args );
            int ret = activateRunDeactivate();

            System.exit( ret );

        } catch( Throwable ex ) {

            StringWriter sw = new StringWriter();
            PrintWriter  pw = new PrintWriter( sw );

            pw.append( "Something bad happened that should not have:\n" );
            ex.printStackTrace( pw );
            pw.flush();

            fatal( sw.toString() );
        }
    }

    /**
     * Parse arguments.
     *
     * @param args arguments provided by the user
     */
    static void parseArguments(
            String [] args )
        throws
            IOException
    {
        CmdlineParameters parameters = new CmdlineParameters(
            new Parameter( "help",        0 ),
            new Parameter( "directory",   1, true ),
            new Parameter( "directories", 1 ),
            new Parameter( "runclass",    1 ),
            new Parameter( "runmethod",   1 ),
            new Parameter( "config",      1 )
        );

        String [] remaining = parameters.parse( args );

        if( parameters.containsKey( "help" )) {
            helpAndQuit();
        }

        // split module names and arguments
        int dashDash = remaining.length;
        for( int i=0 ; i<remaining.length ; ++i ) {
            if( "--".equals( remaining[i] )) {
                dashDash = i;
                break;
            }
        }
        String [] moduleNames;
        if( dashDash > 0 ) {
            moduleNames = new String[ dashDash ];
            System.arraycopy( remaining, 0, moduleNames, 0, dashDash );
        } else {
            moduleNames = null;
        }

        if( dashDash >= remaining.length-1 ) {
            theRunArguments = null;
        } else {
            theRunArguments = new String[ remaining.length - dashDash - 1 ];
            System.arraycopy( remaining, dashDash+1, theRunArguments, 0 , theRunArguments.length );
        }

        theRunClassName  = parameters.get( "runclass" );
        theRunMethodName = parameters.get( "runmethod" );

        ArrayList<String> directories = new ArrayList<>();
        if( parameters.get( "directorY" ) != null ) {
            for( String dir : parameters.getMany( "directory" )) {
                directories.add( dir );
            }
        }
        if( parameters.get( "directories" ) != null ) {
            for( String dir : parameters.get( "directories" ).split( "[:;]+" )) {
                directories.add( dir );
            }
        }

        Map<ModuleRequirement,Map<String,String>> rawModuleSettings = new HashMap<>();

        String config = parameters.get( "config" );
        if( config != null ) {
            Properties configProps = new Properties();

            try ( FileInputStream configStream = new FileInputStream( config ) ) {
                configProps.load(configStream);

            } catch( IOException ex ) {
                fatal( "Cannot read config file: " + config );
            }

            if( configProps.containsKey( "diet4j!directory" )) {
                if( !directories.isEmpty() ) {
                    fatal( "Specified both as argument and in config file: directory" );
                }
                for( String dir : configProps.getProperty( "diet4j!directory" ).split( "[,\\s]+" )) {
                    directories.add( dir );
                }
            }
            if( configProps.containsKey( "diet4j!directories" )) {
                if( !directories.isEmpty() ) {
                    fatal( "Specified both as argument and in config file: directories" );
                }
                for( String dir : configProps.getProperty( "diet4j!directories" ).split( "[:;]+" )) {
                    directories.add( dir );
                }
            }
            if( configProps.containsKey( "diet4j!module" )) {
                if( moduleNames != null ) {
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
                if( theRunArguments != null ) {
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
                        ModuleRequirement req1 = ModuleRequirement.create( n1 );
                        Map<String,String> forThisModule = rawModuleSettings.get( req1 );
                        if( forThisModule == null ) {
                            forThisModule = new HashMap<>();
                            rawModuleSettings.put( req1, forThisModule );
                        }
                        forThisModule.put( n2, configProps.getProperty( realKey ));
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
                if( !fileDirs.add( new File( dir ).getCanonicalFile() )) {
                    fatal( "Directory (indirectly?) specified more than once in moduledirectory parameter: " + dir );
                }
            }
        }
        theDirectories = new File[ fileDirs.size() ];
        fileDirs.toArray(theDirectories );

        if( theRunArguments == null ) {
            theRunArguments = new String[0];
        }

        Map<ModuleRequirement,ModuleSettings> moduleSettings = new HashMap<>();
        for( Map.Entry<ModuleRequirement,Map<String,String>> e : rawModuleSettings.entrySet() ) {
            if( !e.getValue().isEmpty() ) {
                moduleSettings.put( e.getKey(), ModuleSettings.create( e.getValue() ));
            }
        }

        theRegistry = ScanningDirectoriesModuleRegistry.create( theDirectories, moduleSettings );
    }

    /**
     * Execute.
     *
     * @return desired system exit code
     */
    static int activateRunDeactivate()
    {

        // find and resolve modules
        ModuleMeta [] theModuleMetas = new ModuleMeta[ theModuleRequirements.length ];
        Module []     theModules     = new Module[     theModuleRequirements.length ];

        int ret = 0;
        try {
            for( int i=0 ; i<theModuleMetas.length ; ++i ) {
                try {
                    theModuleMetas[i] = theRegistry.determineSingleResolutionCandidate( theModuleRequirements[i] );

                } catch( Throwable ex ) {
                    fatal( "Cannot uniquely find module " + theModuleRequirements[i] + " in registry " + theRegistry );
                }
            }

            for( int i=0 ; i<theModuleMetas.length ; ++i ) {
                try {
                    theModules[i] = theRegistry.resolve( theModuleMetas[i] );
                    theModules[i].activateRecursively();

                } catch( Throwable ex ) {
                    log.log( Level.SEVERE, "Activation of module " + theModuleMetas[i] + " failed", ex );

                    ret = 1;
                    break;
                }
            }

            if( ret == 0 ) {
                try {
                    if( theRunClassName != null || theModules[0].getModuleMeta().getRunClassName() != null ) {
                        ret = theModules[0].run( theRunClassName, theRunMethodName, theRunArguments );
                    }

                } catch( Throwable ex ) {
                    log.log( Level.SEVERE, "Run of module " + theModules[0].getModuleMeta() + " failed", ex );
                    ret = 1;

                }
            }

        } finally {
            for( int i=theModules.length-1 ; i>=0 ; --i ) {
                if( theModules[i] == null ) {
                    continue;
                }
                try {
                    theModules[i].deactivateRecursively();

                } catch( Throwable ex ) {
                    log.log( Level.SEVERE, "Deactivation of module " + theModules[i].getModuleMeta() + " failed", ex );

                    ret = 1;
                }
            }
        }
        return ret;
    }

    /**
     * Print help text and quit.
     */
    public static void helpAndQuit()
    {
        PrintStream w = System.out;

        w.println( "Synopsis: (diet4j-core " + Version.VERSION + ", built " + Version.BUILDTIME + ")" );
        w.println( "[ --directory <directory> ]... [ --directories <directories> ]... [ --runclass <class> ][ --runmethod <method> ] <module> [<module> ...] [ -- <runarg> ... ] " );
        w.println( "    where:" );
        w.println( "        <directory>:   directory in which to look for modules" );
        w.println( "        <directories>: colon or semicolon-separated list of directories in which to look for modules" );
        w.println( "        <runclass>:    name of a non-default class whose main() method to run, instead of the rootmodule's" );
        w.println( "        <runmethod>:   name of a method in the run class to run, instead of main()" );
        w.println( "        <module>:      name of the module(s) to activate, given as groupId:artifactId:version or groupId:artifactId" );
        w.println( "                       The first specified module is the module that is run unless <runclass> is given" );
        w.println( "        <runarg> ...:  argument(s) to the main() method of the run class" );
        w.println( "--config <configfile>" );
        w.println( "    where:" );
        w.println( "        <configfile>: name of a properties file containing the above values as properties" );
        w.println( "    names of the properties:" );
        w.println( "        diet4j!directory:   comma or space-separated list of directories in which to look for modules" );
        w.println( "        diet4j!directories: colon or semicolon-separated list of directories in which to look for modules" );
        w.println( "        diet4j!runclass:    name of a non-default class whose main() method to run, instead of the rootmodule's" );
        w.println( "        diet4j!runmethod:   name of a method in the run class to run, instead of main()" );
        w.println( "        diet4j!module:      comma or space-separated list of module(s) to activate, given as groupId:artifactId:version or groupId:artifactId" );
        w.println( "                            The first specified module is the module that is run unless <runclass> is given" );
        w.println( "        diet4j!runarg:      comma or space-separated argument(s) to the main() method of the run class" );
        w.println( "--help:" );
        w.println( "    this message" );
        w.flush();
        System.exit( 0 );
    }

    /**
     * Print fatal error and quit.
     *
     * @param msg the error message
     */
    public static void fatal(
            String msg )
    {
        log.log( Level.SEVERE, msg );
        System.exit( 1 );
    }

    /**
     * Print fatal error and quit.
     *
     * @param msg the error message
     * @param cause what happened
     */
    public static void fatal(
            String    msg,
            Throwable cause )
    {
        log.log( Level.SEVERE, msg, cause );
        System.exit( 1 );
    }

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

    /**
     * The ModuleRegistry
     */
    protected static ModuleRegistry theRegistry;

    /**
     * Logger.
     */
    private static final Logger log = Logger.getLogger( CmdlineBootLoader.class.getName() );
}
