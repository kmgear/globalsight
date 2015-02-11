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
package com.globalsight.machineTranslation.promt;

import com.globalsight.machineTranslation.MachineTranslationException;
import com.globalsight.machineTranslation.MachineTranslator;
import com.globalsight.machineTranslation.AbstractTranslator;
import com.globalsight.util.GlobalSightLocale;
import com.globalsight.util.edit.GxmlUtil;
import com.globalsight.util.gxml.GxmlElement;
import com.globalsight.util.gxml.GxmlFragmentReader;
import com.globalsight.util.gxml.GxmlFragmentReaderPool;

import com.globalsight.everest.page.ExtractedSourceFile;
import com.globalsight.everest.page.SourcePage;
import com.globalsight.everest.projecthandler.ProMTInfo;
import com.globalsight.everest.projecthandler.TranslationMemoryProfile;
import com.globalsight.everest.servlet.util.ServerProxy;
import com.globalsight.everest.webapp.pagehandler.administration.tmprofile.TMProfileConstants;
import com.globalsight.ling.docproc.DiplomatAPI;
import com.globalsight.ling.docproc.DocumentElement;
import com.globalsight.ling.docproc.Output;
import com.globalsight.ling.docproc.SegmentNode;
import com.globalsight.ling.docproc.TranslatableElement;
import com.globalsight.log.GlobalSightCategory;

import java.util.*;

/**
 * Acts as a proxy to the free translation Machine Translation Service.
 */
public class ProMTProxy extends AbstractTranslator implements MachineTranslator
{
    private static final GlobalSightCategory s_logger = (GlobalSightCategory) GlobalSightCategory
            .getLogger(ProMTProxy.class);

    private static final String ENGINE_NAME = "ProMT";
    private DiplomatAPI m_diplomat = null;
    private int count = 0;
    
    public ProMTProxy() throws MachineTranslationException
    {
    }

    public String getEngineName()
    {
        return ENGINE_NAME;
    }

