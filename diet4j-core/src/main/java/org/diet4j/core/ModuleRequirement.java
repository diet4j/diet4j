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

import java.io.Serializable;

/**
 * This collects all information needed to find a Module.
 */
public class ModuleRequirement
        implements
            Serializable
{
    private static final long serialVersionUID = 1L; // helps with serialization

    /**
      * Factory method.
      *
      * @param requiredModuleName the name of the required Module, in any version
      * @return the created ModuleRequirement
      */
    public static ModuleRequirement create1(
            String requiredModuleName )
    {
        return new ModuleRequirement( requiredModuleName, null, false );
    }

    /**
      * Factory method.
      *
      * @param requiredModuleName the name of the required Module, in any version
      * @param isOptional if true, this ModuleRequirement is optional
      * @return the created ModuleRequirement
      */
    public static ModuleRequirement create1(
            String  requiredModuleName,
            boolean isOptional )
    {
        return new ModuleRequirement( requiredModuleName, null, isOptional );
    }

    /**
      * Factory method.
      *
      * @param requiredModuleName the name of the required Module
      * @param requiredModuleVersion the version of the required Module, null if any
      * @return the created ModuleRequirement
      */
    public static ModuleRequirement create1(
            String  requiredModuleName,
            String  requiredModuleVersion )
    {
        return new ModuleRequirement( requiredModuleName, requiredModuleVersion, false );
    }

    /**
      * Factory method.
      *
      * @param requiredModuleName the name of the required Module
      * @param requiredModuleVersion the version of the required Module, null if any
      * @param isOptional if true, this ModuleRequirement is optional
      * @return the created ModuleRequirement
      */
    public static ModuleRequirement create1(
            String  requiredModuleName,
            String  requiredModuleVersion,
            boolean isOptional )
    {
        return new ModuleRequirement( requiredModuleName, requiredModuleVersion, isOptional );
    }
    
    /**
      * Factory method using a String representation corresponding to toString().
      * 
      * @param s the String to be parsed
      * @return the created ModuleRequirement
      */
    public static ModuleRequirement parse(
            String s )
    {
        int colon = s.indexOf( ":" );
        if( colon >= 0 ) {
            return new ModuleRequirement( s.substring( 0, colon ), s.substring( colon+1 ), false );
        } else {
            return new ModuleRequirement( s, null, false );
        }
    }

    /**
      * Construct one.
      *
      * @param requiredModuleName the name of the required Module
      * @param requiredModuleVersion the version of the required Module, null if any
      * @param isOptional if true, this ModuleRequirement is optional
      */
    protected ModuleRequirement(
            String  requiredModuleName,
            String  requiredModuleVersion,
            boolean isOptional )
    {
        if( requiredModuleName == null || requiredModuleName.isEmpty() ) {
            throw new IllegalArgumentException( "Required module name must not be null or an empty string" );
        }
        if( requiredModuleVersion != null && requiredModuleVersion.isEmpty() ) {
            throw new IllegalArgumentException( "Required module version must not be an empty string" );
        }
        theRequiredModuleName    = requiredModuleName;
        theRequiredModuleVersion = requiredModuleVersion;
        theIsOptional            = isOptional;
        
    }

    /**
      * Obtain the name of the Module that we require.
      *
      * @return the name of the Module that we require
      */
    public final String getRequiredModuleName()
    {
        return theRequiredModuleName;
    }

    /**
     * Obtain the version of the Module that we required.
     *
     * @return the version of the Module that we require
     */
    public final String getRequiredModuleVersion()
    {
        return theRequiredModuleVersion;
    }

    /**
     * Determine whether this ModuleRequirement is optional.
     * 
     * @return true, if the ModuleRequirement is optional
     */
    public final boolean isOptional()
    {
        return theIsOptional;
    }

    /**
     * Determine whether a candidate ModuleMeta meets this ModuleRequirement.
     *
     * @param candidate the candidate ModuleMeta
     * @return true if the ModuleMeta matches this ModuleRequirement
     */
    public boolean matches(
            ModuleMeta candidate )
    {
        if( theRequiredModuleName != null ) {
            if( !theRequiredModuleName.equals( candidate.getModuleName() ) ) {
                return false;
            }
            if( theRequiredModuleVersion == null ) {
                return true;
            }
            if( candidate.getModuleVersion() == null ) {
                return true;
            }
            return theRequiredModuleVersion.equals( candidate.getModuleVersion() );
        }
        return false;
    }

    /**
     * Obtain a string representation of this object, for debugging purposes.
     *
     * @return string representation of this object
     */
    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append( theRequiredModuleName );
        buf.append( ":" );
        if( theRequiredModuleVersion != null ) {
            buf.append( theRequiredModuleVersion );
        } else {
            buf.append( "?" );
        }
        return buf.toString();
    }

    /**
     * The name of the required Module.
     */
    protected String theRequiredModuleName;

    /**
     * The version of the required Module.
     */
    protected String theRequiredModuleVersion;

    /**
     * Is this dependency optional.
     */
    protected boolean theIsOptional;
}
