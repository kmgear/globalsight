/**
 *  Copyright 2009 Welocalize, Inc. 
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  
 *  You may obtain a copy of the License at 
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  
 */
package com.globalsight.cxe.adapter.database.source;

import com.globalsight.diplomat.util.XmlUtil;

import java.util.Date;

/**
 * Abstract class to define common behavior for xml generators.
 */
public abstract class XmlGenerator
{
    //
    // PRIVATE CONSTANTS
    //
    private static final int TAB_SIZE = 4;
    private static final String EMPTY = "";
    private static final String SPACE = " ";
    private static final String LT = "<";
    private static final String GT = ">";
    private static final String EQUALS = "=";
    private static final String SLASH = "/";
    private static final String DBLQUOTE = "\"";
    private static final String NEWLINE = "\n";

    //
    // PROTECTED CONSTANTS
    //
    protected static final String VERSION = "version";
    protected static final String UNKNOWN = "Unknown";

    //
    // PRIVATE MEMBER VARIABLES
    //
    private transient String m_tabString;
    private transient StringBuffer m_buffer;
    private transient int m_indentLevel = 0;
    
    //
    // PUBLIC CONSTRUCTOR
    //
    /**
     * Create an initialized instance.
     */
    public XmlGenerator()
    {
        super();
        m_tabString = EMPTY;

        for (int i = 0 ; i < TAB_SIZE ; i++)
        {
            m_tabString = m_tabString + SPACE;
        }
    }

    //
    // PROTECTED ABSTRACT METHODS
    //
    /* Return the type of xml being generated*/
    protected abstract String xmlType();

    /* Return the text of the dtd for this xml */
    protected abstract String[] dtdText();

    //
    // PROTECTED METHODS
    //
    /* Initialize for creating new XML. */
    protected void reset()
    {
        m_buffer = new StringBuffer();
        m_indentLevel = 0;
    }

    /* Return the buffer containing the XML text. */
    protected StringBuffer getBuffer()
    {
        return m_buffer;
    }

    /* Increase the indent level to increase the amount of leading space. */
    protected void incrementIndent()
    {
        m_indentLevel++;
    }

    /* Reduce the indent level to reduce the amount of leading space. */
    protected void decrementIndent()
    {
        m_indentLevel--;
        if (m_indentLevel < 0)
        {
            m_indentLevel = 0;
        }
    }

    /* Start the body with the root tag. */
    protected void addPreamble()
    {
        openStartTag(xmlType());
        closeTag();
    }

    /* Close the root tag portion of the body. */
    protected void addPostamble()
    {
        openEndTag(xmlType());
        closeTag();
    }

    /* Write out the given object as a string onto the buffer. */
    protected void addString(Object p_obj)
    {
        m_buffer.append(p_obj.toString());
    }

    /* Write a string of the form 'key="value"' */
    protected void addKeyValuePair(String p_key, String p_val)
    {
        addString(p_key + EQUALS + DBLQUOTE + p_val + DBLQUOTE);
    }

    /* Write a single space into the buffer. */
    protected void addSpace()
    {
        addString(SPACE);
    }

    /* Write out the number of tabs determined by the currend indent level. */
    protected void addIndent()
    {
        for (int i = 0 ; i < m_indentLevel ; i++)
        {
            addString(m_tabString);
        }
    }

    /* Output the document definition tag and embedded DTD. */
    protected void addXmlHeader()
    {
        openStartTag("?xml");
        addSpace();
        addKeyValuePair(VERSION, "1.0");
        addString(" ?");
        closeTag();
        openStartTag("!-- " + capitalize(xmlType()) + ", autogenerated ");
        addString(new Date(System.currentTimeMillis()));
        addString(" --");
        closeTag();
        addEmbeddedDtd();
    }

    /* Capitalize the first letter of the given string. */
    protected String capitalize(String p_str)
    {
        String str = EMPTY;
        if (p_str != null && p_str.length() > 0)
        {
            str = (p_str.substring(0, 1).toUpperCase()) + p_str.substring(1);
        }
        return str;
    }

    /* Output the given string as a starting tag. */
    protected void openStartTag(String p_str)
    {
        openTag(p_str, false);
    }

    /* Output the given string as an ending tag. */
    protected void openEndTag(String p_str)
    {
        openTag(p_str, true);
    }

    /* Open the given tag; if p_isEnd is true, add the preceding slash. */
    protected void openTag(String p_str, boolean p_isEnd)
    {
        addString(LT + (p_isEnd ? SLASH : EMPTY) + p_str);
    }

    /* Write the closing tag marker, followed by a default newline. */
    protected void closeTag()
    {
        closeTag(true);
    }

    /* Close the current tag; if p_useCR is true, add a newline at the end. */
    protected void closeTag(boolean p_useCR)
    {
        addString(GT + (p_useCR ? NEWLINE : EMPTY));
    }

    /* Escape the given string and return the result. */
    protected String escapeString(String p_str)
    {
        return XmlUtil.escapeString(p_str);
    }

    //
    // PRIVATE SUPPORT METHODS
    //
    /* Format and output the DTD text. */
    private void addEmbeddedDtd()
    {
        String[] text = dtdText();
        if (text.length > 1)
        {
            addString(text[0] + NEWLINE);
            incrementIndent();
            for (int i = 1 ; i < text.length - 1 ; i++)
            {
                String st = text[i];
                boolean needExtraIndent = (st.indexOf(LT) == 0);
                if (needExtraIndent)
                {
                    incrementIndent();
                }
                addIndent();
                addString(st + NEWLINE);
                if (needExtraIndent)
                {
                    decrementIndent();
                }
            }
            decrementIndent();
            addString(text[text.length - 1] + NEWLINE);
        }
    }
}
