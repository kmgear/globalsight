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
package com.globalsight.everest.edit.offline.page;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;

import com.globalsight.everest.tm.util.Tmx;
import com.globalsight.everest.tm.util.Tmx.Prop;
import com.globalsight.everest.webapp.pagehandler.administration.users.UserUtil;
import com.globalsight.util.FileUtil;
import com.globalsight.util.UTC;
import com.globalsight.util.XmlParser;
import com.globalsight.util.edit.EditUtil;

/**
 * This Util is mainly dedicated to downloading tmx file when work offline. But
 * most of the methods can be used as standard functions to process tmx related
 * concepts.
 * 
 */
public class TmxUtil
{
    static private final Logger s_logger = Logger
            .getLogger(TmxUtil.class);
    public static final int TMX_LEVEL_ONE = 1;
    public static final int TMX_LEVEL_TWO = 2;
    public static final String TMX_ENCODING = FileUtil.UTF16LE;
    
    /**
     * Include all tmx data
     */
    public static final int TMX_MODE_INC_ALL = 0;
    /**
     * Include tm only
     */
    public static final int TMX_MODE_TM_ONLY = 1;
    /**
     * Include local machine translation only
     */
    public static final int TMX_MODE_MT_ONLY = 2;
    /**
     * Include ICE match only
     */
    public static final int TMX_MODE_ICE_ONLY = 3;
    /**
     * Include non ICE match only
     */
    public static final int TMX_MODE_NON_ICE = 4;

    public static void writeTmxOpenTag(OutputStreamWriter p_writer,
            int p_tmxLevel) throws IOException
    {
        String version = null;
        if (p_tmxLevel == TMX_LEVEL_ONE)
        {
            version = Tmx.TMX_11;
        }
        else if (p_tmxLevel == TMX_LEVEL_TWO)
        {
            version = Tmx.TMX_14;
        }
        p_writer.write("<tmx");
        if (version != null)
        {
            p_writer.write(" version=\"" + version + "\"");
        }
        p_writer.write(">\r\n");
    }

    public static void writeTmxCloseTag(OutputStreamWriter p_writer)
            throws IOException
    {
        p_writer.write("</tmx>\r\n");
    }

    public static void writeBodyOpenTag(OutputStreamWriter p_writer)
            throws IOException
    {
        p_writer.write("<body>\r\n");
    }

    public static void writeBodyCloseTag(OutputStreamWriter p_writer)
            throws IOException
    {
        p_writer.write("</body>\r\n");
    }

    public static void writeXmlHeader(OutputStreamWriter p_writer, int tmxLevel)
            throws IOException
    {
        p_writer.write("<?xml version=\"1.0\" encoding=\"" + TMX_ENCODING
                + "\" ?>\r\n");
        if (tmxLevel == TMX_LEVEL_ONE)
        {
            p_writer.write("<!DOCTYPE tmx SYSTEM \"tmx11.dtd\" >\r\n");
        }
        if (tmxLevel == TMX_LEVEL_TWO)
        {
            p_writer.write("<!DOCTYPE tmx SYSTEM \"tmx14.dtd\" >\r\n");
        }

    }

    public static void writeTmxHeader(String m_sourceLocaleName,
            OutputStreamWriter p_writer, int p_tmxLevel) throws IOException
    {
        Tmx result = new Tmx();
        result.setSourceLang(m_sourceLocaleName);
        result.setDatatype(Tmx.DATATYPE_HTML);
        result.setCreationTool(Tmx.GLOBALSIGHT);
        result.setCreationToolVersion(Tmx.GLOBALSIGHTVERSION);
        result.setSegmentationType(Tmx.SEGMENTATION_SENTENCE);
        result.setOriginalFormat(Tmx.TMF_GXML);
        result.setAdminLang(Tmx.DEFAULT_ADMINLANG);
        p_writer.write(result.getHeaderXml());
    }

    public static String composeTuWithoutTuCloseTag(
            TmxUtil.TmxTuvInfo srcTuvInfo, TmxUtil.TmxTuvInfo trgTuvInfo,
            int p_tmxLevel, String sid)
    {
        StringBuffer result = new StringBuffer();

        result.append("<tu>\r\n");
        if (sid != null)
        {
            Prop prop = new Tmx.Prop(Tmx.PROP_TM_UDA_SID, sid);
            result.append(prop.asXML());
        }

        // Source TUV
        result.append(composeTmTuv(srcTuvInfo, p_tmxLevel));

        // Target TUV
        result.append(composeTmTuv(trgTuvInfo, p_tmxLevel));

        return result.toString();
    }

