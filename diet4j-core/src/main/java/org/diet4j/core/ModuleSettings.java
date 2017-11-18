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

import java.util.Iterator;
import java.util.Map;

/**
 * Settings for a particular Module. The settings are usually provided by
 * the BootLoader using data from its environment.
 */
public class ModuleSettings
{
    /**
     * Create a new ModuleSettings object.
     *
     * @param map the content
     * @return the ModuleSettings
     */
    public static ModuleSettings create(
            Map<String,String> map )
    {
        return new ModuleSettings( map );
    }

    /**
     * Constructor for subclasses only.
     *
     * @param map the content
     */
    protected ModuleSettings(
            Map<String,String> map )
    {
        theMap = map;
    }

    /**
     * Obtain iterator over all keys in the settings.
     *
     * @return the Iterator over the keys
     */
    public Iterable<String> keysIterator()
    {
        if( theMap != null ) {
            return theMap.keySet();
        } else {
            return EMPTY;
        }
    }

    /**
     * Obtain a a particular setting as string.
     *
     * @param key the key for the setting
     * @return the value for the setting, or null
     */
    public String getString(
            String key )
    {
        return getString( key, null );
    }

    /**
     * Obtain a a particular setting as string.
     *
     * @param key the key for the setting
     * @param defaultValue the default value, if the setting does not exist
     * @return the value for the setting, or the defaultValue
     */
    public String getString(
            String key,
            String defaultValue )
    {
        if( theMap == null ) {
            return defaultValue;
        }
        String found = theMap.get( key );
        if( found != null ) {
            return found;
        } else {
            return defaultValue;
        }
    }

    /**
     * Obtain a a particular setting as integer.
     *
     * @param key the key for the setting
     * @return the value for the setting, or null
     */
    public Integer getInteger(
            String key )
    {
        return getInteger( key, null );
    }

    /**
     * Obtain a a particular setting as string.
     *
     * @param key the key for the setting
     * @param defaultValue the default value, if the setting does not exist
     * @return the value for the setting, or the defaultValue
     */
    public Integer getInteger(
            String  key,
            Integer defaultValue )
    {
        if( theMap == null ) {
            return defaultValue;
        }
        String found = theMap.get( key );
        if( found != null ) {
            return Integer.parseInt( found );
        } else {
            return defaultValue;
        }
    }

    /**
     * Determine whether a particular key exists.
     *
     * @param key the key for the setting
     * @return true or false
     */
    public boolean containsKey(
            String key )
    {
        if( theMap != null ) {
            return theMap.containsKey( key );
        } else {
            return false;
        }
    }

    /**
     * Internal map.
     */
    protected Map<String,String> theMap;

    /**
     * An iterable that always returns nothing.
     */
    protected static final Iterable<String> EMPTY = () -> new Iterator<String>() {
        @Override
        public String next() {
            return null;
        }
        @Override
        public boolean hasNext() {
            return false;
        }
    };
}
