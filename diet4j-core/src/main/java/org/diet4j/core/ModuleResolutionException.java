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
 * This Exception indicates that a Module cannot be resolved.
 */
public class ModuleResolutionException
        extends
            ModuleException
{
    private static final long serialVersionUID = 1L; // helps with serialization

    /**
      * Constructor.
      *
      * @param meta the ModuleMeta whose ModuleRequirement could not be met
      * @param req the ModuleRequirement that could not be met
      * @param cause the Throwable that caused this Exception, if any
      */
    public ModuleResolutionException(
             ModuleMeta        meta,
             ModuleRequirement req,
             Throwable         cause )
    {
        super( meta, cause );

        theRequirement = req;
    }

    /**
     * Obtain the ModuleRequirement that could not be met.
     * 
     * @return the ModuleRequirement
     */
    public ModuleRequirement getModuleRequirement()
    {
        return theRequirement;
    }

    /**
     * Returns the detail message string of this Throwable.
     *
     * @return  the detail message string of this <tt>Throwable</tt> instance
     *          (which may be <tt>null</tt>).
     */
    @Override
    public String getMessage()
    {
        StringBuilder buf = new StringBuilder( 100 ); // fudge
        buf.append( "Could not resolve ModuleRequirement " );
        if( theRequirement != null ) {
            buf.append( theRequirement.toString() );
        } else {
            buf.append( "null" );
        }

        buf.append( " within Module " );
        if( theModuleMeta != null ) {
            buf.append( theModuleMeta.toString() );
        } else {
            buf.append( "null" );
        }

        return buf.toString();
    }

    /**
     * The ModuleRequirement that could not be met.
     */
    protected ModuleRequirement theRequirement;
}