    public static String composeTu(TmxUtil.TmxTuvInfo srcTuvInfo,
            TmxUtil.TmxTuvInfo trgTuvInfo, int p_tmxLevel, String sid)
    {
        StringBuffer result = new StringBuffer();

        result.append(composeTuWithoutTuCloseTag(srcTuvInfo, trgTuvInfo,
                p_tmxLevel, sid));

        // TU close tag
        result.append(TmxUtil.composeTuTail());

        return result.toString();
    }

    public static String composeTmTuv(TmxUtil.TmxTuvInfo tmxTuvInfo,
            int p_tmxLevel)
    {
        StringBuffer result = new StringBuffer();
        if (tmxTuvInfo != null)
        {
            String tuvText = tmxTuvInfo.getTuvText();
            String tuvLocale = tmxTuvInfo.getTuvLocale();
            String creationUser = tmxTuvInfo.getCreationUser();
            Timestamp creationDate = tmxTuvInfo.getCreationDate();
            String modifyUser = tmxTuvInfo.getModifyUser();
            Timestamp modifyDate = tmxTuvInfo.getModifyDate();

            String tuv = operateCDATA(tuvText);
            result.append("<tuv xml:lang=\"");
            result.append(tuvLocale.replace("_", "-")).append("\"");

            if (creationUser != null)
            {
                result.append(" ").append(Tmx.CREATIONID).append("=\"")
                        .append(EditUtil.encodeXmlEntities(creationUser))
                        .append("\"");
            }
            if (creationDate != null)
            {
                result.append(" ").append(Tmx.CREATIONDATE).append("=\"")
                        .append(UTC.valueOfNoSeparators(creationDate))
                        .append("\"");
            }
            if (modifyUser != null && !modifyUser.equals(creationUser))
            {
                result.append(" ").append(Tmx.CHANGEID).append("=\"")
                        .append(EditUtil.encodeXmlEntities(modifyUser))
                        .append("\"");
            }
            if (modifyDate != null && !modifyDate.equals(creationDate))
            {
                result.append(" ").append(Tmx.CHANGEDATE).append("=\"")
                        .append(UTC.valueOfNoSeparators(modifyDate))
                        .append("\"");
            }

            result.append(">\r\n");
            result.append(convertToTmxLevel(tuv, p_tmxLevel));
            result.append("</tuv>\r\n");
        }

        return result.toString();
    }

    //Encodes Greater Than and Less Than in CDATA element.
    public static String operateCDATA(String p_segText)
    {
        String segment = p_segText;
        String pattern = "!\\[CDATA.*?\\]\\]";
        Pattern r = Pattern.compile(pattern);
        Matcher matcher = r.matcher(segment);
        while (matcher.find())
        {
            String orig = matcher.group();
            segment = segment.replace(orig,
                    orig.replace("<", "&lt;").replace(">", "&gt;"));
        }
        
        return segment;
    }

    public static String composeTuTail()
    {
        return "</tu>\r\n";
    }

    private static String convertToTmxLevel(String p_xml, int p_tmxLevel)
    {
        Document dom = null;
    	try {
    		//normal operation
            dom = getDom("<seg>" + p_xml + "</seg>");    		
    	} catch (RuntimeException re) {
            //p_xml may contain "<" or ">" which will result in parse error,so encode it.
        	String p_xml2 = EditUtil.encodeHtmlEntities(p_xml);
        	dom = getDom("<seg>" + p_xml2 + "</seg>");
    	}

        Element root = dom.getRootElement();
        if (p_tmxLevel == TMX_LEVEL_ONE)
        {
            String[] strs = { "//bpt", "//ept", "//ph", "//it", "//ut", "//hi" };
            replaceNbsps(root);
            for (int i = 0; i < strs.length; i++)
            {
                removeNodes(root, strs[i]);
            }
        }
        else if (p_tmxLevel == TMX_LEVEL_TWO)
        {
            String[] attributes = { "locType", "wordcount", "erasable", "movable" };
            // TMX Compliance: nbsp must be output as character.
            replaceNbsps(root);
            // Remove any SUB tags (but TM2 doesn't contain any).
            removeSubElements(root);
            removeUncompliantAttributes(root, attributes);
            removeAttributeForNode(root, "it", new String[]{ "i" });
        }

        return root.asXML();
    }

