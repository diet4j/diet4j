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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A software Module. On an operating system level, this is usually called package.
 */
public class Module
{
    /**
     * Constructor. This should not be directly invoked by the application programmer.
     *
     * @param meta this Module's ModuleMeta
     * @param registry the registry of Modules in which we try to find dependent Modules
     * @param parentClassLoader the class loader of our parent Module
     */
    public Module(
            ModuleMeta     meta,
            AbstractModuleRegistry registry,
            ClassLoader    parentClassLoader )
    {
        if( meta == null ) {
            throw new NullPointerException( "Null ModuleMeta" );
        }
        if( registry == null ) {
            throw new NullPointerException( "Null Module Registry" );
        }
        if( parentClassLoader == null ) {
            throw new NullPointerException( "Null parent ClassLoader" );
        }

        theModuleMeta        = meta;
        theRegistry          = registry;
        theParentClassLoader = parentClassLoader;
    }

    /**
     * Obtain the groupId of this Module. It is the same as the groupId of its ModuleMeta.
     *
     * @return the groupId of this Module
     */
    public final String getModuleGroupId()
    {
        return theModuleMeta.getModuleGroupId();
    }

    /**
     * Obtain the artifactId of this Module. It is the same as the artifactId of its ModuleMeta.
     *
     * @return the artifactId of this Module
     */
    public final String getModuleArtifactId()
    {
        return theModuleMeta.getModuleArtifactId();
    }

    /**
     * Obtain the version of this Module. It is the same as the version of its ModuleMeta.
     *
     * @return the version of this Module
     */
    public final String getModuleVersion()
    {
        return theModuleMeta.getModuleVersion();
    }

    /**
      * Obtain the ModuleMeta for this Module.
      *
      * @return the ModuleMeta for this Module
      */
    public final ModuleMeta getModuleMeta()
    {
        return theModuleMeta;
    }

    /**
     * Obtain the ModuleRegistry in which this Module has been registered.
     *
     * @return the ModuleRegistry in which this Module has been registered
     */
    public final ModuleRegistry getModuleRegistry()
    {
        return theRegistry;
    }

    /**
     * Convenience method to determine the dependent Modules of this Module
     * at run-time.
     *
     * @return the dependent Modules at run-time
     */
    public final Module [] determineRuntimeDependencies()
    {
        return theRegistry.determineRuntimeDependencies( this );
    }

    /**
     * Convenience method to determine the Modules that use this Module at
     * run-time.
     *
     * This is the inverse relationship found by determineRuntimeDependencies. However,
     * unlike determineRuntimeDependencies, the returned values may change dramatically
     * during operation of the system as additional Modules become users.
     *
     * @return the set of Modules that this Module currently is used by at run-time
     */
    public final Module [] determineRuntimeUses()
    {
        return theRegistry.determineRuntimeUses( this );
    }

    /**
     * Obtain a ClassLoader that knows how to load the code belonging to this Module.
     * This ClassLoader will delegate to the ClassLoader of the parent Module.
     *
     * @return this Module's ClassLoader
     * @throws MalformedURLException thrown if one of the URLs identifying the Module's JAR files is malformed
     */
    public synchronized final ClassLoader getClassLoader()
        throws
            MalformedURLException
    {
        if( theClassLoader == null ) {
            theClassLoader = theRegistry.createClassLoader( this, theParentClassLoader );
        }

        return theClassLoader;
    }

    /**
     * Determine whether this Module is active. A Module is active if it has been
     * activated and not deactivated afterwards.
     *
     * @return if true, this Module is active
     */
    public final boolean isActive()
    {
        return theActivationCount > 0 ;
    }

    /**
     * This recursively activates this Module. First, this method recursively activates all Modules
     * that it this Module depends on, and then it activates itself.
     *
     * @throws ModuleResolutionException thrown if a dependent Module cannot be resolved
     * @throws ModuleNotFoundException thrown if a dependent Module cannot be found
     * @throws ModuleActivationException thrown if this Module, or a dependent Module could not be activated
     */
    public final void activateRecursively()
        throws
            ModuleResolutionException,
            ModuleNotFoundException,
            ModuleActivationException
    {
        activateRecursively( getDefaultModuleActivator() );
    }

