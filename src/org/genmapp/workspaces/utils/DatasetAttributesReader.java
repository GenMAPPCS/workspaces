/*
  File: CyAttributesReader.java

  Copyright (c) 2006, 2010, The Cytoscape Consortium (www.cytoscape.org)

  This library is free software; you can redistribute it and/or modify it
  under the terms of the GNU Lesser General Public License as published
  by the Free Software Foundation; either version 2.1 of the License, or
  any later version.

  This library is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
  MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
  documentation provided hereunder is on an "as is" basis, and the
  Institute for Systems Biology and the Whitehead Institute
  have no obligations to provide maintenance, support,
  updates, enhancements or modifications.  In no event shall the
  Institute for Systems Biology and the Whitehead Institute
  be liable to any party for direct, indirect, special,
  incidental or consequential damages, including lost profits, arising
  out of the use of this software and its documentation, even if the
  Institute for Systems Biology and the Whitehead Institute
  have been advised of the possibility of such damage.  See
  the GNU Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public License
  along with this library; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */
package org.genmapp.workspaces.utils;

import java.io.BufferedReader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URLDecoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Vector;

import org.genmapp.workspaces.command.WorkspacesCommandHandler;

import cytoscape.data.CyAttributes;
import cytoscape.data.attr.MultiHashMapDefinition;
import cytoscape.data.readers.CyAttributesReader;
import cytoscape.data.writers.CyAttributesWriter;
import cytoscape.logger.CyLogger;
import cytoscape.plugin.PluginManager;

public class DatasetAttributesReader 
{
	public static final String DECODE_PROPERTY = "cytoscape.decode.attributes";

	static boolean badDecode = false; 
	static int lineNum = 0;
	
	// 
	public static void loadAttributes( CyAttributes cyAttrs, FileReader fileIn, CyLogger logger ) throws IOException
	{
		WorkspacesCommandHandler.showMessage( "loadAttributes");
		CyLogger.getLogger().info( "loadAttributes" );
		// read entire file and store in 2-dim array, where each column is a separate sub-array ( 0 = attrname, 1 = attrtype, 2 ... n = values )
		Vector< Vector< String > > bufferCols = null;
		
		// read in csv file
		BufferedReader inStream = new BufferedReader( fileIn );
		String line = null;
		int colTotal = 0;
		while( null != ( line = inStream.readLine() ) )
		{
			CyLogger.getLogger().debug( "read line: " + line );

			// split by comma	
			String cols[] = line.split( "," );
			if ( colTotal != 0 && cols.length != colTotal )
			{
				CyLogger.getLogger().error( "uneven column total while loading attributes file: # of columns is != " + colTotal );
			}
			colTotal = cols.length;
			
			// initialize first time
			if ( bufferCols == null ) 
			{ 
				CyLogger.getLogger().debug( "init bufferCols: colTotal =  " + colTotal );
				bufferCols = new Vector< Vector < String > >();
				CyLogger.getLogger().debug( "bufferCols inited = " + bufferCols + " with length " + bufferCols.size() );
				
				for ( int i = 0; i < colTotal; i++ )
				{
					CyLogger.getLogger().debug( "i = " + i );
					bufferCols.add( new Vector< String >() );
				}

				
			}
			
			for( int i = 0; i < colTotal; i++ )
			{
				CyLogger.getLogger().debug( "adding cols " + cols[ i ] );
				
				bufferCols.get( i ).add( cols[ i ] );
			}
		}
		CyLogger.getLogger().info( "read file: " + colTotal + " columns" );
		WorkspacesCommandHandler.showMessage( "read file: " + colTotal + " cols ");
				
		// for each col, send to loadAttributesInternal
		for ( int i = 0; i < colTotal; i++ )
		{
			loadAttributesInternal( cyAttrs, bufferCols.get( 0 ), bufferCols.get( i ) );
		}
		WorkspacesCommandHandler.showMessage( "loadAttributes done");

	}
	