    public static String convertXlfToTmxFormat(String p_xml)
    {
        s_logger.debug("Begin convert xlf to tmx: " + p_xml);
        Document dom = null;

        try
        {
            dom = getDom("<seg>" + p_xml + "</seg>");
        }
        catch (RuntimeException e)
        {
            return p_xml;
        }

        Element root = dom.getRootElement();
        // String[] strs = { "//bpt", "//ept", "//ph", "//it", "//ut", "//hi" };
        String[] attributes =
        { "locType", "wordcount", "erasable", "movable" };
        //removeEmbedTags(root);
        // TMX Compliance: nbsp must be output as character.
        // GBS-587, comment out nbsp converting for testing patch 7.1.4.1
        // replaceNbsps(root);
        // Remove any SUB tags (but TM2 doesn't contain any).
        removeSubElements(root);
        removeUncompliantAttributes(root, attributes);
        removeAttributeForNode(root, "it", new String[]
        { "i" });
        String result = root.asXML();

        s_logger.debug("The result string: " + result);

        int firstIndex = 5;
        int endIndex = result.length() - 6;

        if (endIndex < firstIndex)
        {
            return "";
        }

        return result.substring(firstIndex, endIndex);
    }

    /**
     * Remove the embed tags to consist with the TM. EX: The input might
     * contains: <bpt type="bold" x="1">&lt;b&gt;</bpt>, Just remove
     * "&lt;b&gt;".
     * 
     * @param root
     *            the root element.
     */
    private static void removeEmbedTags(Element root)
    {
        List elems = root.elements();
        for (int i = 0; i < elems.size(); i++)
        {
            Object obj = elems.get(i);
            if (obj instanceof org.dom4j.Element)
            {
                org.dom4j.Element element = (org.dom4j.Element) obj;
                element.clearContent();
            }
        }
    }

    private static void removeAttributeForNode(Element root, String nodeName,
            String[] attributes)
    {
        List elements = root.elements(nodeName);
        Iterator it = elements.iterator();
        while (it.hasNext())
        {
            Element element = (Element) it.next();
            for (int i = 0; i < attributes.length; i++)
            {
                Attribute attribute = element.attribute(attributes[i]);
                if (attribute != null)
                {
                    element.remove(element.attribute(attributes[i]));
                }
            }
        }
    }

    private static void removeUncompliantAttributes(Element root,
            String[] attributes)
    {
        List elements = root.elements();
        Iterator it = elements.iterator();
        while (it.hasNext())
        {
            Element element = (Element) it.next();
            for (int i = 0; i < attributes.length; i++)
            {
                Attribute attribute = element.attribute(attributes[i]);
                if (attribute != null)
                {
                    element.remove(element.attribute(attributes[i]));
                }
            }
        }
    }

    private static Document getDom(String p_xml)
    {
        XmlParser parser = null;

        try
        {
            parser = XmlParser.hire();
            return parser.parseXml(p_xml);
        }
        catch (Exception ex)
        {
            throw new RuntimeException("invalid GXML `" + p_xml + "': "
                    + ex.getMessage());
        }
        finally
        {
            XmlParser.fire(parser);
        }
    }

    public static void replaceNbsps(Element p_seg)
    {
        ArrayList elems = new ArrayList();

        findNbspElements(elems, p_seg);

        for (int i = 0; i < elems.size(); i++)
        {
            Element elem = (Element) elems.get(i);

            replaceNbsp(elem);
        }
    }

    public static void removeNodes(Element p_segment, String p_path)
    {
        List nodes = p_segment.selectNodes(p_path);

        for (int i = 0; i < nodes.size(); i++)
        {
            Node node = (Node) nodes.get(i);

            node.detach();
        }
    }

    public static void findNbspElements(ArrayList p_result, Element p_element)
    {
        // Depth-first traversal: add embedded <ph x-nbspace> to the list first.
        for (int i = 0, max = p_element.nodeCount(); i < max; i++)
        {
            Node child = (Node) p_element.node(i);

            if (child instanceof Element)
            {
                findNbspElements(p_result, (Element) child);
            }
        }

        if (p_element.getName().equals("ph"))
        {
            String attr = p_element.attributeValue("type");

            if (attr != null && attr.equals("x-nbspace"))
            {
                p_result.add(p_element);
            }
        }
    }

