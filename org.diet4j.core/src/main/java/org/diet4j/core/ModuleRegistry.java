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
 * A ModuleRegistry is a place where Modules can be found and resolved.
 * Typically, an application instantiates and uses only a single ModuleRegistry.
 */
public interface ModuleRegistry
{
    /**
     * Determine the ModuleMetas that are candidates to resolve a Module dependency, based
     * on the knowledge of this ModuleRegistry.
     *
     * @param req the ModuleRequirement that we attempt to resolve
     * @return the ModuleMetas that are candidates for the resolution of the ModuleRequirement, in sequence from
     *         best matched to least matched if the ModuleRegistry makes such distinctions
     */
    public ModuleMeta [] determineResolutionCandidates(
            ModuleRequirement req );

    /**
     * Convenience method to determine the one and only ModuleMeta that is the only candidate to resolve a Module
     * dependency, based on the knowledge of this ModuleRegistry. If there is more or less than one
     * match, thrown an Exception.
     * 
     * @param req the ModuleRequirement that we attempt to resolve
     * @return the ModuleMeta that is the only candidate for the resolution of the ModuleRequirement
     * @throws ModuleResolutionCandidateNotUniqueException thrown if there were fewer or more than one ModuleMeta found
     */
    public ModuleMeta determineSingleResolutionCandidate(
            ModuleRequirement req )
        throws
            ModuleResolutionCandidateNotUniqueException;

    /**
     * Recursively resolve this ModuleMeta into a Module.
     *
     * @param meta the ModuleMeta to resolve
     * @return the resolved Module.
     * @throws ModuleNotFoundException thrown if the Module could not be found
     * @throws ModuleResolutionException thrown if the Module could not be resolved
     */
    public Module resolve(
            ModuleMeta meta )
        throws
            ModuleNotFoundException,
            ModuleResolutionException;

    /**
     * Resolve this ModuleMeta into a Module.
     *
     * @param meta the ModuleMeta to resolve
     * @param recursive resolve recursively if set to true
     * @return the resolved Module.
     * @throws ModuleNotFoundException thrown if the Module could not be found
     * @throws ModuleResolutionException thrown if the Module could not be resolved
     */
    public Module resolve(
            ModuleMeta meta,
            boolean    recursive )
        throws
            ModuleNotFoundException,
            ModuleResolutionException;

    /**
     * Determine whether a certain ModuleMeta is resolved already, and if so,
     * return it. Do not attempt to resolve.
     *
     * @param meta the ModuleMeta whose resolution we check
     * @return the Module to which the ModuleMeta has been resolved already, or null if not
     */
    public Module getResolutionOf(
            ModuleMeta meta );

    /**
     * Given a Module, this allows us to determine which other Modules it depends on.
     * This is similar to the result of determineResolutionCandidates(), except that
     * this returns one resolved Module for each dependency, not a set of ModuleMetas
     * that may or may not be resolvable.
     *
     * @param theModule the Module whose dependencies we want to determine
     * @return the set of Modules that this Module depends on.
     * @throws ModuleNotFoundException thrown if the Module could not be found
     * @throws ModuleResolutionException thrown if the Module could not be resolved
     * @see #determineUses
     */
    public Module [] determineDependencies(
            Module theModule )
        throws
            ModuleNotFoundException,
            ModuleResolutionException;

    /**
     * Given a Module, this allows us to determine which other Modules use it.
     *
     * This is the inverse relationship found by determineDependencies. However,
     * unlike determineDependencies, the returned values may change dramatically
     * during operation of the system as additional Modules become users.
     *
     * @param theModule the Module whose uses we want to determine
     * @return the set of Modules that this Module currently is used by
     * @see #determineDependencies
     */
    public Module [] determineUses(
            Module theModule );
    
    /**
     * Add a ModuleRegistry listener to be notified when new Modules become available etc.
     *
     * @param newListener the new listener to add
     * @see #removeModuleRegistryListener
     */
    public void addModuleRegistryListener(
            ModuleRegistryListener newListener );

    /**
     * Remove a ModuleRegistry listener.
     *
     * @param oldListener the listener to remove
     * @see #addModuleRegistryListener
     */
    public void removeModuleRegistryListener(
            ModuleRegistryListener oldListener );
}
