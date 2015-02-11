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
package com.globalsight.ling.docproc.extractor.xml;

// Java
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.globalsight.cxe.entity.filterconfiguration.Filter;
import com.globalsight.cxe.entity.filterconfiguration.XMLRuleFilter;
import com.globalsight.cxe.entity.filterconfiguration.XmlFilterConfigParser;
import com.globalsight.cxe.entity.filterconfiguration.XmlFilterConstants;
import com.globalsight.ling.common.RegEx;
import com.globalsight.ling.common.RegExException;
import com.globalsight.ling.common.RegExMatchInterface;
import com.globalsight.ling.common.XmlEntities;
import com.globalsight.ling.docproc.AbstractExtractor;
import com.globalsight.ling.docproc.DocumentElement;
import com.globalsight.ling.docproc.DocumentElementException;
import com.globalsight.ling.docproc.ExtractorException;
import com.globalsight.ling.docproc.ExtractorExceptionConstants;
import com.globalsight.ling.docproc.ExtractorInterface;
import com.globalsight.ling.docproc.ExtractorRegistry;
import com.globalsight.ling.docproc.IFormatNames;
import com.globalsight.ling.docproc.Output;
import com.globalsight.ling.docproc.Segmentable;
import com.globalsight.ling.docproc.SkeletonElement;
import com.globalsight.ling.docproc.TranslatableElement;
import com.globalsight.util.StringUtil;
import com.globalsight.util.edit.SegmentUtil;

/**
 * XML Extractor.
 * 
 * <p>
 * The behavior of the XML Extractor is rule-file-driven (see schemarules.dtd).
 * If no rules are specified, the default rules are:
 * </p>
 * 
 * <ul>
 * <li>Contents of all element nodes are extracted as translatable.</li>
 * <li>All elements break a segment. In other words, all tags go into the
 * skeleton and no tag is included in an extracted translatable segment.</li>
 * <li>No attributes are extracted.</li>
 * </ul>
 * 
 * The rule file basically contains two sets of rules:
 * 
 * <ol>
 * <li>&lt;dont-translate&gt; elements specify elements or attributes that
 * should not be extracted.</li>
 * 
 * <li>&lt;translate&gt; elements specify elements or attributes that are to be
 * extracted for translation or localization.</li>
 * </ol>
 * 
 * <p>
 * Attributes on &lt;translatable&gt;:
 * </p>
 * 
 * <ul>
 * <li>path: XPath expression to address the elements and attributes that are
 * to be extracted for translation or localization.</li>
 * 
 * <li>loctype: Localization type. Specifies whether the extracted data are
 * translatable or localizable. Possible values are "translatable" or
 * "localizable". The default value is "translatable".</li>
 * 
 * <li>datatype: Format of the data. If the extracted data needs further
 * extraction, the data format should be specified in this attribute. The
 * typical use case is that a HTML snippet is stored in an XML element. When the
 * datatype attribute has the value "html", the XML extractor extracts the
 * content of the element and calls the HTML extractor, passing it the extracted
 * content.</li>
 * 
 * <li>type: Type of the data. This attribute is used when the type of the
 * extracted data needs to be explicitly specified. Examples of types are
 * "link", "bold", "underline" etc.</li>
 * 
 * <li>inline: This attribute specifies whether the elements specified by the
 * path attribute break a segment. If an element breaks a segment, the element
 * tag is not included in the extracted data. If an element does not break a
 * segment (if the tag is inline), the element tag is included in the extracted
 * data. Possible values for the attribute are "yes" or "no". "yes" means the
 * tag does not break segments. "no" means the tag breaks segments. The default
 * value is "no".</li>
 * 
 * <li>movable: the DiplomatXML attribute for bpt,it,ut,ph tags, specifying
 * whether these tags can be moved around in the editor.</li>
 * 
 * <li>erasable: the DiplomatXML attribute for bpt,it,ut,ph tags, specifying
 * whether these tags can be deleted in the editor.</li>
 * </ul>
 * 
 * <p>
 * When multiple rules match a single node, the rules are merged according to
 * the algorithm in Rule.java. A side-effect of merging is that the first
 * matching rule determines whether a node is translatable or not; sub-sequent
 * rule matches will never change the type of the first rule.
 * </p>
 * 
 * <p>
 * A tag that switches to a different extractor can not be embeddable.
 * </p>
 */