    /**
     * Removes the given <sub> element from the segment. <sub> is special since
     * it does not only surround embedded tags but also text, which must be
     * pulled out of the <sub> and added to the parent tag.
     */
    public static void replaceNbsp(Element p_element)
    {
        Element parent = p_element.getParent();
        int index = parent.indexOf(p_element);

        // We copy the current content, clear out the parent, and then
        // re-add the old content, inserting the <sub>'s textual
        // content instead of the <sub> (this clears any embedded TMX
        // tags in the subflow).

        ArrayList newContent = new ArrayList();
        List content = parent.content();

        for (int i = content.size() - 1; i >= 0; --i)
        {
            Node node = (Node) content.get(i);

            newContent.add(node.detach());
        }

        Collections.reverse(newContent);
        parent.clearContent();

        for (int i = 0, max = newContent.size(); i < max; ++i)
        {
            Node node = (Node) newContent.get(i);

            if (i == index)
            {
                parent.addText("\u00A0");
            }
            else
            {
                parent.add(node);
            }
        }
    }

    /**
     * Removes all <sub> elements from the segment. <sub> is special since it
     * does not only surround embedded tags but also text, which must be pulled
     * out of the <sub> and added to the parent tag.
     */
    public static void removeSubElements(Element p_seg)
    {
        ArrayList elems = new ArrayList();

        findSubElements(elems, p_seg);

        for (int i = 0; i < elems.size(); i++)
        {
            Element elem = (Element) elems.get(i);

            removeSubElement(elem);
        }
    }

    public static void findSubElements(ArrayList p_result, Element p_element)
    {
        // Depth-first traversal: add embedded <sub> to the list first.
        for (int i = 0, max = p_element.nodeCount(); i < max; i++)
        {
            Node child = (Node) p_element.node(i);

            if (child instanceof Element)
            {
                findSubElements(p_result, (Element) child);
            }
        }

        if (p_element.getName().equals("sub"))
        {
            p_result.add(p_element);
        }
    }

    /**
     * Removes the given <sub> element from the segment. <sub> is special since
     * it does not only surround embedded tags but also text, which must be
     * pulled out of the <sub> and added to the parent tag.
     */
    public static void removeSubElement(Element p_element)
    {
        Element parent = p_element.getParent();
        int index = parent.indexOf(p_element);

        // We copy the current content, clear out the parent, and then
        // re-add the old content, inserting the <sub>'s textual
        // content instead of the <sub> (this clears any embedded TMX
        // tags in the subflow).

        ArrayList newContent = new ArrayList();
        List content = parent.content();

        for (int i = content.size() - 1; i >= 0; --i)
        {
            Node node = (Node) content.get(i);

            newContent.add(node.detach());
        }

        Collections.reverse(newContent);
        parent.clearContent();

        for (int i = 0, max = newContent.size(); i < max; ++i)
        {
            Node node = (Node) newContent.get(i);

            if (i == index)
            {
                parent.addText(p_element.getText());
            }
            else
            {
                parent.add(node);
            }
        }
    }

    static class TmxTuvInfo
    {
        String tuvText = null;
        String tuvLocale = null;
        String creationUser = null;
        String modifyUser = null;
        Timestamp creationDate = null;
        Timestamp modifyDate = null;

        TmxTuvInfo(String tuvText, String tuvLocale, String creationUser,
                Timestamp creationDate, String modifyUser, Timestamp modifyDate)
        {
            this.tuvText = tuvText;
            this.tuvLocale = tuvLocale;
            this.creationUser = creationUser;
            this.creationDate = creationDate;
            this.modifyUser = modifyUser;
            this.modifyDate = modifyDate;
        }

        TmxTuvInfo(String tuvText, String tuvLocale, String creationUser,
                Date creationDate, String modifyUser, Date modifyDate)
        {
            this.tuvText = tuvText;
            this.tuvLocale = tuvLocale;
            this.creationUser = creationUser;
            if (creationDate != null)
            {
                this.creationDate = new Timestamp(creationDate.getTime());                
            }
            this.modifyUser = modifyUser;
            if (modifyDate != null)
            {
                this.modifyDate = new Timestamp(modifyDate.getTime());                
            }
        }

        String getTuvText()
        {
            return this.tuvText;
        }
        
        String getTuvLocale()
        {
            return this.tuvLocale;
        }

        String getCreationUser()
        {
            return EditUtil.encodeXmlEntities(UserUtil
                    .getUserNameById(this.creationUser));
        }

        Timestamp getCreationDate()
        {
            return this.creationDate;
        }

        String getModifyUser()
        {
            return EditUtil.encodeXmlEntities(UserUtil
                    .getUserNameById(this.modifyUser));
        }

        Timestamp getModifyDate()
        {
            return this.modifyDate;
        }

    }
}
