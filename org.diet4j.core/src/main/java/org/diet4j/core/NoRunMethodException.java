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
 * This exception indicates that a Module's specified run method was invoked, but
 * that the method could not be found.
 */
public class NoRunMethodException
        extends
            ModuleException
{
    private static final long serialVersionUID = 1L; // helps with serialization

    /**
      * Constructor.
      *
      * @param meta the ModuleMeta whose Module we wanted to run
      * @param runClassName the name of the class whose run method we wanted to execute, or null if not known
      * @param runMethodName the name of the method within the runClassName that we wanted to execute, or null if not known
      * @param cause the Throwable that caused this Exception
      */
    public NoRunMethodException(
             ModuleMeta meta,
             String     runClassName,
             String     runMethodName,
             Throwable  cause )
    {
        super( meta, cause );

        theRunClassName  = runClassName;
        theRunMethodName = runMethodName;
    }

    /**
     * For debugging.
     *
     * @return this object in printable format
     */
    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder( 100 );
        buf.append( "NoRunMethodException: Module: " );
        if( theModuleMeta != null ) {
            buf.append( theModuleMeta.toString() );
        } else {
            buf.append( "null" );
        }

        buf.append( ", cannot find or access run method " );
        buf.append( theRunMethodName );
        buf.append( " in class " );
        buf.append( theRunClassName );
        return buf.toString();
    }

    /**
     * Name of the class whose run method we were trying to run.
     */
    protected String theRunClassName;

    /**
     * Name of the method within theRunClassName that we were trying to run.
     */
    protected String theRunMethodName;
}
