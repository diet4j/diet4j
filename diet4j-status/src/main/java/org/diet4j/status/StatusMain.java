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

package org.diet4j.status;

import java.io.PrintStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.diet4j.core.Module;
import org.diet4j.core.ModuleClassLoader;
import org.diet4j.core.ModuleMeta;
import org.diet4j.core.ModuleNotFoundException;
import org.diet4j.core.ModuleRegistry;
import org.diet4j.core.ModuleRequirement;
import org.diet4j.core.ModuleResolutionException;

/**
 * Print out a variety of status information about modules available to diet4j.
 */
public class StatusMain
{
    /**
     * Main program, must be invoked by diet4j framework.
     *
     * @param args command-line arguments
     */
    public static void main(
            String [] args )
    {
        String flag = null;
        HashMap<String,String> flags = new HashMap<>();

        final String NOARG = "";

        for( String arg : args ) {
            if( arg.startsWith( "-" )) {
                if( flag != null ) {
                    flags.put( flag, NOARG );
                }
                if( arg.startsWith( "--")) {
                    flag = arg.substring( 2 );
                } else {
                    flag = arg.substring( 1 );
                }
            } else {
                if( flag != null ) {
                    flags.put( flag, arg );
                    flag = null;
                } else {
                    synopsis();
                    return;
                }
            }
        }
        if( flag != null ) {
            flags.put( flag, NOARG );
        }

        if( flags.remove( "h" ) != null || flags.remove( "help" ) != null ) {
            synopsis();
            return;
        }
        if( flags.remove( "s" ) != null || flags.remove( "showmoduleregistry" ) != null ) {
            try {
                if( flags.remove( "recursive" ) != null || flags.remove( "r" ) != null ) {
                    showHierarchicalModuleRegistry();
                } else {
                    showFlatModuleRegistry();
                }
            } catch( ModuleNotFoundException | ParseException ex ) {

                StringBuilder msg = new StringBuilder();
                msg.append( ex.getLocalizedMessage() );

                Throwable cause = ex.getCause();
                while( cause != null ) {
                    msg.append( "\n    caused by: " ).append( cause.getLocalizedMessage() );
                    cause = cause.getCause();
                }
                log.severe( msg.toString() );
            }
            return;
        }
        String module = flags.remove( "m" );
        if( module == null ) {
            module = flags.remove( "module" );
        }

        if( module == (Object) NOARG ) { // get rid of silly IDE warning
            synopsis();
            return;

        } else if( module != null ) {
            try {
                showModule(
                        module,
                        flags.remove( "recursive" ) != null || flags.remove( "r" ) != null,
                        flags.remove( "long"   )    != null,
                        flags.remove( "verbose" )   != null || flags.remove( "v"   ) != null );

            } catch( ModuleNotFoundException|ModuleResolutionException|ParseException ex ) {

                StringBuilder msg = new StringBuilder();
                msg.append( ex.getLocalizedMessage() );

                Throwable cause = ex.getCause();
                while( cause != null ) {
                    msg.append( "\n    caused by: " ).append( cause.getLocalizedMessage() );
                    cause = cause.getCause();
                }
                log.severe( msg.toString() );
            }
            return;
        }
        synopsis();
    }

    /**
     * Find the ModuleRegistry.
     *
     * @return the ModuleRegistry
     */
    static ModuleRegistry findRegistry()
    {
        ClassLoader loader = StatusMain.class.getClassLoader();
        if( loader instanceof ModuleClassLoader ) {
            return ((ModuleClassLoader)loader).getModuleRegistry();
        } else {
            throw new RuntimeException( "Cannot determine ModuleClassLoader. Are you running this using diet4j?" );
        }
    }

    /**
     * Show the content of the Module Registry by printing a flat list of all Modules.
     */
    static void showFlatModuleRegistry()
    {
        ModuleRegistry registry = findRegistry();
        Set<String>    names    = registry.nameSet();
        PrintStream    out      = System.out;

        ArrayList<String> sortedNames = new ArrayList<>( names );
        Collections.sort( sortedNames );

        out.println( registry.toString() );
        for( String name : sortedNames ) {
            out.print( name );
            out.print( ":" );

            try {
                ModuleMeta [] versions = registry.determineResolutionCandidates( ModuleRequirement.parse( name ) );
                for( ModuleMeta meta : versions ) {
                    String version = meta.getModuleVersion();
                    out.print( " " );
                    if( version != null ) {
                        out.print( version );
                    } else {
                        out.print( "<?>" );
                    }
                    out.print( " (" );
                    out.print( meta.toString() );
                    out.print( ")" );
                }
            } catch( ParseException ex ) {
                log.severe( "Cannot parse into ModuleRequirement: " + name );
            }
            out.print( "\n" );
        }
    }

