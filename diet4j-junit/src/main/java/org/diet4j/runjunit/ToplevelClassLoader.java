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
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.diet4j.core.ModuleClassLoader;
import org.diet4j.core.NoClassDefFoundWithClassLoaderError;

/**
 * Overrides ModuleClassLoader so the test classes can be found in the local
 * file system even if they have not been put into a Module.
 */
public class ToplevelClassLoader
    extends
        ModuleClassLoader
{
    /**
      * Construct one with the Module whose classes this ModuleClassLoader is
      * supposed to load, the parent/system ClassLoader, and the ClassLoaders of dependent Modules.
      *
      * @param dirs identifies the local sources of classes
      * @param parent the parent ClassLoader of this ClassLoader
      * @param dependencyClassLoaders  the ModuleClassLoaders of the Module's dependent Modules
      * @param doNotLoadClassPrefixes prefixes of classes always to be loaded through the system class loader, not this one
      */
    public ToplevelClassLoader(
            File []              dirs,
            ClassLoader          parent,
            ModuleClassLoader [] dependencyClassLoaders,
            String []            doNotLoadClassPrefixes )
    {
        super( null, parent, dependencyClassLoaders, doNotLoadClassPrefixes );
        
        theDirs = dirs;
    }    

    /**
     * Override loadClass().
     *
     * @param name name of the to-be-loaded class
     * @param resolve do we also resolve the class
     * @return the loaded class
     * @throws ClassNotFoundException loading the class failed, it could not be found
     */
    @Override
    public synchronized Class loadClass(
            String  name,
            boolean resolve )
        throws
            ClassNotFoundException
    {
        boolean closeReporting = false;

        Class c = findLoadedClass( name );
        if( c == null ) {
            closeReporting = true;
            log.log( Level.FINER, "loadClassAttemptStart: {0} ({1})", new Object [] { getClass().getName(), name } );

            if( cannotFindTable.get( name ) == null ) {

                ClassLoader consultDefaultClassLoader = null;
                for( String prefix : theDoNotLoadClassPrefixes ) {
                    if( name.startsWith( prefix )) {
                        consultDefaultClassLoader = getClass().getClassLoader();
                        break; // we won't have more than one prefix match
                    }
                }
                if( consultDefaultClassLoader != null ) {
                    try {
                        c = consultDefaultClassLoader.loadClass( name );
                    } catch( ClassNotFoundException ex ) {
                        // do nothing
                    }
                }

                if( c == null ) {
                    String  path  = name.replace('.', '/').concat(".class");
                    
                    for( File dir : theDirs ) {
                        File file = new File( dir, path );
                        if( file.canRead() ) {
                            try {
                                byte [] classBytes = slurpFile( file );
                                if( classBytes != null && classBytes.length > 0 ) {

                                    c = defineClass( name, classBytes, 0, classBytes.length );

                                }
                            } catch( IOException ex ) {
                                log.log( Level.WARNING, "Failed to read from " + file.getPath(), ex );

                            } catch( NoClassDefFoundWithClassLoaderError ex ) {
                                throw ex; // just rethrow

                            } catch( NoClassDefFoundError ex ) {
                                throw new NoClassDefFoundWithClassLoaderError( name, ex.getMessage(), this );

                            } catch( ClassFormatError ex ) {
                                log.log( Level.SEVERE, "loadClassAttemptStart: " + this + " (" + name + ")", ex );
                            }
                        }
                    }
                }

                if( c == null ) {
                    for( int i=0 ; i<theDependencyClassLoaders.length ; ++i ) {
                        if( theDependencyClassLoaders[i] != null ) {
                            try {
                                c = theDependencyClassLoaders[i].loadClass( name, false );
                            } catch( ClassNotFoundException ex ) {
                                // do nothing
                            }
                            if( c != null ) {
                                break;
                            }
                        }
                    }
                }
            }
        }
        if( c == null ) {
            // we caught all exceptions, so we need to throw ourselves
            cannotFindTable.put( name, CANNOT_FIND_OBJECT );

            if( closeReporting ) {
                log.log( Level.FINE, "loadClass failed: Module {0} (class: {1})", new Object[] { getClass().getName(), name } );
            }
            throw new ClassNotFoundException( name + " (" + getClass().getName() + ")" );
        }

        if( resolve ) {
            resolveClass( c );
        }
        if( closeReporting ) {
            log.log( Level.FINER, "loadClass succeeded: {0} ({1})", new Object[] { getClass().getName(), name } );
        }

        return c;
    }

    /**
     * Find a URL.
     *
     * @param name the name of the resource
     * @return the URL of the resource, if found
     */
    @Override
    public URL findResource(
            String name )
    {
        for( File dir : theDirs ) {
            File file = new File( dir, name );
            if( file.canRead() ) {
                try {
                    return file.toURI().toURL();

                } catch( MalformedURLException ex ) {
                    log.log( Level.SEVERE, "findResource: " +this + " (" + name + ")", ex  );
                }
            }
        }
        return null;
    }
    
    /**
     * Helper method to read a byte array from a File.
     *
     * @param file the File from where to read
     * @return the found byte array
     * @throws IOException thrown if an I/O error occurred
     */
    protected static byte [] slurpFile(
            File file )
        throws
            IOException
    {
        FileInputStream inStream = new FileInputStream( file );

        byte [] buf = new byte[ 512 ];

        int offset = 0;

        while( true ) {
            
            int stillToReadIntoBuf = buf.length - offset;

            int count = inStream.read( buf, offset, stillToReadIntoBuf );
        
            if( count == -1 ) {
                int length = offset;
                if( count > 0 ) {
                    length += count;
                }
                if( buf.length == length ) {
                    return buf;
                } else {
                    byte [] ret = new byte[ length ];
                    System.arraycopy( buf, 0, ret, 0, length );
                    return ret;
                }
            } else if( offset + count == buf.length ) {
                // double buffer and keep reading
                byte [] newBuf = new byte[ buf.length*2 ];
                System.arraycopy( buf, 0, newBuf, 0, buf.length );
                buf    = newBuf;
            }
            offset += count;
        }
    }

    /**
     * The local classes not packaged into Modules.
     */
    protected File [] theDirs;

    /**
     * Logger.
     */
    private static final Logger log = Logger.getLogger( ToplevelClassLoader.class.getName() );
}
