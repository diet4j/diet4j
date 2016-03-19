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
            Parameter ... pars )
    {
        thePars = pars;
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
        if( theValues != null ) {
            throw new IllegalStateException( "Parsed already" );
        }
        theValues = new HashMap<>();

        String [] ret = null;

        int i=0;
        while( i<args.length ) {
            if( args[i].startsWith( "-" )) {
                args[i] = args[i].substring( 1 );
                if( args[i].startsWith( "-" )) {
                    args[i] = args[i].substring( 1 );
                }

                Parameter foundPar = null;
                for( int p = 0 ; p<thePars.length ; ++p ) {
                    if( args[i].equals( thePars[p].theName )) {
                        foundPar = thePars[p];
                        break;
                    }
                }
                if( foundPar == null ) {
                    CmdlineBootLoader.fatal( "Unknown parameter: " + args[i] );

                } else if( i + foundPar.theNumValues > args.length ) {
                    CmdlineBootLoader.fatal( "Parameter " + args[i] + " requires " + foundPar.theNumValues + " values." );

                } else {
                    if( theValues.containsKey( foundPar.theName )) {
                        if( foundPar.theMayRepeat ) {
                            String [] values = theValues.get( foundPar.theName );
                            String [] values2 = new String[ values.length + foundPar.theNumValues ];
                            System.arraycopy( values, 0, values2, 0, values.length );
                            System.arraycopy( args, i+1, values2, values.length, foundPar.theNumValues );
                            theValues.put( foundPar.theName, values2 );

                        } else {
                            CmdlineBootLoader.fatal( "Parameter must not repeat: " + args[i] );
                        }

                    } else {
                        String [] values = new String[ foundPar.theNumValues ];
                        System.arraycopy( args, i+1, values, 0, foundPar.theNumValues );
                        theValues.put( foundPar.theName, values );
                    }
                    ++i;                        // the name of the parameter
                    i += foundPar.theNumValues; // the values of the parameter
                }

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
     * Has this named parameter been given?
     * 
     * @param name name of the parameter
     * @return true or false
     */
    public boolean containsKey(
            String name )
    {
        return theValues.containsKey( name );
    }

    /**
     * Obtain the value of a single-valued parameter.
     *
     * @param name name of the parameter
     * @return the provided value, or null
     */
    public String get(
            String name )
    {
        String [] found = theValues.get( name );
        if( found == null ) {
            return null;
        } else {
            return found[0];
        }
    }

    /**
     * Obtain the value(s) of a multi-valued parameter
     *
     * @param name name of the parameter
     * @return the provided value(s), or null
     */
    public String [] getMany(
            String name )
    {
        return theValues.get( name );
    }

    /**
     * The defined parameters.
     */
    protected Parameter [] thePars;

    /**
     * The found values.
     */
    protected Map<String,String[]> theValues = null;

    /**
     * Defines one allowed parameter.
     */
    public static class Parameter
    {
        /**
         * Constructor.
         *
         * @param name name of the parameter
         * @param numValues the number of values for the parameter, e.g. 0 or 1
         */
        public Parameter(
                String name,
                int    numValues )
        {
            this( name, numValues, false );
        }

        /**
         * Constructor.
         *
         * @param name name of the parameter
         * @param numValues the number of values for the parameter, e.g. 0 or 1
         * @param mayRepeat if true, this parameter may be provided more than once in the arguments
         */
        public Parameter(
                String  name,
                int     numValues,
                boolean mayRepeat )
        {
            theName      = name;
            theNumValues = numValues;
            theMayRepeat = mayRepeat;
        }

        protected String  theName;
        protected int     theNumValues;
        protected boolean theMayRepeat;
    }
}
