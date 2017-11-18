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

package org.diet4j.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Upon instantiation, recursively scans the provided directory to look for available Modules.
 * Keeps list of available Modules in memory and does not rescan.
 */
public class ScanningDirectoriesModuleRegistry
    extends
        AbstractScanningModuleRegistry
{
    /**
     * Factory method. Delegate the default set of classes to the system class loader.
     *
     * @param dirs the directories to scan
     * @param moduleSettings the settings for the modules
     * @return the created ScanningDirectoriesModuleRegistry
     */
    public static ScanningDirectoriesModuleRegistry create(
            File []                               dirs,
            Map<ModuleRequirement,ModuleSettings> moduleSettings )
    {
        return create( dirs, moduleSettings, AbstractModuleRegistry.DEFAULT_DO_NOT_LOAD_CLASS_PREFIXES );
    }

    /**
     * Factory method.
     *
     * @param dirs the directories to scan
     * @param moduleSettings the settings for the modules
     * @param doNotLoadClassPrefixes prefixes of classes always to be loaded through the system class loader, not this one
     * @return the created ScanningDirectoriesModuleRegistry
     */
    public static ScanningDirectoriesModuleRegistry create(
            File []                               dirs,
            Map<ModuleRequirement,ModuleSettings> moduleSettings,
            String []                             doNotLoadClassPrefixes )
    {
        if( dirs == null || dirs.length == 0 ) {
            dirs = DEFAULT_MODULE_DIRECTORIES;
        }

        List<JarFile> jars     = new ArrayList<>();
        for( int i=0 ; i<dirs.length ; ++i ) {

            if( !dirs[i].exists() ) {
                // silently ignore
                continue;
            }
            if( !dirs[i].isDirectory() ) {
                throw new IllegalArgumentException( "Not a directory: " + dirs[i].getAbsolutePath() );
            }

            try {
                List<JarFile> newJars = Files.walk( dirs[i].toPath() )
                        .filter( ( Path f ) -> {
                                String name = f.getFileName().toString();
                                return name.endsWith( ".jar" ) || name.endsWith( ".war" ); } )
                        .filter( f -> Files.isRegularFile( f, LinkOption.NOFOLLOW_LINKS ))
                        .map( (Path f ) -> {
                                try {
                                    return new JarFile( f.toFile() );
                                } catch( IOException ex ) {
                                    log.log( Level.SEVERE, "Cannot access {0}: {1}", new Object[]{ f.toString(), ex.getLocalizedMessage() });
                                    return null;
                                }} )
                        .filter( f-> ( f != null )) // happens if previous step returned null
                        .collect( Collectors.toList() );
                jars.addAll( newJars );

            } catch( IOException ex ) {
                log.log( Level.SEVERE, "I/O Error", ex );
            }
        }

        HashMap<String,MiniModuleMetaMap> metas = new HashMap<>();
        addParsedModuleMetasFromJars( jars, metas );

        ScanningDirectoriesModuleRegistry ret = new ScanningDirectoriesModuleRegistry(
                dirs,
                metas,
                moduleSettings,
                doNotLoadClassPrefixes );
        return ret;
    }

    /**
     * Private constructor, use factory method.
     *
     * @param dirs the directories that were scanned
     * @param metas the ModuleMetas found during boot, keyed by their name, and then ordered by version
     * @param moduleSettings the settings for the modules
     * @param doNotLoadClassPrefixes prefixes of classes always to be loaded through the system class loader, not this one
     */
    protected ScanningDirectoriesModuleRegistry(
            File []                               dirs,
            HashMap<String,MiniModuleMetaMap>     metas,
            Map<ModuleRequirement,ModuleSettings> moduleSettings,
            String []                             doNotLoadClassPrefixes )
    {
        super( metas, moduleSettings, doNotLoadClassPrefixes );

        theDirectories = dirs;
    }

    /**
     * Determine the directories that were scanned.
     *
     * @return the directories
     */
    public File [] getScannedDirectories()
    {
        return theDirectories;
    }

    /**
     * Obtain String representation.
     *
     * @return String representation
     */
    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append( getClass().getName() );
        buf.append( " (" );
        buf.append( nameSet().size());
        buf.append( " known modules, dirs:" );
        for( File d : theDirectories ) {
            buf.append( ' ' );
            buf.append( d.getPath());
        }
        buf.append( ')' );
        return buf.toString();
    }

    /**
     * The directories that were scanned.
     */
    protected File [] theDirectories;

    /**
     * The directories scanned by default if none are given as parameters.
     */
    public static final File [] DEFAULT_MODULE_DIRECTORIES;
    static {
        DEFAULT_MODULE_DIRECTORIES = new File [] {
            new File( System.getProperty( "user.home" ) + File.separatorChar + ".m2" )
        };
    }

    /**
     * Logger.
     */
    private static final Logger log = Logger.getLogger( ScanningDirectoriesModuleRegistry.class.getName() );
}
