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

import java.util.Locale;
import java.util.Map;
import java.util.jar.JarFile;

/**
 * The metadata for a Module.
 */
public class ModuleMeta
{
    /**
      * Constructor. This should not be directly invoked by the application programmer.
      *
      * @param moduleGroupId the Maven groupId of the to-be-created Module
      * @param moduleArtifactId the Maven artifactId of the to-be-created Module
      * @param moduleVersion the version of the to-be-created Module, may be null (but that's discouraged)
      * @param moduleUserNames the name shown to the user of the to-be-created Module, keyed by the locale
      * @param moduleUserDescriptions the description shown to the user of the to-be-created Module, keyed by the locale
      * @param moduleBuildDate the time when this Module was built
      * @param license the license for the to-be-created Module
      * @param runtimeModuleRequirements the ModuleRequirements of this Module at run time
      * @param moduleJar JAR file provided by this Module
      * @param activationClassName name of the Module's activation/deactivation class, or null
      * @param runClassName name of the class contained in this Module that contains the Module's run method, or null
      */
    protected ModuleMeta(
            String               moduleGroupId,
            String               moduleArtifactId,
            String               moduleVersion,
            Map<String,String>   moduleUserNames,
            Map<String,String>   moduleUserDescriptions,
            long                 moduleBuildDate,
            ModuleLicense        license,
            ModuleRequirement [] runtimeModuleRequirements,
            JarFile              moduleJar,
            String               activationClassName,
            String               runClassName )
    {
        theModuleGroupId               = moduleGroupId;
        theModuleArtifactId            = moduleArtifactId;
        theModuleVersion               = moduleVersion;
        theModuleUserNames             = moduleUserNames;
        theModuleUserDescriptions      = moduleUserDescriptions;
        theModuleBuildDate             = moduleBuildDate;
        theModuleLicense               = license;
        theRuntimeModuleRequirements   = runtimeModuleRequirements;
        theModuleJar                   = moduleJar;
        theActivationClassName         = activationClassName;
        theRunClassName                = runClassName;

        if( moduleJar == null ) {
            theResourceJarEntryPrefix = UNPACKED_RESOURCE_JAR_ENTRY_PREFIX;
        } else if( moduleJar.getName().endsWith( ".war" )) {
            theResourceJarEntryPrefix = WAR_RESOURCE_JAR_ENTRY_PREFIX;
        } else {
            theResourceJarEntryPrefix = JAR_RESOURCE_JAR_ENTRY_PREFIX;
        }
    }

    /**
      * Obtain the groupId of the Module.
      *
      * @return the groupId for this Module
      */
    public final String getModuleGroupId()
    {
        return theModuleGroupId;
    }

    /**
      * Obtain the artifactId of the Module.
      *
      * @return the artifactId for this Module
      */
    public final String getModuleArtifactId()
    {
        return theModuleArtifactId;
    }

    /**
     * Obtain the internationalized name of the Module in the current locale.
     *
     * @return the internationalized name of the Module, in the current locale
     */
    public String getModuleUserName()
    {
        return getModuleUserName( Locale.getDefault() );
    }

    /**
     * Obtain the internationalized name of the Module in a specified locale.
     *
     * @param loc the locale
     * @return the internationalized name of the Module, in the specified locale
     */
    public String getModuleUserName(
            Locale loc )
    {
        String ret = null;
        if( theModuleUserNames != null ) {
            String key1 = loc.getCountry();
            if( loc.getLanguage() != null ) {
                String key2 = key1 + "." + loc.getLanguage();
                if( loc.getVariant() != null ) {
                    String key3 = key2 + "." + loc.getVariant();

                    ret = theModuleUserNames.get( key3 );
                }
                if( ret == null ) {
                    ret = theModuleUserNames.get( key2 );
                }
            }
            if( ret == null ) {
                ret = theModuleUserNames.get( key1 );
            }
        }
        if( ret == null ) {
            ret = toString(); // reasonable default
        }
        return ret;
    }

    /**
     * Obtain the internationalized description of the Module in a specified locale.
     *
     * @param loc the locale
     * @return the internationalized description of the Module, in the specified locale
     */
    public String getModuleUserDescription(
            Locale loc )
    {
        String ret = null;
        if( theModuleUserDescriptions != null ) {
            String key1 = loc.getCountry();
            if( loc.getLanguage() != null ) {
                String key2 = key1 + "." + loc.getLanguage();
                if( loc.getVariant() != null ) {
                    String key3 = key2 + "." + loc.getVariant();

                    ret = theModuleUserDescriptions.get( key3 );
                }
                if( ret == null ) {
                    ret = theModuleUserDescriptions.get( key2 );
                }
            }
            if( ret == null ) {
                ret = theModuleUserDescriptions.get( key1 );
            }
        }
        return ret;
    }

    /**
     * Obtain the Map of internationalized names of the Module.
     *
     * @return the Map of internationalized names of the Module, keyed by locale
     */
    public Map<String,String> getModuleUserNames()
    {
        return theModuleUserNames;
    }

    /**
     * Obtain the Map of internationalized descriptions of the Module.
     *
     * @return the Map of internationalized descriptions of the Module, keyed by locale
     */
    public Map<String,String> getModuleUserDescriptions()
    {
        return theModuleUserDescriptions;
    }

    /**
     * Obtain the version of this Module.
     *
     * @return the version for of this Module; may be null
     */
    public final String getModuleVersion()
    {
        return theModuleVersion;
    }

