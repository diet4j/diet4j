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
 * A minimal, resource-efficient Map implementation for ModuleMetas.
 */
public class MiniModuleMetaMap
{
    /**
     * Constructor with default initial size.
     */
    public MiniModuleMetaMap()
    {
        this( DEFAULT_SIZE );
    }

    /**
     * Constructor with specified initial size.
     *
     * @param size the initial size
     */
    public MiniModuleMetaMap(
            int size )
    {
        theData  = new Object[size*2];
        theIndex = 0;
    }

    /**
     * Add a key-value mapping to the map. Replace if exists already.
     *
     * @param key the key
     * @param value the value
     * @return the previous value, or null
     */
    public synchronized ModuleMeta [] put(
            String        key,
            ModuleMeta [] value )
    {
        for( int i=0 ; i<theIndex ; i+=2 ) {
            if( key.equals( theData[i] )) {
                ModuleMeta [] ret = (ModuleMeta []) theData[i+1];
                theData[i+1] = value;
                return ret;
            }
        }
        if( theIndex == theData.length ) {
            Object [] oldData = theData;
            theData = new Object[ oldData.length * 2 ];
            System.arraycopy( oldData, 0, theData, 0, oldData.length );
        }
        theData[theIndex++] = key;
        theData[theIndex++] = value;

        return null;
    }

    /**
     * Obtain the value for a given key.
     *
     * @param key the key
     * @return the value, or null
     */
    public synchronized ModuleMeta [] get(
            String key )
    {
        for( int i=0 ; i<theIndex ; i+=2 ) {
            if( key.equals( theData[i] )) {
                return (ModuleMeta []) theData[i+1];
            }
        }
        return null;
    }

    /**
     * Obtain all keys in the map.
     *
     * @return all keys
     */
    public synchronized String [] allKeys()
    {
        String [] ret = new String[ theIndex/2 ];
        for( int i=0 ; i<ret.length ; ++i ) {
            ret[i] = (String) theData[i*2];
        }
        return ret;
    }

    /**
     * Obtain all values in the map, regardless of key.
     *
     * @return all values
     */
    public synchronized ModuleMeta [] allValues()
    {
        int count=0;
        for( int i=0 ; i<theIndex ; i+=2 ) {
            count += ((ModuleMeta []) theData[i+1]).length;
        }
        ModuleMeta [] ret = new ModuleMeta[ count ];
        int index = 0;
        for( int i=0 ; i<theIndex ; i+=2 ) {
            for( int j=0 ; j<((ModuleMeta []) theData[i+1]).length ; ++j ) {
                ret[index++] = ((ModuleMeta []) theData[i+1])[j];
            }
        }
        return ret;
    }

    /**
     * Contains the map's data. Keys are at even indices,
     * values at the element following the key.
     */
    protected Object [] theData;

    /**
     * The index of the next free element in the array.
     */
    protected int theIndex;

    /**
     * The initial default size for the MiniModuleMetaMap.
     */
    public static final int DEFAULT_SIZE = 2;
}
