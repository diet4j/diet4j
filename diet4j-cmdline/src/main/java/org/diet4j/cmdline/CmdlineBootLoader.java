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
import java.io.IOException;
import java.io.PrintStream;
import java.text.ParseException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.diet4j.core.Module;
import org.diet4j.core.ModuleMeta;
import org.diet4j.core.ModuleRegistry;
import org.diet4j.core.ModuleRequirement;
import org.diet4j.core.ScanningDirectoriesModuleRegistry;

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
            log.log( Level.SEVERE, "Something bad happened", ex  );
            System.exit( 1 );
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
        HashMap<File,String> dirs = new HashMap<>();

        int i;
        String flag = null;
        for( i=0; i < args.length; ++i ) {
            String arg = args[i];
            if( flag == null ) {
                if( arg.startsWith( "-" )) {
                    if( arg.startsWith( "-- ")) {
                        flag = arg.substring( 2 );
                    } else {
                        flag = arg.substring( 1 );
                    }
                } else {
                    // root module
                    try {
                        theRootModuleRequirement = ModuleRequirement.parse( arg );
                    } catch( ParseException ex ) {
                        helpAndQuit();
                    }
                    ++i; // so we are on the first argument
                    break; // while loop
                }
            } else {
                switch( flag ) {
                    case "d":
                    case "directory":
                        File dir = new File( arg ).getCanonicalFile();
                        if( dirs.put( dir, arg ) != null ) {
                            helpAndQuit();
                        }
                        break;
                    case "r":
                    case "run":
                        if( theRunClassName != null ) {
                            helpAndQuit();
                        }
                        theRunClassName = arg;
                        break;
                    case "m":
                    case "method":
                        if( theRunMethodName != null ) {
                            helpAndQuit();
                        }
                        theRunMethodName = arg;
                        break;
                    default:
                        helpAndQuit();
                        break;
                }
            }            
        }
        if( flag != null ) {
            helpAndQuit();            
        }
        if( theRootModuleRequirement == null ) {
            helpAndQuit();
        }

        // the remaining arguments
        theRunArguments = new String[ args.length - i ];
        if( theRunArguments.length > 0 ) {
            System.arraycopy( args, i, theRunArguments, 0, args.length-1 );
        }
    }

    /**
     * Execute.
     * 
     * @return desired system exit code
     */
    static int activateRunDeactivate()
    {
        // create ModuleRegistry
        ModuleRegistry registry = ScanningDirectoriesModuleRegistry.create( theModuleDirectories );

        // find and resolve the main module
        ModuleMeta rootModuleMeta = null;
        try {
            rootModuleMeta = registry.determineSingleResolutionCandidate( theRootModuleRequirement );

        } catch( Throwable ex ) {
            log.log( Level.SEVERE, "Cannot find module " + theRootModuleRequirement, ex );
            return 1;
        }

        Module rootModule;
        int ret;
        try {
            rootModule = registry.resolve( rootModuleMeta );

            rootModule.activateRecursively();
            
            ret = 0;

        } catch( Throwable ex ) {
            log.log( Level.SEVERE, "Activation of module " + rootModuleMeta + " failed", ex );
            
            rootModule = null;
            ret = 1;
        }

        if( rootModule != null ) {
            try {
                ret = rootModule.run( theRunClassName, theRunMethodName, theRunArguments );

            } catch( Throwable ex ) {
                log.log( Level.SEVERE, "Run of module " + rootModuleMeta + " failed", ex );
                ret = 1;

            } finally {
                if( rootModule != null ) {
                    try {
                        rootModule.deactivateRecursively();
                    } catch( Throwable ex ) {
                        log.log( Level.SEVERE, "Dectivation of module " + rootModuleMeta + " failed", ex );

                        ret = 1;
                    }
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
        
        w.println( "Synopsis:" );
        w.println( "[ --directory <directory> ]... [ --run <class> ][ --method <method> ] <rootmodule> [ <arg> ... ] " );
        w.println( "    where:" );
        w.println( "       <directory>:  directory in which to look for modules" );
        w.println( "       <class>:      name of a non-default class whose main() method to run" );
        w.println( "       <method>:     name of a method in the run class to run, instead of main()" );
        w.println( "       <rootmodule>: name of the root module to activate, given as groupId:artifactId:version or groupId:artifactId" );
        w.println( "       <arg> ...:    argument(s) to the main() method of the run class" );
        w.println( "--help: this message" );
        w.flush();
        System.exit( 0 );
    }

    /**
     * The paths to the Module JAR files.
     */
    protected static String [] theModuleDirectories;

    /**
     * The name of the run class in the root Module, if specified on the command-line.
     */
    protected static String theRunClassName;

    /**
     * The name of the run method in the run class, if specified on the command-line.
     */
    protected static String theRunMethodName;

    /**
     * The ModuleRequirement for the root Module to start.
     */
    protected static ModuleRequirement theRootModuleRequirement;

    /**
     * The arguments to the run
     */
    protected static String [] theRunArguments;

    /**
     * Logger.
     */
    private static final Logger log = Logger.getLogger(CmdlineBootLoader.class.getName() );
}