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
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
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
      * @param doNotLoadClassPrefixes prefixes of classes always to be loaded through the system class loader, not this one
      */
    public ModuleClassLoader(
            Module               mod,
            ClassLoader          parent,
            ModuleClassLoader [] dependencyClassLoaders,
            String []            doNotLoadClassPrefixes )
    {
        super( parent );

        theModule                 = mod;
        theDependencyClassLoaders = dependencyClassLoaders;
        theDoNotLoadClassPrefixes = doNotLoadClassPrefixes;
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
     * Convenience method to obtain the ModuleRegistry in use.
     *
     * @return the ModuleRegistry
     */
    public ModuleRegistry getModuleRegistry()
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
            return new CompoundIterator<>( localResource, parent.getResources( name ));
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
            log.log( Level.FINER, "loadClassAttemptStart: {0} ({1})", new Object [] { theModule, name } );

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
                    ModuleMeta meta   = theModule.getModuleMeta();
                    JarFile    jar    = meta.getProvidesJar();
                    String     prefix = meta.getResourceJarEntryPrefix();
                    JarEntry   entry  = jar.getJarEntry( prefix + path );

                    try {
                        byte [] classBytes = slurpJarEntry( jar, entry );
                        if( classBytes != null && classBytes.length > 0 ) {
                            // Define a Package if there is one
                            int lastDot = name.lastIndexOf( '.' );
                            if( lastDot != -1 ) {
                                String pkgName = name.substring( 0, lastDot );

                                URL      url = new URL( "file://" + jar.getName() );

                                Manifest man = jar.getManifest();

                                if( getAndVerifyPackage( pkgName, man, url ) == null ) {
                                    if( man != null ) {
                                        definePackage( pkgName, man, url );
                                    } else {
                                        definePackage( pkgName, null, null, null, null, null, null, null );
                                    }
                                }
                            }

                            c = defineClass( name, classBytes, 0, classBytes.length );

                        }
                    } catch( IOException ex ) {
                        log.log( Level.WARNING, "Failed to read from Jar file " + jar, ex );

                    } catch( NoClassDefFoundWithClassLoaderError ex ) {
                        throw ex; // just rethrow

                    } catch( NoClassDefFoundError ex ) {
                        throw new NoClassDefFoundWithClassLoaderError( ex.getMessage(), this );

                    } catch( ClassFormatError ex ) {
                        log.log( Level.SEVERE, "loadClassAttemptStart: " + this + " (" + name + ")", ex );
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
                log.log( Level.FINE, "loadClass failed: Module {0} (class: {1})", new Object[] { theModule, name } );
            }
            throw new ClassNotFoundException( name + " (ClassLoader for module " + theModule.toString() + ")" );
        }

        if( resolve ) {
            resolveClass( c );
        }
        if( closeReporting ) {
            log.log( Level.FINER, "loadClass succeeded: {0} ({1})", new Object[] { theModule, name } );
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
        ModuleMeta meta       = theModule.getModuleMeta();
        JarFile    jar        = meta.getProvidesJar();
        String     prefix     = meta.getResourceJarEntryPrefix();
        JarEntry   foundEntry = jar.getJarEntry( prefix + name );

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
            log.log( Level.SEVERE, "findResource: " +this + " (" + name + ")", ex  );
            return null;
        }
    }

    /**
     * Helper method to read a byte array from a JarEntry.
     *
     * @param file the JarFile from which to read the JarEntry
     * @param entry the JarEntry to read. If null, return null
     * @return the found byte array
     * @throws IOException thrown if an I/O error occurred
     */
    protected static byte [] slurpJarEntry(
            JarFile  file,
            JarEntry entry )
        throws
            IOException
    {
        if( entry == null ) {
            return null;
        }

        InputStream inStream = file.getInputStream( entry );

        byte [] buf    = new byte[ (int) entry.getSize() ];
        int     offset = 0;

        while( offset < buf.length ) {
            int read = inStream.read( buf, offset, buf.length-offset );

            if( read <= 0 ) {
                log.log(
                        Level.WARNING,
                        "Attempted to read {0} bytes, but only read {1} from JarEntry {2} in {3}",
                        new Object[] { buf.length, offset, entry, file.getName() } );
                return null;
            }
            offset += read;
        }
        return buf;
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
     * Verbatim copy from JDK's URLClassLoader.
     *
     * Retrieve the package using the specified package name.
     * If non-null, verify the package using the specified code
     * source and manifest.
     */
    private Package getAndVerifyPackage(
            String   pkgname,
            Manifest man,
            URL      url )
    {
        Package pkg = getPackage(pkgname);
        if (pkg != null) {
            // Package found, so check package sealing.
            if (pkg.isSealed()) {
                // Verify that code source URL is the same.
                if (!pkg.isSealed(url)) {
                    throw new SecurityException(
                        "sealing violation: package " + pkgname + " is sealed");
                }

            } else {
                // Make sure we are not attempting to seal the package
                // at this code source URL.
                if ((man != null) && isSealed(pkgname, man)) {
                    throw new SecurityException(
                        "sealing violation: can't seal package " + pkgname +
                        ": already loaded");
                }
            }
        }
        return pkg;
    }

    /*
     * Verbatim copy from JDK's URLClassLoader.
     *
     * Returns true if the specified package name is sealed according to the
     * given manifest.
     */
    private boolean isSealed(String name, Manifest man) {
        String path = name.replace('.', '/').concat("/");
        Attributes attr = man.getAttributes(path);
        String sealed = null;
        if (attr != null) {
            sealed = attr.getValue(Attributes.Name.SEALED);
        }
        if (sealed == null) {
            if ((attr = man.getMainAttributes()) != null) {
                sealed = attr.getValue(Attributes.Name.SEALED);
            }
        }
        return "true".equalsIgnoreCase(sealed);
    }

    /**
     * Verbatim copy from JDK's URLClassLoader.
     *
     * Defines a new package by name in this ClassLoader. The attributes
     * contained in the specified Manifest will be used to obtain package
     * version and sealing information. For sealed packages, the additional
     * URL specifies the code source URL from which the package was loaded.
     *
     * @param name  the package name
     * @param man   the Manifest containing package version and sealing
     *              information
     * @param url   the code source url for the package, or null if none
     * @exception   IllegalArgumentException if the package name duplicates
     *              an existing package either in this class loader or one
     *              of its ancestors
     * @return the newly defined Package object
     */
    protected Package definePackage(String name, Manifest man, URL url)
        throws IllegalArgumentException
    {
        String path = name.replace('.', '/').concat("/");
        String specTitle = null, specVersion = null, specVendor = null;
        String implTitle = null, implVersion = null, implVendor = null;
        String sealed = null;
        URL sealBase = null;

        Attributes attr = man.getAttributes(path);
        if (attr != null) {
            specTitle   = attr.getValue(Name.SPECIFICATION_TITLE);
            specVersion = attr.getValue(Name.SPECIFICATION_VERSION);
            specVendor  = attr.getValue(Name.SPECIFICATION_VENDOR);
            implTitle   = attr.getValue(Name.IMPLEMENTATION_TITLE);
            implVersion = attr.getValue(Name.IMPLEMENTATION_VERSION);
            implVendor  = attr.getValue(Name.IMPLEMENTATION_VENDOR);
            sealed      = attr.getValue(Name.SEALED);
        }
        attr = man.getMainAttributes();
        if (attr != null) {
            if (specTitle == null) {
                specTitle = attr.getValue(Name.SPECIFICATION_TITLE);
            }
            if (specVersion == null) {
                specVersion = attr.getValue(Name.SPECIFICATION_VERSION);
            }
            if (specVendor == null) {
                specVendor = attr.getValue(Name.SPECIFICATION_VENDOR);
            }
            if (implTitle == null) {
                implTitle = attr.getValue(Name.IMPLEMENTATION_TITLE);
            }
            if (implVersion == null) {
                implVersion = attr.getValue(Name.IMPLEMENTATION_VERSION);
            }
            if (implVendor == null) {
                implVendor = attr.getValue(Name.IMPLEMENTATION_VENDOR);
            }
            if (sealed == null) {
                sealed = attr.getValue(Name.SEALED);
            }
        }
        if ("true".equalsIgnoreCase(sealed)) {
            sealBase = url;
        }
        return definePackage(name, specTitle, specVersion, specVendor,
                             implTitle, implVersion, implVendor, sealBase);
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
     * Always load classes with these prefixes through the default ClassLoader.
     */
    protected String [] theDoNotLoadClassPrefixes;

    /**
     * This map maps names of resources that we know for sure we can't load to a
     * marker object, so we stop attempting to load here and not delegate.
     */
    protected HashMap<String,Object> cannotFindTable = new HashMap<>( 20 );

    /**
     * Marker object to be inserted into the cannotFindTable.
     */
    private static final Object CANNOT_FIND_OBJECT = new Object();

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
