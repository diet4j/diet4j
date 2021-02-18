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

import java.util.Map;
import java.util.Set;

/**
 * Captures one capability of a Module. This is a rather abstract
 * concept and can be used in a variety of ways by developers.
 */
public class ModuleCapability
{
    /**
     * Constructor.
     *
     * @param name name of the capability
     * @param properties the properties of the capability, if any
     */
    public ModuleCapability(
            String             name,
            Map<String,String> properties )
    {
        theName       = name;
        theProperties = properties;
    }

    /**
     * Obtain the name of the capability.
     *
     * @return the name of the capability
     */
    public final String getName()
    {
        return theName;
    }

    /**
     * Obtain a value for one of the properties of this capability
     *
     * @param propertyName name of the property
     * @return the value, or null
     */
    public final String getProperty(
            String propertyName )
    {
        return theProperties.get( propertyName );
    }

    /**
     * Obtain the names of all the properties of this capability.
     *
     * @return set of names
     */
    public final Set<String> getPropertyNames()
    {
        return theProperties.keySet();
    }

    /**
     * The name of the capability.
     */
    protected final String theName;

    /**
     * The properties of the capability.
     */
    protected final Map<String,String> theProperties;

    /**
     * Name of the property that expresses match quality.
     */
    public static final String MATCH_QUALITY_PROPERTY = "MatchQuality";
}
