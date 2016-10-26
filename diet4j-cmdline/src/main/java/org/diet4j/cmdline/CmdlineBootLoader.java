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
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.diet4j.cmdline.CmdlineParameters.Parameter;
import org.diet4j.core.Module;
import org.diet4j.core.ModuleMeta;
import org.diet4j.core.ModuleRegistry;
import org.diet4j.core.ModuleRequirement;
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
            fatal( "Something bad happened:" + ( ex.getMessage() != null ? ex.getMessage() : ex.toString() ));
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
            new Parameter( "help",      0 ),
            new Parameter( "directory", 1, true ),
            new Parameter( "run",       1 ),
            new Parameter( "method",    1 )
        );

        String [] remaining = parameters.parse( args );

        if( parameters.containsKey( "help" )) {
            helpAndQuit();
        }

        if( remaining.length < 1 ) {
            fatal( "Must provide the name of one root module" );
        }
        try {
            theRootModuleRequirement = ModuleRequirement.parse( remaining[0] );
        } catch( ParseException ex ) {
            fatal( ex.getLocalizedMessage() );
        }

        theRunArguments = new String[ remaining.length-1 ];
        System.arraycopy( remaining, 1, theRunArguments, 0, theRunArguments.length );

        String [] dirs = parameters.getMany( "directory" );
        HashSet<File> fileDirs = new HashSet<>();
        if( dirs != null ) {
            for( String dir : dirs ) {
                if( !fileDirs.add( new File( dir ).getCanonicalFile() )) {
                    fatal( "Directory (indirectly?) specified more than once in moduledirectory parameter: " + dir );
                }
            }
        }
        theModuleDirectories = new File[ fileDirs.size() ];
        fileDirs.toArray( theModuleDirectories );

        theRunClassName  = parameters.get( "run" );
        theRunMethodName = parameters.get( "method" );
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
        ModuleMeta rootModuleMeta;
        try {
            rootModuleMeta = registry.determineSingleResolutionCandidate( theRootModuleRequirement );

        } catch( Throwable ex ) {
            log.log( Level.SEVERE, "Cannot find module " + theRootModuleRequirement );
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
                try {
                    rootModule.deactivateRecursively();
                } catch( Throwable ex ) {
                    log.log( Level.SEVERE, "Deactivation of module " + rootModuleMeta + " failed", ex );

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
     * The paths to the Module JAR files.
     */
    protected static File [] theModuleDirectories;

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
     * 
    */
    /**
     * Logger.
     */
    private static final Logger log = Logger.getLogger(CmdlineBootLoader.class.getName() );
}