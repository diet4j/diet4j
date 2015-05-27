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

/**
 * Instances supporting this interface know how to activate a Module.
 */
public interface ModuleActivator
{
    /**
     * Obtain the Module that we can activate.
     *
     * @return the Module that we can activate
     */
    public Module getModule();

    /**
     * Activate a Module.
     *
     * @return a context object that is Module-specific, or null if none
     * @throws ModuleActivationException throws if the Module could not be activated
     */
    public Object activate()
        throws
            ModuleActivationException;

    /**
     * Deactivate a Module.
     *
     * @throws ModuleDeactivationException throws if the Module could not be deactivated
     */
    public void deactivate()
        throws
            ModuleDeactivationException;

    /**
     * Obtain a ModuleActivator that is responsible for activating a dependent Module.
     *
     * @param dependentModule the dependent Module to activate
     * @return the ModuleActivator for the dependent Module
     */
    public ModuleActivator dependentModuleActivator(
            Module dependentModule );
}
