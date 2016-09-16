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

package org.diet4j.runjunit;

import java.io.File;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.diet4j.core.Module;
import org.diet4j.core.ModuleActivationException;
import org.diet4j.core.ModuleClassLoader;
import org.diet4j.core.ModuleException;
import org.diet4j.core.ModuleRegistry;
import org.diet4j.core.ModuleRequirement;
import org.junit.internal.Classes;
import org.junit.internal.TextListener;
import org.junit.runner.Computer;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;

/**
 * Main program.
 */
public class Main
{
    /**
     * Main method.
     * 
     * @param args command-line arguments
     * @return desired exit code
     * @throws ModuleException module resolution failed
     */
    public static int main(
            String [] args )
        throws
            ModuleException,
            ParseException,
            MalformedURLException,
            ClassNotFoundException
    {
    // parse command-line options

        String       cp          = null; // classpath
        Set<String>  moduleNames = new HashSet<>();
        List<String> classNames  = new ArrayList<>(); // allow repeats
        
        try {
            for( int i=0 ; i<args.length ; ++i ) {
                if( args[i].startsWith( "-" )) {
                    String flag = args[i].substring( 1 );
                    if( flag.startsWith( "-" )) {
                        flag = flag.substring( 1 );
                    }
                    switch( flag ) {
                        case "cp":
                        case "classpath":
                            if( cp != null ) {
                                return synopsis();
                            } else {
                                cp = args[++i];
                            }
                            break;
                        case "m":
                        case "module":
                            if( !moduleNames.add( args[++i] )) {
                                return synopsis();
                            }
                            break;
                        case "h":
                        case "help":
                            return synopsis();
                        default:
                            return synopsis();
                    }
                } else {
                    classNames.add( args[i] );
                }
            }
        } catch( ArrayIndexOutOfBoundsException ex ) {
            return synopsis();
        }
        if( moduleNames.isEmpty() ) {
            return synopsis();
        }

    // set up test classloader
    
        if( cp == null ) {
            cp = ".";
        }
        
        Set<File> classLoaderDirs = new HashSet<>();
        for( String s : cp.split( File.pathSeparator )) {
            classLoaderDirs.add( new File( s ));
        }
        File [] classLoaderDirArray = new File[ classLoaderDirs.size() ];
        classLoaderDirs.toArray( classLoaderDirArray );

    // activate modules

        Module []            modules                = new Module[ moduleNames.size() ];
        ModuleClassLoader [] dependencyClassLoaders = new ModuleClassLoader[ moduleNames.size() ];
        int count=0;
        for( String moduleName : moduleNames ) {
            modules[count] = theRegistry.resolve( theRegistry.determineSingleResolutionCandidate( ModuleRequirement.parse( moduleName )));
            modules[count].activateRecursively();
            dependencyClassLoaders[count] = (ModuleClassLoader) modules[count].getClassLoader();
            ++count;
        }
        
    // We define an ad-hoc "module" that depends on all the other modules
    // This automatically solves our Class loading problem.
    
        ToplevelClassLoader topClassLoader = new ToplevelClassLoader(
                classLoaderDirArray,
                Main.class.getClassLoader(),
                dependencyClassLoaders,
                new String[0] );

    // resolve the test classes
        Class<?> [] testClasses = new Class[ classNames.size() ];
        for( int i=0 ; i<testClasses.length ; ++i ) {
            testClasses[i] = topClassLoader.loadClass( classNames.get( i ));
        }
    // invoke JUnit
    
        RunNotifier notifier = new RunNotifier();
        Result      result   = new Result();

        notifier.addFirstListener( result.createListener() );

        RunListener listener = new TextListener( System.out );
        notifier.addListener(listener);

        Request request = Request.classes(
                new Computer(),
                testClasses );

        Runner runner = request.getRunner();
        try {
            notifier.fireTestRunStarted(runner.getDescription());
            runner.run(notifier);
            notifier.fireTestRunFinished( result );
        } finally {
            notifier.removeListener(listener);
        }

        return result.wasSuccessful() ? 0 : 1;
    } 

    /**
     * Print the synopsis.
     * 
     * @return exit code
     */
    protected static int synopsis()
    {
        System.out.println( "Synopsis:" );
        System.out.println( "    [ --cp <classpath> ]      : classpath for the test classes" );
        System.out.println( "    [ --module <module> ] ... : one or more modules to activate" );
        System.out.println( "    <testclass> ...           : one or more jUnit test classes to run" );
        
        return 1;
    }

    /**
     * Diet4j module activation.
     * 
     * @param thisModule the Module being activated
     * @throws ModuleActivationException thrown if module activation failed
     */
    public static void moduleActivate(
            Module thisModule )
        throws
            ModuleActivationException
    {
        theRegistry = thisModule.getModuleRegistry();
    }
    
    /**
     * The ModuleRegistry.
     */
    protected static ModuleRegistry theRegistry;
}