    /**
     * Obtain the time at which this Module was built, in System.currentTimeMillis() format.
     *
     * @return the time at which this Module was built.
     */
    public final long getModuleBuildDate()
    {
        return theModuleBuildDate;
    }

    /**
     * Obtain the license for this Module.
     *
     * @return the license for this Module
     */
    public final ModuleLicense getModuleLicense()
    {
        return theModuleLicense;
    }

    /**
     * Obtain the list of requirements for other Modules that this Module depends on
     * at run time.
     *
     * @return the list of ModuleRequirements of this Module at run time
     */
    public final ModuleRequirement [] getRuntimeModuleRequirements()
    {
        return theRuntimeModuleRequirements;
    }

    /**
     * Obtain the JAR file that this Module provides as JarFile.
     *
     * @return the JarFile
     */
    public final JarFile getProvidesJar()
    {
        return theModuleJar;
    }

    /**
     * Obtain the relative path below which resources, such as a class files,
     * are to be found. In a WAR file, for example, that would be "WEB-INF/classes/".
     *
     * @return the prefix
     */
    public final String getResourceJarEntryPrefix()
    {
        return theResourceJarEntryPrefix;
    }

    /**
     * Obtain the name of the Module activation/deactivation class.
     * If this returns null, it means the Module has no activation method.
     *
     * @return name of the activation/deactivation class contained in this Module, or null if none
     */
    public final String getActivationClassName()
    {
        return theActivationClassName;
    }

    /**
     * Obtain the name of the class that contains the Module's run method.
     * If this returns null, it means the Module has no run method.
     *
     * @return name of the run class contained in this Module, or null if none
     */
    public final String getRunClassName()
    {
        return theRunClassName;
    }

    /**
     * Two ModuleMetas are the same if they have the same name and version.
     *
     * @param other the object to test against
     * @return if they are equal, return true
     */
    @Override
    public boolean equals(
            Object other )
    {
        if( other == null ) {
            return false;
        }
        if( getClass() != other.getClass() ) {
            return false;
        }
        ModuleMeta realOther = (ModuleMeta) other;

        if( ! theModuleGroupId.equals( realOther.theModuleGroupId )) {
            return false;
        }
        if( ! theModuleArtifactId.equals( realOther.theModuleArtifactId )) {
            return false;
        }
        return theModuleVersion.equals( realOther.theModuleVersion );
    }

    /**
     * We determine the hash code by looking at the Module's name and version.
     *
     * @return a hash code
     */
    @Override
    public int hashCode()
    {
        int ret = theModuleGroupId.hashCode() % theModuleArtifactId.hashCode() % theModuleVersion.hashCode();

        return ret;
    }

    /**
     * Create a Module from this ModuleMeta. This is not supposed to be invoked
     * by the application programmer.
     *
     * @param settings the settings for the to-be-created Module
     * @param registry the AbstractModuleRegistry in which the to-be-created Module will look for dependent Modules
     * @param parentClassLoader the ClassLoader of our parent Module
     * @return the created Module
     */
    protected Module createModule(
            ModuleSettings         settings,
            AbstractModuleRegistry registry,
            ClassLoader            parentClassLoader )
    {
        return new Module( this, settings, registry, parentClassLoader );
    }

    /**
     * Obtain a String representation.
     *
     * @return String representation
     */
    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append( theModuleGroupId );
        buf.append( ":" );
        buf.append( theModuleArtifactId );
        buf.append( ":" );
        buf.append( theModuleVersion );
        return buf.toString();
    }

    /**
     * The groupId of the module
     */
    protected String theModuleGroupId;

    /**
     * The artifactId of the module.
     */
    protected String theModuleArtifactId;

    /**
     * The version of the module.
     */
    protected String theModuleVersion;

    /**
     * The time when this Module was built.
     */
    protected long theModuleBuildDate;

    /**
     * The user-visible names for this Module, keyed by the locale.
     */
    protected Map<String,String> theModuleUserNames;

    /**
     * The user-visible descriptions for this Module, keyed by the locale.
     */
    protected Map<String,String> theModuleUserDescriptions;

    /**
     * The requirements for other modules that this Module will have at run time.
     */
    protected ModuleRequirement [] theRuntimeModuleRequirements;

    /**
     * The license of the Module.
     */
    protected ModuleLicense theModuleLicense;

    /**
     * The JAR that this Module provides.
     */
    protected JarFile theModuleJar;

    /**
     * The relative path below which resources, such as a class files,
     * are to be found. Must be suitable for prepending without further processing,
     * i.e. must end with slash unless at the root.
     */
    protected String theResourceJarEntryPrefix;

    /**
     * The resourceJarEntryPrefix for JAR files.
     */
    public static final String JAR_RESOURCE_JAR_ENTRY_PREFIX = "";

    /**
     * The resourceJarEntryPrefix for WAR files.
     */
    public static final String WAR_RESOURCE_JAR_ENTRY_PREFIX = "WEB-INF/classes/";

    /**
     * The resourceJarEntryPrefix for unpacked directories.
     */
    public static final String UNPACKED_RESOURCE_JAR_ENTRY_PREFIX = "";

    /**
     * The name of the Module activation/deactivation class.
     */
    protected String theActivationClassName;

    /**
     * The name of the class in this Module which provides a method to
     * run this Module.
     */
    protected String theRunClassName;
}