    /**
     * Show the content of the Module Registry by printing a dependency tree of all Modules.
     *
     * @throws ModuleNotFoundException thrown if a needed Module could not be found
     * @throws ParseException thrown if the provided Module name was invalid
     */
    static void showHierarchicalModuleRegistry()
        throws
            ModuleNotFoundException,
            ParseException
    {
        ModuleRegistry registry = findRegistry();
        Set<String>    names    = registry.nameSet();
        PrintStream    out      = System.out;

        List<Module>            allModules = new ArrayList<>();
        HashMap<Module,Boolean> usageMap   = new HashMap<>();

        // create usage map
        for( String name : names ) {
            ModuleRequirement req = ModuleRequirement.parse( name );
            ModuleMeta [] moduleMetas = registry.determineResolutionCandidates( req );
            if( moduleMetas.length == 0  ) {
                throw new RuntimeException( "Cannot find module " + req + " in registry " + registry );
            }
            if( moduleMetas.length > 1 ) {
                StringBuilder msg = new StringBuilder();
                msg.append( "More than one module found:" );
                for( ModuleMeta meta : moduleMetas ) {
                    msg.append( "\n    " );
                    msg.append( meta.toString() );
                }
                throw new RuntimeException( msg.toString() );
            }

            try {
                Module   module       = registry.resolve( moduleMetas[0], true );
                Module[] dependencies = registry.determineRuntimeDependencies( module );

                allModules.add( module );

                if( dependencies != null ) {
                    for( Module dep : dependencies ) {
                        if( dep != null ) {
                            usageMap.put( dep, Boolean.TRUE );
                        }
                    }
                }
            } catch( ModuleResolutionException ex ) {
                log.severe( "Cannot resolve recursively " + moduleMetas[0] + ": " + ex.getMessage() );
            }
        }

        Module [] topModules = allModules.stream().filter( (Module m) -> usageMap.get( m ) == null ).toArray( Module[]::new );

        out.println( "# Note: resolved run-time dependencies only" );
        Set<Module> haveAlready = new HashSet<>();

        showModulesInHierarchy( topModules, 0, haveAlready, registry, out );
    }

    /**
     * Helper to show an unordered array of Modules as part of a hierarchy.
     *
     * @param ms the Modules to show on this level of the hierarchy
     * @param indent how many levels of indent
     * @param haveAlready keep track of modules we already have displayed
     * @param registry the ModuleRegistry
     * @param out the stream to write to
     */
    static void showModulesInHierarchy(
            Module []      ms,
            int            indent,
            Set<Module>    haveAlready,
            ModuleRegistry registry,
            PrintStream    out )
    {
        Arrays.sort( ms, (Module a, Module b) ->
                ( a == null || b == null )
                ? 0
                : a.getModuleMeta().toString().compareTo( b.getModuleMeta().toString() ));

        for( Module m : ms ) {
            if( m == null ) {
                continue;
            }
            for( int i=0 ; i<indent ; ++i ) {
                out.print( "    " );
            }
            out.print( m.getModuleMeta().toString() );

            if( haveAlready != null && haveAlready.contains( m )) {
                out.println(  " -- repeated, see above" );

            } else {
                haveAlready.add( m );

                out.println( ":" );

                Module[] dependencies = registry.determineRuntimeDependencies( m );
                showModulesInHierarchy( dependencies, indent + 1, haveAlready, registry, out );
            }
        }
    }

