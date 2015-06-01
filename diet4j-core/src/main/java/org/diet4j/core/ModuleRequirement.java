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
import java.text.ParseException;

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
      * @param requiredModuleGroupId the groupId of the required Module
      * @param requiredModuleArtifactId the artifactId of the required Module
      * @return the created ModuleRequirement
      */
    public static ModuleRequirement create(
            String  requiredModuleGroupId,
            String  requiredModuleArtifactId )
    {
        return new ModuleRequirement( requiredModuleGroupId, requiredModuleArtifactId, null, false );
    }

    /**
      * Factory method.
      *
      * @param requiredModuleGroupId the groupId of the required Module
      * @param requiredModuleArtifactId the artifactId of the required Module
      * @param isOptional if true, this ModuleRequirement is optional
      * @return the created ModuleRequirement
      */
    public static ModuleRequirement create(
            String  requiredModuleGroupId,
            String  requiredModuleArtifactId,
            boolean isOptional )
    {
        return new ModuleRequirement( requiredModuleGroupId, requiredModuleArtifactId, null, isOptional );
    }

    /**
      * Factory method.
      *
      * @param requiredModuleGroupId the groupId of the required Module
      * @param requiredModuleArtifactId the artifactId of the required Module
      * @param requiredModuleVersion the version of the required Module, null if any
      * @return the created ModuleRequirement
      */
    public static ModuleRequirement create(
            String requiredModuleGroupId,
            String requiredModuleArtifactId,
            String requiredModuleVersion )
    {
        return new ModuleRequirement( requiredModuleGroupId, requiredModuleArtifactId, requiredModuleVersion, false );
    }

    /**
      * Factory method.
      *
      * @param requiredModuleGroupId the groupId of the required Module
      * @param requiredModuleArtifactId the artifactId of the required Module
      * @param requiredModuleVersion the version of the required Module, null if any
      * @param isOptional if true, this ModuleRequirement is optional
      * @return the created ModuleRequirement
      */
    public static ModuleRequirement create(
            String  requiredModuleGroupId,
            String  requiredModuleArtifactId,
            String  requiredModuleVersion,
            boolean isOptional )
    {
        return new ModuleRequirement( requiredModuleGroupId, requiredModuleArtifactId, requiredModuleVersion, isOptional );
    }
    
    /**
      * Factory method using a String representation corresponding to toString().
      * 
      * @param s the String to be parsed
      * @return the created ModuleRequirement
      * @throws ParseException thrown if the provided s did have an invalid syntax
      */
    public static ModuleRequirement parse(
            String s )
        throws
            ParseException
    {
        String [] parts = s.split( ":" );
        switch( parts.length ) {
            case 2:
                return new ModuleRequirement( parts[0], parts[1], null, false );
            case 3:
                return new ModuleRequirement( parts[0], parts[1], parts[2], false );
            default:
                throw new ParseException( "Not a valid Module identifier, needs one or two colons: " + s, 0 );
        }
    }

    /**
      * Construct one.
      *
      * @param requiredModuleGroupId the groupId of the required Module
      * @param requiredModuleArtifactId the artifactId of the required Module
      * @param requiredModuleVersion the version of the required Module, null if any
      * @param isOptional if true, this ModuleRequirement is optional
      */
    protected ModuleRequirement(
            String  requiredModuleGroupId,
            String  requiredModuleArtifactId,
            String  requiredModuleVersion,
            boolean isOptional )
    {
        if( requiredModuleGroupId == null || requiredModuleGroupId.isEmpty() ) {
            throw new IllegalArgumentException( "Required module groupId must not be null or an empty string" );
        }
        if( requiredModuleArtifactId == null || requiredModuleArtifactId.isEmpty() ) {
            throw new IllegalArgumentException( "Required module artifactId must not be null or an empty string" );
        }
        if( requiredModuleVersion != null && requiredModuleVersion.isEmpty() ) {
            throw new IllegalArgumentException( "Required module version must not be an empty string" );
        }
        theRequiredModuleGroupId    = requiredModuleGroupId;
        theRequiredModuleArtifactId = requiredModuleArtifactId;
        theRequiredModuleVersion    = requiredModuleVersion;
        theIsOptional               = isOptional;
        
    }

    /**
      * Obtain the groupId of the Module that we require.
      *
      * @return the groupId of the Module that we require
      */
    public final String getRequiredModuleGroupId()
    {
        return theRequiredModuleGroupId;
    }

    /**
      * Obtain the artifactId of the Module that we require.
      *
      * @return the artifactId of the Module that we require
      */
    public final String getRequiredModuleArtifactId()
    {
        return theRequiredModuleArtifactId;
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
        if( !theRequiredModuleGroupId.equals( candidate.getModuleGroupId()) ) {
            return false;
        }
        if( !theRequiredModuleArtifactId.equals( candidate.getModuleArtifactId()) ) {
            return false;
        }
        if( theRequiredModuleVersion == null ) {
            return true;
        }
        return theRequiredModuleVersion.equals( candidate.getModuleVersion() );
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
        buf.append( theRequiredModuleGroupId );
        buf.append( ":" );
        buf.append( theRequiredModuleArtifactId );
        buf.append( ":" );
        if( theRequiredModuleVersion != null ) {
            buf.append( theRequiredModuleVersion );
        } else {
            buf.append( "?" );
        }
        return buf.toString();
    }

    /**
     * The groupId of the required Module.
     */
    protected String theRequiredModuleGroupId;

    /**
     * The artifactId of the required Module.
     */
    protected String theRequiredModuleArtifactId;

    /**
     * The version of the required Module.
     */
    protected String theRequiredModuleVersion;

    /**
     * Is this dependency optional.
     */
    protected boolean theIsOptional;
}