    /**
     * This recursively activates this Module. First, this method recursively activates all Modules
     * that it this Module depends on, and then it activates itself. By specifying a ModuleActivator,
     * a non-standard way of activating the Module can be performed.
     *
     * @param activator a ModuleActivator instance that knows how to activate this Module
     * @throws ModuleResolutionException thrown if a dependent Module cannot be resolved
     * @throws ModuleNotFoundException thrown if a dependent Module cannot be found
     * @throws ModuleActivationException thrown if this Module, or a dependent Module could not be activated
     */
    public final void activateRecursively(
            ModuleActivator activator )
        throws
            ModuleResolutionException,
            ModuleNotFoundException,
            ModuleActivationException
    {
        if( theActivationCount == 0 ) {
            boolean success = false;
            try {
                log.log( Level.FINER, "moduleActivateRecursivelyStarted: {0}", this );

                Module [] dependencies = theRegistry.determineRuntimeDependencies( this );

                for( int i=0 ; i<dependencies.length ; ++i ) {
                    if( dependencies[i] != null ) {
                        ModuleActivator childActivator = activator.dependentModuleActivator( dependencies[i] );
                        try {
                            dependencies[i].activateRecursively( childActivator ); // FIXME? Arguments?
                        } catch( Exception ex ) {
                            throw new ModuleActivationException( theModuleMeta, ex );
                        }
                    }
                }
                theContextObject = activator.activate();
                // this may throw an exception

                success = true;

            } finally {
                if( success ) {
                    log.log( Level.FINER, "moduleActivateRecursivelySucceeded: {0}", this );
                } else {
                    log.log( Level.FINER, "moduleActivateRecursivelyFailed: {0}", this );
                }

            }
        }
        ++theActivationCount;
    }

    /**
     * This recursively deactivates this Module. First, this method deactivates itself, and then
     * it recursively activates all Modules that this Module depends on
     *
     * @throws ModuleDeactivationException throws if the Module could not be deactivated
     */
    public final void deactivateRecursively()
        throws
            ModuleDeactivationException
    {
        deactivateRecursively( getDefaultModuleActivator() );
    }

    /**
     * This recursively deactivates this Module. First, this method deactivates itself, and then
     * it recursively activates all Modules that this Module depends on. By specifying a ModuleActivator,
     * a non-standard way of deactivating the Module can be performed.
     *
     * @param activator a ModuleActivator instance that knows how to deactivate instead of the default one specified in the ModuleMeta
     * @throws ModuleDeactivationException throws if the Module could not be deactivated
     */
    public final void deactivateRecursively(
            ModuleActivator activator )
        throws
            ModuleDeactivationException
    {
        --theActivationCount;
        if( theActivationCount == 0 ) {
            Module [] dependencies = theRegistry.determineRuntimeDependencies( this );

            activator.deactivate();

            for( int i=0 ; i<dependencies.length ; ++i ) {
                if( dependencies[i] != null ) {
                    // might be an optional dependency
                    ModuleActivator childDeactivator = activator.dependentModuleActivator( dependencies[i] );
                    dependencies[i].deactivateRecursively( childDeactivator );
                }
            }
            // this may throw an exception
        }
    }

    /**
     * Obtain the context object returned by the activation method of this Module, if any.
     *
     * @return the context object, or null
     */
    public final Object getContextObject()
    {
        return theContextObject;
    }

    /**
     * Run this Module as a root Module.
     *
     * @param arguments arguments to run, similar to the arguments of a standard main(...) method
     * @throws ClassNotFoundException thrown if the specified run class cannot be found
     * @throws ModuleRunException thrown if the specified run method threw an Exception
     * @throws NoRunMethodException thrown if a suitable run method cannot be found
     * @throws InvocationTargetException thrown if the run method throws an Exception
     */
    public final void run(
            String [] arguments )
        throws
            ClassNotFoundException,
            ModuleRunException,
            NoRunMethodException,
            InvocationTargetException
    {
        run( null, null, arguments );
    }

