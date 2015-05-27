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
 * A listener for ModuleRegistryEvents. Currently, Modules never go away,
 * so there is no notification method for that event.
 */
public interface ModuleRegistryListener
{
    /**
      * The registry has learned about a new Module.
      *
      * @param theEvent the event
      */
    public void newModuleAvailable(
            ModuleRegistryEvent theEvent );
}
