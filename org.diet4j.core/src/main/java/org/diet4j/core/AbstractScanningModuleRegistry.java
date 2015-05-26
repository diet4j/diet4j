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

import java.io.CharConversionException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Collections functionality common to ModuleRegistry implementations that determine the
 * set of available ModuleMetas through a scan.
 * Keeps the set of available Modules in memory and does not rescan.
 */
public abstract class AbstractScanningModuleRegistry
    extends
        AbstractModuleRegistry
{
    /**
     * Private constructor, for subclasses only.
     *
     * @param metas the ModuleMetas found, keyed by their name, and then ordered by version
     */
    protected AbstractScanningModuleRegistry(
            HashMap<String,ModuleMeta[]> metas )
    {
        theMetas = metas;
    }
    /**
     * {@inheritDoc }
     */
    public ModuleMeta [] determineResolutionCandidates(
            ModuleRequirement req )
    {
        ModuleMeta [] found;
        synchronized( RESOLVE_LOCK ) {
            found = theMetas.get( req.getRequiredModuleName() );
        }
        if( found == null ) {
            return new ModuleMeta[0];
        }
        String version = req.getRequiredModuleVersion();
        if( version != null ) {
            for( ModuleMeta current : found ) {
                if( version.equals( current.getModuleVersion() )) {
                    return new ModuleMeta[] { current };
                }
            }
            return new ModuleMeta[0];
        } else {
            return new ModuleMeta[] { found[0] };
        }
    }

    /**
     * Add another ModuleMeta during runtime. Take care that we don't accidentally
     * add the same one again.
     *
     * @param add the ModuleMeta to be added
     * @param metas the existing ModuleMetas
     */
    protected static void addModuleMeta(
            ModuleMeta                   add,
            HashMap<String,ModuleMeta[]> metas )
    {
        String name    = add.getModuleName();
        String version = add.getModuleVersion();

        ModuleMeta [] already = metas.get( name );
        ModuleMeta [] newArray;

        if( already != null ) {
            newArray = new ModuleMeta[ already.length + 1 ];
            int offset = 0;
            for( int i=0 ; i<already.length ; ++i ) {
                if( offset == 0 ) { // not found yet
                    int comp = rpmvercmp( already[i].getModuleVersion(), version );
                    if( comp == 0 ) {
                        log.log( Level.WARNING,
                                "Adding module again: {0}: {1}, was: {2}",
                                new Object[] {
                                        name,
                                        add.toString(),
                                        add.getProvidesJar() != null ? add.getProvidesJar().getName() : "<no jar>",
                                        already[i].getProvidesJar() != null ? already[i].getProvidesJar().getName() : "<no jar>"
                                });
                        return;
                    }
                    if( comp < 0 ) {
                        newArray[i] = add;
                        offset = 1;
                    }
                }
                newArray[i+offset] = already[i];
            }
            if( offset == 0 ) { // add at end
                newArray[ newArray.length-1 ] = add;
            }
        } else {
            newArray = new ModuleMeta[] { add };
        }
        metas.put( name, newArray );
    }

    /**
     * Obtain the set of Module names in the registry
     * 
     * @return the set of Module names
     */
    public Set<String> nameSet()
    {
        return theMetas.keySet();
    }

    /**
     * Obtain the ModuleMeta(s) for a given Module name that are known to the registry.
     * 
     * @param name name of the Module
     * @return set of ModuleMetas for that name, most current first
     */
    public ModuleMeta [] getModuleMetasFor(
            String name )
    {
        return theMetas.get( name );
    }
    
    /**
     * Given a list of Jar filenames, parse the JARs and determine the ModuleMetas that they contain.
     * Add them to the provided hash.
     * 
     * @param jars the JarFiles to parse
     * @param result the hash to add results to
     */
    protected static void addParsedModuleMetasFromJars(
            List<JarFile>                 jars,
            HashMap<String,ModuleMeta []> result )
    {       
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        try {
            for( JarFile jarFile : jars ) {
                try {
                    String pomSiblingName = jarFile.getName();
                    pomSiblingName = pomSiblingName.substring( 0, pomSiblingName.length()-3 ) + "pom";
                    File pomSibling = new File( pomSiblingName );
                    if( !pomSibling.canRead() ) {
                        pomSibling = null;
                    }

                    Stream<JarEntry> metaFiles = jarFile.stream().filter( f -> f.getName().startsWith( "META-INF/" ) );
                    // does not like to be processed twice, and doesn't like to be an iterable

                    JarEntry pomXmlEntry        = null;
                    JarEntry pomPropertiesEntry = null;
                    JarEntry manifestEntry      = null;

                    Iterator<JarEntry> iter = metaFiles.iterator();
                    while( iter.hasNext() ) {
                        JarEntry f = iter.next();
                        String   n = f.getName();

                        if( n.startsWith( "META-INF/maven" )) {
                            if( n.endsWith( "pom.xml")) {
                                pomXmlEntry = f;
                            } else if( f.getName().endsWith( "pom.properties")) {
                                pomPropertiesEntry = f;
                            }
                        } else if( f.getName().equals( "META-INF/MANIFEST.MF")) {
                            manifestEntry = f;
                        }
                    }

                    ModuleMeta meta = parseMetadataFiles(
                            dbf,
                            jarFile,
                            pomXmlEntry != null        ? jarFile.getInputStream( pomXmlEntry )        : null,
                            pomPropertiesEntry != null ? jarFile.getInputStream( pomPropertiesEntry ) : null,
                            manifestEntry != null      ? jarFile.getInputStream( manifestEntry )      : null,
                            pomSibling != null         ? new FileInputStream( pomSibling )            : null );
                    if( meta != null ) {
                        addModuleMeta( meta, result );
                    }
                } catch( IOException|SAXException ex ) {
                    log.log( Level.SEVERE, "Failed to read/parse file", ex );
                }
            }
        } catch( ParserConfigurationException ex ) {
            log.log( Level.SEVERE, "Failed to instantiate XML parser", ex );
        }
    }

    /**
     * Given a list of META-INF directories, determine the ModuleMetas that they contain.
     * Add them to the provided hash.
     * 
     * @param dirs the META-INF directories
     * @param result the hash to add the results to
     */
    protected static void addParsedModuleMetasFromDirectories(
            List<File>                    dirs,
            HashMap<String,ModuleMeta []> result )
    {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        try {
            for( File dir : dirs ) {
                try {
                    Path dirPath = dir.toPath();
                    Stream<Path> metaFiles
                            = Files.walk( dir.toPath() )
                                    .map( d -> dirPath.relativize( d ))
                                    .filter( f -> f.toString().startsWith( "maven/" ) );

                    Iterator<Path> iter = metaFiles.iterator();
                    while( iter.hasNext() ) {
                        Path   p = iter.next();
                        String n = p.toString();

                        if( n.endsWith( "pom.xml" )) {
                            ModuleMeta meta = parseMetadataFiles(
                                    dbf,
                                    null,
                                    new FileInputStream( new File( dir, n )),
                                    null,
                                    null,
                                    null );
                            if( meta != null ) {
                                addModuleMeta( meta, result );
                            }
                        }
                    }
                } catch( IOException|SAXException ex ) {
                    log.log( Level.SEVERE, "Failed to read/parse file", ex );
                }
            }
        } catch( ParserConfigurationException ex ) {
           log.log( Level.SEVERE, "Failed to configure XML parser", ex );
        }
    }

    /**
     * Helper method to parse metadata jar file entries into a ModuleMeta.
     * 
     * @param dbf the XML parser factory
     * @param jar the JAR file containing the entries
     * @param pomXmlEntryStream reading the maven pom.xml file
     * @param pomPropertiesEntryStream reading the maven pom.properties file (if any)
     * @param manifestEntryStream reading the MANIFEST.MF file
     * @param pomFileStream read the POM from this stream, if given, otherwise use what's inside the JAR
     * @return the created ModuleMeta
     * @throws ParserConfigurationException misconfigured XML parser
     * @throws IOException I/O error
     * @throws SAXException XML syntax error
     */
    protected static ModuleMeta parseMetadataFiles(
            DocumentBuilderFactory dbf,
            JarFile         jar,
            InputStream     pomXmlEntryStream,
            InputStream     pomPropertiesEntryStream,
            InputStream     manifestEntryStream,
            InputStream     pomFileStream )
        throws
            ParserConfigurationException,
            IOException,
            SAXException
    {
        HashMap<String,String> pomProperties = new HashMap<>();
        if( pomPropertiesEntryStream != null ) {
            // Unlike all others, these Properties don't have the ${project.XXX} prefix
            Properties shortPomProperties = new Properties(); 
            shortPomProperties.load( pomPropertiesEntryStream);
            for( String propName : shortPomProperties.stringPropertyNames() ) {
                pomProperties.put( "project." + propName, shortPomProperties.getProperty( propName ));
            }
        }
        if( !pomProperties.containsKey( "mavenVersion" )) {
            pomProperties.put( "mavenVersion", null );
        }

        DocumentBuilder parser = dbf.newDocumentBuilder();
        Document        doc    = null;
        if( pomFileStream != null ) {
            try {
                doc = parser.parse( pomFileStream );
            } catch( CharConversionException ex ) {
                // happens when an XML document is in a weird character encoding, not something the parser can deal with
            }
        }
        if( doc == null && pomXmlEntryStream != null ) {
            try {
                doc = parser.parse( pomXmlEntryStream );
            } catch( CharConversionException ex ) {
                // happens when an XML document is in a weird character encoding, not something the parser can deal with
                return null;
                // doc = parser.parse( new ReaderInputStream( new InputStreamReader( jar.getInputStream( pomXmlEntry ), "ISO-8859" )), "UTF-8" );
                // This misses ReaderInputStream, which is in Apache Commons IO
            }
        }
        if( doc == null ) {
            // cannot read pom, skipping
            return null;
        }

        Element root = doc.getDocumentElement();
        root.normalize();
        
        String moduleName    = "${project.artifactId}"; // default to what's in the Properties
        String moduleVersion = "${project.version}";    // default to what's in the Properties
        String parentVersion = null;
        
        ArrayList<ModuleRequirement> buildTimeRequirements = new ArrayList<>();
        ArrayList<ModuleRequirement> runTimeRequirements   = new ArrayList<>();

        NodeList rootChildren = root.getChildNodes();
        for( int i=0 ; i<rootChildren.getLength(); ++i ) {
            Node   rootChild     = rootChildren.item( i );
            String rootChildName = rootChild.getNodeName();
            
            if( rootChildName == null ) {
                continue;
            }
            
            switch( rootChildName ) {
                case "artifactId":
                    moduleName = rootChild.getTextContent();
                    break;
                case "version":
                    moduleVersion = rootChild.getTextContent();
                    break;
                case "properties":
                    NodeList propertiesChildren = rootChild.getChildNodes();
                    for( int j=0 ; j<propertiesChildren.getLength() ; ++j ) {
                        Node   propertiesChild     = propertiesChildren.item( j );
                        String propertiesChildName = propertiesChild.getNodeName();
                        
                        if( propertiesChild.getNodeType() != Node.ELEMENT_NODE ) {
                            continue;
                        }
                        if( propertiesChildName == null ) {
                            continue;
                        }
                        pomProperties.put( propertiesChildName, propertiesChild.getTextContent() );
                    }
                    break;
                case "parent":
                    NodeList parentChildren = rootChild.getChildNodes();
                    for( int j=0 ; j<parentChildren.getLength() ; ++j ) {
                        Node   parentChild     = parentChildren.item( j );
                        String parentChildName = parentChild.getNodeName();
                        
                        if( parentChildName == null ) {
                            continue;
                        }
                        if( parentChildName.equals( "version" )) {
                            parentVersion = parentChild.getTextContent();
                        }
                    }
                    break;
                case "dependencies":
                    NodeList dependenciesChildren = rootChild.getChildNodes();
                    for( int j=0 ; j<dependenciesChildren.getLength() ; ++j ) {
                        Node   dependenciesChild     = dependenciesChildren.item( j );
                        String dependenciesChildName = dependenciesChild.getNodeName();
                        
                        if( dependenciesChildName == null ) {
                            continue;
                        }
                        
                        if( dependenciesChildName.equals( "dependency" )) {
                            NodeList dependencyChildren = dependenciesChild.getChildNodes();
                            
                            String  dependencyName    = null;
                            String  dependencyVersion = null;
                            String  dependencyScope   = null;
                            boolean isOptional        = false;
                            
                            for( int k=0 ; k<dependencyChildren.getLength() ; ++k ) {
                                Node   dependencyChild     = dependencyChildren.item( k );
                                String dependencyChildName = dependencyChild.getNodeName();
                                
                                if( dependencyChildName == null ) {
                                    continue;
                                }
                                
                                switch( dependencyChildName ) {
                                    case "artifactId":
                                        dependencyName = dependencyChild.getTextContent();
                                        break;
                                    case "version":
                                        dependencyVersion = dependencyChild.getTextContent();
                                        break;
                                    case "scope":
                                        dependencyScope = dependencyChild.getTextContent();
                                        break;
                                    case "optional":
                                        isOptional = "true".equalsIgnoreCase( dependencyChild.getTextContent() );
                                        break;
                                }
                            }
                            if( dependencyName != null && dependencyVersion != null ) {
                                if( !dependencyName.startsWith( "org.infogrid.module" )) {
                                    if( "compile".equals( dependencyScope )) {
                                        ModuleRequirement req = new ModuleRequirement( dependencyName, dependencyVersion, isOptional );
                                        buildTimeRequirements.add( req );
                                        
                                    } else if( "test".equals( dependencyScope )) {
                                        // ignore
                                        
                                    } else if( !"provided".equals( dependencyScope )) {
                                        ModuleRequirement req = new ModuleRequirement( dependencyName, dependencyVersion, isOptional );
                                        runTimeRequirements.add( req );
                                    } // Just like Maven, we ignore "provided" modules
                                }
                        }
                    }
                }   break;
            }
        }
        
        moduleName    = replaceProperties( pomProperties, moduleName );
        moduleVersion = replaceProperties( pomProperties, moduleVersion );

        if( moduleName.startsWith( "org.diet4j" ) && !moduleName.startsWith( "org.diet4j.example" )) {
            return null;
        }
        if( moduleVersion == null || moduleVersion.isEmpty() ) {
            moduleVersion = parentVersion;
        }
        if( moduleVersion == null || moduleVersion.isEmpty() ) {
            // try to get it from the filename
            String name = jar.getName();
            if( name.startsWith( moduleName + "-" )) {
                moduleVersion = name.substring( moduleName.length() + 1, name.length() - 4 );
            }
        }
        
        // if this is a JAR, do not add non-main artifacts that we might find in ~/.m2
        if( jar != null ) {
            String jarFileBaseName = jar.getName();
            int    lastSlash       = jarFileBaseName.lastIndexOf( '/' );
            if( lastSlash > 0 ) {
                jarFileBaseName = jarFileBaseName.substring( lastSlash + 1 );
            }
            int lastPeriod = jarFileBaseName.lastIndexOf( '.' );
            if( lastPeriod > 0 ) {
                jarFileBaseName = jarFileBaseName.substring( 0, lastPeriod );
            }
            if( !jarFileBaseName.equals( moduleName + "-" + moduleVersion )) {
                return null; // not a main artifact
            }
        }
        
        String runClassName = null;
        if( manifestEntryStream != null ) {
            Properties manifestProperties = new Properties();
            manifestProperties.load( manifestEntryStream );
            runClassName = manifestProperties.getProperty( "Main-Class" );
        }
        
        ModuleMeta ret = null;
        if( moduleName != null ) {
            ModuleRequirement [] buildTime = new ModuleRequirement[ buildTimeRequirements.size() ];
            ModuleRequirement [] runTime   = new ModuleRequirement[ runTimeRequirements.size() ];
            
            // copy into arrays, and while we are at it, replace symbolic version dependencies where needed
            for( int i=0 ; i<buildTime.length ; ++i ) {
                ModuleRequirement current = buildTimeRequirements.get( i );
                String            version = current.getRequiredModuleVersion();
                if( version == null ) {
                    buildTime[i] = current;
                } else {
                    String version2 = replaceProperties( pomProperties, version );
                    if( version2.equals( version )) {
                        buildTime[i] = current;
                    } else {
                        buildTime[i] = new ModuleRequirement( current.getRequiredModuleName(), version2, current.isOptional() );
                    }
                }
            }
            for( int i=0 ; i<runTime.length ; ++i ) {
                ModuleRequirement current = runTimeRequirements.get( i );
                String            version = current.getRequiredModuleVersion();
                if( version == null ) {
                    runTime[i] = current;
                } else {
                    String version2 = replaceProperties( pomProperties, version );
                    if( version.equals( version2 )) {
                        runTime[i] = current;
                    } else {
                        runTime[i] = new ModuleRequirement( current.getRequiredModuleName(), version2, current.isOptional() );
                    }
                }
            }

            ret = new ModuleMeta( // FIXME: extract more info from pom files
                    moduleName,
                    moduleVersion,
                    null,
                    null,
                    0,
                    null,
                    runTime,
                    jar,
                    null,
                    runClassName );
        }
        return ret;
    }
    
    /**
     * Replace properties in Strings.
     * 
     * @param prop the map of properties
     * @param s the String
     * @return the replaced string
     */
    protected static String replaceProperties(
            HashMap<String,String> prop,
            String                 s )
    {
        Pattern p = Pattern.compile( "\\$\\{(.+)\\}" );
        Matcher m = p.matcher( s );
        
        // attempt to keep return value at null, if all we have is a String consisting of replacements to null
        StringBuffer b = null;
        
        while( m.find() ) {
            String value = prop.get( m.group(1) );
            if( value != null ) {
                if( b == null ) {
                    b = new StringBuffer();
                }
                m.appendReplacement( b, Matcher.quoteReplacement( value ));
            } else {
                if( b != null ) {
                    m.appendReplacement( b, "" );
                } else {
                    b = new StringBuffer();
                    m.appendReplacement( b, "" );
                    if( b.length() == 0 ) {
                        b = null;
                    }
                }
            }
        }
        if( b != null ) {
            m.appendTail( b );
            return b.toString();
        } else {
            b = new StringBuffer();
            m.appendTail( b );
            if( b.length() > 0 ) {
                return b.toString();
            } else {
                return null;
            }
        }
    }

    /**
     * Helper method to compare two versions the way RPM does it.
     * Null comes first, then ordered by dot-separate 
     * 
     * @param a: version 1 to compare
     * @param b: version 2 to compare
     * @return: -1, 0, or 1 like strcmp
     */
    public static int rpmvercmp(
            String a,
            String b )
    {
        if( a == null ) {
            a = "0";
        }
        if( b == null ) {
            b = "0";
        }

        int ret;

        // easy comparison to see if versions are identical
        if( a.equals( b )) {
            return 0;
        }

        int aLen = a.length();
        int bLen = b.length();

        int one = 0; // Need indices instead of pointers in Perl vs C
        int two = 0;
        int i1;
        int i2;
    
        // loop through each version segment of str1 and str2 and compare them
        while( one < aLen && two < bLen ) {
            while( one < aLen && !Character.isLetterOrDigit(a.charAt( one ))) {
                ++one;
            }

            while( two < bLen && !Character.isLetterOrDigit( b.charAt( two ))) {
                ++two;
            }

            // If we ran to the end of either, we are finished with the loop
            if( one >= aLen || two >= bLen ) {
                break;
            }

            // If the separator lengths were different, we are also finished
            if( one != two ) {
                return ( one < two ) ? -1 : 1;
            }

            i1 = one;
            i2 = two;

            // grab first completely alpha or completely numeric segment
            // leave one and two pointing to the start of the alpha or numeric
            // segment and walk i1 and i2 to end of segment
            boolean isnum;
            if( Character.isDigit( a.charAt( i1 )) ) {
                while( i1 < aLen && Character.isDigit( a.charAt( i1 ))) {
                    ++i1;
                }
                while( i2 < bLen && Character.isDigit( b.charAt( i2 ))) {
                    ++i2;
                }
                isnum = true;
                
            } else {
                while( i1 < aLen && Character.isLetter(a.charAt( i1 ))) {
                    ++i1;
                }
                while( i2 < bLen && Character.isLetter( b.charAt( i2 ))) {
                    ++i2;
                }
                isnum = false;
            }

            // this cannot happen, as we previously tested to make sure that
            // the first string has a non-null segment
            if( one == i1 ) {
                ret = -1; // arbitrary
                return ret;
            }

            // take care of the case where the two version segments are
            // different types: one numeric, the other alpha (i.e. empty)
            // numeric segments are always newer than alpha segments
            // XXX See patch #60884 (and details) from bugzilla #50977.
            if( two == i2 ) {
                ret = isnum ? 1 : -1;
                return ret;
            }

            if( isnum ) {
                // this used to be done by converting the digit segments
                // to ints using atoi() - it's changed because long
                // digit segments can overflow an int - this should fix that.

                // throw away any leading zeros - it's a number, right? */
                while( one < aLen && a.charAt( one ) == '0' ) {
                    one++;
                }
                while( two < bLen && b.charAt( two ) == '0' ) {
                    two++;
                }

                // whichever number has more digits wins
                if( i1 - one > i2 - two ) {
                    ret = 1;
                    return ret;
                }
                if( i2 - two > i1 - one ) {
                    ret = -1;
                    return ret;
                }
            }

            // strcmp will return which one is greater - even if the two
            // segments are alpha or if they are numeric.  don't return
            // if they are equal because there might be more segments to
            // compare
            int rc = a.substring( one, i1 ).compareTo( b.substring( two, i2 ));
            if( rc != 0 ) {
                ret = rc < 1 ? -1 : 1;
                return ret;
            }

            one = i1;
            two = i2;
        }

        // this catches the case where all numeric and alpha segments have */
        // compared identically but the segment separating characters were */
        // different */
        if( one == aLen && two == bLen ) {
            ret = 0;
            return ret;
        }

        // the final showdown. we never want a remaining alpha string to
        // beat an empty string. the logic is a bit weird, but:
        // - if one is empty and two is not an alpha, two is newer.
        // - if one is an alpha, two is newer.
        // - otherwise one is newer.
        if(    ( one == aLen && !Character.isLetter( b.charAt( two )))
            || Character.isLetter( a.charAt( one )))
        {
            ret = -1;
        } else {
            ret = 1;
        }
        return ret;
    }
    
    /**
     * The set of known ModuleMetas, keyed by name. Multiple
     * versions of the ModuleMeta are ordered with the newest first.
     */
    protected final HashMap<String,ModuleMeta[]> theMetas; 
    
    /**
    * Logger.
    */
    private static final Logger log = Logger.getLogger( AbstractScanningModuleRegistry.class.getName() );
}