public class Extractor extends AbstractExtractor implements ExtractorInterface,
        EntityResolver, ExtractorExceptionConstants, ErrorHandler
{
    private ExtractorAdmin m_admin = new ExtractorAdmin(null);

    // Rules Engine.
    private RuleSet m_rules = null;
    private Map m_ruleMap = null;
    private boolean m_useEmptyTag = true;

    // XML declaration flags
    private boolean m_haveXMLDecl = false;
    private String m_version = null;
    private String m_standalone = null;
    private String encoding = null;

    // XML encoder
    private XmlEntities m_xmlEncoder = new XmlEntities();

    // for extractor switching
    private String m_switchExtractionBuffer = new String();
    private String m_otherFormat = null;

    // for xml converted from InDesign files.
    private final String PARAGRAPH_NODE_NAME = "Inddgsparagraph";
    private final String PARAGRAPH_HAS_DIFFERENT_STYLE = "hasDifferentStyle";
    private final String VALUE_TRUE = "true";
    private final String VALUE_FALSE = "false";
    
    // for xml filter implement
    private XMLRuleFilter m_xmlFilter = null;
    private XmlFilterHelper m_xmlFilterHelper = null;
    private boolean m_checkWellFormed = true;
    private String m_elementPostFormat = null;
    private String m_cdataPostFormat = null;
    private boolean m_isElementPost = false;
    private boolean m_isElementPostToHtml = false;
    private boolean m_isOriginalXmlNode = false;
    private boolean m_isCdataPost = false;
    
    // for office xml
    private boolean m_isOfficeXml = false;
    protected boolean m_isIdmlXml = false;

    //
    // Constructors
    //
    public Extractor()
    {
        super();

        m_admin = new ExtractorAdmin(null);
        m_rules = new RuleSet();
        m_ruleMap = null;
        m_haveXMLDecl = false;
        m_version = null;
        m_standalone = null;
        encoding = null;
        m_switchExtractionBuffer = new String();
        m_otherFormat = null;
        m_elementPostFormat = null;
        m_cdataPostFormat = null;
    }

    //
    // Will be overwritten in classes derived from XML extractor (eBay PRJ)
    //
    public void setFormat()
    {
        setMainFormat(ExtractorRegistry.FORMAT_XML);
    }

    /**
     * Extracts the input document.
     * 
     * Parses the XML File into DOM using xerces.
     * 
     * Skips the external entity (DTD, etc) by providing a null byte array.
     * 
     * Then invokes domNodeVisitor for the Document 'Node' ('virtual root') to
     * traverse the DOM tree recursively, using the AbstractExtractor API to
     * write out skeleton and segments.
     */
    public void extract() throws ExtractorException
    {
        try
        {
            // Set the main format depending on which (derived) class
            // we're called in.
            setFormat();
            
            m_isOfficeXml = ExtractorRegistry.FORMAT_OFFICE_XML.equals(getMainFormat());
            
            // init for xml filter
            Filter mainFilter = getMainFilter();
            m_xmlFilter = (mainFilter != null && mainFilter instanceof XMLRuleFilter) ? (XMLRuleFilter) mainFilter : null;
            m_xmlFilterHelper = new XmlFilterHelper(m_xmlFilter);
            m_xmlFilterHelper.init();
            m_xmlFilterHelper.setXmlEntities(m_xmlEncoder);
            m_checkWellFormed = m_xmlFilterHelper.isCheckWellFormed();
            
            if (m_checkWellFormed)
            {
                XmlFilterChecker.checkWellFormed(readInput());
            }

            GsDOMParser parser = new GsDOMParser();

            // don't read external DTDs
            parser.setEntityResolver(this);
            // provide detailed error report
            parser.setErrorHandler(this);

            // parse and create DOM tree
            parser.parse(new InputSource(readInput()));

            // preserve the values in the inputs' XML declaration
            m_haveXMLDecl = parser.getHaveXMLDecl();
            m_version = parser.getXMLVersion();
            m_standalone = parser.getStandalone();
            encoding = parser.getEncoding();
            
            // for xml filter implement
            m_elementPostFormat = m_xmlFilterHelper.getElementPostFormat();
            m_isElementPost = m_xmlFilterHelper.isElementPostFilter();
            m_isElementPostToHtml = m_isElementPost ? (IFormatNames.FORMAT_HTML.equals(m_elementPostFormat)) : false;
            m_cdataPostFormat = m_xmlFilterHelper.getCdataPostFormat();
            m_isCdataPost = m_xmlFilterHelper.isCdataPostFilter();

            // get rule map for the document
            m_ruleMap = m_rules.buildRulesWithFilter(parser.getDocument(), m_xmlFilterHelper.getXmlFilterTags(), getMainFormat());
            m_useEmptyTag = m_rules.usesEmptyTag();
            m_useEmptyTag = m_xmlFilterHelper.usesEmptyTag();

            // traverse the DOM tree
            Node doc = parser.getDocument();
            domNodeVisitor(doc, false, true, false);
        }
        catch (Exception e)
        {
            throw new ExtractorException(e);
        }
    }

    /**
     * This method is invoked by AbstractExractor framework. It is used to point
     * the XML Extracator to the file containing the XML extraction rules.
     * 
     * If the path to Extraction Rules is not specified via
     * Input.m_strProjectRules, it defaults to "file:/gsrules.xml". (CvdL: I
     * think it defaults to a null string.)
     */
    public void loadRules() throws ExtractorException
    {
        String ruleString = getInput().getRules();
        m_rules.loadRule(ruleString);
    }

    /** Provide an alternate way to load rules */
    public void loadRules(String p_rules) throws ExtractorException
    {
        m_rules.loadRule(p_rules);
    }

    /**
     * Overrides EntityResolver#resolveEntity.
     * 
     * The purpose of this method is to read Schemarules.dtd from resource and
     * feed it to the validating parser, but what it really does is returning a
     * null byte array to the XML parser.
     */
    public InputSource resolveEntity(String publicId, String systemId)
            throws SAXException, IOException
    {
        return new InputSource(new ByteArrayInputStream(new byte[0]));
    }

    // ErrorHandler interface methods

    public void error(SAXParseException e) throws SAXException
    {
        throw new SAXException("XML parse error at\n  line "
                + e.getLineNumber() + "\n  column " + e.getColumnNumber()
                + "\n  Message:" + e.getMessage());
    }

    public void fatalError(SAXParseException e) throws SAXException
    {
        error(e);
    }

    public void warning(SAXParseException e)
    {
        System.err.println("XML parse warning at\n  line " + e.getLineNumber()
                + "\n  column " + e.getColumnNumber() + "\n  Message:"
                + e.getMessage());
    }

    private void outputXMLDeclaration()
    {
        outputSkeleton("<?xml");
        if (m_version != null)
        {
            outputSkeleton(" version=\"" + m_version + "\"");
        }
        if (encoding != null)
        {
            outputSkeleton(" encoding=\"" + encoding + "\"");
        }
        if (m_standalone != null)
        {
            outputSkeleton(" standalone=\"" + m_standalone + "\"");
        }
        outputSkeleton(" ?>\n");
    }

    private void docTypeProcessor(DocumentType docType)
    {
        String systemId = docType.getSystemId();
        String publicId = docType.getPublicId();
        String internalSubset = docType.getInternalSubset();
        if (systemId != null || publicId != null || internalSubset != null)
        {
            outputSkeleton("<!DOCTYPE " + docType.getName() + " ");

            String externalId = null;

            if (systemId != null && publicId != null)
            {
                externalId = "PUBLIC \"" + publicId + "\" \"" + systemId + "\"";
            }
            else if (systemId != null)
            {
                externalId = "SYSTEM \"" + systemId + "\"";
            }

            if (externalId != null)
            {
                outputSkeleton(externalId);
            }
            if (internalSubset != null)
            {
                outputSkeleton(" [" + internalSubset + "]>\n");
            }
            else
            {
                outputSkeleton(">\n");
            }
        }
    }

    private void commentProcessor(boolean switchesExtraction, Node p_node,
            boolean isInExtraction, boolean isTranslatable)
    {
        if (switchesExtraction)
        {
            outputOtherFormat();
        }

        if (!processGsaSnippet(p_node.getNodeValue()))
        {
            String comment = "<!--" + p_node.getNodeValue() + "-->";

            if (isInExtraction)
            {
                String stuff = "<ph type=\"comment\">"
                        + m_xmlEncoder.encodeStringBasic(comment) + "</ph>";
                outputExtractedStuff(stuff, isTranslatable, false);
            }
            else
            {
                outputSkeleton(comment);
            }
        }
        // else: the GSA snippet is to be ignored.
    }

    private void outputPi(Node p_node, boolean switchesExtraction,
            boolean isInExtraction, boolean isTranslatable)
    {
        if (switchesExtraction)
            outputOtherFormat();
        
        String nodeName = p_node.getNodeName();
        XmlFilterProcessIns xmlPI = m_xmlFilterHelper.getMatchedProcessIns(nodeName);
        String piString = "<?" + nodeName + " " + p_node.getNodeValue() + "?>";
        boolean handled = false;
        
        if (xmlPI != null)
        {
            if (xmlPI.getHandleType() == XmlFilterConstants.PI_MARKUP)
            {
                outputSkeleton(piString);
                handled = true;
            }
            else if (xmlPI.getHandleType() == XmlFilterConstants.PI_MARKUP_EMB)
            {
                if (isInExtraction)
                {
                    String stuff = "<ph type=\"pi\">" + m_xmlEncoder.encodeStringBasic(piString)
                            + "</ph>";
                    outputExtractedStuff(stuff, isTranslatable, false);
                }
                else
                {
                    outputSkeleton(piString);
                }
                
                handled = true;
            }
            else if (xmlPI.getHandleType() == XmlFilterConstants.PI_REMOVE)
            {
                handled = true;
            }
            else
            {
                handled = false;
            }
        }
        
        if (!handled)
        {
            if (isInExtraction)
            {
                String stuff = "<ph type=\"pi\">"
                        + m_xmlEncoder.encodeStringBasic(piString) + "</ph>";
                outputExtractedStuff(stuff, isTranslatable, false);
            }
            else
                outputSkeleton(piString);
        }
    }

    private void textProcessor(Node p_node, boolean switchesExtraction,
            boolean isInExtraction, boolean isTranslatable,
            boolean... isTextNodeDontTranslateInline)
    {
        if (isInExtraction)
        {
            // Marks words that not need count and translate.
            Set words = Rule.getWords(m_ruleMap, p_node);
            if (words != null && words.size() > 0)
            {
                String text = p_node.getNodeValue();
                Iterator iterator = words.iterator();
                while (iterator.hasNext())
                {
                    String w = (String) iterator.next();
                    String w1 = w.trim();
                    
                    StringBuffer temp = new StringBuffer("<");
                    temp.append(SegmentUtil.XML_NOTCOUNT_TAG).append(">");
                    temp.append(w1).append("</");
                    temp.append(SegmentUtil.XML_NOTCOUNT_TAG).append(">");
                    
                    String w2 = StringUtil.replace(w, w1, temp.toString());
                    text = StringUtil.replace(text, w, w2);
                }

                p_node.setNodeValue(text);
            }
            
            if (switchesExtraction || m_isElementPost)
            {
                m_switchExtractionBuffer += p_node.getNodeValue();
            }
            else
            {
                String nodeValue = p_node.getNodeValue();

                // Special treat for xml files coverted from Indesign
                // files (.indd)
                // Because we have inserted font style information into
                // the text of
                // a paragraph (a unit in Indesign application) if this
                // paragraph's text
                // have different font styles, we should process these
                // font style information
                // tags.
                // The purpose of processing to these tags is for them
                // being converted into
                // Ptags easily.
                String newValue = null;
                Node parentNode = p_node.getParentNode();
                if (parentNode != null
                        && parentNode.getNodeName().equals(PARAGRAPH_NODE_NAME))
                {
                    // Here we are in a Indesign file's paragraph text
                    NamedNodeMap attrs = parentNode.getAttributes();
                    String attname = null;
                    String value = null;
                    for (int i = 0; i < attrs.getLength(); ++i)
                    {
                        Node att = attrs.item(i);
                        attname = att.getNodeName();
                        if (attname.equals(PARAGRAPH_HAS_DIFFERENT_STYLE))
                        {
                            value = att.getNodeValue();
                            break;
                        }
                    }
                    if (value != null && value.equals(VALUE_TRUE))
                    {
                        // This attribute indicates the text of this
                        // paragraph has different
                        // font styles.
                        newValue = markIndesignParagraphTextStyleTag(nodeValue);
                    }
                }

                String sid = Rule.getSid(m_ruleMap, p_node);

                if (newValue != null)
                {
                    outputExtractedStuff(newValue, isTranslatable, false);
                }
                else
                {
                    boolean isInline = Rule.isInline(m_ruleMap, getChildNode(
                            parentNode, 1));
                    boolean isPreserveWS = Rule.isPreserveWhiteSpace(m_ruleMap,
                            parentNode, m_xmlFilterHelper
                                    .isPreserveWhiteSpaces());
                    String temp = m_xmlFilterHelper.processText(nodeValue,
                            isInline, isPreserveWS);
                    outputExtractedStuff(temp, isTranslatable, isPreserveWS);
                }
                setSid(sid);
            }
        }
        else
        {
            Node parentNode = p_node.getParentNode();
            boolean parentInline = Rule.isInline(m_ruleMap, parentNode);
            if (parentInline)
            {
                outputExtractedStuff(m_xmlEncoder.encodeStringBasic(p_node
                        .getNodeValue()), isTranslatable, false);
                if (isTextNodeDontTranslateInline != null
                        && !isTextNodeDontTranslateInline[0])
                {
                    outputExtractedStuff("</bpt>", isTranslatable, false);
                }
            }
            else
            {
                outputSkeleton(m_xmlEncoder.encodeStringBasic(p_node
                        .getNodeValue()));
            }
        }
    }

    private void cdataProcessor(Node p_node, boolean switchesExtraction,
            boolean isInExtraction, boolean isTranslatable)
    {
        XmlFilterCDataTag tag = m_xmlFilterHelper.getRuleForCData(p_node);
        boolean isCdataTranslatable = (tag == null || tag.isTranslatable());
        // String cdata = "<![CDATA[" + p_node.getNodeValue() + "]]>";

        // (CvdL: yes, this will lose CDATA nodes.)
        // 2006-09-12 Updated For CDATA issue: this will pick up CDATA
        // nodes.
        if (isInExtraction && isCdataTranslatable)
        {
            Filter postFilter = null;
            try
            {
                postFilter = (tag != null) ? tag.getPostFilter() : null;
            }
            catch (Exception e)
            {
                CATEGORY.error("Can not get post filter for CData", e);
            }
            
            String otherFormat = (postFilter != null)? m_xmlFilterHelper.getFormatForFilter(postFilter.getFilterTableName()) : null;
            
            if (switchesExtraction || m_isCdataPost || postFilter != null)
            {
                outputOtherFormat();
                outputSkeleton("<![CDATA[");
                m_switchExtractionBuffer += p_node.getNodeValue();
                
                if (postFilter != null && otherFormat != null)
                {
                    outputOtherFormatForCdata(otherFormat, postFilter, false);
                }
                else if (m_isCdataPost)
                {
                    outputOtherFormatForCdata(null, null, true);
                }
                else
                {
                    outputOtherFormatForCdata(null, null, false);
                }
                
                outputSkeleton("]]>");
            }
            else
            {
                outputSkeleton("<![CDATA[");
                outputExtractedStuff(m_xmlEncoder.encodeStringBasic(p_node
                        .getNodeValue()), isTranslatable, false);
                outputSkeleton("]]>");
            }
        }
        else
        {
            outputSkeleton("<![CDATA[" + p_node.getNodeValue() + "]]>");
        }
    }

    private void entityProcessor(Node p_node, boolean switchesExtraction,
            boolean isInExtraction, boolean isTranslatable)
    {
        String entityTag = p_node.getNodeName();
        String name = "&" + entityTag + ";";
        XmlFilterEntity xmlEntity = m_xmlFilterHelper.getMatchedXmlFilterEntity(entityTag);
        boolean handled = false;
        
        if (xmlEntity != null && !switchesExtraction && !m_isElementPost)
        {
            if (xmlEntity.getHandleType() == XmlFilterConstants.ENTITY_PLACEHOLDER)
            {
                if (isInExtraction)
                {
                    String entityRef = m_xmlEncoder.encodeStringBasic(name);
                    
                    StringBuffer temp = new StringBuffer();
                    temp.append("<ph type=\"").append("entity-");
                    temp.append(entityTag).append("\">").append(entityRef);
                    temp.append("</ph>");
                    
                    String stuff = temp.toString();
                    outputExtractedStuff(stuff, isTranslatable, false);
                }
                else
                {
                    outputSkeleton(name);
                }
                handled = true;
            }
            else if (xmlEntity.getHandleType() == XmlFilterConstants.ENTITY_TEXT)
            {
                if (xmlEntity.getSaveAs() == XmlFilterConstants.ENTITY_SAVE_AS_ENTITY)
                {
                    if (isInExtraction)
                    {
                        String entityRef = m_xmlEncoder.encodeStringBasic(name);
                        
                        StringBuffer temp = new StringBuffer();
                        temp.append("<ph type=\"").append("entity-");
                        temp.append(entityTag).append("\">").append(entityRef);
                        temp.append("</ph>");
                        
                        String stuff = temp.toString();
                        outputExtractedStuff(stuff, isTranslatable, false);
                    }
                    else
                    {
                        outputSkeleton(name);
                    }
                    handled = true;
                }
                else if (xmlEntity.getSaveAs() == XmlFilterConstants.ENTITY_SAVE_AS_CHAR)
                {
                    m_admin.addContent(xmlEntity.getEntityCharacter());
                    handled = true;
                }
                else
                {
                    handled = false;
                }
            }
            else
            {
                handled = false;
            }
        }

        if (!handled)
        {
            if (isInExtraction)
            {
                if (switchesExtraction || m_isElementPost)
                {
                    m_switchExtractionBuffer += name;
                }
                else
                {
                    String entityRef = m_xmlEncoder.encodeStringBasic(name);
                    
                    StringBuffer temp = new StringBuffer();
                    temp.append("<ph type=\"").append("entity-");
                    temp.append(entityTag).append("\">").append(entityRef);
                    temp.append("</ph>");
                    
                    String stuff = temp.toString();
                    
                    outputExtractedStuff(stuff, isTranslatable, false);
                }
            }
            else
            {
                outputSkeleton(name);
            }
        }
    }

    /**
     * A visitor that recursivly traverses the input document. Element nodes are
     * handed off to domElementProcessor().
     */
    private void domNodeVisitor(Node p_node, boolean isInExtraction,
            boolean isTranslatable, boolean switchesExtraction,
            boolean... isTextNodeDontTranslateInline) throws ExtractorException
    {
        while (true)
        {
            if (p_node == null)
            {
                return;
            }

            switch (p_node.getNodeType())
            {
            case Node.DOCUMENT_NODE: // the document itself
                // XML Declaration
                if (m_haveXMLDecl)
                    outputXMLDeclaration();

                // Document Type Declaration <!DOCTYPE...>
                DocumentType docType = ((Document) p_node).getDoctype();
                if (docType != null)
                    docTypeProcessor(docType);

                domNodeVisitor(p_node.getFirstChild(), isInExtraction,
                        isTranslatable, switchesExtraction);

                return;

            case Node.PROCESSING_INSTRUCTION_NODE: // PI
                outputPi(p_node, switchesExtraction, isInExtraction,
                        isTranslatable);
                p_node = p_node.getNextSibling();

                break;

            case Node.ELEMENT_NODE:
                domElementProcessor(p_node, isInExtraction, switchesExtraction);
                p_node = p_node.getNextSibling();

                break;

            case Node.COMMENT_NODE:
                commentProcessor(switchesExtraction, p_node, isInExtraction,
                        isTranslatable);
                p_node = p_node.getNextSibling();

                break;

            case Node.ENTITY_REFERENCE_NODE:
                entityProcessor(p_node, switchesExtraction, isInExtraction,
                        isTranslatable);
                p_node = p_node.getNextSibling();

                break;

            case Node.TEXT_NODE:
                textProcessor(p_node, switchesExtraction, isInExtraction,
                        isTranslatable, isTextNodeDontTranslateInline);
                p_node = p_node.getNextSibling();

                break;

            case Node.CDATA_SECTION_NODE:
                // String cdata = "<![CDATA[" + p_node.getNodeValue() + "]]>";
                // (CvdL: yes, this will lose CDATA nodes.)
                // 2006-09-12 Updated For CDATA issue: this will pick up CDATA
                // nodes.
                cdataProcessor(p_node, switchesExtraction, isInExtraction,
                        isTranslatable);

                p_node = p_node.getNextSibling();

                break;

            default:
                // shouldn't reach here.
                // outputSkeleton(domDumpXML(p_node));
                domNodeVisitor(p_node.getNextSibling(), false, isTranslatable,
                        switchesExtraction);

                return;
            }
        }
    }

    /**
     * This method is just for xml files converted from Indesign files (.indd).
     * Mark a font style tag by TMX specification tags. After being marked by
     * TMX specification tags, these font style tag can be easily converted into
     * Ptags.
     * 
     * @param p_paraText
     * @return
     */
    private String markIndesignParagraphTextStyleTag(String p_paraText)
    {
        StringBuffer newParaText = new StringBuffer();
        int bptIndex = 0;
        // [contents-contents-contents]
        Pattern openTagPattern = Pattern
                .compile("\\[([^/-]+-[^-]+-[^\\]]+)\\]");
        // [/contents-contents-contents]
        Pattern closeTagPattern = Pattern
                .compile("\\[/([^-]+-[^-]+-[^\\]]+)\\]");
        Matcher openMatcher = openTagPattern.matcher(p_paraText);
        Matcher closeMatcher = closeTagPattern.matcher(p_paraText);
        while (openMatcher.find())
        {
            String tagName = openMatcher.group(1);
            int openTagEndIndex = openMatcher.end();
            if (closeMatcher.find(openTagEndIndex)
                    && tagName.equals(closeMatcher.group(1)))
            {
                bptIndex = m_admin.incrementBptIndex();
                // Mark open font style tag
                newParaText.append("<bpt i=\"" + bptIndex + "\" type=\"");
                newParaText.append(tagName.toString() + "\"");
                newParaText.append(">[" + tagName.toString() + "]</bpt>");
                // Append translatable fragment
                newParaText.append(m_xmlEncoder.encodeStringBasic(p_paraText
                        .substring(openTagEndIndex, closeMatcher.start())));
                // Mark close font style tag
                newParaText.append("<ept i=\"" + bptIndex + "\">[/");
                newParaText.append(tagName.toString() + "]</ept>");
            }
        }
        return newParaText.length() > 0 ? newParaText.toString() : p_paraText;
    }

    /**
     * Recursively processes an element node, its attributes and children. The
     * rules are consulted to determine whether the node needs to be extracted
     * etc. Attributes are handed off to outputAttributes(), and all nodes below
     * this node are passed to domNodeVisitor().
     */
    private void domElementProcessor(Node p_node, boolean isInExtraction,
            boolean switchesExtraction) throws ExtractorException
    {
        String name = p_node.getNodeName();
        int bptIndex = 0;

        String dataFormat = null;
        String type = null;
        boolean isEmbeddable = false;
        boolean isTranslatable = true;
        boolean isMovable = true;
        boolean isErasable = false;
        boolean extracts = Rule.extracts(m_ruleMap, p_node);
        boolean containedInHtml = Rule.isContainedInHtml(m_ruleMap, p_node);
        isEmbeddable = Rule.isInline(m_ruleMap, p_node);
        boolean isInternal = Rule.isInternal(m_ruleMap, p_node);
        boolean isPreserveWS = false;

        if (extracts)
        {
            isTranslatable = Rule.isTranslatable(m_ruleMap, p_node);
            isMovable = Rule.isMovable(m_ruleMap, p_node);
            isErasable = Rule.isErasable(m_ruleMap, p_node);
            dataFormat = Rule.getDataFormat(m_ruleMap, p_node);
            type = Rule.getType(m_ruleMap, p_node);
            isPreserveWS = Rule.isPreserveWhiteSpace(m_ruleMap, p_node,
                    m_xmlFilterHelper.isPreserveWhiteSpaces());
        }

        boolean isEmptyTag = p_node.getFirstChild() == null ? true : false;
        boolean hasProcessInlineChildNode = false;
        // Wed Dec 14 16:27:49 2005 CvdL: Process XML element in
        // another XML element that gets extracted as HTML.
        // Note this is UNTESTED CODE that may break many files.
        if ((isInExtraction && switchesExtraction && containedInHtml)
                || (isInExtraction && extracts && isEmbeddable && m_isElementPostToHtml))
        {
            m_isOriginalXmlNode = true;
            m_switchExtractionBuffer += "<" + name;

            // Write out all attributes.
            NamedNodeMap attrs = p_node.getAttributes();
            for (int i = 0; i < attrs.getLength(); ++i)
            {
                Node att = attrs.item(i);
                String attname = att.getNodeName();
                String value = att.getNodeValue();

                m_switchExtractionBuffer += " " + attname + "=\"" + value
                        + "\"";
            }

            if (isEmptyTag)
            {
                m_switchExtractionBuffer += "/>";
            }
            else
            {
                m_switchExtractionBuffer += ">";

                // Traverse the tree in isInExtraction mode
                domNodeVisitor(p_node.getFirstChild(), true, true, true);

                m_switchExtractionBuffer += "</" + name + ">";
            }

            return;
        }

        if (switchesExtraction || m_isElementPost)
        {
            outputOtherFormat();
        }

        int phConsolidationCount = 0;
        int phTrimCount = 0;
        boolean usePhForNode = false;
        boolean combinWR = false;
        // Open the element
        if (extracts)
        {
            if (dataFormat != null)
            {
                m_otherFormat = dataFormat;
                switchesExtraction = true;
                isEmbeddable = false;
            }

            String stuff = null;

            if (isEmbeddable)
            {
                if (isInternal)
                {
                    bptIndex = m_admin.incrementBptIndex();
                    stuff = "<bpt i=\"" + bptIndex + "\" internal=\"yes\"";
                }
                else
                {
                    if (isEmptyTag)
                    {
                        stuff = "<ph type=\"" + (type != null ? type : name) + "\"";
                    }
                    else
                    {
                        if (m_isOfficeXml && isWRNode(name))
                        {
                            combinWR = isCombinWR(p_node);
                        }
                        
                        if (!combinWR)
                        {
                            int mode = -1;
                            if (m_isOfficeXml)
                            {
                                mode = XmlFilterConfigParser.PH_CONSOLIDATE_ADJACENT;
                            }
                            else if (m_isIdmlXml)
                            {
                                mode = XmlFilterConfigParser.PH_CONSOLIDATE_ADJACENT_IGNORE_SPACE;
                            }
                            
                            if (mode != 1)
                            {
                                phConsolidationCount = m_xmlFilterHelper
                                        .countPhConsolidation(p_node,
                                                m_ruleMap, mode, m_isIdmlXml);
                            }
                            else
                            {
                                phConsolidationCount = m_xmlFilterHelper
                                        .countPhConsolidation(p_node, m_ruleMap);
                            }
                            
                            // move the current node if consolidate
                            if (phConsolidationCount > 0)
                            {
                                p_node = getChildNode(p_node, phConsolidationCount);
                            }
                        }
                        else
                        {
                            p_node = getCombinWT(p_node);
                        }
                        
                        if (combinWR || phConsolidationCount > 0)
                        {
                            isMovable = Rule.isMovable(m_ruleMap, p_node);
                            isErasable = Rule.isErasable(m_ruleMap, p_node);
                            type = Rule.getType(m_ruleMap, p_node);
                            isPreserveWS = Rule.isPreserveWhiteSpace(m_ruleMap, p_node,
                                    m_xmlFilterHelper.isPreserveWhiteSpaces());
                            name = p_node.getNodeName();
                        }
                        
                        boolean isTranslate = true;
                        if (checkTextNode(p_node))
                        {
                            isTranslate = true;
                        }
                        else
                        {
                            isTranslate = false;
                        }
                        String innerTextNodeIndex = getTextNodeIndex(p_node);
                        bptIndex = m_admin.incrementBptIndex();
                        stuff = "<bpt i=\"" + bptIndex + "\" type=\""
                                + (type != null ? type : name)
                                + "\" isTranslate=\"" + isTranslate
                                + "\" innerTextNodeIndex=\"" + innerTextNodeIndex
                                + "\"";
                    }

                    if (isErasable)
                    {
                        stuff += " erasable=\"yes\"";
                    }
    
                    if (!isMovable)
                    {
                        stuff += " movable=\"no\"";
                    }
                }
                
                stuff += ">";
                outputExtractedStuff(stuff, isTranslatable, isPreserveWS);
                
                // output tags into bpt 
                if (phConsolidationCount > 0)
                {
                    Node startNode = p_node;
                    String startNodeName = null;
                    startNode = getParentNode(startNode, phConsolidationCount);
                    
                    for(int i = 0; i < phConsolidationCount; i++)
                    {
                        startNodeName = startNode.getNodeName();
                        outputExtractedStuff("&lt;" + startNodeName, isTranslatable, isPreserveWS);
                        
                        // Process the attributes
                        NamedNodeMap startNodeAttrs = startNode.getAttributes();
                        boolean shouldNotExtractAttrs = isShouldNotExtract(startNodeName, startNodeAttrs);
                        outputAttributes(startNode, startNodeAttrs, isEmbeddable, shouldNotExtractAttrs);
                        
                        outputExtractedStuff("&gt;", isTranslatable, isPreserveWS);
                        
                        NodeList nodes = startNode.getChildNodes();
                        for (int j = 0; j < nodes.getLength(); j++)
                        {
                            Node node = nodes.item(j);
                            if (node.getNodeType() == Node.ELEMENT_NODE)
                            {
                                startNode = node;
                                
                                if (m_isIdmlXml && p_node.equals(node))
                                {
                                    break;
                                }

                            }
                            else if (node.getNodeType() == Node.TEXT_NODE)
                            {
                                outputExtractedStuff(node.getNodeValue(), isTranslatable, isPreserveWS);
                            }
                        }
                    }
                }
                
                // output combined w:r, w:r is parent of w:t
                if (combinWR)
                {
                    Node wrNode = p_node.getParentNode();
                    outputExtractedStuff("&lt;" + wrNode.getNodeName(), isTranslatable,
                            isPreserveWS);
                    outputAttributes(wrNode, wrNode.getAttributes(), isEmbeddable, false);
                    outputExtractedStuff("&gt;", isTranslatable, isPreserveWS);

                    NodeList wrChild = wrNode.getChildNodes();

                    if (wrChild.getLength() > 1)
                    {
                        Node wrprNode = wrChild.item(0);
                        String wrprNodeName = wrprNode.getNodeName();
                        outputExtractedStuff("&lt;" + wrprNodeName, isTranslatable, isPreserveWS);
                        outputAttributes(wrprNode, wrprNode.getAttributes(), isEmbeddable, false);
                        outputExtractedStuff("&gt;", isTranslatable, isPreserveWS);

                        outputChildUsePh(wrprNode, isTranslatable, isPreserveWS);

                        outputExtractedStuff("&lt;/" + wrprNodeName + "&gt;", isTranslatable,
                                isPreserveWS);
                    }
                }

                stuff = "&lt;" + name;
                outputExtractedStuff(stuff, isTranslatable, isPreserveWS);
            }
            else
            // isEmbeddable
            {
                outputSkeleton("<" + name);
            }
        }
        else
        // extracts
        {
            // Process the don't translate inline attribute
            String stuff = null;

            if (isEmbeddable)
            {
                if (!isEmptyTag)
                {
                    // usePhForNode = checkIsAllNonExtract(p_node);
                    usePhForNode = true;
                }
                
                if (usePhForNode)
                {
                    stuff = "<ph type=\"" + (type != null ? type : name) + "\"";
                    if (isErasable)
                    {
                        stuff += " erasable=\"yes\"";
                    }
    
                    if (!isMovable)
                    {
                        stuff += " movable=\"no\"";
                    }
                }
                else if (isInternal)
                {
                    bptIndex = m_admin.incrementBptIndex();
                    stuff = "<bpt i=\"" + bptIndex + "\" internal=\"yes\"";
                }
                else
                {
                    if (isEmptyTag)
                    {
                        stuff = "<ph type=\"" + (type != null ? type : name) + "\"";
                    }
                    else
                    {
                        bptIndex = m_admin.incrementBptIndex();
                        stuff = "<bpt i=\"" + bptIndex + "\" type=\""
                                + (type != null ? type : name) + "\"";
                    }
    
                    if (isErasable)
                    {
                        stuff += " erasable=\"yes\"";
                    }
    
                    if (!isMovable)
                    {
                        stuff += " movable=\"no\"";
                    }
                }

                stuff += ">&lt;" + name;

                outputExtractedStuff(stuff, isTranslatable, isPreserveWS);
            }
            else
            // isEmbeddable
            {
                outputSkeleton("<" + name);
            }
        }

        // Process the attributes
        NamedNodeMap attrs = p_node.getAttributes();
        boolean shouldNotExtract = isShouldNotExtract(name, attrs);
        outputAttributes(p_node, attrs, isEmbeddable, shouldNotExtract);

        if (extracts && isEmbeddable)
        {
            if (isEmptyTag)
            {
                if (m_useEmptyTag)
                {
                    outputExtractedStuff("/&gt;</ph>", isTranslatable, isPreserveWS);
                }
                else
                {
                    outputExtractedStuff("&gt;&lt;/" + name + "&gt;</ph>",
                            isTranslatable, isPreserveWS);
                }
            }
            else
            {
                outputExtractedStuff("&gt;</bpt>", isTranslatable, isPreserveWS);
            }
        }
        else
        // extracts && isEmbeddable
        {
            if (isEmbeddable)
            {
                if (usePhForNode)
                {
                    outputExtractedStuff("&gt;", isTranslatable, isPreserveWS);
                    outputChildUsePh(p_node, isTranslatable, isPreserveWS);
                    outputExtractedStuff("&lt;/" + name + "&gt;</ph>",
                            isTranslatable, isPreserveWS);
                    hasProcessInlineChildNode = true;
                }
                else if (isEmptyTag)
                {
                    if (m_useEmptyTag)
                    {
                        outputExtractedStuff("/&gt;</ph>", isTranslatable, isPreserveWS);
                    }
                    else
                    {
                        outputExtractedStuff("&gt;&lt;/" + name + "&gt;</ph>",
                                isTranslatable, isPreserveWS);
                    }
                }
                else
                {
                    if (p_node.getFirstChild() != null
                            && p_node.getFirstChild().getNodeType() == Node.TEXT_NODE)
                    {
                        if (p_node.getChildNodes().getLength() == 1)
                        {
                            outputExtractedStuff("&gt;", isTranslatable, isPreserveWS);
                            domNodeVisitor(p_node.getFirstChild(), true,
                                    isTranslatable, switchesExtraction);
                            outputExtractedStuff("</bpt>", isTranslatable, isPreserveWS);
                            hasProcessInlineChildNode = true;
                        }
                        else
                        {
                            outputExtractedStuff("&gt;", isTranslatable, isPreserveWS);
                            domNodeVisitor(p_node.getFirstChild(), false,
                                    isTranslatable, switchesExtraction,
                                    isInExtraction);
                            if (isInExtraction)
                            {
                                outputExtractedStuff("</bpt>", isTranslatable, isPreserveWS);
                            }
                            hasProcessInlineChildNode = true;
                        }
                    }
                    if (p_node.getFirstChild() != null
                            && p_node.getFirstChild().getNodeType() == Node.ELEMENT_NODE)
                    {
                        if (Rule.isInline(m_ruleMap, p_node.getFirstChild()))
                        {
                            outputExtractedStuff("&gt;", isTranslatable, isPreserveWS);
                            domNodeVisitor(p_node.getFirstChild(), isInExtraction,
                                    isTranslatable, switchesExtraction,
                                    isInExtraction);
                            outputExtractedStuff("</bpt>", isTranslatable, isPreserveWS);
                        }
                        else
                        {
                            outputExtractedStuff("&gt;</bpt>", isTranslatable, isPreserveWS);
                            domNodeVisitor(p_node.getFirstChild(), isInExtraction,
                                    isTranslatable, switchesExtraction,
                                    isInExtraction);
                        }
                        
                        hasProcessInlineChildNode = true;
                    }
                }
            }
            else
            // isEmbeddable
            {
                if (isEmptyTag)
                {
                    if (m_useEmptyTag)
                    {
                        outputSkeleton("/>");
                    }
                    else
                    {
                        outputSkeleton("></" + name + ">");
                    }
                }
                else
                {
                    outputSkeleton(">");
                    boolean force = m_isOfficeXml | m_isIdmlXml;
                    phTrimCount = m_xmlFilterHelper.countPhTrim(p_node, m_ruleMap, force);
                    
                    // output trimmed tags into skeleton
                    if (phTrimCount > 0)
                    {
                        Node startNode = p_node;
                        String startNodeName = null;
                        
                        for(int i = 0; i < phTrimCount; i++)
                        {
                            startNode = getChildNode(startNode, 1);
                            startNodeName = startNode.getNodeName();
                            outputSkeleton("<" + startNodeName);
                            
                            // Process the attributes
                            NamedNodeMap startNodeAttrs = startNode.getAttributes();
                            boolean shouldNotExtractAttrs = isShouldNotExtract(startNodeName, startNodeAttrs);
                            outputAttributes(startNode, startNodeAttrs, isEmbeddable, shouldNotExtractAttrs);
                            
                            outputSkeleton(">");
                        }
                        
                        p_node = startNode;
                        name = p_node.getNodeName();
                    }
                }
            }
        }

        // Traverse the tree
        if (!hasProcessInlineChildNode)
        {
            domNodeVisitor(p_node.getFirstChild(), extracts, isTranslatable,
                    switchesExtraction);
        }
        
        if (phTrimCount > 0 || phConsolidationCount > 0)
        {
            int count = phTrimCount > 0 ? phTrimCount : phConsolidationCount;
            Node currentNode = p_node;
            
            if (!m_isIdmlXml)
            {
                domNodeVisitor(currentNode.getNextSibling(), extracts, isTranslatable,
                        switchesExtraction);
            

                for (int i = 0; i < count - 1; i++)
                {
                    currentNode = getParentNode(currentNode, 1);
                    domNodeVisitor(currentNode.getNextSibling(), extracts, isTranslatable,
                            switchesExtraction);
                }
            }
        }
        
        if (switchesExtraction || m_isElementPost)
        {
            outputOtherFormat();
        }

        // Close the element.
        if (!isEmptyTag && !usePhForNode)
        {
            // if (extracts && isEmbeddable)
            if (isEmbeddable)
            {
                outputExtractedStuff("<ept i=\"" + bptIndex + "\">&lt;/" + name + "&gt;", isTranslatable, isPreserveWS);

                // output consolidation tags into ept
                if (combinWR)
                {
                    Node wrNode = p_node.getParentNode();
                    String wrNodeName = wrNode.getNodeName();
                    outputExtractedStuff("&lt;/" + wrNodeName + "&gt;", isTranslatable, isPreserveWS);
                }
                else if (phConsolidationCount > 0)
                {
                    Node parentNode = p_node;
                    
                    if (m_isIdmlXml)
                    {
                        domNodeVisitor(parentNode.getNextSibling(), extracts, isTranslatable,
                                switchesExtraction);
                    }
                    
                    String parentNodeName = null;
                    for (int i = 0; i < phConsolidationCount; i++)
                    {
                        parentNode = parentNode.getParentNode();
                        parentNodeName = parentNode.getNodeName();
                        outputExtractedStuff("&lt;/" + parentNodeName + "&gt;", isTranslatable, isPreserveWS);
                    }
                }

                outputExtractedStuff("</ept>", isTranslatable, isPreserveWS);
            }
            else
            {
                outputSkeleton("</" + name + ">");
                
                // output trimed tags into ept
                if (phTrimCount > 0)
                {
                    Node parentNode = p_node;
                    String parentNodeName = null;
                    for (int i = 0; i < phTrimCount; i++)
                    {
                        parentNode = parentNode.getParentNode();
                        parentNodeName = parentNode.getNodeName();
                        outputSkeleton("</" + parentNodeName + ">");
                    }
                }
            }
        }
    }

    private boolean isWRNode(String name)
    {
        return "w:r r a:r".contains(name);
    }

    /**
     * get w:t for combined w:r
     * @param pNode
     * @return
     */
    private Node getCombinWT(Node pNode)
    {
        NodeList childList = pNode.getChildNodes();
        return childList.getLength() == 1 ? childList.item(0) : childList.item(1);
    }

    /**
     * check if is this format w:r w:rPr w:t for office xml
     * @param pNode
     * @return
     */
    private boolean isCombinWR(Node pNode)
    {
        String wtNodeName = "w:t t a:t";
        String wrprNodeName = "w:rPr rpr a:rPr";
        NodeList childList = pNode.getChildNodes();

        if (childList.getLength() == 2)
        {
            Node node0 = childList.item(0);
            Node node1 = childList.item(1);

            if (wrprNodeName.contains(node0.getNodeName()) && wtNodeName.contains(node1.getNodeName()))
            {
                return true;
            }
        }
        
        if (childList.getLength() == 1)
        {
            Node node0 = childList.item(0);

            if (wtNodeName.contains(node0.getNodeName()))
            {
                return true;
            }
        }

        return false;
    }

    private void outputChildUsePh(Node p_node, boolean isTranslatable, boolean isPreserveWS)
    {
        NodeList list = p_node.getChildNodes();
        if (list != null)
        {
            for(int i = 0; i < list.getLength(); i++)
            {
                Node node = list.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE)
                {
                    String name = node.getNodeName();
                    boolean isEmptyTag = node.getFirstChild() == null ? true : false;
                    outputExtractedStuff("&lt;" + name, isTranslatable, isPreserveWS);
                    
                    // Process the attributes
                    NamedNodeMap attrs = node.getAttributes();
                    boolean shouldNotExtract = isShouldNotExtract(name, attrs);
                    outputAttributes(node, attrs, true, shouldNotExtract);
                    
                    if (isEmptyTag)
                    {
                        if (m_useEmptyTag)
                        {
                            outputExtractedStuff("/&gt;", isTranslatable, isPreserveWS);
                        }
                        else
                        {
                            outputExtractedStuff("&gt;&lt;/" + name + "&gt;",
                                    isTranslatable, isPreserveWS);
                        }
                    }
                    else
                    {
                        outputExtractedStuff("&gt;", isTranslatable, isPreserveWS);
                        outputChildUsePh(node, isTranslatable, isPreserveWS);
                        outputExtractedStuff("&lt;/" + name + "&gt;",
                                isTranslatable, isPreserveWS);
                    }
                }
                else if (node.getNodeType() == Node.TEXT_NODE)
                {
                    outputExtractedStuff(node.getNodeValue(), isTranslatable, isPreserveWS);
                }
                else
                {
                    // should not here
                }
            }
        }
        
    }
    
    private boolean checkIsAllNonExtract(Node p_node)
    {
        NodeList list = p_node.getChildNodes();
        
        if (list != null)
        {
            for(int i = 0; i < list.getLength(); i++)
            {
                Node node = list.item(i);
                
                if (node.getNodeType() != Node.ELEMENT_NODE
                        && node.getNodeType() != Node.TEXT_NODE)
                {
                    return false;
                }
                
                if (node.getNodeType() == Node.ELEMENT_NODE)
                {
                    boolean extract = Rule.extracts(m_ruleMap, node);
                    if (extract)
                    {
                        return false;
                    }
                    else
                    {
                        return checkIsAllNonExtract(node);
                    }
                }
            }
        }
        
        return true;
    }

    private Node getParentNode(Node p_node, int p_parentLayer)
    {
        for(int i = 0; i < p_parentLayer; i++)
        {
            p_node = p_node.getParentNode();
        }
        return p_node;
    }

    private Node getChildNode(Node p_node, int p_childLayer)
    {
        for(int i = 0; i < p_childLayer; i++)
        {
            NodeList nodes = p_node.getChildNodes();
            for (int j = 0; j < nodes.getLength(); j++)
            {
                Node node = nodes.item(j);
                if (node.getNodeType() == Node.ELEMENT_NODE)
                {
                    p_node = node;
                    break;
                }
            }
        }
        
        return p_node;
    }

    private String getTextNodeIndex(Node p_node)
    {
        String s = "";
        NodeList nodes = p_node.getChildNodes();
        if (nodes.getLength() > 0)
        {
            for (int i = 0; i < nodes.getLength(); i++)
            {
                Node node = nodes.item(i);
                if (node.getNodeType() == Node.TEXT_NODE)
                {
                    s += i + ",";
                }
            }
        }
        return s;
    }

    private boolean checkTextNode(Node p_node)
    {
        return getTextNodeIndex(p_node).length() != 0;
        // NodeList nodes = p_node.getChildNodes();
        // if(nodes.getLength() > 0){
        // for(int i = 0; i < nodes.getLength(); i++)
        // {
        // Node node = nodes.item(i);
        // if(node.getNodeType() == Node.TEXT_NODE)
        // {
        // return true;
        // }
        // }
        // }
        // else
        // {
        // return false;
        // }
        // return false;
    }
    
    private boolean isAttributeInline(Node p_node)
    {
        if (p_node == null)
        {
            return false;
        }
        
        NamedNodeMap attrs = p_node.getAttributes();
        if (attrs == null)
        {
            return false;
        }
        
        for (int i = 0; i < attrs.getLength(); ++i)
        {
            Node att = attrs.item(i);
            boolean extracts = Rule.extracts(m_ruleMap, att);
            boolean isAttrInline = Rule.isInline(m_ruleMap, att);
            
            if (extracts && isAttrInline)
            {
                return true;
            }
        }
        
        return false;
    }

    /**
     * <p>
     * Outputs the attributes of the element node being processed by
     * domElementProcessor().
     * </p>
     * 
     * <p>
     * Note that the <code>isInTranslatable</code> argument is not used.
     * </p>
     */
    private void outputAttributes(Node parentNode, NamedNodeMap attrs, boolean isEmbeded,
            boolean shouldNotExtract) throws ExtractorException
    {
        if (attrs == null)
        {
            return;
        }
        for (int i = 0; i < attrs.getLength(); ++i)
        {
            Node att = attrs.item(i);
            String attname = att.getNodeName();
            String value = att.getNodeValue();
            String sid = Rule.getSid(m_ruleMap, att);
            boolean extracts = Rule.extracts(m_ruleMap, att);
            boolean isAttrInline = Rule.isInline(m_ruleMap, att);
            // Only for xml files converted from Indesign.
            if (shouldNotExtract)
            {
                extracts = false;
            }
            
            // if is office xml and is http, do not extract
            if (m_isOfficeXml && isURL(value))
            {
                extracts = false;
            }

            boolean isTranslatable = true;
            String dataFormat = null;
            String type = null;

            isTranslatable = Rule.isTranslatable(m_ruleMap, att);
            dataFormat = Rule.getDataFormat(m_ruleMap, att);
            type = Rule.getType(m_ruleMap, att);

            if (isEmbeded)
            {
                String stuff = null;
                if (extracts)
                {
                    stuff = " " + attname + "=&quot;";

                    if (dataFormat != null)
                    {
                        try
                        {
                            Output output = switchExtractor(value, dataFormat);
                            Iterator it = output.documentElementIterator();

                            while (it.hasNext())
                            {
                                DocumentElement element = (DocumentElement) it.next();
                                boolean isTransOrLoc = false; // true=translatable

                                switch (element.type())
                                {
                                    case DocumentElement.TRANSLATABLE:
                                        isTransOrLoc = true;
                                        // fall through
                                    case DocumentElement.LOCALIZABLE:
                                        Segmentable seg = (Segmentable) element;
                                        stuff += createSubTag(isTransOrLoc, seg.getType(), seg
                                                .getDataType())
                                                + seg.getChunk() + "</sub>";
                                        break;

                                    case DocumentElement.SKELETON:
                                        stuff += ((SkeletonElement) element).getSkeleton();
                                        break;
                                }
                            }
                        }
                        catch (ExtractorException ex)
                        {
                            stuff += createSubTag(isTranslatable, type, dataFormat)
                                    + m_xmlEncoder.encodeStringBasic(value) + "</sub>";
                        }
                    }
                    else
                    {
                        stuff += createSubTag(isTranslatable, type, dataFormat)
                                + m_xmlEncoder.encodeStringBasic(value) + "</sub>";
                    }

                    stuff += "&quot;";
                }
                else
                // extracts
                {
                    // encode twice to get a correct merge result
                    stuff = " " + attname + "=&quot;"
                            + m_xmlEncoder.encodeStringBasic(m_xmlEncoder.encodeStringBasic(value))
                            + "&quot;";
                }

                m_admin.addContent(stuff);
            }
            else
            // isEmbeded Not embeddable. But is it translatable ?
            {
                if (extracts)
                {
                    boolean isPreserveWS = Rule.isPreserveWhiteSpace(m_ruleMap, parentNode, m_xmlFilterHelper.isPreserveWhiteSpaces());
                    outputSkeleton(" " + attname + "=\"");

                    if (dataFormat != null)
                    {
                        try
                        {
                            Output output = switchExtractor(value, dataFormat);
                            Iterator it = output.documentElementIterator();
                            while (it.hasNext())
                            {
                                outputDocumentElement((DocumentElement) it.next(), sid);
                            }
                        }
                        catch (ExtractorException ex)
                        {
                            String stuff = m_xmlEncoder.encodeStringBasic(value);
                            outputExtractedStuff(stuff, isTranslatable, isPreserveWS);
                            setSid(sid);
                        }
                    }
                    else
                    {
                        String stuff = m_xmlEncoder.encodeStringBasic(value);
                        outputExtractedStuff(stuff, isTranslatable, isPreserveWS);
                        setSid(sid);
                    }

                    outputSkeleton("\"");
                }
                else
                {
                    outputSkeleton(" " + attname + "=\"" + m_xmlEncoder.encodeStringBasic(value)
                            + "\"");
                }
            }
        }
    }

    /**
     * Output translatable text
     */
    private void outputTranslatable(String p_ToAdd, boolean isPreserveWS)
    {
        if (m_admin.getOutputType() != OutputWriter.TRANSLATABLE)
        {
            TranslatableWriter tw = new TranslatableWriter(getOutput());
            tw.setXmlFilterHelper(m_xmlFilterHelper);
            tw.setPreserveWhiteSpace(isPreserveWS);
            m_admin.reset(tw);
        }
        m_admin.addContent(p_ToAdd);
    }

    private void setSid(String sid)
    {
        m_admin.setSid(sid);
    }

    /**
     * Output localizable text
     */
    private void outputLocalizable(String p_ToAdd)
    {
        if (m_admin.getOutputType() != OutputWriter.LOCALIZABLE)
        {
            LocalizableWriter lw = new LocalizableWriter(getOutput());
            lw.setXmlFilterHelper(m_xmlFilterHelper);
            m_admin.reset(lw);
        }
        m_admin.addContent(p_ToAdd);
    }

    /**
     * Outputs skeleton text.
     */
    private void outputSkeleton(String p_ToAdd)
    {
        if (m_admin.getOutputType() != OutputWriter.SKELETON)
        {
            m_admin.reset(new SkeletonWriter(getOutput()));
        }
        m_admin.addContent(p_ToAdd);
    }

    private void outputDocumentElement(DocumentElement element)
    {
        outputDocumentElement(element, null);
    }

    private void outputDocumentElement(DocumentElement element, String sid)
    {
        if (sid != null && element instanceof TranslatableElement)
        {
            ((TranslatableElement) element).setSid(sid);
        }
        m_admin.reset(null);
        getOutput().addDocumentElement(element, true);
    }

    /**
     * Utility function that outputs translatable or localizable text.
     * @param isPreserveWS
     */
    private void outputExtractedStuff(String stuff, boolean isTranslatable, boolean isPreserveWS)
    {
        if (isTranslatable)
        {
            outputTranslatable(stuff, isPreserveWS);
        }
        else
        {
            outputLocalizable(stuff);
        }
    }

    private String createSubTag(boolean isTranslatable, String type,
            String dataFormat)
    {
        String stuff = "<sub";
        stuff += " locType=\""
                + (isTranslatable ? "translatable" : "localizable") + "\"";

        if (type != null)
        {
            stuff += " type=\"" + type + "\"";
        }

        if (dataFormat != null)
        {
            stuff += " datatype=\"" + dataFormat + "\"";
        }
        stuff += ">";

        return stuff;
    }

    /**
     * Flushes text collected for another Extractor by calling the Extractor on
     * the text in m_switchExtractionBuffer and writing its output to the output
     * object.
     */
    private void outputOtherFormat() throws ExtractorException
    {
        try
        {
            String otherFormat = (m_isElementPost) ? m_elementPostFormat : m_otherFormat;
            Filter otherFilter = (m_isElementPost) ? m_xmlFilterHelper.getElementPostFilter()
                    : null;
            outputOtherFormat(otherFormat, otherFilter, false);
        }
        catch (Exception ex)
        {
            CATEGORY.error("Output other format with error: ", ex);
            outputTranslatable(m_xmlEncoder.encodeStringBasic(m_switchExtractionBuffer), false);
            m_switchExtractionBuffer = new String();
        }
    }
    
    private void outputOtherFormatForCdata(String p_otherFormat, Filter p_otherFilter, boolean p_useGlobal)
    throws ExtractorException
    {
        try
        {
            String otherFormat = (p_useGlobal) ? m_cdataPostFormat : null;
            if (otherFormat == null)
            {
                otherFormat = (p_otherFormat != null) ? p_otherFormat : m_otherFormat;
            }
            
            Filter otherFilter = (p_useGlobal) ? m_xmlFilterHelper.getCdataPostFilter() : null;
            if (otherFilter == null)
            {
                otherFilter = (p_otherFilter != null) ? p_otherFilter : null;
            }
            
            outputOtherFormat(otherFormat, otherFilter, true);
        }
        catch (Exception ex)
        {
            CATEGORY.error("Output other format with error: ", ex);
            outputTranslatable(m_xmlEncoder.encodeStringBasic(m_switchExtractionBuffer), false);
            m_switchExtractionBuffer = new String();
        }
    }
    
    /**
     * Flushes text collected for another Extractor by calling the Extractor on
     * the text in m_switchExtractionBuffer and writing its output to the output
     * object.
     * @param otherFormat
     * @param otherFilter
     */
    private void outputOtherFormat(String otherFormat, Filter otherFilter, boolean isCdata)
    throws ExtractorException
    {
        if (m_switchExtractionBuffer.length() == 0)
        {
            return;
        }
        
        if (m_xmlFilterHelper.isBlankOrExblank(m_switchExtractionBuffer))
        {
            outputSkeleton(m_switchExtractionBuffer);
        }
        else
        {
            try
            {
                Output output = switchExtractor(m_switchExtractionBuffer, otherFormat, otherFilter);
                Iterator it = output.documentElementIterator();

                while (it.hasNext())
                {
                    DocumentElement element = (DocumentElement) it.next();
                    switch (element.type())
                    {
                    case DocumentElement.TRANSLATABLE: // fall through
                    case DocumentElement.LOCALIZABLE:
                        Segmentable segmentableElement = (Segmentable) element;
                        segmentableElement.setDataType(otherFormat);
                        fixEntitiesForOtherFormat(segmentableElement, (isCdata || m_isOriginalXmlNode));
                        outputDocumentElement(element);
                        break;

                    case DocumentElement.SKELETON:
                        String skeleton = ((SkeletonElement) element).getSkeleton();
                        skeleton = (isCdata || m_isOriginalXmlNode) ? m_xmlEncoder.decodeStringBasic(skeleton) : skeleton;
                        outputSkeleton(skeleton);
                        break;
                    }
                }
            }
            catch (ExtractorException ex)
            {
                CATEGORY.error("Output other format with error: ", ex);
                outputTranslatable(m_xmlEncoder
                        .encodeStringBasic(m_switchExtractionBuffer), false);
            }
        }

        m_switchExtractionBuffer = new String();
        m_isOriginalXmlNode = false;
        // m_otherFormat = null;
    }

    /**
     * encode twice for this kind of element text : &amp;lt;p&amp;gt; here is p &amp;lt;/p&amp;gt;
     * TODO : but not for original XML element
     */
    private void fixEntitiesForOtherFormat(Segmentable element, boolean isCdata)
    {
        if (isCdata)
        {
            return;
        }

        String[] tagNames = { "bpt", "ept", "it" };
        String result = encodingEntitiesForOtherFormat(element.getChunk(), tagNames);
        element.setChunk(result);
    }

    private String encodingEntitiesForOtherFormat(String chunk, String[] tagNames)
    {
        String result = chunk;
        for (String tagName : tagNames)
        {
            result = encodingEntitiesForOtherFormat(result, tagName);
        }

        return result;
    }

    private String encodingEntitiesForOtherFormat(String chunk, String tagName)
    {
        StringBuffer ori = new StringBuffer(chunk);
        StringBuffer result = new StringBuffer(chunk.length());
        String endTag = "</" + tagName + ">";
        String startTag = "<" + tagName;
        int fromIndex = 0;
        int index_e = ori.indexOf(endTag, fromIndex);

        if (index_e != -1)
        {
            int index_s = ori.indexOf(startTag, fromIndex);

            while (index_e != -1 && index_s != -1 && index_s < index_e)
            {
                int endIndex = index_e + endTag.length();
                int index_se = ori.indexOf(">", index_s);
                if (index_se != -1)
                {
                    result.append(ori.substring(fromIndex, index_se + 1));
                    String temp = ori.substring(index_se + 1, index_e);
                    String encoded = encodeStringBasicExceptSub(temp);
                    result.append(encoded);
                    result.append(ori.substring(index_e, endIndex));
                }
                else
                {
                    result.append(ori.substring(fromIndex, endIndex));
                }

                fromIndex = endIndex;
                index_e = ori.indexOf(endTag, fromIndex);
                index_s = ori.indexOf(startTag, fromIndex);
            }

            if (ori.length() > fromIndex)
            {
                result.append(ori.substring(fromIndex));
            }

            return result.toString();
        }
        else
        {
            return chunk;
        }
    }

    private String encodeStringBasicExceptSub(String temp)
    {
        if (temp.indexOf("<") == -1)
        {
            return m_xmlEncoder.encodeStringBasic(temp);
        }
        else
        {
            StringBuffer result = new StringBuffer();
            Pattern p = Pattern.compile("<[^<>]*>[^<>]*</[^<>]*>|<[^<>]*/\\s*>");
            Matcher m = p.matcher(temp);
            int fromIndex = 0;
            if (m.find())
            {
                do
                {
                    int start = m.start();
                    int end = m.end();
                    result.append(m_xmlEncoder.encodeStringBasic(temp.substring(fromIndex, start)));
                    result.append(temp.substring(start, end));
                    fromIndex = end;
                } while (m.find());
                
                if (fromIndex < temp.length())
                {
                    result.append(m_xmlEncoder.encodeStringBasic(temp.substring(fromIndex)));
                }
            }
            else
            {
                result.append(temp);
            }

            return result.toString();
        }
    }

    /**
     * Processes a GSA comment. If it's GSA snippet, adds the GSA tag to the
     * Output object and returns true.
     */
    private boolean processGsaSnippet(String comments)
            throws ExtractorException
    {
        try
        {
            RegExMatchInterface match = RegEx.matchSubstring(comments,
                    "^\\s*gs\\s", false);

            if (match == null)
            {
                match = RegEx.matchSubstring(comments, "^\\s*/gs", false);
                if (match == null)
                {
                    return false;
                }

                outputGsaEnd();
                return true;
            }

            boolean delete = false;
            String extract = null;
            String description = null;
            String locale = null;
            String add = null;
            String added = null;
            String deleted = null;
            String snippetName = null;
            String snippetId = null;

            match = RegEx.matchSubstring(comments,
                    "\\sadd\\s*=\\s*\"([^\"]+)\"\\s", false);
            if (match != null)
            {
                add = match.group(1);
            }

            match = RegEx.matchSubstring(comments,
                    "\\sextract\\s*=\\s*\"([^\"]+)\"\\s", false);
            if (match != null)
            {
                extract = match.group(1);
            }

            match = RegEx.matchSubstring(comments,
                    "\\sdescription\\s*=\\s*\"([^\"]+)\"\\s", false);
            if (match != null)
            {
                description = match.group(1);
            }

            match = RegEx.matchSubstring(comments,
                    "\\slocale\\s*=\\s*\"([^\"]+)\"\\s", false);
            if (match != null)
            {
                locale = match.group(1);
            }

            match = RegEx.matchSubstring(comments,
                    "\\sname\\s*=\\s*\"([^\"]+)\"\\s", false);
            if (match != null)
            {
                snippetName = match.group(1);
            }

            match = RegEx.matchSubstring(comments,
                    "\\sid\\s*=\\s*\"([^\"]+)\"\\s", false);
            if (match != null)
            {
                snippetId = match.group(1);
            }

            match = RegEx.matchSubstring(comments,
                    "\\sdelete\\s*=\\s*\"?(1|yes|true)\"?\\s", false);
            if (match != null)
            {
                delete = true;
            }

            match = RegEx.matchSubstring(comments,
                    "\\sdeleted\\s*=\\s*\"([^\"]+)\"\\s", false);
            if (match != null)
            {
                deleted = match.group(1);
            }

            match = RegEx.matchSubstring(comments,
                    "\\sadded\\s*=\\s*\"([^\"]+)\"\\s", false);
            if (match != null)
            {
                added = match.group(1);
            }

            outputGsaStart(extract, description, locale, add, delete, added,
                    deleted, snippetName, snippetId);
        }
        catch (RegExException e)
        {
            // Shouldn't reach here.
            System.err.println("Malformed re pattern in XML extractor.");
        }

        return true;
    }

    private void outputGsaStart(String extract, String description,
            String locale, String add, boolean delete, String added,
            String deleted, String snippetName, String snippetId)
            throws ExtractorException
    {
        m_admin.reset(null);

        try
        {
            getOutput().addGsaStart(extract, description, locale, add, delete,
                    added, deleted, snippetName, snippetId);
        }
        catch (DocumentElementException ex)
        {
            throw new ExtractorException(HTML_GS_TAG_ERROR, ex.toString());
        }
    }

    private void outputGsaEnd()
    {
        m_admin.reset(null);

        getOutput().addGsaEnd();
    }
    
    /**
     * Special treat for InDesign paragraph attributes
     * Because we rely on some attributes to convert XML for adjusting
     * font style information, we could not let them changed by translator.
     * 
     * @param nodeName
     * @param nodeAttrs
     * 
     * @return true | false
     */
    private boolean isShouldNotExtract(String nodeName, NamedNodeMap nodeAttrs)
    {
        // 
        boolean shouldNotExtract = false;
        if (nodeName.equals(PARAGRAPH_NODE_NAME))
        {
            for (int i = 0; i < nodeAttrs.getLength(); ++i)
            {
                Node att = nodeAttrs.item(i);
                String attname = att.getNodeName();
                if (attname.equals(PARAGRAPH_HAS_DIFFERENT_STYLE))
                {
                    String value = att.getNodeValue();
                    if (value.equals(VALUE_TRUE))
                    {
                        shouldNotExtract = true;
                        break;
                    }
                }
            }
        }
        
        return shouldNotExtract;
    }
    
    /**
     * Checks if this attribute value is an http or https url
     * 
     * @param value the value in the attribute
     * 
     * @return true | false
     */
    private boolean isURL(String value) 
    {
        if (value == null)
        {
            return false;
        }
        
        String s = value.trim().toLowerCase();
        if (s.startsWith("http://") || s.startsWith("https://")) 
        {
            return true;
        }
        return false;
    }
    
    public boolean isIdmlXml()
    {
        return m_isIdmlXml;
    }

    public void setIsIdmlXml(boolean mIsIdmlXml)
    {
        m_isIdmlXml = mIsIdmlXml;
    }
}
