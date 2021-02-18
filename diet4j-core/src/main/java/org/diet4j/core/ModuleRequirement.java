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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
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
                return new ModuleRequirement(
                        null,
                        parts[0],
                        null,
                        false,
                        null );
            case 2:
                return new ModuleRequirement(
                        parts[0].isEmpty() ? null : parts[0],
                        parts[1],
                        null,
                        false,
                        null );
            case 3:
                return new ModuleRequirement(
                        parts[0].isEmpty() ? null : parts[0],
                        parts[1],
                        parts[2].isEmpty() ? null : parts[2],
                        false,
                        null );
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
      * @param requiredCapabilities names of the required ModuleCapabilities, may be empty
      */
    protected ModuleRequirement(
            String    requiredModuleGroupId,
            String    requiredModuleArtifactId,
            String    requiredModuleVersion,
            boolean   isOptional,
            String [] requiredCapabilities )
    {
        if( requiredModuleGroupId != null && !MAVEN_ID_REGEX.matcher( requiredModuleGroupId ).matches() ) {
            throw new IllegalArgumentException( "Required module groupId contains invalid characters: " + requiredModuleGroupId );
        }
        if( requiredModuleArtifactId == null ) {
            if( requiredCapabilities.length == 0 ) {
                throw new IllegalArgumentException( "Must provide at least either a required module artifactId or required capabilities" );
            }
        } else {
            if( requiredModuleArtifactId.isEmpty() ) {
                throw new IllegalArgumentException( "Required module artifactId must not be null or an empty string" );
            }
            if( !MAVEN_ID_REGEX.matcher( requiredModuleArtifactId ).matches() ) {
                throw new IllegalArgumentException( "Required module artifactId contains invalid characters: " + requiredModuleArtifactId );
            }
        }
        if( requiredModuleVersion != null && requiredModuleVersion.isEmpty() ) {
            throw new IllegalArgumentException( "Required module version must not be an empty string" );
        }
        theRequiredModuleGroupId    = requiredModuleGroupId;
        theRequiredModuleArtifactId = requiredModuleArtifactId;
        theIsOptional               = isOptional;

        parseAndSetMinMaxVersions( requiredModuleVersion );

        theRequiredCapabilities = requiredCapabilities;
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
     * Determine the ModuleCapabilities provided by this Module.
     *
     * @return the ModuleCapabilities
     */
    public String [] getRequiredModuleCapabilities()
    {
        return theRequiredCapabilities;
    }

    /**
     * Determine how well a candidate ModuleMeta meets this ModuleRequirement.
     *
     * @param candidate the candidate ModuleMeta
     * @return 0: does not match: 1: perfect match, 2: new version
     */
    public double matches(
            ModuleMeta candidate )
    {
        if( !matchesCoordinates( candidate )) {
            return 0.0d;
        }

        double score = 1.0d;
        if( theRequiredCapabilities != null ) {
            for( int i=0 ; i<theRequiredCapabilities.length ; ++i ) {
                double match = candidate.meetsCapability( theRequiredCapabilities[i] );
                if( match == 0.0d ) {
                    return 0;
                } else {
                    score *= match;
                }
            }
        }
        return score;
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
        ModuleMeta [] perfect = new ModuleMeta[ candidates.length ];
        ModuleMeta [] later   = new ModuleMeta[ candidates.length ];

        int perfectCount = 0;
        int laterCount   = 0;
        for( int i=0 ; i<candidates.length ; ++i ) {
            switch( matchesVersion( candidates[i] )) {
                case 1:
                    perfect[perfectCount++] = candidates[i];
                    break;
                case 2:
                    later[laterCount++] = candidates[i];
                    break;
            }
        }
        ModuleMeta [] ret = new ModuleMeta[ perfectCount + laterCount ];
        System.arraycopy( perfect, 0, ret, 0, perfectCount );
        System.arraycopy( later, 0, ret, perfectCount, laterCount );

        return ret;
    }

    /**
     * Determine whether the provide ModuleMeta meets the version requirements (but no other requirements).
     *
     * @param candidate the candidate ModuleMeta
     * @return 0: does not match: 1: matches perfectly, 2: new version
     */
    public int matchesVersion(
            ModuleMeta candidate )
    {
        return matchesVersion( candidate.getModuleVersion() );
    }

    /**
     * Determine whether the provided version String meets the version requirement in this ModuleRequirement.
     *
     * @param version the version string
     * @return 0: does not match: 1: matches perfectly, 2: new version
     */
    public int matchesVersion(
            String version )
    {
        // for speed purposes, get exact min version requirement out of the way
        if( theMinRequiredModuleVersionIsInclusive && version != null && version.equals( theMaxRequiredModuleVersion )) {
            return 1;
        }

        ensureVersionsParsed();

        Object [][] parsedVersion = parseVersion( version );

        int ret = 1;
        if( theParsedMinRequiredModuleVersion != null ) {
            int comp = compareParsedVersions( theParsedMinRequiredModuleVersion, parsedVersion );
            if( comp > 0 ) {
                return 0;
            }
            if( comp == 0 && !theMinRequiredModuleVersionIsInclusive ) {
                return 0;
            }
            if( comp != 0 ) {
                ret = 2;
            }
        }

        if( theParsedMaxRequiredModuleVersion != null ) {
            int comp = compareParsedVersions( theParsedMaxRequiredModuleVersion, parsedVersion );
            if( comp < 0 ) {
                return 0;
            }
            if( comp == 0 && !theMaxRequiredModuleVersionIsInclusive ) {
                return 0;
            }
        }

        return ret;
    }

    /**
     * Determine whether the provided ModuleMeta matches the coordinate requirements,
     * i.e. excluding the CapabilityRequirements.
     *
     * @param candidate the candidate ModuleMeta
     * @return true or false
     */
    public boolean matchesCoordinates(
            ModuleMeta candidate )
    {
        if(    theRequiredModuleGroupId != null
            && !theRequiredModuleGroupId.equals( candidate.getModuleGroupId()) )
        {
            return false;
        }

        if(    theRequiredModuleArtifactId != null
            && !theRequiredModuleArtifactId.equals( candidate.getModuleArtifactId()) )
        {
            return false;
        }

        if( matchesVersion( candidate.getModuleVersion() ) == 0 ) {
            return false;
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
        } else if( b.length > max ) {
            return -1;
        } else {
            return 0;
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
        } else if( b.length > max ) {
            if( b[max] != null ) {
                return -1;
            } else {
                return 0; // should that happen?
            }
        } else {
            return 0;
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
        String []                    major  = v.split( "\\." );
        ArrayList<ArrayList<Object>> almost = new ArrayList<>();

        for( int i=0 ; i<major.length ; ++i ) {
            StringBuilder currentString = null; // once non-null, we know we are parsing a string
            long          currentLong   = -1;

            almost.add( new ArrayList<>());
            for( int j=0 ; j<major[i].length() ; ++j ) {
                char c = major[i].charAt( j );
                if( Character.isDigit( c )) {
                    if( currentString != null ) {
                        almost.get( i ).add( currentString.toString());
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
                        almost.get( i ).add( currentLong );
                        currentLong = -1;
                    }
                }
            }
            if( currentString != null ) {
                almost.get( i ).add( currentString.toString());
            } else if( currentLong != -1 ) {
                almost.get( i ).add( currentLong );
            }
        }

        Object [][] ret = new Object[ almost.size() ][];
        for( int i=0 ; i<ret.length ; ++i ) {
            ret[i] = new Object[ almost.get( i ).size() ];
            for( int j=0 ; j<ret[i].length ; ++j ) {
                ret[i][j] = almost.get(i).get(j);
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
     * The set of names of the required ModuleCapabilities.
     */
    protected String [] theRequiredCapabilities;

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


    /**
     * A builder class for ModuleRequirement.
     */
    public static class Builder
    {
        /**
         * Constructor.
         */
        public Builder()
        {}

        /**
         * Set the groupId of the required Module.
         *
         * @param requiredModuleGroupId the groupId of the required Module
         * @return self
         */
        public Builder requiredModuleGroupId(
                String requiredModuleGroupId )
        {
            theRequiredModuleArtifactId = requiredModuleGroupId;
            return this;
        }

        /**
         * Set the artifactId of the required Module.
         *
         * @param requiredModuleArtifactId the artifactId of the required Module
         * @return self
         */
        public Builder requiredModuleArtifactId(
                String requiredModuleArtifactId )
        {
            theRequiredModuleArtifactId = requiredModuleArtifactId;
            return this;
        }

        /**
         * Set the version of the required Module.
         *
         * @param requiredModuleVersion the version of the required Module
         * @return self
         */
        public Builder requiredModuleVersion(
                String requiredModuleVersion )
        {
            theRequiredModuleVersion = requiredModuleVersion;
            return this;
        }

        /**
         * Set whether the required Module is optional.
         *
         * @param isOptional if true, the Module is optional
         * @return self
         */
        public Builder isOptional(
                boolean isOptional )
        {
            theIsOptional = isOptional;
            return this;
        }

        /**
         * Add a named required ModuleCapability.
         *
         * @param name the name of the required ModuleCapability
         * @return self
         */
        public Builder requiredCapability(
                String name )
        {
            if( theCapabilities == null ) {
                theCapabilities = new HashSet<>();
            }
            theCapabilities.add( name );
            return this;
        }

        /**
         * Create the ModuleRequirement
         *
         * @return the ModuleRequirement
         */
        public ModuleRequirement build()
        {
            String [] capabilitiesArray;
            if( theCapabilities == null ) {
                capabilitiesArray = new String[0];
            } else {
                capabilitiesArray = new String[ theCapabilities.size() ];
                theCapabilities.toArray( capabilitiesArray );
            }
            return new ModuleRequirement(
                    theRequiredModuleGroupId,
                    theRequiredModuleArtifactId,
                    theRequiredModuleVersion,
                    theIsOptional,
                    capabilitiesArray );
        }

        /**
         * The required Module groupId, if any.
         */
        protected String theRequiredModuleGroupId;

        /**
         * The required Module artifactId, if any.
         */
        protected String theRequiredModuleArtifactId;

        /**
         * The required Module version, if any.
         */
        protected String theRequiredModuleVersion;

        /**
         * The optionality of the Module.
         */
        protected boolean theIsOptional = false;

        /**
         * Set of named capabilities.
         */
        protected Set<String> theCapabilities;
    }
}
