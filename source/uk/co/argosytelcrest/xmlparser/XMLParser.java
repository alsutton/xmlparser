/*
  Copyright (c) 2000,2001 Al Sutton (al@alsutton.com)
  All rights reserved.
  Redistribution and use in source and binary forms, with or without modification, are permitted
  provided that the following conditions are met:

  1. Redistributions of source code must retain the above copyright notice, this list of conditions
  and the following disclaimer.

  2. Redistributions in binary form must reproduce the above copyright notice, this list of
  conditions and the following disclaimer in the documentation and/or other materials provided with
  the distribution.

  Neither the name of Al Sutton nor the names of its contributors may be used to endorse or promote
  products derived from this software without specific prior written permission.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS OR
  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
  FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE
  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
  OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
  THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.alsutton.xmlparser;

/**
 * The main XML Parser class.
 */

import java.io.*;

import java.util.*;

public class XMLParser
{
  /**
   * The reader from which the stream is being read
   */

  private Reader inputReader;

  /**
   * The handler for XML Events.
   */

  private XMLEventListener eventHandler;

  /**
   * The root tag for the document.
   */

  private String rootTag = null;

  /**
   * Constructor, Used to override default dispatcher.
   *
   * @param _eventHandler The event handle to dispatch events through.
   */

  public XMLParser( XMLEventListener _eventHandler )
  {
    eventHandler = _eventHandler;
  }

  /**
   * Method to read until an end condition.
   *
   * @param checker The class used to check if the end condition has occurred.
   * @return A string representation of the data read.
   */
  private String readUntilEnd( ReadEndChecker checker ) throws IOException
  {
    StringBuffer data = new StringBuffer();
    boolean inQuote = false;

    int nextChar = inputReader.read();
    if( nextChar == '\"' )
      inQuote = true;
    while( nextChar != -1 && (inQuote == true || checker.shouldStop( nextChar ) == false) )
    {
      if( nextChar == '\"')
      {
        if( inQuote )
          inQuote=false;
        else
          inQuote=true;
      }
      else
      {
        data.append( (char) nextChar );
      }
      nextChar = inputReader.read();
    }
    if( nextChar != '<' && nextChar != '>')
      data.append( (char) nextChar );

    String returnData = data.toString();
    return returnData;
  }

  /**
   * Method to handle the reading and dispatch of tag data.
   */

  private void handleTag()
    throws IOException, EndOfXMLException
  {
    boolean startTag = true,
            emptyTag = false,
            hasMoreData = true;
    String tagName = null;
    Hashtable attributes = null;

    do
    {
      String data = readUntilEnd ( inTagReadEndChecker );
      int substringStart = 0,
          substringEnd = data.length();

      if( data.startsWith( "/" )  )
      {
        startTag = false;
        substringStart++;
      }

      if( data.endsWith( "/" ) )
      {
        emptyTag = true;
        substringEnd--;
      }

      hasMoreData = data.endsWith( " " );
      if( hasMoreData )
        substringEnd--;

      data = data.substring( substringStart, substringEnd );

      if( tagName == null )
      {
        tagName = data.toLowerCase();
        continue;
      }

      if( attributes == null )
        attributes = new Hashtable();

      int stringLength = data.length();
      int equalitySign = data.indexOf( '=' );
      if( equalitySign == -1 )
      {
        if( hasMoreData )
          continue;
        else
          break;
      }

      String attributeName = data.substring(0, equalitySign);
      int valueStart = equalitySign+1;
      if( valueStart >= data.length() )
      {
        attributes.put( attributeName, "" );
        continue;
      }

      substringStart = valueStart;
      char startChar = data.charAt( substringStart );
      if( startChar  == '\"' || startChar  == '\'' )
        substringStart++;

      substringEnd = stringLength;
      char endChar = data.charAt( substringEnd-1 );
      if( substringEnd > substringStart && endChar  == '\"' || endChar  == '\'' )
        substringEnd--;

      attributes.put( attributeName, data.substring( substringStart, substringEnd ) );
    } while( hasMoreData );

    if( tagName.startsWith( "?") )
      return;

    tagName = tagName.toLowerCase();
    if( startTag )
    {
      if( rootTag == null )
        rootTag = tagName;
      eventHandler.tagStarted( tagName, attributes);
    }

    if( emptyTag || !startTag )
    {
      eventHandler.tagEnded( tagName );
      if( rootTag != null && tagName.equals( rootTag ) )
        throw new EndOfXMLException();
    }
  }

  /**
   * Method to handle the reading in and dispatching of events for plain text.
   */

  private void handlePlainText() throws IOException
  {
    String data = readUntilEnd ( inPlaintextReadEndChecker );
    eventHandler.plaintextEncountered( data );
  }

  /**
   * The main parsing loop.
   *
   * @param _inputReader The reader for the XML stream.
   */

  public void  parse ( Reader _inputReader )
    throws IOException
  {
    inputReader = _inputReader;
    try
    {
      while( true )
      {
        handlePlainText();
        handleTag();
      }
    }
    catch( EndOfXMLException x )
    {
      // The EndOfXMLException is purely used to drop out of the
      // continuous loop.
    }
  }



/*

------------------------------------------------------

Classes for handling the control of the reading stream

------------------------------------------------------

*/

  /**
   * Class to indicate the end of reading a plain text section
   */

  class InPlaintextReadEndChecker implements ReadEndChecker
  {
    /**
     * The method to issue a stop message when a start tag symbol (&gt;)
     * is encountered .
     *
     * @param c The character to check
     * @return true if it is the symbol, false otehrwise.
     */

    public boolean shouldStop( int c )
    {
      return (c == '<');
    }
  }

  /**
   * Shared instance of the plain text end checker.
   */

  private final InPlaintextReadEndChecker inPlaintextReadEndChecker = new InPlaintextReadEndChecker();

  /**
   * Class to indicate the end of reading a tag section
   */

  class InTagReadEndChecker implements ReadEndChecker
  {
    /**
     * The method to issue a stop message when either a space of close
     * tag symbol (&lt;) is encountered .
     *
     * @param c The character to check.
     * @return true if c is either symbol, false otehrwise.
     */

    public boolean shouldStop( int c )
    {
      return (c == '>' || c == ' ');
    }
  }

  /**
   * Shared instance of the tag end checker.
   */

  private final InTagReadEndChecker inTagReadEndChecker = new InTagReadEndChecker();
}