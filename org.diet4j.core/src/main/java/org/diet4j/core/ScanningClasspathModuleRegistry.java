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
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.jar.JarFile;
import org.diet4j.core.AbstractModuleRegistry;
import org.diet4j.core.Module;
import org.diet4j.core.ModuleMeta;

/**
 * A (mock) ModuleRegistry that can be used when all Modules are on the class path,
 * but they still need to be activated/deactivated.
 */
public class ScanningClasspathModuleRegistry
   extends
        AbstractScanningModuleRegistry
{
    /**
     * Obtain the singleton instance of this class.
     * 
     * @return the singleton instance
     * @throws IOException reading files failed
     */
    public static synchronized ScanningClasspathModuleRegistry getSingleton()
            throws
                IOException
    {
        if( theSingleton == null ) {            
            HashMap<String,ModuleMeta[]> metas = findModuleMetas(ScanningClasspathModuleRegistry.class.getClassLoader() );
            theSingleton = new ScanningClasspathModuleRegistry( metas );
        }
        return theSingleton;
    }
    
    /**
     * Private constructor; use factory method.
     * 
     * @para metas the found ModuleMetas
     */
    private ScanningClasspathModuleRegistry(
            HashMap<String,ModuleMeta[]> metas )
    {
        super( metas );
    }
    
    /**
     * ModuleRegistry also acts as a factory for the Modules' ClassLoaders.
     * Here, all Modules share the same ClassLoaders.
     *
     * @param module the Module for which to create a ClassLoader
     * @param parentClassLoader the ClassLoader to use as the parent ClassLoader
     * @return the ClassLoader to use with the Module
     */
    @Override
    public ClassLoader createClassLoader(
            Module      module,
            ClassLoader parentClassLoader )
    {
        return parentClassLoader;
    }
    
    /**
     * Find the ModuleMetas available to this ClassLoader.
     * 
     * @param cl the class loader
     * @return the found ModuleMetas, keyed by module name, and ordered by version
     * @throws IOException reading files failed
     */
    protected static HashMap<String,ModuleMeta[]> findModuleMetas(
            ClassLoader cl )
        throws
            IOException
    {
        Enumeration<URL>   metaInfoUrls = cl.getResources( "META-INF/" );
        ArrayList<JarFile> jars         = new ArrayList<>();
        ArrayList<File>    dirs         = new ArrayList<>();

        while( metaInfoUrls.hasMoreElements() ) {
            URL metaInfoUrl = metaInfoUrls.nextElement();
            
            switch( metaInfoUrl.getProtocol() ) {
                case "jar":
                    String jarFile = metaInfoUrl.getFile();

                    int colon = jarFile.indexOf( ":" );
                    if( colon > 0 ) {
                        jarFile = jarFile.substring( colon+1 );
                    }
                    int excl = jarFile.indexOf( "!" );
                    if( excl > 0 ) {
                        jarFile = jarFile.substring( 0, excl );
                    }

                    JarFile jar = new JarFile( jarFile );
                    jars.add( jar );
                    break;

                case "file":
                    File dir = new File( metaInfoUrl.getFile() );
                    if( dir.isDirectory() ) {
                        dirs.add( dir );
                    }
                    break;
            }
        }
        
        HashMap<String,ModuleMeta []> metas = new HashMap<>();
        addParsedModuleMetasFromJars( jars, metas );          // looks into the JARs, from the top
        addParsedModuleMetasFromDirectories( dirs, metas );   // looks into META-INF dirs

        return metas;
    }

    /**
     * The singleton instance.
    */
    protected static ScanningClasspathModuleRegistry theSingleton;
}