    /**
     * Show a particular Module.
     *
     * @param name the name of the Module
     * @param recursive if true, also recursively show all dependencies
     * @param loong if true, show the entire tree, do not attempt to shorten
     * @param verbose if true, show more detail
     *
     * @throws ModuleNotFoundException thrown if a needed Module could not be found
     * @throws ModuleResolutionException thrown if a needed Module could not be resolved
     * @throws ParseException thrown if the provided Module name was invalid
     */
    static void showModule(
            String  name,
            boolean recursive,
            boolean loong,
            boolean verbose )
        throws
            ModuleNotFoundException,
            ModuleResolutionException,
            ParseException
    {
        ModuleRegistry registry = findRegistry();

        // find and resolve the main module
        ModuleRequirement req         = ModuleRequirement.parse( name );
        ModuleMeta []     moduleMetas = registry.determineResolutionCandidates( req );

        if( moduleMetas.length == 0  ) {
            throw new RuntimeException( "Cannot find module " + req + " in registry " + registry );
        }
        if( moduleMetas.length > 1 ) {
            StringBuilder msg = new StringBuilder();
            msg.append( "More than one module found:" );
            for( ModuleMeta meta : moduleMetas ) {
                msg.append( "\n    " );
                msg.append( meta.toString() );
            }
            throw new RuntimeException( msg.toString() );
        }

        Module module = registry.resolve( moduleMetas[0], recursive );

        HashSet<Module> haveAlready = loong ? null : new HashSet<>();

        showModule( req, module, 0, haveAlready, recursive, verbose, System.out );
    }

    /**
     * Recursive helper method to dump a Module hierarchy.
     *
     * @param req the ModuleRequirements that lead to the Module
     * @param mod the Module or null if you could not be resolved
     * @param indent how many levels of indent
     * @param haveAlready keep track of modules we already have displayed
     * @param recursive if true, show dependent modules as well
     * @param verbose if true, show more detail
     * @param out the stream to print to
     */
     static void showModule(
            ModuleRequirement req,
            Module            mod,
            int               indent,
            Set<Module>       haveAlready,
            boolean           recursive,
            boolean           verbose,
            PrintStream       out )
    {
        for( int i=0 ; i<indent ; ++i ) {
            out.print( "    " );
        }
        if( mod != null ) {
            out.print( mod.toString() );
            if( verbose ) {
                out.print( " (" );
                out.print( mod.getModuleMeta().getProvidesJar().getName());
                if( req.isOptional() ) {
                    out.println( " optional" );
                }
                out.print( ")" );
            }
            if( haveAlready != null && haveAlready.contains( mod )) {
                out.println(  " -- repeated, see above" );
                return;
            }
            out.println();

            if( haveAlready != null ) {
                haveAlready.add( mod );
            }

            if( recursive ) {
                ModuleRequirement [] reqs = mod.getModuleMeta().getRuntimeModuleRequirements();
                Module []            deps = mod.determineRuntimeDependencies();

                for( int i=0 ; i<reqs.length ; ++i ) {
                    showModule( reqs[i], deps[i], indent+1, haveAlready, recursive, verbose, out );
                }
            } else if( verbose ) {
                ModuleRequirement [] dependencyReqs = mod.getModuleMeta().getRuntimeModuleRequirements();
                for( ModuleRequirement dependencyReq : dependencyReqs ) {
                    for( int j = 0; j <= indent; ++j ) {
                        out.print( "    " );
                    }
                    out.print( dependencyReq );
                    out.print( " (" );
                    if( dependencyReq.isOptional() ) {
                        out.println( "optional, " );
                    }
                    if( mod.getModuleRegistry().determineResolutionCandidates( dependencyReq ).length > 0 ) {
                        out.print( "resolvable" );
                    } else {
                        out.print( "unresolvable" );
                    }
                    out.println( ")" );
                }
            }
        } else if( req.isOptional() ) {
            // optional dependency, not resolved
            out.println( "<optional not resolved: " + req.toString() + ">" );
        } else {
            // dependency, not resolved
            out.println( "<NOT RESOLVED: " + req.toString() + ">" );
        }
    }

    /**
     * Print the synopsis for this app.
     */
    public static void synopsis()
    {
        System.out.println( "Synopsis:" );
        System.out.println( "    --module <module> [--recursive [--long]][--verbose] display information about the named Module" );
        System.out.println( "    --showmoduleregistry [--recursive]                  show all known Modules" );
        System.out.println( "    --help                                              this message" );
    }

    /**
     * Logger.
     */
    private static final Logger log = Logger.getLogger( StatusMain.class.getName() );
}