    /**
     * Returns true if the given locale pair is supported by MT.
     */
    public boolean supportsLocalePair(Locale p_sourceLocale,
            Locale p_targetLocale) throws MachineTranslationException
    {
        ProMTInfo ptsInfo = getProMTInfoBySrcTrgLocale(p_sourceLocale,
                p_targetLocale);
        if (ptsInfo == null)
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    /**
     * As PROMT does not support batch translation, this is the unique API to
     * translate text in PTS implementation.
     */
    protected String doTranslation(Locale p_sourceLocale,
            Locale p_targetLocale, String p_string)
            throws MachineTranslationException
    {
        if (p_string == null || "".equals(p_string.trim()))
        {
            return "";
        }

        String result = "";
        // get ptsUrlFlag
        String ptsUrlFlag = null;
        TranslationMemoryProfile tmProfile = getTMProfile();
        if (tmProfile != null)
        {
            ptsUrlFlag = tmProfile.getPtsUrlFlag();
        }

        // Translate via PTS8 APIs
        if (ptsUrlFlag != null
                && TMProfileConstants.MT_PTS_URL_FLAG_V8.equals(ptsUrlFlag))
        {
            try
            {
                ProMTInfo ptsInfo = getProMTInfoBySrcTrgLocale(p_sourceLocale,
                        p_targetLocale);

                long dirId = -1;
                String topicTemplateId = "";
                if (ptsInfo != null)
                {
                    dirId = ptsInfo.getDirId();
                    topicTemplateId = ptsInfo.getTopicTemplateId();

                    ProMtInvoker invoker = getProMtInvoker();
                    result = invoker.translateText(dirId, topicTemplateId,
                            p_string);
                    if (result == null || "null".equalsIgnoreCase(result))
                    {
                        result = "";
                    }
                }
            }
            catch (Exception ex)
            {
                s_logger.error(ex.getMessage());
            }
        }
        // Translate via PTS9 APIs
        else if (ptsUrlFlag != null
                && TMProfileConstants.MT_PTS_URL_FLAG_V9.equals(ptsUrlFlag))
        {
            try
            {
                ProMTInfo ptsInfo = getProMTInfoBySrcTrgLocale(p_sourceLocale,
                        p_targetLocale);

                long dirId = -1;
                String topicTemplateId = "";
                if (ptsInfo != null)
                {
                    dirId = ptsInfo.getDirId();
                    topicTemplateId = ptsInfo.getTopicTemplateId();

                    ProMtPts9Invoker invoker = getProMtPts9Invoker();
                    int times = 0;
                    String stringBak = new String(p_string);
                    while (times < 2)
                    {
                        // All segments from XLF file are re-wrapped,before send
                        // them to PTS9,need revert them.
                        GlobalSightLocale sourceLocale = getSourceLocale();
                        GxmlElement ge = null;
                        List idList = null;
                        List xList = null;
                        boolean isXlf = needRevertXlfSegment();
                        boolean containTags = isContainTags();
                        if (isXlf && containTags)
                        {
                            ge = getSourceGxmlElement(p_string);
                            if (ge != null)
                            {
                                idList = getAttValuesByName(ge, "id");
                                xList = getAttValuesByName(ge, "x");
                            }
                            String locale = sourceLocale.getLanguage() + "_"
                                    + sourceLocale.getCountry();
                            stringBak = wrappText(p_string, locale);
                            stringBak = revertXlfSegment(stringBak, locale);
                            stringBak = encodeLtGtInGxmlAttributeValueForPTS9Trans(stringBak);
                        }
                        // Send to PTS9 for translation
                        result = invoker.translateText(dirId, topicTemplateId, stringBak);
                        if (result != null && !result.startsWith("-1 Error")
                                && isXlf && containTags)
                        {
                            result = result.replaceAll("_gt;_", ">");
                            /** 
                            result = result.replaceAll("&#x9;", "&amp;#x9;");// while-space
                            result = result.replaceAll("&#xa;", "&amp;#xa;");// \r
                            result = result.replaceAll("&#xd;", "&amp;#xd;");// \n
                            // handle '<' and '"' in attribute value
                            result = encodeGxmlAttributeEntities2(result);
                            */
                            // handle single '&' in MT translation
                            result = encodeSeparatedAndChar(result);
                        }
                        // Parse the translation back 
                        if (isXlf && containTags)
                        {
                            SegmentNode sn = extractSegment(result, "xlf", sourceLocale);
                            if (sn != null)
                            {
                                result = sn.getSegment();
                                // Handle entity
                                result = encodeTranslationResult(result, idList, xList);
                            }
                            else
                            {
                                result = null;
                            }
                        }
                        if (result != null && !"null".equalsIgnoreCase(result)
                                && !result.startsWith("-1"))
                        {
                            break;
                        }
                        times++;
                    }

                    if (result != null && result.startsWith("-1 Error:"))
                    {
                        s_logger.error("Failed to get translation from PTS9 engine.");
                        if (s_logger.isDebugEnabled())
                        {
                            s_logger.error(result);                            
                        }
                    }
                    if (result == null || "null".equalsIgnoreCase(result)
                            || result.startsWith("-1"))
                    {
                        result = "";
                    }
                }
            }
            catch (Exception ex)
            {
//                s_logger.error(ex.getMessage());
            }
        }

        return result;
    }

    /**
     * Get a PROMT invoker object for PTS version 8.
     * 
     * @return ProMtInvoker object
     */
    private ProMtInvoker getProMtInvoker()
    {
        ProMtInvoker invoker = null;

        HashMap paramMap = getMtParameterMap();
        String ptsurl = (String) paramMap.get(MachineTranslator.PROMT_PTSURL);
        String username = (String) paramMap
                .get(MachineTranslator.PROMT_USERNAME);
        String password = (String) paramMap
                .get(MachineTranslator.PROMT_PASSWORD);

        if (ptsurl != null && !"".equals(ptsurl.trim())
                && !"null".equals(ptsurl))
        {
            if (username != null)
            {
                invoker = new ProMtInvoker(ptsurl, username, password);
            }
            else
            {
                invoker = new ProMtInvoker(ptsurl);
            }
        }

        return invoker;
    }
    
    /**
     *  Get a PROMT invoker object for PTS version 9.
     *  
     * @return ProMtPts9Invoker object.
     */
    private ProMtPts9Invoker getProMtPts9Invoker()
    {
        ProMtPts9Invoker invoker = null;
        
        if (invoker == null)
        {
            HashMap paramMap = getMtParameterMap();
            String ptsurl = (String) paramMap.get(MachineTranslator.PROMT_PTSURL);
            String username = (String) paramMap
                    .get(MachineTranslator.PROMT_USERNAME);
            String password = (String) paramMap
                    .get(MachineTranslator.PROMT_PASSWORD);

            if (ptsurl != null && !"".equals(ptsurl.trim())
                    && !"null".equals(ptsurl))
            {
                if (username != null)
                {
                    invoker = new ProMtPts9Invoker(ptsurl, username, password);
                }
                else
                {
                    invoker = new ProMtPts9Invoker(ptsurl);
                }
            }            
        }

        return invoker;
    }
    
    /**
     * Try to find a matched PROMT setting for specified source and target
     * languages.
     * 
     * @param p_sourceLocale
     * @param p_targetLocale
     * @return
     */
    private ProMTInfo getProMTInfoBySrcTrgLocale(Locale p_sourceLocale,
            Locale p_targetLocale)
    {
        ProMTInfo result = null;

        String lpName = getLanguagePairName(p_sourceLocale, p_targetLocale);

        TranslationMemoryProfile tmProfile = getTMProfile();
        if (tmProfile != null)
        {
            Set promtInfos = tmProfile.getTmProfilePromtInfoSet();
            if (promtInfos != null && promtInfos.size() > 0)
            {
                Iterator promtInfoIter = promtInfos.iterator();
                while (promtInfoIter.hasNext())
                {
                    ProMTInfo ptsInfo = (ProMTInfo) promtInfoIter.next();
                    String dirName = ptsInfo.getDirName();
                    if (dirName != null && dirName.equalsIgnoreCase(lpName))
                    {
                        result = ptsInfo;
                        break;
                    }
                }
            }
        }

        return result;
    }
    
    /**
     * Get language pair name. Note that PROMT only support simplified Chinese.
     * 
     * @param p_sourceLocale
     * @param p_targetLocale
     * @return
     */
    private String getLanguagePairName(Locale p_sourceLocale,
            Locale p_targetLocale)
    {
        if (p_sourceLocale == null || p_targetLocale == null)
        {
            return null;
        }

        String srcLang = p_sourceLocale.getDisplayLanguage(Locale.ENGLISH);
        String srcCountry = p_sourceLocale.getDisplayCountry(Locale.ENGLISH);
        if ("Chinese".equals(srcLang) && "China".equals(srcCountry))
        {
            srcLang = "Chinese (Simplified)";
        }

        String trgLang = p_targetLocale.getDisplayLanguage(Locale.ENGLISH);
        String trgCountry = p_targetLocale.getDisplayCountry(Locale.ENGLISH);
        if ("Chinese".equals(trgLang) && "China".equals(trgCountry))
        {
            trgLang = "Chinese (Simplified)";
        }

        return (srcLang + "-" + trgLang);
    }
    
    /**
     * If the source page data type is XLF,need revert the segment content.
     * 
     * @return boolean
     */
    private boolean needRevertXlfSegment()
    {
        SourcePage sp = getSourcePage();
        String spDataType = null;
        if (sp != null)
        {
            ExtractedSourceFile esf = (ExtractedSourceFile) sp.getExtractedFile();
            spDataType = esf.getDataType();
        }
        if (spDataType != null
                && ("xlf".equalsIgnoreCase(spDataType) || "xliff"
                        .equalsIgnoreCase(spDataType)))
        {
            return true;
        }

        return false;
    }
    
    private GlobalSightLocale getSourceLocale()
    {
        SourcePage sp = getSourcePage();
        GlobalSightLocale sourceLocale = null;
        if (sp != null)
        {
            sourceLocale = sp.getGlobalSightLocale();
        }
        
        return sourceLocale;
    }
    
    /**
     * Retrieve source page by source page ID.
     * 
     * @return
     */
    private SourcePage getSourcePage()
    {
        HashMap paramMap = getMtParameterMap();
        Long sourcePageID = (Long) paramMap
                .get(MachineTranslator.SOURCE_PAGE_ID);
        SourcePage sp = null;
        try
        {
            sp = ServerProxy.getPageManager().getSourcePage(sourcePageID);
        }
        catch (Exception e)
        {
            if (s_logger.isDebugEnabled())
            {
                s_logger.error("Failed to get source page by pageID : "
                        + sourcePageID + ";" + e.getMessage());
            }
        }

        return sp;
    }

    public static String wrappText(String p_text, String locale)
    {
        if (p_text == null || p_text.trim().length() == 0)
        {
            return null;
        }

        StringBuffer sb = new StringBuffer();
        sb.append("<?xml version=\"1.0\"?>");
        sb.append("<diplomat locale=\"").append(locale).append("\" version=\"2.0\" datatype=\"xlf\">");
        sb.append("<translatable>");
        sb.append(p_text);
        sb.append("</translatable>");
        sb.append("</diplomat>");

        return sb.toString();
    }
    
    public static String revertXlfSegment(String text, String locale)
    {
        String result = null;

        try
        {
            DiplomatAPI diplomat = new DiplomatAPI();
            diplomat.setFileProfileId("-1");
            diplomat.setFilterId(-1);
            diplomat.setFilterTableName(null);
            diplomat.setTargetLocale(locale);
            byte[] mergeResult = diplomat.merge(text, "UTF-8", false);
            result = new String(mergeResult, "UTF-8");
        }
        catch (Exception e)
        {
            if (s_logger.isDebugEnabled())
            {
                s_logger.error("Failed to revert XLF segment : "
                        + e.getMessage());
            }
        }

        return result;
    }
    
    private DiplomatAPI getDiplomatApi()
    {
        if (m_diplomat == null)
        {
            m_diplomat = new DiplomatAPI();
        }

        m_diplomat.reset();

        return m_diplomat;
    }
    
    /**
     * Extract segment to get translatable element.
     * 
     * @param p_segment
     * @param p_datatype
     * @param p_sourceLocale
     * @return
     */
    private SegmentNode extractSegment(String p_segment,
            String p_datatype, GlobalSightLocale p_sourceLocale)
    {
        if (p_segment == null || "null".equalsIgnoreCase(p_segment)
                || "".equals(p_segment.trim()))
        {
            return null;
        }
        
        DiplomatAPI api = getDiplomatApi();

        api.setEncoding("UTF-8");
        api.setLocale(p_sourceLocale.getLocale());
        api.setInputFormat(p_datatype);
        api.setSentenceSegmentation(false);
        api.setSegmenterPreserveWhitespace(true);
        StringBuffer sourceString = new StringBuffer();
        sourceString.append("<trans-unit><source>" + p_segment + "</source>");
        sourceString.append("<target>" + p_segment + "</target></trans-unit>");
        api.setSourceString(sourceString.toString());

        try
        {
            api.extract();
            Output output = api.getOutput();

            for (Iterator it = output.documentElementIterator(); it.hasNext();)
            {
                DocumentElement element = (DocumentElement) it.next();

                if (element instanceof TranslatableElement)
                {
                    TranslatableElement trans = (TranslatableElement) element;

                    return (SegmentNode) (trans.getSegments().get(0));
                }
            }
        }
        catch (Exception e)
        {
            if (s_logger.isDebugEnabled())
            {
                s_logger.error(e.getMessage());
            }
        }

        return null;
    }
    
    private GxmlElement getSourceGxmlElement(String p_segString)
    {
        GxmlElement gxmlElement = null;
        try 
        {
            StringBuffer sb = new StringBuffer();
            sb.append("<segment>").append(p_segString).append("</segment>");
            gxmlElement = getGxmlElement(sb.toString());
        }
        catch (Exception e)
        {
        }
        
        return gxmlElement;
    }
    
    private List getAttValuesByName(GxmlElement element, String attName)
    {
        List list = new ArrayList();
        if (element != null)
        {
            String value = element.getAttribute(attName);
            if (value != null)
            {
                list.add(value);
            }
            Iterator childIt = element.getChildElements().iterator();
            while (childIt != null && childIt.hasNext())
            {
                GxmlElement ele = (GxmlElement) childIt.next();
                List subList = getAttValuesByName(ele, attName);
                list.addAll(subList);
            }
        }
        
        return list;
    }
    
    /**
     * Indicate if the segments contain tags.
     * 
     * @return
     */
    private boolean isContainTags()
    {
        HashMap paramMap = getMtParameterMap();
        String containTags = (String) paramMap
                .get(MachineTranslator.CONTAIN_TAGS);
        if (containTags != null && "Y".equalsIgnoreCase(containTags))
        {
            return true;
        }

        return false;
    }
    
    /**
     * Keep same with original source TUV content.
     * 
     * @param p_segString
     * @return
     */
    private String encodeTranslationResult(String p_segString, List idList, List xList)
    {
        if (p_segString == null || p_segString.trim().length() == 0)
        {
            return null;
        }
        
        String result = null;
        try 
        {
            StringBuffer sb = new StringBuffer();
            sb.append("<segment>").append(p_segString).append("</segment>");
            GxmlElement gxmlElement = getGxmlElement(sb.toString());

            this.count = 0;
            resetIdAndX(gxmlElement, idList, xList);
            
            String gxml = gxmlElement.toGxml("xlf");
            result = GxmlUtil.stripRootTag(gxml);
        }
        catch (Exception e)
        {
            result = p_segString;
        }
        
        return result;
    }
    
    private void resetIdAndX(GxmlElement element, List idList, List xList)
    {
        if (element == null)
        {
            return;
        }
        
        String id = element.getAttribute("id");
        if (id != null)
        {
            String idValue = null;
            if (idList != null && idList.size() > count)
            {
                idValue = (String) idList.get(count);
            }
            else
            {
                idValue  = String.valueOf(count + 1);
            }
            element.setAttribute("id", idValue);
        }
        
        String x = element.getAttribute("x");        
        if (x != null)
        {
            String xValue = null;
            if (xList != null && xList.size() > count)
            {
                xValue = (String) xList.get(count);
            }
            else
            {
                xValue = String.valueOf(count + 1);
            }
            element.setAttribute("x", xValue);
        }
        
        if (id != null || x != null)
        {
            this.count++;
        }
        
        Iterator childIt = element.getChildElements().iterator();
        while (childIt.hasNext())
        {
            GxmlElement ele = (GxmlElement) childIt.next();
            resetIdAndX(ele, idList, xList);
        }
    }
    
    /**
     * Construct a GxmlElement object with the specified segment string.
     * 
     * This is from TuvImpl,but that is not static.
     * 
     * @param p_segmentString
     * @return
     */
    public static GxmlElement getGxmlElement(String p_segmentString)
    {
        GxmlElement m_gxmlElement = null;

        String segment = encodeLtGtInGxmlAttributeValue(p_segmentString);
        GxmlFragmentReader reader = null;

        try
        {
            reader = GxmlFragmentReaderPool.instance().getGxmlFragmentReader();

            m_gxmlElement = reader.parseFragment(segment);
        }
        catch (Exception e)
        {

        }
        finally
        {
            GxmlFragmentReaderPool.instance().freeGxmlFragmentReader(reader);
        }

        return m_gxmlElement;
    }

    
    /**
     * If XML attribute has "<" in it ,it will parse error. This method replaces
     * the attribute "<" into "&lt;".
     * 
     * As PROMT can not support ">" in attribute value,also replace ">" to "_gt;_".
     * 
     * Note that this method should keep private!
     */
    private static String encodeLtGtInGxmlAttributeValueForPTS9Trans(String segement)
    {
        // this flag for recording the xml element begin.
        boolean flagXML = false;
        // this flag for recording the attribute begin, because if in "<  >",
        // and begin as double quote, it will be a attribute.
        boolean flagQuote = false;
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < segement.length(); i++)
        {
            char c = segement.charAt(i);

            if (!flagXML && !flagQuote && c == '<')
            {
                flagXML = true;
            }
            else if (flagXML && !flagQuote && c == '"')
            {
                flagQuote = true;
            }
            else if (flagXML && !flagQuote && c == '>')
            {
                flagXML = false;
            }
            else if (flagXML && flagQuote && c == '"')
            {
                flagQuote = false;
            }

            if (flagXML && flagQuote && c == '<')
            {
                sb.append("&lt;");
            }
            else if (flagXML && flagQuote && c == '>')
            {
                sb.append("_gt;_");
            }
            else
            {
                sb.append(c);
            }
        }

        return sb.toString();
    }
    
