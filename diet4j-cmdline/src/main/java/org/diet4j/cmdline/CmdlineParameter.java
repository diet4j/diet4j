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

import java.util.ArrayList;
import java.util.List;

/**
 * Defines one allowed parameter, and the found value(s).
 */
public abstract class CmdlineParameter
{
    /**
     * Constructor.
     *
     * @param name name of the parameter
     * @param mayRepeat if true, this parameter may be provided more than once in the arguments
     */
    public CmdlineParameter(
            String  name,
            boolean mayRepeat )
    {
        theName      = name;
        theMayRepeat = mayRepeat;
    }

    /**
     * Obtain the name.
     */
    public String getName()
    {
        return theName;
    }

    /**
     * Parse this parameter's value from this next element in the arguments.
     *
     * @param args the arguments
     * @param index the current index into the arguments
     * @return the number of arguments taken
     */
    public abstract int parseValues(
            String [] args,
            int       index );

    /**
     * Has a value been given for this parameter?
     */
    public abstract boolean hasValueSet();

    protected final String theName;
    protected boolean theMayRepeat;


//                } else if( i + foundPar.theNumValues > args.length ) {
//                    CmdlineBootLoader.fatal( "Value " + args[i] + " requires " + foundPar.theNumValues + " values." );
//
//                } else {
//                    if( theCount.containsKey( foundPar.theName )) {
//                        if( foundPar.theMayRepeat ) {
//                            String [] values = theCount.get( foundPar.theName );
//                            String [] values2 = new String[ values.length + foundPar.theNumValues ];
//                            System.arraycopy( values, 0, values2, 0, values.length );
//                            System.arraycopy( args, i+1, values2, values.length, foundPar.theNumValues );
//                            theCount.put( foundPar.theName, values2 );
//
//                        } else {
//                            CmdlineBootLoader.fatal( "Value must not repeat: " + args[i] );
//                        }
//
//                    } else {
//                        String [] values = new String[ foundPar.theNumValues ];
//                        System.arraycopy( args, i+1, values, 0, foundPar.theNumValues );
//                        theCount.put( foundPar.theName, values );
//                    }
//                    ++i;                        // the name of the parameter
//                    i += foundPar.theNumValues; // the values of the parameter
//                }

    /**
     * A parameter that takes no values but may be repeated.
     */
    public static class Flag
        extends
            CmdlineParameter
    {
        /**
         * Constructor.
         *
         * @param name name of the parameter
         * @param mayRepeat if true, this parameter may be provided more than once in the arguments
         */
        public Flag(
                String  name,
                boolean mayRepeat )
        {
            super( name, mayRepeat );
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int parseValues(
                String [] args,
                int       index )
        {
            if( theCount > 0 && !theMayRepeat ) {
                CmdlineBootLoader.fatal( "Parameter must not be repeated: " + theName );
            }

            ++theCount;
            return 0;
        }

        /**
         * Determine how many times the parameter was set.
         */
        public int getCount()
        {
            return theCount;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasValueSet()
        {
            return theCount > 0;
        }

        /**
         * Counts the number of times the value was set.
         */
        protected int theCount;
    }

    /**
     * A parameter that takes a value and may be repeated.
     */
    public static class Value
        extends
            CmdlineParameter
    {
        /**
         * Constructor.
         *
         * @param name name of the parameter
         * @param mayRepeat if true, this parameter may be provided more than once in the arguments
         */
        public Value(
                String  name,
                boolean mayRepeat )
        {
            super( name, mayRepeat );
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int parseValues(
                String [] args,
                int       index )
        {
            if( index > args.length-1 ) {
                CmdlineBootLoader.fatal( "Parameter needs value: " + theName );
            }
            if( !theValues.isEmpty() && !theMayRepeat ) {
                CmdlineBootLoader.fatal( "Parameter must not be repeated: " + theName );
            }
            theValues.add( args[index++] );
            return 1;
        }

        /**
         * Determine the value(s) of this parameter.
         */
        public List<String> getValues()
        {
            return theValues;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasValueSet()
        {
            return !theValues.isEmpty();
        }

        /**
         * The values of this parameter.
         */
        protected ArrayList<String> theValues = new ArrayList<>();
    }
}
