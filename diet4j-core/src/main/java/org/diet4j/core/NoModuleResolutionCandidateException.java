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
 * This Exception indicates that a ModuleRequirement could not be resolved into
 * a single resolution candidate.
 */
public class NoModuleResolutionCandidateException
        extends
            ModuleException
{
    private static final long serialVersionUID = 1L; // helps with serialization

    /**
      * Constructor.
      *
      * @param req the ModuleRequirement that could not be met
      */
    public NoModuleResolutionCandidateException(
             ModuleRequirement req )
    {
        super( null, null );

        theRequirement = req;
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
        return buf.toString();
    }

    /**
     * The ModuleRequirement that could not be met unambiguously.
     */
    protected ModuleRequirement theRequirement;
}
