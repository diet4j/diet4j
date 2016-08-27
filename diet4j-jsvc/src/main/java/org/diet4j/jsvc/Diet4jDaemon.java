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

package org.diet4j.jsvc;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.HashSet;
import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.diet4j.cmdline.CmdlineParameters;
import org.diet4j.core.Module;
import org.diet4j.core.ModuleActivationException;
import org.diet4j.core.ModuleDeactivationException;
import org.diet4j.core.ModuleMeta;
import org.diet4j.core.ModuleNotFoundException;
import org.diet4j.core.ModuleRegistry;
import org.diet4j.core.ModuleRequirement;
import org.diet4j.core.ModuleResolutionException;
import org.diet4j.core.ModuleRunException;
import org.diet4j.core.NoRunMethodException;
import org.diet4j.core.ScanningDirectoriesModuleRegistry;

/**
 * An implementation of the jsvc Daemon interface that enables jsvc to
 * invoke the diet4j framework and start a diet4j module.
 */
public class Diet4jDaemon
    implements Daemon
{
    @Override
    public void init(
            DaemonContext dc )
        throws
            DaemonInitException
    {
        CmdlineParameters parameters = new CmdlineParameters(
            new CmdlineParameters.Parameter( "directory", 1, true ),
            new CmdlineParameters.Parameter( "run",       1 ),
            new CmdlineParameters.Parameter( "method",    1 )
        );

        String [] remaining = parameters.parse( dc.getArguments() );
        if( remaining.length < 1 ) {
            throw new DaemonInitException( "Must provide the name of one root module" );
        }

        try {
            theRootModuleRequirement = ModuleRequirement.parse( remaining[0] );
        } catch( ParseException ex ) {
            throw new DaemonInitException( "Failed to parse root module requirement " + remaining[0], ex );
        }

        theRunArguments = new String[ remaining.length-1 ];
        System.arraycopy( remaining, 1, theRunArguments, 0, theRunArguments.length );

        String [] dirs = parameters.getMany( "directory" );
        HashSet<File> fileDirs = new HashSet<>();
        if( dirs != null ) {
            for( String dir : dirs ) {
                try {
                    if( !fileDirs.add( new File( dir ).getCanonicalFile() )) {
                        throw new DaemonInitException( "Directory (indirectly?) specified more than once in moduledirectory parameter: " + dir );
                    }
                } catch( IOException ex ) {
                    throw new DaemonInitException( "Failed to read directory " + dir, ex );
                }
            }
        }
        theModuleDirectories = new File[ fileDirs.size() ];
        fileDirs.toArray( theModuleDirectories );

        theRunClassName  = parameters.get( "run" );
        theRunMethodName = parameters.get( "method" );

        // create ModuleRegistry
        theModuleRegistry = ScanningDirectoriesModuleRegistry.create( theModuleDirectories );

        try {
            theRootModuleMeta = theModuleRegistry.determineSingleResolutionCandidate( theRootModuleRequirement );

        } catch( Throwable ex ) {
            throw new DaemonInitException( "Cannot find module " + theRootModuleRequirement );
        }
        try {
            theRootModule = theModuleRegistry.resolve( theRootModuleMeta );
        } catch( Throwable ex ) {
            throw new DaemonInitException( "Cannot resolve module " + theRootModuleMeta.toString() );
        }
    }

    @Override
    public void start()
        throws
            ModuleResolutionException,
            ModuleNotFoundException,
            ModuleActivationException,
            ClassNotFoundException,
            ModuleRunException,
            NoRunMethodException,
            InvocationTargetException
    {
        if( theRootModule != null ) {
            theRootModule.activateRecursively();

            if( theRunClassName != null ) {
                theRootModule.run( theRunClassName, theRunMethodName, theRunArguments );
            }
        }
    }

    @Override
    public void stop()
        throws
            ModuleDeactivationException
    {
        if( theRootModule != null ) {
            theRootModule.deactivateRecursively();
        }
    }

    @Override
    public void destroy()
    {
        // no op
    }

    protected Module theRootModule;
    protected ModuleMeta theRootModuleMeta;
    protected ModuleRegistry theModuleRegistry;
    protected ModuleRequirement theRootModuleRequirement;
    protected String [] theRunArguments;
    protected File [] theModuleDirectories;
    protected String theRunClassName;
    protected String theRunMethodName;
}
