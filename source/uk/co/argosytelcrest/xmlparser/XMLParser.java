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
   * Flag to say whether or not this stream is UTF-8 encoded.
   */

  private boolean isUTF8Encoded;

  /**
   * The buffer for incomming data.
   */

  private StringBuffer dataBuffer;

  /**
   * The input stream being read.
   */

  private InputStream is;

  /**
   * Constructor, Used to override default dispatcher.
   *
   * @param _eventHandler The event handle to dispatch events through.
   */

  public XMLParser( XMLEventListener _eventHandler )
  {
    eventHandler = _eventHandler;
    dataBuffer = new StringBuffer();
  }

  /**
   * Method to set the flag to state whether or not the input is UTF-8
   * encoded. For the UTF-8 decoding to work the parse method MUST be
   * called by passing it a java.io.DataInputStream object.
   *
   * @param flag True if UTF-8 decoding should be performed on the input
   *  stream, false if not.
   */

  public void setInputUTF8Encoded( boolean flag )
  {
    isUTF8Encoded = flag;
  }

  /**
   * Method to get the next character from the input stream.
   */

  public int getNextCharacter()
    throws IOException
  {
    int actualValue = -1;

    int inputValue = inputReader.read();
    if( inputValue == -1 )
      return -1;

    // Single character
    if( isUTF8Encoded == false )
    {
      actualValue = inputValue;
    }
    else
    {
      inputValue &= 0xff;
      if      ( (inputValue & 0x80) == 0 )
      {
        actualValue = inputValue;
      }
      else if ( (inputValue & 0xF8) == 0xF0 )
      {
        actualValue = (inputValue & 0x1f)<<6;

        int nextByte = inputReader.read() & 0xff;
        if( (nextByte & 0xC0) != 0x80 )
          throw new IOException( "Invalid UTF-8 format" );
        actualValue += (nextByte & 0x3F )<<6;

        nextByte = inputReader.read() & 0xff;
        if( (nextByte & 0xC0) != 0x80 )
          throw new IOException( "Invalid UTF-8 format" );
        actualValue += (nextByte & 0x3F )<<6;

        nextByte = inputReader.read() & 0xff;
        if( (nextByte & 0xC0) != 0x80 )
          throw new IOException( "Invalid UTF-8 format" );
        actualValue += (nextByte & 0x3F );
      }
      else if ( (inputValue & 0xF0) == 0xE0 )
      {
        actualValue = (inputValue & 0x1f)<<6;

        int nextByte = inputReader.read() & 0xff;
        if( (nextByte & 0xC0) != 0x80 )
          throw new IOException( "Invalid UTF-8 format" );
        actualValue += (nextByte & 0x3F )<<6;

        nextByte = inputReader.read() & 0xff;
        if( (nextByte & 0xC0) != 0x80 )
          throw new IOException( "Invalid UTF-8 format" );
        actualValue += (nextByte & 0x3F );
      }
      else if ( (inputValue & 0xE0) == 0xC0 )
      {
        actualValue = (inputValue & 0x1f)<<6;

        int nextByte = inputReader.read() & 0xff;
        if( (nextByte & 0xC0) != 0x80 )
          throw new IOException( "Invalid UTF-8 format" );
        actualValue += (nextByte & 0x3F );
      }
    }

    return actualValue;
  }

  /**
   * Method to read until an end condition.
   *
   * @param endChar The character to stop reading on
   * @return A string representation of the data read.
   */

  private String readUntilEnd( char endChar )
    throws IOException, EndOfXMLException
  {
    StringBuffer data = new StringBuffer();

    int nextChar = getNextCharacter();
    if( nextChar == -1 )
      throw new EndOfXMLException();
    while( nextChar != -1 && nextChar != endChar )
    {
      data.append( (char) nextChar );
      nextChar = getNextCharacter();
    }
    if( nextChar != '<' && nextChar != '>')
      data.append( (char) nextChar );

    String returnData = data.toString();
    return returnData;
  }

  /**
   * Method to determine if a character is a whitespace.
   *
   * @param c The character to check.
   * @return true if the character is a whitespace, false if not.
   */

  private boolean isWhitespace( char c )
  {
    if( c == ' '
    ||  c == '\t'
    ||  c == '\r'
    ||  c == '\n' )
      return true;

    return false;
  }

  /**
   * Method to handle the attributes in a tag
   *
   * @param data The section of the tag holding the attribute details
   */

  private Hashtable handleAttributes( String data )
  {
    Hashtable attributes = new Hashtable();

    int length = data.length();
    int i = 0;
    while( i < length )
    {
      StringBuffer nameBuffer = new StringBuffer();

      char thisChar = data.charAt(i);
      while( isWhitespace( thisChar ) && i < length )
      {
        i++;
        thisChar = data.charAt(i);
      }
      if( thisChar == '>' || i == length )
        break;

      while( thisChar != '=' )
      {
        nameBuffer.append(thisChar);

        i++;
        if( i == length )
          break;

        thisChar = data.charAt(i);
      }

      if( i == length )
        break;

      String name = nameBuffer.toString();

      // See if first character is a character
      i++;
      thisChar = data.charAt(i);
      while( isWhitespace( thisChar ) && i < length)
      {
        i++;
        if( i == length )
          break;
        thisChar = data.charAt(i);
      }

      int breakOn = 0;
      if( thisChar == '\"' )
      {
        breakOn = 1;
      }
      else if (thisChar =='\'' )
      {
        breakOn = 2;
      }

      // Set up buffer for value parameter
      StringBuffer valueBuffer = new StringBuffer();
      if( breakOn == 0 )
      {
        valueBuffer.append( thisChar );
      }

      i++;
      while( i < length )
      {
        thisChar = data.charAt(i);
        i++;
        if      ( breakOn == 0 && isWhitespace( thisChar ) )
        {
          break;
        }
        else if ( breakOn == 1 && thisChar == '\"' )
        {
          break;
        }
        else if ( breakOn == 2 && thisChar == '\'' )
        {
          break;
        }
        valueBuffer.append( thisChar );
      }
      String value = valueBuffer.toString();
      attributes.put( name, value );
    }

    return attributes;
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

    String data = readUntilEnd ( '>' );

    if( data.startsWith( "?") )
      return;

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

    data = data.substring( substringStart, substringEnd );
    int spaceIdx = 0;
    while( spaceIdx < data.length()
    &&     isWhitespace( data.charAt(spaceIdx) ) == false )
      spaceIdx++;

    tagName = data.substring(0,spaceIdx).toLowerCase();

    if( spaceIdx != data.length() )
    {
      data = data.substring( spaceIdx+1 );
      attributes = handleAttributes( data );
    }
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

  private void handlePlainText()
    throws IOException, EndOfXMLException
  {
    String data = readUntilEnd ( '<' );
    eventHandler.plaintextEncountered( data );
  }

  /**
   * Parse wrapper for InputStreams
   *
   * @param _inputReader The reader for the XML stream.
   */

  public void  parse ( InputStream _is )
    throws IOException
  {
    is = _is;
    InputStreamReader isr = new InputStreamReader( is );
    parse( isr );
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
}