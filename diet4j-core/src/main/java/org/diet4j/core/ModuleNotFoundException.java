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
 * This Exception is thrown if we cannot find a Module whose ModuleMeta we have.
 * It may also be thrown if a Module is missing resources (such as a JAR file).
 */
public class ModuleNotFoundException
        extends
            ModuleException
{
    private static final long serialVersionUID = 1L; // helps with serialization

    /**
      * Constructor.
      *
      * @param meta the ModuleMeta whose Module we could not find
      * @param cause the cause of this ModuleNotFoundException (e.g. IOException)
      */
    public ModuleNotFoundException(
            ModuleMeta meta,
            Throwable  cause )
    {
        super( meta, cause );
    }

    /**
     * For debugging.
     *
     * @return string representation of this object
     */
    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder( 100 ); // fudge
        buf.append( "ModuleNotFoundException: could not resolve Module " );

        buf.append( theModuleMeta.toString() );
        buf.append( ", was looking for JAR file: " );
        buf.append( theModuleMeta.getProvidesJar() );

        return buf.toString();
    }
}
