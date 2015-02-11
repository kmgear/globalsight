package com.globalsight.cxe.entity.filterconfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.globalsight.ling.docproc.IFormatNames;

public class FilterConstants
{
    public final static String TABLENAME = "FILTER_TABLE_NAME";
    public final static String TABLEID = "FILTER_TABLE_ID";
    
    public final static String JAVAPROPERTIES_TABLENAME = "java_properties_filter";
    public final static String JAVASCRIPT_TABLENAME = "java_script_filter";
    public final static String MSOFFICEDOC_TABLENAME = "ms_office_doc_filter";
    public final static String XMLRULE_TABLENAME = "xml_rule_filter";
    public final static String HTML_TABLENAME = "html_filter";
    public final static String JSP_TABLENAME = "jsp_filter";
    public final static String MSOFFICEEXCEL_TABLENAME = "ms_office_excel_filter";
    public final static String INDD_TABLENAME = "indd_filter";
    public final static String OPENOFFICE_TABLENAME = "openoffice_filter";
    public final static String MSOFFICEPPT_TABLENAME = "ms_office_ppt_filter";
    public final static String OFFICE2010_TABLENAME = "office2010_filter";
    public final static String PO_TABLENAME = "po_filter";
    

    public final static ArrayList<String> ALL_FILTER_TABLE_NAMES = new ArrayList<String>();
    public final static Map<String, String> FILTER_TABLE_NAMES_FORMAT = new HashMap<String, String>();
    static
    {
        ALL_FILTER_TABLE_NAMES.add(JAVAPROPERTIES_TABLENAME);
        ALL_FILTER_TABLE_NAMES.add(JAVASCRIPT_TABLENAME);
        ALL_FILTER_TABLE_NAMES.add(MSOFFICEDOC_TABLENAME);
        ALL_FILTER_TABLE_NAMES.add(XMLRULE_TABLENAME);
        ALL_FILTER_TABLE_NAMES.add(HTML_TABLENAME);
        ALL_FILTER_TABLE_NAMES.add(JSP_TABLENAME);
        ALL_FILTER_TABLE_NAMES.add(MSOFFICEEXCEL_TABLENAME);
        ALL_FILTER_TABLE_NAMES.add(INDD_TABLENAME);
        ALL_FILTER_TABLE_NAMES.add(OPENOFFICE_TABLENAME);
        ALL_FILTER_TABLE_NAMES.add(MSOFFICEPPT_TABLENAME);
        ALL_FILTER_TABLE_NAMES.add(OFFICE2010_TABLENAME);
        ALL_FILTER_TABLE_NAMES.add(PO_TABLENAME);
        
        FILTER_TABLE_NAMES_FORMAT.put(HTML_TABLENAME, IFormatNames.FORMAT_HTML);
        FILTER_TABLE_NAMES_FORMAT.put(MSOFFICEDOC_TABLENAME, IFormatNames.FORMAT_WORD_HTML);
        FILTER_TABLE_NAMES_FORMAT.put(XMLRULE_TABLENAME, IFormatNames.FORMAT_XML);
        FILTER_TABLE_NAMES_FORMAT.put(MSOFFICEEXCEL_TABLENAME, IFormatNames.FORMAT_EXCEL_HTML);
        FILTER_TABLE_NAMES_FORMAT.put(JAVAPROPERTIES_TABLENAME, IFormatNames.FORMAT_JAVAPROP);
        FILTER_TABLE_NAMES_FORMAT.put(JSP_TABLENAME, IFormatNames.FORMAT_JSP);
        FILTER_TABLE_NAMES_FORMAT.put(JAVASCRIPT_TABLENAME, IFormatNames.FORMAT_JAVASCRIPT);
        FILTER_TABLE_NAMES_FORMAT.put(OPENOFFICE_TABLENAME, IFormatNames.FORMAT_OPENOFFICE_XML);
        FILTER_TABLE_NAMES_FORMAT.put(MSOFFICEPPT_TABLENAME,IFormatNames.FORMAT_POWERPOINT_HTML);
        FILTER_TABLE_NAMES_FORMAT.put(OFFICE2010_TABLENAME,IFormatNames.FORMAT_OFFICE_XML);
        FILTER_TABLE_NAMES_FORMAT.put(PO_TABLENAME,IFormatNames.FORMAT_PO);
    }
    
    
}
