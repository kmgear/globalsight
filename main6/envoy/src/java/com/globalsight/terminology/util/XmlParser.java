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

package com.globalsight.terminology.util;

import com.globalsight.terminology.TermbaseException;
import com.globalsight.terminology.TermbaseExceptionMessages;

import com.globalsight.log.GlobalSightCategory;
import com.globalsight.util.ObjectPool;

import org.dom4j.io.SAXReader;
import org.dom4j.Document;
import org.xml.sax.XMLReader;
import org.dom4j.io.aelfred.SAXDriver;

import java.io.*;
import java.util.*;

/**
 * <p>An XML Parser wrapper that simplifies parsing from an XML string
 * and returns Document objects. Also manages instances of itself in a
 * pool of parser objects.</p>
 */
public class XmlParser
    extends SAXReader
    implements TermbaseExceptionMessages
{
    private static final GlobalSightCategory CATEGORY =
        (GlobalSightCategory)GlobalSightCategory.getLogger(
            XmlParser.class.getName());
    //
    // Pool Management
    //
    static private ObjectPool s_pool = new ObjectPool(XmlParser.class);

    static public XmlParser hire()
    {
        return (XmlParser)s_pool.getInstance();
    }

    static public void fire(XmlParser p_object)
    {
        if (p_object != null)
        {
            try
            {
                s_pool.freeInstance(p_object);
            }
            catch (Exception e)
            {
                // reset() can throw exception meaning object is
                // unusable, just don't free it then and let pool
                // allocate a new instance.
            }
        }
    }

    //
    // Constructor
    //
    public XmlParser()
    {
        super();

        // set parsing features here
        try
        {
            setXMLReader(new org.dom4j.io.aelfred.SAXDriver());
            setValidation(false);
            // setStringInternEnabled(true);
            // in jdom1.1+ use this to remove ignorable whitespace
            //setMergeAdjacentText(true);
            //setStripWhitespaceText(true);
        }
        catch (Exception e)
        {
            CATEGORY.error("Booboo", e);
        }
    }

    public Document parseXml(String p_xml)
        throws TermbaseException
    {
        try
        {
            return this.read(new StringReader(p_xml));
        }
        catch (Exception e)
        {
            // CATEGORY.error("Error while parsing XML", e);
            throw new TermbaseException(MSG_XML_ERROR, null, e);
        }
    }
}