    /**
     * If XML attribute has "<" in it ,it will parse error. This method replaces
     * the attribute "<" into "&lt;".
     * 
     * As PROMT can not support ">" in attribute value,also replace ">" to "&gt;".
     * 
     * Note that this method should keep private!
     */
    private static String encodeLtGtInGxmlAttributeValue(String segement)
    {
        // this flag for recording the xml element begin.
        boolean flagXML = false;
        // this flag for recording the attribute begin, because if in "<  >",
        // and begin as double quote, it will be a attribute.
        boolean flagQuote = false;
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < segement.length(); i++)
        {
            char c = segement.charAt(i);

            if (!flagXML && !flagQuote && c == '<')
            {
                flagXML = true;
            }
            else if (flagXML && !flagQuote && c == '"')
            {
                flagQuote = true;
            }
            else if (flagXML && !flagQuote && c == '>')
            {
                flagXML = false;
            }
            else if (flagXML && flagQuote && c == '"')
            {
                flagQuote = false;
            }

            if (flagXML && flagQuote && c == '<')
            {
                sb.append("&lt;");
            }
            else if (flagXML && flagQuote && c == '>')
            {
                sb.append("&gt;");
            }
            else
            {
                sb.append(c);
            }
        }

        return sb.toString();
    }

     /**
     * XML attribute value can't have '<' and '"', so find all "&lt;" and 
     * "&quot;" in attribute value and encode them again. 
     * So in exported file, there won't be '<' and '"' after decoded.
     * 
     * Not care '>' and '''.
     * 
     * @deprecated
     * @param segement
     * @return
     */
    private static String encodeGxmlAttributeEntities2(String segement)
    {
        // this flag for recording the xml element begin.
        boolean flagXML = false;
        // this flag for recording the attribute begin, because if in "<  >",
        // and begin as double quote, it will be a attribute.
        boolean flagQuote = false;
        
        StringBuffer sb = new StringBuffer();
        StringBuffer attributeSB = new StringBuffer();

        for (int i = 0; i < segement.length(); i++)
        {
            char c = segement.charAt(i);

            if (!flagXML && !flagQuote && c == '<')
            {
                flagXML = true;
            }
            else if (flagXML && !flagQuote && c == '"')
            {
                flagQuote = true;
            }
            else if (flagXML && !flagQuote && c == '>')
            {
                flagXML = false;
            }
            else if (flagXML && flagQuote && c == '"')
            {
                flagQuote = false;
            }
            
            // In element
            if (flagXML)
            {
                //attribute value
                if (flagQuote)
                {
                    //current char is START '"'.
                    if (c == '"')
                    {
                        sb.append(c);//append START '"'
                    }
                    //in attribute value
                    else
                    {
                        attributeSB.append(c);
                    }
                }
                else
                {
                    //current char is END '"'.
                    if (c == '"')
                    {
                        if (attributeSB != null && attributeSB.length() > 0)
                        {
                            String str = attributeSB.toString();
                            str = str.replaceAll("&lt;", "&amp;lt;");
                            str = str.replaceAll("&quot;", "&amp;quot;");
                            sb.append(str);
                            attributeSB = new StringBuffer();
                        }
                        
                        sb.append(c);//append END '"'
                    }
                    else
                    {
                        sb.append(c);
                    }
                }
            }
            else
            {
                sb.append(c);
            }
            
        }

        return sb.toString();
    }
    
    public static void main(String[] args) throws Exception
    {
        ProMTProxy prox = new ProMTProxy();
        
        /**
        StringBuffer sb = new StringBuffer();
        sb.append("<?xml version=\"1.0\"?>");
        sb.append("<diplomat locale=\"en_US\" version=\"2.0\" datatype=\"xlf\">");
        sb.append("<translatable>");
        sb.append("<ph type=\"ph\" id=\"1\" x=\"1\">&lt;ph id=&quot;1&quot; x=&quot;&amp;lt;Fragment&gt;&quot;&gt;{1}&lt;/ph&gt;</ph>PayPal Revolving Credit<ph type=\"ph\" id=\"2\" x=\"2\">&lt;ph id=&quot;2&quot; x=&quot;&amp;lt;/Fragment&gt;&quot;&gt;{2}&lt;/ph&gt;</ph>");
        sb.append("<ph id=\"1\" x=\"&lt;Fragment>\">{1}</ph>PayPal Revolving Credit<ph id=\"2\" x=\"&lt;/Fragment>\">{2}</ph>");
        sb.append("</translatable>");
        sb.append("</diplomat>");
        String result = revertXlfSegment(sb.toString());
        System.out.println(result);
        */
        
        /**
        String p_segment = "<trans-unit><source><ph id=\"3\" x=\"&lt;Fragment>\">{3}</ph>credit1, simulation2, financing3, money4 needs<ph id=\"4\" x=\"&lt;/Fragment>\">{4}</ph></source></trans-unit>";
        String p_dataType = "xlf";
        GlobalSightLocale p_sourceLocale = new GlobalSightLocale("en", "US", true);
        try
        {
            SegmentNode segNode = extractSegment(p_segment, p_dataType,
                    p_sourceLocale);
        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        */
        
        String segString = "<segment><ph type=\"ph\" id=\"1\" x=\"1\">&lt;ph id=\"5\" x=\"&lt;Fragment&gt;\"&gt;{5}&lt;/ph&gt;</ph>The PayPal revolving credit meets your money needs with simplicity.</segment>";
        GxmlElement gxmlElement = prox.getGxmlElement(segString);
        String str = gxmlElement.toGxml("xlf");
        System.out.println(str);
    }
    
}
