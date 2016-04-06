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
 * This is the default implementation of ModuleActivator. If an activation class
 * was provided for the Module, it looks for static methods 'moduleActivate' and 'moduleDeactivate'
 * in this class. If they exist, they will be invoked when the Module is activated or deactivated.
 *
 * The methods must have the following signature:
 *
 * <pre>
 * public static void moduleActivate(
 *         Module thisModule );
 *
 * public static void moduleDeactivate(
 *         Module thisModule );
 * </pre>
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
     * @return a context object that is Module-specific, or null if none
     * @throws ModuleActivationException throws if the Module could not be activated
     */
    @Override
    public Object activate()
        throws
            ModuleActivationException
    {
        ModuleMeta meta = theModule.getModuleMeta();

        String activationClassName = meta.getActivationClassName();
        if( activationClassName == null ) {
            return null;
        }

        try {
            log.log( Level.FINER, "moduleActivateStarted: {0}", theModule );

            // FIXME? I think this does not distinguish between ClassNotFoundExceptions that are triggered
            // by not finding the activationClass, and those triggered by those not finding a class while
            // running it.
            Class<?> activationClass = Class.forName( activationClassName, true, theModule.getClassLoader() );

            Method activationMethod = activationClass.getMethod(
                    ACTIVATION_METHOD_NAME,
                    new Class[] { Module.class } );

            log.log( Level.FINER, "moduleActivate {0} ({1} {2})", new Object[] { theModule, activationClassName, activationMethod } );

            Object ret = activationMethod.invoke(
                    null,
                    new Object[] { theModule } ); // may throw activation exception

            log.log( Level.FINER, "moduleActivateSucceeded: {0}", theModule );

            return ret;

        } catch( NoSuchMethodException ex ) {
            log.log( Level.FINER, "moduleActivate no activation method: " + theModule, ex );

        } catch( InvocationTargetException ex ) {
            log.log( Level.FINE, "moduleActivateFailed: " + theModule, ex.getTargetException() );

            if( ex.getTargetException() instanceof ModuleActivationException ) {
                throw (ModuleActivationException) ex.getTargetException();
            }

            throw new ModuleActivationException( meta, ex.getTargetException() );

        } catch( Throwable ex ) {
            log.log( Level.FINE, "moduleActivateFailed: " + theModule, ex );
            throw new ModuleActivationException( meta, ex );
        }
    }

    /**
     * Deactivate a Module.
     *
     * @throws ModuleDeactivationException throws if the Module could not be deactivated
     */
    @Override
    public void deactivate()
        throws
            ModuleDeactivationException
    {
        ModuleMeta meta = theModule.getModuleMeta();

        String deactivationClassName  = meta.getActivationClassName();
        if( deactivationClassName == null ) {
            return;
        }

        try {
            log.log( Level.FINER, "moduleDeactivateStarted: {0}", theModule );

            Class<?> deactivationClass = Class.forName( deactivationClassName, true, theModule.getClassLoader() );

            Method deactivationMethod = deactivationClass.getMethod(
                    DEACTIVATION_METHOD_NAME,
                    new Class[] { Module.class } );

            log.log( Level.FINER, "moduleDeactivate: {0} ({1} {2})", new Object[] { theModule, deactivationClassName, deactivationMethod } );

            deactivationMethod.invoke(
                    null,
                    new Object[] { theModule } );

            log.log( Level.FINER, "moduleDeactivateSucceeded: {0}", theModule );

        } catch( NoSuchMethodException ex ) {
            log.log( Level.FINER, "moduleDeactivate no deactivation method: " + theModule, ex );

        } catch( InvocationTargetException ex ) {
            log.log( Level.FINE, "moduleDeactivateFailed: " + theModule, ex.getTargetException() );

            if( ex.getTargetException() instanceof ModuleDeactivationException ) {
                throw (ModuleDeactivationException) ex.getTargetException();
            }

            throw new ModuleDeactivationException( meta, ex.getTargetException() );

        } catch( Throwable ex ) {
            log.log( Level.FINE, "moduleDeactivateFailed: " + theModule, ex );

            throw new ModuleDeactivationException( theModule.getModuleMeta(), ex );
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
     * Name of the activation method in the ModuleInit class.
     */
    public static final String ACTIVATION_METHOD_NAME = "moduleActivate";

    /**
     * Name of the deactivation method in the ModuleInit class.
     */
    public static final String DEACTIVATION_METHOD_NAME = "moduleDeactivate";

    /**
     * Logger.
     */
    private static final Logger log = Logger.getLogger( DefaultModuleActivator.class.getName() );
}