    /**
     * Run this Module as a root Module. Specify a class to run other than the default,
     * and/or a method other than the main() method.
     *
     * @param overriddenRunClassName optional name of the class to run instead of the default one specified in the ModuleMeta
     * @param overriddenRunMethodName optional name of the method in the class to run instead of the default one specified in the ModuleMeta
     * @param arguments arguments to run, similar to the arguments of a standard main(...) method
     * @return the desired System exit code
     * @throws ClassNotFoundException thrown if the specified run class cannot be found
     * @throws ModuleRunException thrown if the specified run method threw an Exception
     * @throws NoRunMethodException thrown if a suitable run method cannot be found
     * @throws InvocationTargetException thrown if the run method throws an Exception
     */
    public int run(
            String    overriddenRunClassName,
            String    overriddenRunMethodName,
            String [] arguments )
        throws
            ClassNotFoundException,
            ModuleRunException,
            NoRunMethodException,
            InvocationTargetException
    {
        log.log( Level.FINER, "runStarted: {0} ({1} {2})", new Object[] { this, overriddenRunClassName, overriddenRunMethodName } );

        // look for a run method. If none found, throw an exception

        String runClassName  = overriddenRunClassName;
        String runMethodName = overriddenRunMethodName;

        try {
            if( runClassName == null ) {
                runClassName = theModuleMeta.getRunClassName();
            }
            if( runMethodName == null ) {
                runMethodName = RUN_METHOD_NAME;
            }

            if( runClassName == null ) {
                throw new NoRunMethodException( theModuleMeta, runClassName, runMethodName, null );
            }

            // invoke
            Class<?> runClass = Class.forName( runClassName, true, getClassLoader() );

            Method runMethod = runClass.getMethod(
                    runMethodName,
                    new Class[] {
                            String[].class } );

            log.log( Level.FINER, "run: {0} ({1} {2})", new Object[] { this, runClass, runMethod } );

            Object ret = runMethod.invoke(
                    null,
                    new Object[] {
                            arguments } );

            log.log( Level.FINER, "runSucceeded: {0}", this );

            if( ret instanceof Number ) {
                return ((Number)ret).intValue();
            }  else {
                return 0; // everything seems fine
            }

        } catch( MalformedURLException ex ) {
            log.log( Level.SEVERE, "run failed: " + this, ex );
            return 1;

        } catch( InvocationTargetException ex ) {
            if( ex.getTargetException() instanceof ModuleRunException ) {
                throw (ModuleRunException) ex.getTargetException();
            } else {
                throw new ModuleRunException( theModuleMeta, runClassName, runMethodName, ex.getTargetException() );
            }

        } catch( Throwable ex ) {
            throw new NoRunMethodException( theModuleMeta, runClassName, runMethodName, ex );
       }
    }

    /**
     * Obtain the default ModuleActivator for this Module.
     *
     * @return the ModuleActivator
     */
    public ModuleActivator getDefaultModuleActivator()
    {
        return new DefaultModuleActivator( this );
    }

    /**
     * Determine Module equality. Two Modules are the same if they have the same
     * Module Advertisement.
     *
     * @param other other object to compare this Module to
     * @return returns true if the objects are the same
     */
    @Override
    public boolean equals(
            Object other )
    {
        if( other instanceof Module ) {
            return theModuleMeta.equals( ((Module)other).theModuleMeta );
        }
        return false;
    }

    /**
     * Determine hash code. We use the hash code of our ModuleMeta.
     *
     * @return the hash code
     */
    @Override
    public int hashCode()
    {
        return theModuleMeta.hashCode();
    }

    /**
     * For debugging.
     *
     * @return string representation of this object
     */
    @Override
    public String toString()
    {
        return theModuleMeta.toString();
    }

    /**
     * The AbstractModuleRegistry in which this Module is registered.
     */
    protected ModuleRegistry theRegistry;

    /**
     * The ModuleMeta for this Module.
     */
    protected ModuleMeta theModuleMeta;

    /**
     * This Module's ClassLoader. Allocated as needed.
     */
    protected ClassLoader theClassLoader = null;

    /**
     * The parent ClassLoader of this Module's ClassLoader.
     */
    protected ClassLoader theParentClassLoader;

    /**
     * Once this Module has been activated, this is set to a non-zero value. Zero means
     * the Module is deactivated. A non-zero value N indicates that N other Modules
     * depend on this Module. In other words, other than keeping track whether or not
     * a Module is initialized, we also track how many Modules depend on it and thus
     * implement a form of "garbage-collection" policy.
     */
    private int theActivationCount = 0;

    /**
     * Activation of this Module may create a context object, which is buffered here.
     */
    private Object theContextObject;

    /**
     * Name of the run method on a class.
     */
    public static final String RUN_METHOD_NAME = "main";

    /**
     * Logger.
     */
    private static final Logger log = Logger.getLogger( Module.class.getName() );
}
