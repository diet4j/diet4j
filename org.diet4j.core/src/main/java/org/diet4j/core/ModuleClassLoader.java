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
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>This is a ClassLoader that knows how to load the code for a Module. It first looks
 * for code in its own JAR, and then delegates to the ModuleClassLoaders of the Modules
 * that this Module depends on.</p>
 *
 * <p>This used to inherit from URLClassLoader, but the URLClassLoader did mysterious
 * things (it suddenly added additional JARs to itself and I have no idea where
 * they came from), so I did it myself.</p>
 */
public class ModuleClassLoader
        extends
            ClassLoader
{
    /**
      * Construct one with the Module whose classes this ModuleClassLoader is
      * supposed to load, the parent/system ClassLoader, and the ClassLoaders of dependent Modules.
      *
      * @param mod the Module whose classes this ClassLoader will load
      * @param parent the parent ClassLoader of this ClassLoader
      * @param dependencyClassLoaders  the ModuleClassLoaders of the Module's dependent Modules
      */
    public ModuleClassLoader(
            Module               mod,
            ClassLoader          parent,
            ModuleClassLoader [] dependencyClassLoaders )
    {
        super( parent );

        theModule                 = mod;
        theDependencyClassLoaders = dependencyClassLoaders;
    }

    /**
     * Obtain the Module whose classes this ModuleClassLoader loads.
     *
     * @return the Module for whose classes this ModuleClassLoader loads
     */
    public Module getModule()
    {
        return theModule;
    }

    /**
     * Convenience method to oObtain the AbstractModuleRegistry in use.
     * 
     * @return the AbstractModuleRegistry
     */
    public AbstractModuleRegistry getModuleRegistry()
    {
        return theModule.getModuleRegistry();
    }

    /**
     * Add, to the provided set, all URLs to Module JARs, of this ModuleClassLoader
     * and all dependent ClassLoaders. This is a method required to make Jasper and
     * other JSP technologies happy.
     *
     * @param set the set to add
     * @throws MalformedURLException thrown if a URL was invalid
     */
    public void addModuleJarUrls(
            Set<URL> set )
        throws
            MalformedURLException
    {
        set.add( ( new File( theModule.getModuleMeta().getProvidesJar().getName() )).toURI().toURL() );

        for( ModuleClassLoader dep : theDependencyClassLoaders ) {
            if( dep != null ) {
                dep.addModuleJarUrls( set );
            }
        }
    }

    /**
     * Find a resource through this ClassLoader. First look for a resource locally to this ClassLoader, then in
     * the ClassLoaders of our dependent Modules.
     *
     * @param name name of the resource to find
     * @return URL to the resource
     */
    @Override
    public URL getResource(
            String name )
    {
        URL ret = findResource( name );
        if( ret != null ) {
            return ret;
        }

        for( int i=0 ; i<theDependencyClassLoaders.length ; ++i ) {
            if( theDependencyClassLoaders[i] != null ) {
                ret = theDependencyClassLoaders[i].getResource( name );
                if( ret != null ) {
                    return ret;
                }
            }
        }
        return null;
    }

    /**
     * Obtain an Enumeration of Resources.
     *
     * @param name the name of the Resource
     * @return the Enumeration
     * @throws IOException thrown if an I/O error occurred
     */
    @Override
    public Enumeration<URL> getResources(
            String name )
        throws
            IOException
    {
        ClassLoader parent = getParent();

        URL localResource = getResource( name );
        if( localResource != null ) {
            return new CompoundIterator<URL>( localResource, parent.getResources( name ));
        } else {
            return parent.getResources( name );
        }
    }

    /**
     * Override loadClass() per comment above.
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
            log.log( Level.FINER, "loadClassAttemptStart", new Object [] { theModule, name } );

            if( cannotFindTable.get( name ) == null ) {

                ClassLoader consultDefaultClassLoader = null;
                for( String prefix : MODULE_CLASSES_PREFIXES ) {
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

                String path = name.replace('.', '/').concat(".class");
                byte [] classBytes = findBlob( path );
                if( classBytes != null && classBytes.length > 0 ) {
                    try {
                        c = defineClass( name, classBytes, 0, classBytes.length );

                    } catch( NoClassDefFoundWithClassLoaderError ex ) {
                        throw ex; // just rethrow

                    } catch( NoClassDefFoundError ex ) {
                        throw new NoClassDefFoundWithClassLoaderError( ex.getMessage(), this );

                    } catch( ClassFormatError ex ) {
                        log.log( Level.SEVERE, "loadClassAttemptStart", ex );
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
                log.log( Level.SEVERE, "loadClass failed", new Object[] { theModule, name } );
            }
            throw new ClassNotFoundException( name );
        }

        if( resolve ) {
            resolveClass( c );
        }
        if( closeReporting ) {
            log.log( Level.FINER, "loadClass succeeded", new Object[] { theModule, name } );
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
    public synchronized URL findResource(
            String name )
    {
        JarFile  jar        = theModule.getModuleMeta().getProvidesJar();
        JarEntry foundEntry = jar.getJarEntry( name );

        if( foundEntry == null ) {
            return null;
        }
        try {
            StringBuilder urlSpec = new StringBuilder();
            urlSpec.append( "jar:file:" );
            urlSpec.append( jar.getName() );
            urlSpec.append( "!/" );
            return new URL( new URL( urlSpec.toString() ), foundEntry.getName() );

        } catch( MalformedURLException ex ) {
            log.log( Level.SEVERE, "findResource " + name, ex  );
            return null;
        }
    }

    /**
     * Find a blob of data.
     *
     * @param name the name of the resource
     * @return the blob of data, as byte array, if found
     */
    protected synchronized byte [] findBlob(
            String name )
    {
        JarFile jar = theModule.getModuleMeta().getProvidesJar();
        try {
            JarEntry entry = jar.getJarEntry( name );
            if( entry != null ) {
                InputStream stream = jar.getInputStream( entry );
                if( stream != null ) {
                    return slurp( stream, (int) entry.getSize(), -1 );
                }
            }

        } catch( IOException ex ) {
            // Files that don't have the requested resource throw this exception, so don't do anything
        }
        return null;
    }

    /**
     * Helper method to read a byte array from a stream until EOF.
     *
     * @param inStream the stream to read from
     * @param initial the initial size of the buffer
     * @param maxBytes the maximum number of bytes we accept
     * @return the found byte array
     * @throws IOException thrown if an I/O error occurred
     */
    protected static byte [] slurp(
            InputStream inStream,
            int         initial,
            int         maxBytes )
        throws
            IOException
    {
        int bufsize = 1024;
        if( initial > 0 ) {
            bufsize = initial;
        }
        if( maxBytes > 0 && bufsize > maxBytes ) {
            bufsize = maxBytes;
        }
        byte[] buf    = new byte[ bufsize ];
        int    offset = 0;

        while( true ) {
            int toRead = buf.length;
            if( maxBytes > 0 && maxBytes < toRead ) {
                toRead = maxBytes;
            }
            int read = inStream.read( buf, offset, toRead  - offset);
            if( read <= 0 ) {
                break;
            }
            offset += read;
            if( offset == buf.length ) {
                byte [] temp = new byte[ buf.length * 2 ];
                System.arraycopy( buf, 0, temp, 0, offset );
                buf = temp;
            }
        }
        
        // now chop if necessary
        if( buf.length > offset ) {
            byte [] temp = new byte[ offset ];
            System.arraycopy( buf, 0, temp, 0, offset );
            return temp;
        } else {
            return buf;
        }
    }

    /**
     * Obtain the ClassLoaders from dependent Modules.
     *
     * @return the ClassLoaders from dependent Modules
     */
    public ModuleClassLoader [] getDependencyClassLoaders()
    {
        return theDependencyClassLoaders;
    }

    /**
     * Convert to a string representation for debugging.
     *
     * @return string representation of this object
     */
    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder( 100 ); // fudge
        buf.append( getClass().getName() );
        buf.append( " (Module: " );
        buf.append( theModule.toString() );
        buf.append( ")" );

        return buf.toString();
    }

    /**
     * The Module whose classes this ClassLoader is responsible for loading.
     */
    protected Module theModule;

    /**
     * The set of ModuleClassLoaders from the dependent Modules. Allocated as needed.
     */
    protected ModuleClassLoader [] theDependencyClassLoaders = null;

    /**
     * Our StreamHandler, allocated as needed.
     */
    protected URLStreamHandler theStreamHandler;

    /**
     * This map maps names of resources that we know for sure we can't load to a
     * marker object, so we stop attempting to load here and not delegate.
     */
    protected HashMap<String,Object> cannotFindTable = new HashMap<String,Object>( 20 );

    /**
     * Marker object to be inserted into the cannotFindTable.
     */
    private static final Object CANNOT_FIND_OBJECT = new Object();
    
    /**
     * Only load classes with this prefix from the default ClassLoader.
     */
    public static final String [] MODULE_CLASSES_PREFIXES = {
        "java", // java, javax
        "com.sun.",
        "sun", // sun, sunw
        "org.diet4j.cmdline",
        "org.diet4j.core",
        "org.diet4j.tomcat",
        "org.ietf.jgss",
        "org.omg.",
        "org.w3c.dom",
        "org.xml.sax"
    };

    /**
     * Logger.
     */
    private static final Logger log = Logger.getLogger( ModuleClassLoader.class.getName() );

    /**
     * Compound iterator helper class.
     * 
     * @param <T> the type of element to iterate over
     */
    public static class CompoundIterator<T>
            implements
                Enumeration<T>
    {
        /**
         * Constructor.
         *
         * @param firstElement the first element to return
         * @param continued Enumeration over the remaining elements
         */
         public CompoundIterator(
                 T              firstElement,
                 Enumeration<T> continued )
         {
             theFirstElement = firstElement;
             theContinued    = continued;
         }

         /**
         * Tests if this enumeration contains more elements.
         *
         * @return  <code>true</code> if and only if this enumeration object
         *           contains at least one more element to provide;
         *          <code>false</code> otherwise.
         */
        @Override
        public boolean hasMoreElements()
        {
            if( doFirst ) {
                return true;
            }
            return theContinued.hasMoreElements();
        }

        /**
         * Returns the next element of this enumeration if this enumeration
         * object has at least one more element to provide.
         *
         * @return     the next element of this enumeration.
         * @throws  NoSuchElementException  if no more elements exist.
         */
        @Override
        public T nextElement()
        {
            if( doFirst ) {
                doFirst = false;
                return theFirstElement;
            }
            return theContinued.nextElement();
        }
        
        /**
          * The first element to return.
          */
        protected T theFirstElement;
         
        /**
          * The Enumeration over all other elements to return after the first.
          */
        protected Enumeration<T> theContinued;

        /**
          * Flag that tells whether to return the first element next.
          */
        protected boolean doFirst = true;
    }
}
