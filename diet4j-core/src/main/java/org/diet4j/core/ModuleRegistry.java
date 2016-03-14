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

import java.util.Set;
import java.util.regex.Pattern;

/**
 * A ModuleRegistry is a place where Modules can be found and resolved.
 * Typically, an application instantiates and uses only a single ModuleRegistry.
 */
public abstract class ModuleRegistry
{
    /**
     * Obtain the singleton instance of this class, or a subclass.
     * 
     * @return the singleton instance, or null if not set
     */
    public static ModuleRegistry getSingleton()
    {
        return theSingleton;
    }

    /**
     * Determine the ModuleMetas that are candidates to resolve a Module dependency, based
     * on the knowledge of this ModuleRegistry.
     *
     * @param req the ModuleRequirement that we attempt to resolve
     * @return the ModuleMetas that are candidates for the resolution of the ModuleRequirement, in sequence from
     *         best matched to least matched if the ModuleRegistry makes such distinctions
     */
    public abstract ModuleMeta [] determineResolutionCandidates(
            ModuleRequirement req );

    /**
     * Convenience method to determine the one and only ModuleMeta that is the only candidate to resolve a Module
     * dependency, based on the knowledge of this ModuleRegistry. If there is more or less than one
     * match, thrown an Exception.
     * 
     * @param req the ModuleRequirement that we attempt to resolve
     * @return the ModuleMeta that is the only candidate for the resolution of the ModuleRequirement
     * @throws NoModuleResolutionCandidateException thrown if no ModuleMeta was found
     * @throws ModuleResolutionCandidateNotUniqueException thrown if there were more than one ModuleMeta found
     */
    public abstract ModuleMeta determineSingleResolutionCandidate(
            ModuleRequirement req )
        throws
            NoModuleResolutionCandidateException,
            ModuleResolutionCandidateNotUniqueException;

    /**
     * Recursively resolve this ModuleMeta into a Module.
     *
     * @param meta the ModuleMeta to resolve
     * @return the resolved Module.
     * @throws ModuleNotFoundException thrown if the Module could not be found
     * @throws ModuleResolutionException thrown if the Module could not be resolved
     */
    public abstract Module resolve(
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
    public abstract Module resolve(
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
    public abstract Module getResolutionOf(
            ModuleMeta meta );

    /**
     * Given a Module, this allows us to determine which other Modules it depends on.
     * This is similar to the result of determineResolutionCandidates(), except that
     * this returns one resolved Module for each dependency, not a set of ModuleMetas
     * that may or may not be resolvable.
     *
     * @param theModule the Module whose dependencies we want to determine
     * @return the set of Modules that this Module depends on
     * @see #determineUses
     */
    public abstract Module [] determineDependencies(
            Module theModule );

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
    public abstract Module [] determineUses(
            Module theModule );
    
    /**
     * ModuleRegistries can also acts as a factory for the Modules' ClassLoaders.
     *
     * @param module the Module for which to create a ClassLoader
     * @param parentClassLoader the ClassLoader to use as the parent ClassLoader
     * @return the ClassLoader to use with the Module
     */
    protected abstract ClassLoader createClassLoader(
            Module      module,
            ClassLoader parentClassLoader );

    /**
     * Obtain the set of Module names currently contained in the registry.
     * 
     * @return the set of Module names
     */
    public abstract Set<String> nameSet();

    /**
     * Obtain the set of Module names currently contained in the registry that match a
     * naming pattern.
     * 
     * @param regex the regular expression
     * @return the set of Module names
     */
    public abstract Set<String> nameSet(
            Pattern regex );

    /**
     * Add a ModuleRegistry listener to be notified when new Modules become available etc.
     *
     * @param newListener the new listener to add
     * @see #removeModuleRegistryListener
     */
    public abstract void addModuleRegistryListener(
            ModuleRegistryListener newListener );

    /**
     * Remove a ModuleRegistry listener.
     *
     * @param oldListener the listener to remove
     * @see #addModuleRegistryListener
     */
    public abstract void removeModuleRegistryListener(
            ModuleRegistryListener oldListener );
    
    /**
     * The singleton instance of this class. Must be set by subclasses.
     */
    protected static ModuleRegistry theSingleton;
}