	// nodeNames and col are parallel arrays
	public static void loadAttributesInternal(CyAttributes cyAttrs, Vector< String > nodeNames, Vector< String > col )	throws IOException 
	{
		WorkspacesCommandHandler.showMessage( "loadAttributesInternal " + col.get( 0 ) );

		CyLogger.getLogger().debug( "loadAttributesInternal " + col.get( 0 ) );

	badDecode = false;

	try 
	{
		// read the header first ( first two lines, first = attrname, second = attrtype )
		
		int lineNum = 0;
		String attributeName = col.get( lineNum++ );
		byte type = -1;
		boolean bIsListType = false;   
		{
			// got rid of some heavy code here, was it meant to be for lists/maps?  
			final String className = col.get( lineNum++ );
		
			if (className.equalsIgnoreCase("java.lang.String")
			    || className.equalsIgnoreCase("String")) {
				type = MultiHashMapDefinition.TYPE_STRING;
			} else if (className.equalsIgnoreCase("java.lang.Boolean")
			           || className.equalsIgnoreCase("Boolean")) {
				type = MultiHashMapDefinition.TYPE_BOOLEAN;
			} else if (className.equalsIgnoreCase("java.lang.Integer")
			           || className.equalsIgnoreCase("Integer")) {
				type = MultiHashMapDefinition.TYPE_INTEGER;
			} else if (className.equalsIgnoreCase("java.lang.Double")
			           || className.equalsIgnoreCase("Double")
			           || className.equalsIgnoreCase("java.lang.Float")
			           || className.equalsIgnoreCase("Float")) {
				type = MultiHashMapDefinition.TYPE_FLOATING_POINT;
			}
			CyLogger.getLogger().debug( "className: " + className);
		}
		
		boolean firstLine = true;
		for( lineNum = 2; lineNum < col.size(); lineNum++ )
		{
			String key = nodeNames.get( lineNum );
			String val = col.get( lineNum );
			if ( val.startsWith("(") )
			{
				bIsListType = true;
			}
			
			if ( bIsListType )
			{			
				// Chop away leading '(' and trailing ')'.
				val = val.substring(1).trim();
				val = val.substring(0, val.length() - 1).trim();

				String[] elms = val.split("::");
				final ArrayList elmsBuff = new ArrayList();

				for (String vs : elms) {
					vs = decodeString(vs);
					vs = decodeSlashEscapes(vs);
					elmsBuff.add(vs);
				}

				for (int i = 0; i < elmsBuff.size(); i++) {
					if (type == MultiHashMapDefinition.TYPE_INTEGER) {
						elmsBuff.set(i, new Integer((String) elmsBuff.get(i)));
					} else if (type == MultiHashMapDefinition.TYPE_BOOLEAN) {
						elmsBuff.set(i, new Boolean((String) elmsBuff.get(i)));
					} else if (type == MultiHashMapDefinition.TYPE_FLOATING_POINT) {
						elmsBuff.set(i, new Double((String) elmsBuff.get(i)));
					} else {
						// A string; do nothing.
					}
				}

				cyAttrs.setListAttribute(key, attributeName, elmsBuff);		}
			else
			{
				
				// Not a list.
				val = decodeString(val);
				val = decodeSlashEscapes(val);
	
				if (firstLine) {
					// determine the type by casting case-by-case
					if (type < 0) {
						while (true) {
							try {
								new Integer(val);
								type = MultiHashMapDefinition.TYPE_INTEGER;
	
								break;
							} catch (Exception e) {
							}
	
							try {
								new Double(val);
								type = MultiHashMapDefinition.TYPE_FLOATING_POINT;
	
								break;
							} catch (Exception e) {
							}
	
							//               try {
							//                 new Boolean(val);
							//                 type = MultiHashMapDefinition.TYPE_BOOLEAN;
							//                 break; }
							//               catch (Exception e) {}
							type = MultiHashMapDefinition.TYPE_STRING;
	
							break;
						}
					}
					firstLine = false;
				}
	
				// set the attribute for this key/value pair
				if (type == MultiHashMapDefinition.TYPE_INTEGER) {
					cyAttrs.setAttribute(key, attributeName, new Integer(val));
				} else if (type == MultiHashMapDefinition.TYPE_BOOLEAN) {
					cyAttrs.setAttribute(key, attributeName, new Boolean(val));
				} else if (type == MultiHashMapDefinition.TYPE_FLOATING_POINT) {
					cyAttrs.setAttribute(key, attributeName, new Double(val));
				} else {
					cyAttrs.setAttribute(key, attributeName, val);
				}
			}
		}
	} catch (Exception e) {
		String message = "failed parsing attributes file at line: " + lineNum
		                 + " with exception: " + e.getMessage();
		CyLogger.getLogger(CyAttributesReader.class).warn(message, e);
		throw new IOException(message);
	}
}

private static String decodeString(String in) throws IOException {
	try {
		in = URLDecoder.decode(in, CyAttributesWriter.ENCODING_SCHEME);
	}
	catch (IllegalArgumentException iae) {
		if (!badDecode) {
			CyLogger.getLogger(CyAttributesReader.class).info(MessageFormat.format("Couldn't decode!", lineNum), iae);
			badDecode = true;
		}
	}

	return in;
}

private static String decodeSlashEscapes(String in) {
    final StringBuilder elmBuff = new StringBuilder();
    int inx2;

    for (inx2 = 0; inx2 < in.length(); inx2++) {
        char ch = in.charAt(inx2);

        if (ch == '\\') {
            if ((inx2 + 1) < in.length()) {
                inx2++;

                char ch2 = in.charAt(inx2);

                if (ch2 == 'n') {
                    elmBuff.append('\n');
                } else if (ch2 == 't') {
                    elmBuff.append('\t');
                } else if (ch2 == 'b') {
                    elmBuff.append('\b');
                } else if (ch2 == 'r') {
                    elmBuff.append('\r');
                } else if (ch2 == 'f') {
                    elmBuff.append('\f');
                } else {
                    elmBuff.append(ch2);
                }
            } else {
                /* val ends in '\' - just ignore it. */ }
        } else {
            elmBuff.append(ch);
        }
    }

    return elmBuff.toString();
}

	/*
	 * TODO: we should detect when "null" string is used as Numeric attribute
	 * place holder and convert simply skip writing those lines
	 */
	
}