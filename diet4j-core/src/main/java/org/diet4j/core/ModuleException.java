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
 * The abstract superclass of all diet4j-related Exceptions.
 */
public abstract class ModuleException
        extends
            Exception
{
    private static final long serialVersionUID = 1L; // helps with serialization

    /**
     * Constructor for subclasses only.
     *
     * @param meta the ModuleMeta of the Module that caused this Exception
     * @param cause the Throwable that caused this Exception
     */
    protected ModuleException(
            ModuleMeta meta,
            Throwable  cause )
    {
        super( null, cause ); // do not create default message

        theModuleMeta = meta;
    }

    /**
     * Constructor for subclasses only.
     *
     * @param meta the ModuleMeta of the Module that caused this Exception
     * @param msg the message
     * @param cause the Throwable that caused this Exception
     */
    protected ModuleException(
            ModuleMeta meta,
            String     msg,
            Throwable  cause )
    {
        super( msg, cause );

        theModuleMeta = meta;
    }
    
    /**
     * Obtain the ModuleMeta whose Module had a problem.
     * 
     * @return the ModuleMeta
     */
    public ModuleMeta getModuleMeta()
    {
        return theModuleMeta;
    }

    /**
     * The ModuleMeta whose Module had a problem.
     */
    protected ModuleMeta theModuleMeta;
}
