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

package org.diet4j.cmdline;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple command-line parser.
 */
public class CmdlineParameters
{
    /**
     * Constructor, list allowed Parameters.
     *
     * @param pars the allowed parameters
     */
    public CmdlineParameters(
            CmdlineParameter ... pars )
    {
        for( CmdlineParameter par : pars ) {
            if( thePars.put( par.getName(), par ) != null ) {
                CmdlineBootLoader.fatal( "Programming error: have parameter with name already: " + par.getName() );
            }
            if( par.getShortName() != null ) {
                if( thePars.put( par.getShortName(), par ) != null ) {
                    CmdlineBootLoader.fatal( "Programming error: have parameter with name already: " + par.getShortName() );
                }
            }
        }
    }

    /**
     * Parse the command-line arguments.
     *
     * @param args the command-line arguments
     * @return the remaining arguments that are not interpreted as parameters
     */
    public String [] parse(
            String [] args )
    {
        String [] ret = null;

        int i=0;
        while( i<args.length ) {
            if( args[i].startsWith( "-" )) {
                args[i] = args[i].substring( 1 );
                if( args[i].startsWith( "-" )) {
                    args[i] = args[i].substring( 1 );
                }

                CmdlineParameter foundPar = thePars.get( args[i] );
                if( foundPar == null ) {
                    CmdlineBootLoader.fatal( "Unknown parameter: " + args[i] );
                }

                int taken = foundPar.parseValues( args, ++i ); // may fatal out
                i += taken;

            } else {
                // we are done
                ret = new String[ args.length-i ];
                System.arraycopy( args, i, ret, 0, args.length-i );
                break;
            }
        }
        if( ret != null ) {
            return ret;
        } else {
            return new String[0];
        }
    }

    /**
     * Has this named CmdlineParameter been given?
     *
     * @param name name of the CmdlineParameter
     * @return true or false
     */
    public boolean hasValueSetForKey(
            String name )
    {
        CmdlineParameter par = thePars.get( name );
        if( par == null ) {
            return false;
        }
        return par.hasValueSet();
    }

    /**
     * Obtain a CmdlineParameter
     *
     * @param name name of the CmdlineParameter
     * @return the CmdlineParameter
     */
    public CmdlineParameter get(
            String name )
    {
        return thePars.get( name );
    }

    /**
     * Obtain the value of flag parameter.
     */
    public int getFlagCount(
            String name )
    {
        CmdlineParameter par = thePars.get( name );

        int ret = ((CmdlineParameter.Flag)par).getCount();
        return ret;
    }

    /**
     * Obtain a single-valued parameter value.
     */
    public String getSingleValued(
            String name )
    {
        CmdlineParameter par = thePars.get( name );

        List<String> values = ((CmdlineParameter.Value)par).getValues();
        if( values.isEmpty() ) {
            return null;
        }
        if( values.size() == 1 ) {
            return values.get( 0 );
        }

        CmdlineBootLoader.fatal( "Internal error" );
        return null;
    }

    /**
     * Obtain a multiple-valued parameter value.
     */
    public List<String> getManyValued(
            String name )
    {
        CmdlineParameter par = thePars.get( name );

        List<String> values = ((CmdlineParameter.Value)par).getValues();
        return values;
    }

    /**
     * The defined parameters.
     */
    protected Map<String,CmdlineParameter> thePars = new HashMap<>();
}
