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
 * Extends NoClassDefFoundError with the ModuleClassLoader that attempted to find the Class.
 * Simplifies debugging.
 */
public class NoClassDefFoundWithClassLoaderError
        extends
            NoClassDefFoundError
{
    private static final long serialVersionUID = 1L; // helps with serialization

    /**
     * Constructs a <code>NoClassDefFoundError</code> with the specified
     * detail message.
     *
     * @param s   the detail message.
     * @param loader the ModuleClassLoader that was used
     */
    public NoClassDefFoundWithClassLoaderError(
            String            s,
            ModuleClassLoader loader )
    {
        super( s );

        theClassLoader = loader;
    }

    /**
     * Convert to String representation, for debugging.
     *
     * @return String representation
     */
    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append( super.toString() );
        buf.append( ": ClassLoader of Module " );
        if( theClassLoader.getModule() != null ) {
            buf.append( theClassLoader.getModule().toString() );
        } else {
            buf.append( "? (" ).append( theClassLoader.getClass().getName() ).append( ")" );
        }

        return buf.toString();
    }

    /**
     * The ClassLoader through which loading failed.
     */
    protected ModuleClassLoader theClassLoader;
}
