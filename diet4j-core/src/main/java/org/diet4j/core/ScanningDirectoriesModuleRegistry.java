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
     * Factory method.
     * 
     * @param dirs the directories to scan
     * @return the created ScanningDirectoriesModuleRegistry
     */
    public static ScanningDirectoriesModuleRegistry create(
            String [] dirs )
    {
        if( dirs == null || dirs.length == 0 ) {
            dirs = DEFAULT_MODULE_DIRECTORIES;
        }

        File []       fileDirs = new File[ dirs.length ];
        List<JarFile> jars     = new ArrayList<>();
        for( int i=0 ; i<dirs.length ; ++i ) {
            fileDirs[i] = new File( dirs[i] );

            if( !fileDirs[i].exists() ) {
                // silently ignore
                continue;
            }
            if( !fileDirs[i].isDirectory() ) {
                throw new IllegalArgumentException( "Not a directory: " + fileDirs[i].getAbsolutePath() );
            }
            
            try {
                List<JarFile> newJars = Files.walk( fileDirs[i].toPath() )
                        .filter( ( Path f ) -> {
                                String name = f.getFileName().toString();
                                return name.endsWith( ".jar" ) || name.endsWith( ".war" ); } )
                        .filter( f -> Files.isRegularFile( f, LinkOption.NOFOLLOW_LINKS ))
                        .map( (Path f ) -> {
                                try {
                                    return new JarFile( f.toFile() );
                                } catch( IOException ex ) {
                                    log.log( Level.SEVERE, "Cannot access " + f.toString(), ex );
                                    return null;
                                }} )
                        .collect( Collectors.toList() );
                jars.addAll( newJars );

            } catch( IOException ex ) {
                log.log( Level.SEVERE, "I/O Error", ex );
            }        
        }

        HashMap<String,ModuleMeta []> metas = new HashMap<>();
        addParsedModuleMetasFromJars( jars, metas );

        ScanningDirectoriesModuleRegistry ret = new ScanningDirectoriesModuleRegistry( fileDirs, metas );
        return ret;
    }

    /**
     * Private constructor, use factory method.
     *
     * @param dirs the directories that were scanned
     * @param metas the ModuleMetas found during boot, keyed by their name, and then ordered by version
     */
    protected ScanningDirectoriesModuleRegistry(
            File []                      dirs,
            HashMap<String,ModuleMeta[]> metas )
    {
        super( metas );

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
    public static final String [] DEFAULT_MODULE_DIRECTORIES;
    static {
        if( System.getProperty( "os.name" ).contains( "Windows" )) {
            DEFAULT_MODULE_DIRECTORIES = new String [] {
                System.getProperty( "user.home" ) + "\\.m2"
            };
        } else {
            DEFAULT_MODULE_DIRECTORIES = new String [] {
                "/usr/lib/java",
                System.getProperty( "user.home" ) + "/.m2"
            };
        }
    }

    /**
     * Logger.
     */
    private static final Logger log = Logger.getLogger(ScanningDirectoriesModuleRegistry.class.getName() );    
}
