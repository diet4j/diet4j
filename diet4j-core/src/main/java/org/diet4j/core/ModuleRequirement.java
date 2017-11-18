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
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
      * @param requiredModuleArtifactId the artifactId of the required Module
      * @return the created ModuleRequirement
      */
    public static ModuleRequirement create(
            String requiredModuleArtifactId )
    {
        return new ModuleRequirement( null, requiredModuleArtifactId, null, false );
    }

    /**
      * Factory method.
      *
      * @param requiredModuleGroupId the groupId of the required Module
      * @param requiredModuleArtifactId the artifactId of the required Module
      * @return the created ModuleRequirement
      */
    public static ModuleRequirement create(
            String requiredModuleGroupId,
            String requiredModuleArtifactId )
    {
        return new ModuleRequirement( requiredModuleGroupId, requiredModuleArtifactId, null, false );
    }

    /**
      * Factory method.
      *
      * @param requiredModuleGroupId the groupId of the required Module
      * @param requiredModuleArtifactId the artifactId of the required Module
      * @param requiredModuleVersion the version of the required Module
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
            case 1:
                return new ModuleRequirement( null, parts[0], null, false );
            case 2:
                return new ModuleRequirement( parts[0].isEmpty() ? null : parts[0], parts[1], null, false );
            case 3:
                return new ModuleRequirement( parts[0].isEmpty() ? null : parts[0], parts[1], parts[2].isEmpty() ? null : parts[2], false );
            default:
                throw new ParseException( "Not a valid Module identifier: " + s, 0 );
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
        if( requiredModuleGroupId != null && !MAVEN_ID_REGEX.matcher( requiredModuleGroupId ).matches() ) {
            throw new IllegalArgumentException( "Required module groupId contains invalid characters: " + requiredModuleGroupId );
        }
        if( requiredModuleArtifactId == null || requiredModuleArtifactId.isEmpty() ) {
            throw new IllegalArgumentException( "Required module artifactId must not be null or an empty string" );
        }
        if( !MAVEN_ID_REGEX.matcher( requiredModuleArtifactId ).matches() ) {
            throw new IllegalArgumentException( "Required module artifactId contains invalid characters: " + requiredModuleArtifactId );
        }
        if( requiredModuleVersion != null && requiredModuleVersion.isEmpty() ) {
            throw new IllegalArgumentException( "Required module version must not be an empty string" );
        }
        theRequiredModuleGroupId    = requiredModuleGroupId;
        theRequiredModuleArtifactId = requiredModuleArtifactId;
        theIsOptional               = isOptional;

        parseAndSetMinMaxVersions( requiredModuleVersion );
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
     * Obtain  the version of the Module that we require. This returns the
     * uninterpreted version string.
     *
     * @return the version of the Module that we require
     */
    public final String getUninterpretedRequiredModuleVersion()
    {
        return theUninterpretedRequiredModuleVersion;
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
        if( theRequiredModuleGroupId != null && !theRequiredModuleGroupId.equals( candidate.getModuleGroupId()) ) {
            return false;
        }
        if( !theRequiredModuleArtifactId.equals( candidate.getModuleArtifactId()) ) {
            return false;
        }
        return matchesVersionRequirement( candidate.getModuleVersion() );
    }

    /**
     * Given an ordered set of ModuleMetas, find the matching versions.
     *
     * @param candidates the ModuleMeta candidates
     * @return the matched ModuleMetas
     */
    public ModuleMeta [] findVersionMatchesFrom(
            ModuleMeta [] candidates )
    {
        ModuleMeta [] ret = new ModuleMeta[ candidates.length ];

        int count = 0;
        for( int i=0 ; i<candidates.length ; ++i ) {
            if( matches( candidates[i] )) {
                ret[count++] = candidates[i];
            }
        }
        if( count < ret.length ) {
            ModuleMeta [] tmp = new ModuleMeta[count];
            System.arraycopy( ret, 0, tmp, 0, count );
            ret = tmp;
        }
        return ret;
    }

    /**
     * Determine whether the provided version String matches the version requirement
     * in this ModuleRequirement.
     *
     * @param version the version string
     * @return true or false
     */
    public boolean matchesVersionRequirement(
            String version )
    {
        // for speed purposes, get exact min version requirement out of the way
        if( theMinRequiredModuleVersionIsInclusive && version != null && version.equals( theMaxRequiredModuleVersion )) {
            return true;
        }

        ensureVersionsParsed();

        Object [][] parsedVersion = parseVersion( version );

        if( theParsedMinRequiredModuleVersion != null ) {
            int comp = compareParsedVersions( theParsedMinRequiredModuleVersion, parsedVersion );
            if( comp > 0 ) {
                return false;
            }
            if( comp == 0 && !theMinRequiredModuleVersionIsInclusive ) {
                return false;
            }
        }

        if( theParsedMaxRequiredModuleVersion != null ) {
            int comp = compareParsedVersions( theParsedMaxRequiredModuleVersion, parsedVersion );
            if( comp < 0 ) {
                return false;
            }
            if( comp == 0 && !theMaxRequiredModuleVersionIsInclusive ) {
                return false;
            }
        }

        return true;
    }

    /**
     * Compare two parsed versions.
     *
     * @param a the first parsed version
     * @param b the second parsed version
     * @return -1, 0 or 1 like strcmp()
     */
    protected int compareParsedVersions(
            Object [][] a,
            Object [][] b )
    {
        int max = Math.min( a.length, b.length );
        for( int i=0 ; i<max ; ++i ) {
            if( a[i] != null ) {
                if( b[i] != null ) {
                    int found = compareParsedVersionParts( a[i], b[i] );
                    if( found != 0 ) {
                        return found;
                    }
                } else {
                    return 1;
                }
            } else {
                if( b[i] != null ) {
                    return -1;

                } else {
                    // do nothing; should not really occur
                }
            }
        }
        if( a.length > max ) {
            return 1;
        } else {
            return -1;
        }
    }

    /**
     * Helper method to compare version parts.
     *
     * @param a first version part
     * @param b second version part
     * @return -1, 0 or 1 like strcmp()
     */
    protected int compareParsedVersionParts(
            Object [] a,
            Object [] b )
    {
        int max = Math.min( a.length, b.length );
        while( max > 0 && a[max-1] == null && b[max-1] == null ) {
            --max;
        }
        if( max == 0 ) {
            return 0;
        }
        for( int i=0 ; i<max ; ++i ) {
            if( a[i] != null ) {
                if( b[i] != null ) {
                    if( a[i] instanceof String ) {
                        if( b[i] instanceof String ) {
                            int found = ((String)a[i]).compareTo( (String)b[i] );
                            if( found != 0 ) {
                                return found;
                            }
                        } else {
                            return 1; // int before string
                        }
                    } else {
                        if( b[i] instanceof String ) {
                            return -1; // int before string
                        } else {
                            int found = ((Long)a[i]).compareTo( (Long)b[i] );
                            if( found != 0 ) {
                                return found;
                            }
                        }
                    }
                } else {
                    return 1;
                }
            } else {
                if( b[i] != null ) {
                    return -1;

                } else {
                    return 0;
                }
            }
        }

        if( a.length > max ) {
            if( a[max] != null ) {
                return 1; // so b[max] must not exist or be null
            } else {
                return -1;
            }
        } else {
            if( b[max] != null ) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    /**
     * Parse a version string, and set the properties on this instance accordingly.
     *
     * @param s the version string
     */
    protected void parseAndSetMinMaxVersions(
            String s )
    {
        theUninterpretedRequiredModuleVersion = s;

        theParsedMinRequiredModuleVersion = null;
        theParsedMaxRequiredModuleVersion = null;

        if( s == null ) {
            theMinRequiredModuleVersion = null;
            theMaxRequiredModuleVersion = null;
            theMinRequiredModuleVersionIsInclusive = true;
            theMaxRequiredModuleVersionIsInclusive = true;
        } else {
            Matcher m = MAVEN_VERSION_REGEX.matcher( s );
            if( m.matches() ) {
                theMinRequiredModuleVersion = m.group( 2 );
                theMaxRequiredModuleVersion = m.group( 3 );
                theMinRequiredModuleVersionIsInclusive = "[".equals( m.group( 1 ));
                theMaxRequiredModuleVersionIsInclusive = "]".equals( m.group( 4 ));
            } else {
                theMinRequiredModuleVersion = s;
                theMaxRequiredModuleVersion = null;
                theMinRequiredModuleVersionIsInclusive = true;
                theMaxRequiredModuleVersionIsInclusive = true;
            }
        }
    }

    /**
     * Ensure that the min and max versions have been parsed.
     */
    protected synchronized void ensureVersionsParsed()
    {
        if( theMinRequiredModuleVersion != null ) {
            if( theParsedMinRequiredModuleVersion == null ) {
                theParsedMinRequiredModuleVersion = parseVersion( theMinRequiredModuleVersion );
            }
        } else {
            theParsedMinRequiredModuleVersion = null;
        }
        if( theMaxRequiredModuleVersion != null ) {
            if( theParsedMaxRequiredModuleVersion == null ) {
                theParsedMaxRequiredModuleVersion = parseVersion( theMaxRequiredModuleVersion );
            }
        } else {
            theParsedMaxRequiredModuleVersion = null;
        }
    }

    /**
     * Take a version string and parse it into its components.
     *
     * @param v the version string
     * @return the components, left to right
     */
    protected Object [][] parseVersion(
            String v )
    {
        String []   major = v.split( "." );
        Object [][] ret   = new Object[ major.length ][];

        for( int i=0 ; i<major.length ; ++i ) {
            ret[i] = new Object[ major[i].length() ]; // over-allocated

            int count = 0;

            StringBuilder currentString = null; // once non-null, we know we are parsing a string
            long          currentLong   = -1;

            for( int j=0 ; j<major[i].length() ; ++j ) {
                char c = major[i].charAt( j );
                if( Character.isDigit( c )) {
                    if( currentString != null ) {
                        ret[i][count++] = currentString.toString();
                        currentString = null;

                        currentLong = Character.digit( c, 10 );
                    } else {
                        if( currentLong == -1 ) {
                            currentLong = Character.digit( c, 10 );
                        } else {
                            currentLong = currentLong * 10 + Character.digit( c, 10 );
                        }
                    }
                } else {
                    if( currentString != null ) {
                        currentString.append( c );
                    } else {
                        currentString = new StringBuilder();
                        currentString.append( c );
                        ret[i][count++] = currentLong;
                        currentLong = -1;
                    }
                }
            }
            if( currentString != null ) {
                ret[i][count++] = currentString.toString();
            } else if( currentLong != -1 ) {
                ret[i][count++] = currentLong;
            }
        }
        return ret;
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
        if( theRequiredModuleGroupId != null ) {
            buf.append( theRequiredModuleGroupId );
        }
        buf.append( ":" );
        buf.append( theRequiredModuleArtifactId );
        buf.append( ":" );
        if( theMinRequiredModuleVersion != null && theMinRequiredModuleVersionIsInclusive && theMaxRequiredModuleVersion == null ) {
            // short representation
            buf.append( theMinRequiredModuleVersion );

        } else if( theMinRequiredModuleVersion != null || theMaxRequiredModuleVersion != null ) {
            // interval representation
            if( theMinRequiredModuleVersionIsInclusive ) {
                buf.append( "[" );
            } else {
                buf.append( "(" );
            }
            if( theMinRequiredModuleVersion != null ) {
                buf.append( theMinRequiredModuleVersion );
            }
            buf.append( "," );
            if( theMaxRequiredModuleVersion != null ) {
                buf.append( theMaxRequiredModuleVersion );
            }
            if( theMaxRequiredModuleVersionIsInclusive ) {
                buf.append( "]" );
            } else {
                buf.append( ")" );
            }
        }
        return buf.toString();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(
            Object obj )
    {
        if( this == obj ) {
            return true;
        }
        if( obj == null ) {
            return false;
        }
        if( !( obj instanceof ModuleRequirement )) {
            return false;
        }
        final ModuleRequirement other = (ModuleRequirement) obj;
        if( !Objects.equals( this.theRequiredModuleGroupId, other.theRequiredModuleGroupId )) {
            return false;
        }
        if( !Objects.equals( this.theRequiredModuleArtifactId, other.theRequiredModuleArtifactId )) {
            return false;
        }
        if( !Objects.equals( this.theMinRequiredModuleVersion, other.theMinRequiredModuleVersion )) {
            return false;
        }
        if( !Objects.equals( this.theMaxRequiredModuleVersion, other.theMaxRequiredModuleVersion )) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 31 * hash + Objects.hashCode(this.theRequiredModuleGroupId);
        hash = 31 * hash + Objects.hashCode(this.theRequiredModuleArtifactId);
        hash = 31 * hash + Objects.hashCode(this.theMinRequiredModuleVersion);
        hash = 31 * hash + Objects.hashCode(this.theMaxRequiredModuleVersion);
        return hash;
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
     * The uninterpreted version string of the ModuleRequirement.
     */
    protected String theUninterpretedRequiredModuleVersion;

    /**
     * The minimum version of the required Module.
     */
    protected String theMinRequiredModuleVersion;

    /**
     * Is the minimum version inclusive.
     */
    protected boolean theMinRequiredModuleVersionIsInclusive;

    /**
     * Parsed form of the minimum version of the required Module.
     * Calculated on demand.
     */
    protected Object [][] theParsedMinRequiredModuleVersion;

    /**
     * The maximum version of the required Module.
     */
    protected String theMaxRequiredModuleVersion;

    /**
     * Is the maximum version inclusive.
     */
    protected boolean theMaxRequiredModuleVersionIsInclusive;

    /**
     * Parsed form of the maximum version of the required Module.
     * Calculated on demand.
     */
    protected Object [][] theParsedMaxRequiredModuleVersion;

    /**
     * Is this dependency optional.
     */
    protected boolean theIsOptional;

    /**
     * The regex defining Maven version expressions.
     */
    public static Pattern MAVEN_VERSION_REGEX = Pattern.compile(
            "([\\[\\(])([^,\\[\\]\\(\\)]*),([^,\\[\\]\\(\\)]*)(\\]\\))" );

    /**
     * The regex that groupId and artifactId strings must match.
     */
    public static Pattern MAVEN_ID_REGEX = Pattern.compile(
            "[-a-zA-Z0-9._]+");
}
