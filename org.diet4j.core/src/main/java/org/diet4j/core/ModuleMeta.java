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
 * The metadata for a software Module.
 */
public class ModuleMeta
{
    /**
      * Constructor. This should not be directly invoked by the application programmer.
      *
      * @param moduleName the programmatic name of the to-be-created Module
      * @param moduleVersion the version of the to-be-created Module, may be null (but that's discouraged)
      * @param moduleUserNames the name shown to the user of the to-be-created Module, keyed by the locale
      * @param moduleUserDescriptions the description shown to the user of the to-be-created Module, keyed by the locale
      * @param moduleBuildDate the time when this Module was built
      * @param license the license for the to-be-created Module
      * @param runTimeModuleRequirements the ModuleRequirements of this Module at run time
      * @param moduleJar JAR file provided by this Module
      * @param initClassName name of the Module's initialization class, or null
      * @param runClassName name of the class contained in this Module that contains the Module's run method, or null
      */
    protected ModuleMeta(
            String               moduleName,
            String               moduleVersion,
            Map<String,String>   moduleUserNames,
            Map<String,String>   moduleUserDescriptions,
            long                 moduleBuildDate,
            ModuleLicense        license,
            ModuleRequirement [] runTimeModuleRequirements,
            JarFile              moduleJar,
            String               initClassName,
            String               runClassName )
    {
        theModuleName                  = moduleName;
        theModuleVersion               = moduleVersion;
        theModuleUserNames             = moduleUserNames;
        theModuleUserDescriptions      = moduleUserDescriptions;
        theModuleBuildDate             = moduleBuildDate;
        theModuleLicense               = license;
        theRunTimeModuleRequirements   = runTimeModuleRequirements;
        theModuleJar                   = moduleJar;
        theInitClassName               = initClassName;
        theRunClassName                = runClassName;
    }

    /**
      * Obtain the fully-qualified, unique name of the Module.
      *
      * @return the name for this Module
      */
    public final String getModuleName()
    {
        return theModuleName;
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
            ret = theModuleName; // reasonable default
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
    public final ModuleRequirement [] getRunTimeModuleRequirements()
    {
        return theRunTimeModuleRequirements;
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
     * Obtain the name of the Module initialization class.
     * If this returns null, it means the Module has no activation method.
     *
     * @return name of the init class contained in this Module, or null if none
     */
    public final String getInitClassName()
    {
        return theInitClassName;
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

        if( ! theModuleName.equals( realOther.theModuleName )) {
            return false;
        }
        if( theModuleVersion != null ) {
            return theModuleVersion.equals( realOther.theModuleVersion );
        } else {
            return realOther.theModuleVersion == null;
        }
    }

    /**
     * We determine the hash code by looking at the Module's name and version.
     *
     * @return a hash code
     */
    @Override
    public int hashCode()
    {
        int ret = theModuleName.hashCode();

        if( theModuleVersion != null ) {
            ret %= theModuleVersion.hashCode();
        }
        return ret;
    }

    /**
     * Create a Module from this ModuleMeta. This is not supposed to be invoked
     * by the application programmer.
     *
     * @param registry the AbstractModuleRegistry in which the to-be-created Module will look for dependent Modules
     * @param parentClassLoader the ClassLoader of our parent Module
     * @return the created Module
     */
    protected Module createModule(
            AbstractModuleRegistry registry,
            ClassLoader    parentClassLoader )
    {
        return new Module( this, registry, parentClassLoader );
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
        buf.append( theModuleName );
        buf.append( ":" );
        if( theModuleVersion != null ) {
            buf.append( theModuleVersion );
        } else {
            buf.append( "?" );
        }
        return buf.toString();
    }

    /**
     * The name of the module.
     */
    protected String theModuleName;

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
    protected ModuleRequirement [] theRunTimeModuleRequirements;

    /**
     * The license of the Module.
     */
    protected ModuleLicense theModuleLicense;

    /**
     * The JAR that this Module provides.
     */
    protected JarFile theModuleJar;

    /**
     * The name of the Module initialization class.
     */
    protected String theInitClassName;

    /**
     * The name of the class in this Module which provides a method to
     * run this Module.
     */
    protected String theRunClassName;
}
