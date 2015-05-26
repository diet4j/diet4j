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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is the default implementation of ModuleActivator. It looks for a class named
 * ModuleInit in a Java package that is the same as the Module name. Within this
 * class, it looks for static methods 'activate' and 'deactivate'. If they exist,
 * they will be invoked when the Module is activated or deactivated.
 */
public class DefaultModuleActivator
        implements
            ModuleActivator
{
    /**
     * Constructor.
     *
     * @param mod the Module to activate
     */
    public DefaultModuleActivator(
            Module mod )
    {
        theModule = mod;
    }

    /**
     * Obtain the Module that this ModuleActivator can activate.
     *
     * @return the Module that this ModuleActivator can activate
     */
    @Override
    public Module getModule()
    {
        return theModule;
    }

    /**
     * Activate a Module.
     *
     * @param dependentModules the Modules that this Module depends on, if any
     * @param dependentContextObjects the context objects of the Modules that this Module depends on, if any, in same sequence as dependentModules
     * @return a context object that is Module-specific, or null if none
     * @throws ModuleActivationException throws if the Module could not be activated
     */
    @Override
    public Object activate(
            Module [] dependentModules,
            Object [] dependentContextObjects )
        throws
            ModuleActivationException
    {
        ModuleMeta meta = theModule.getModuleMeta();
        
        String activationClassName  = meta.getInitClassName();

        boolean isDefaultActivationClass  = false;
        if( activationClassName == null || activationClassName.length() == 0 ) {
            activationClassName = meta.getModuleName() + "." + MODULE_INIT_CLASS_NAME;
            isDefaultActivationClass = true;
        }
        try {
            log.log( Level.FINER, "moduleActivateStarted", new Object[] { theModule } );

            // FIXME? I think this does not distinguish between ClassNotFoundExceptions that are triggered
            // by not finding the activationClass, and those triggered by those not finding a class while
            // running it.
            Class<?> activationClass = Class.forName( activationClassName, true, theModule.getClassLoader() );

            Method activationMethod = activationClass.getMethod(
                    ACTIVATION_METHOD_NAME,
                    new Class[] {
                            Module[].class,
                            Object[].class,
                            Module.class } );

            log.log( Level.FINER, "moduleActivate", new Object[] { theModule, activationClassName, activationMethod } );

            Object ret = activationMethod.invoke(
                    null,
                    new Object[] {
                            dependentModules,
                            dependentContextObjects,
                            theModule } ); // may throw activation exception

            log.log( Level.FINER, "moduleActivateSucceeded", new Object[] { theModule } );

            return ret;

        } catch( InvocationTargetException ex ) {
            log.log( Level.SEVERE, "moduleActivateFailed", ex.getTargetException() );

            if( ex.getTargetException() instanceof ModuleActivationException ) {
                throw (ModuleActivationException) ex.getTargetException();
            }

            throw new ModuleActivationException( meta, ex.getTargetException() );

        } catch( ClassNotFoundException|NoSuchMethodException ex ) {
            if( isDefaultActivationClass ) {
                log.log( Level.FINER, "moduleActivateSucceeded", new Object[] { theModule } );
                return null;

            } else {
                log.log( Level.SEVERE, "moduleDeactivateFailed", ex );
                throw new ModuleActivationException( meta, ex );            
            }
        
        } catch( Throwable ex ) {
            log.log( Level.SEVERE, "moduleDeactivateFailed", ex );
            throw new ModuleActivationException( meta, ex );        
        }
    }

    /**
     * Deactivate a Module.
     *
     * @param dependentModules the Modules that this Module depends on, if any
     * @throws ModuleActivationException throws if the Module could not be activated
     */
    @Override
    public void deactivate(
            Module [] dependentModules )
        throws
            ModuleActivationException
    {
        ModuleMeta meta = theModule.getModuleMeta();
        
        String deactivationClassName  = meta.getInitClassName();

        boolean isDefaultDeactivationClass  = false;
        if( deactivationClassName == null || deactivationClassName.length() == 0 ) {
            deactivationClassName = meta.getModuleName() + "." + MODULE_INIT_CLASS_NAME;
            isDefaultDeactivationClass = true;
        }

        try {
            log.log( Level.FINER, "moduleDeactivateStarted", new Object[] { theModule } );

            Class<?> deactivationClass = Class.forName( deactivationClassName, true, theModule.getClassLoader() );
        
            Method deactivationMethod = deactivationClass.getMethod(
                    DEACTIVATION_METHOD_NAME,
                    new Class[] {
                            Module[].class,
                            Module.class
                    } );

            log.log( Level.FINER, "moduleDeactivate", new Object[] { theModule, deactivationClassName, deactivationMethod } );

            deactivationMethod.invoke(
                    null,
                    new Object[] {
                            dependentModules,
                            theModule
                    } );

            log.log( Level.FINER, "moduleDeactivateSucceeded", new Object[] { theModule } );

        } catch( InvocationTargetException ex ) {
            log.log( Level.SEVERE, "moduleDeactivateFailed", ex.getTargetException() );

            if( ex.getTargetException() instanceof ModuleActivationException ) {
                throw (ModuleActivationException) ex.getTargetException();
            }

            throw new ModuleActivationException( meta, ex.getTargetException() );

        } catch( ClassNotFoundException|NoSuchMethodException ex ) {
            if( isDefaultDeactivationClass ) {
                log.log( Level.FINER, "moduleDeactivateSucceeded", new Object[] { theModule } );

            } else {
                log.log( Level.SEVERE, "moduleDeactivateFailed", ex );
                throw new ModuleActivationException( meta, ex );            
            }
        
        } catch( Throwable ex ) {
            log.log( Level.SEVERE, "moduleDeactivateFailed", ex );

            throw new ModuleActivationException( theModule.getModuleMeta(), ex );
        }
    }

    /**
     * Obtain a ModuleActivator that is responsible for activating a dependent Module. This
     * method exists in order to be able to override it.
     *
     * @param dependentModule the dependent Module to activate
     * @return the ModuleActivator for the dependent Module
     */
    @Override
    public ModuleActivator dependentModuleActivator(
            Module dependentModule )
    {
        return dependentModule.getDefaultModuleActivator();
    }

    /**
     * The Module that this ModuleActivator can activate.
     */
    protected final Module theModule;

    /**
     * Name of the ModuleInit class in the package whose name is the same as the
     * Module name.
     */
    public static final String MODULE_INIT_CLASS_NAME = "ModuleInit";

    /**
     * Name of the activation method in the ModuleInit class.
     */
    public static final String ACTIVATION_METHOD_NAME = "activate";

    /**
     * Name of the deactivation method in the ModuleInit class.
     */
    public static final String DEACTIVATION_METHOD_NAME = "deactivate";
    
    /**
     * Logger.
     */
    private static final Logger log = Logger.getLogger( DefaultModuleActivator.class.getName() );
}
