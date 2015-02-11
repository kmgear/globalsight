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

package com.globalsight.everest.webapp.pagehandler.administration.users;

import java.io.Serializable;
import java.util.Vector;

import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;

import com.globalsight.everest.permission.PermissionSet;
import com.globalsight.everest.usermgr.RoleLdapHelper;
import com.globalsight.everest.usermgr.UserLdapHelper;

public class UserSearchParams implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String SEARCH_PARAMS = "searchParams";
    private static final String EMPTY_STR = "";

    public static final int USERNAME_TYPE = 0;
    public static final int FIRSTNAME_TYPE = 1;
    public static final int LASTNAME_TYPE = 2;
    public static final int UNKNOWN_TYPE = -1;

    public static final int BEGINSWITH_FILTER = 0;
    public static final int ENDSWITH_FILTER = 1;
    public static final int CONTAINS_FILTER = 2;
    public static final int UNKNOWN_FILTER = -1;

    private String m_nameParam = null;
    private int m_nameType = UNKNOWN_TYPE;
    private int m_nameFilter = UNKNOWN_FILTER;

    private String m_sourceLocaleParam = null;

    private String m_targetLocaleParam = null;

    private String m_permGroupParam = null;

    private PermissionSet m_permsOfSearcher = null;
    private String m_companyOfSearcher = null;

    /**
     * Default constructor.
     */
    public UserSearchParams() 
    {
        // empty constructor.
    }

    /**
     * Constructs an array of Attribute objects based on the values currently
     * stored in the name, nameType, and nameFilter members.
     *
     * @return An array of Attributes containing search criteria for the
     *         name members. <code>null</code> if m_nameParam is <code>null</code>.
     */
    public Attribute[] getNameAttributes() 
    {
        Attribute[] attrs = null;

        String attrName = null;
        StringBuffer filterToBuild = null;

        if (m_nameParam.trim().equalsIgnoreCase(""))
        {
            //just do a search for all usernames then
            attrs = new BasicAttribute[1];
            attrs[0] = new BasicAttribute(UserLdapHelper.LDAP_ATTR_USERID, "*");
            return attrs;
        }

        if (m_nameParam != null) {
            switch (m_nameType) {
                case USERNAME_TYPE:
                    attrName = UserLdapHelper.LDAP_ATTR_USERID;
                    break;
                case FIRSTNAME_TYPE:
                    attrName = UserLdapHelper.LDAP_ATTR_FIRST_NAME;
                    break;
                case LASTNAME_TYPE:
                    attrName = UserLdapHelper.LDAP_ATTR_LAST_NAME;
                    break;
                default:
                    attrName = UserLdapHelper.LDAP_ATTR_USERID;
                    break;
            }

            filterToBuild = new StringBuffer();

            switch(m_nameFilter) {
                case BEGINSWITH_FILTER:
                    filterToBuild.append(m_nameParam + "*");
                    break;
                case ENDSWITH_FILTER:
                    filterToBuild.append("*" + m_nameParam);
                    break;
                case CONTAINS_FILTER:
                    filterToBuild.append("*" + m_nameParam + "*");
                    break;
                default:
                    filterToBuild.append("*");
                    break;
            }
        }

        if (attrName != null && filterToBuild != null)
        {
            attrs = new BasicAttribute[1];
            attrs[0] = new BasicAttribute(attrName, filterToBuild.toString());
        }
        
        return attrs;

    }

    /**
     * Constructs an array of Attribute objects based on the values currently
     * stored in the sourceLocale and targetLocale members.
     *
     * @return An array of Attributes containing search criteria for the
     *         locale members.
     */
    public Attribute[] getRoleAttributes() 
    {

        Vector vAttrs = new Vector();

        if (m_sourceLocaleParam != null && !(m_sourceLocaleParam.trim().equalsIgnoreCase("")))
        {
            Attribute sourceLocaleAttr = new BasicAttribute(
                    RoleLdapHelper.LDAP_ATTR_SOURCE_LOCALE,
                    m_sourceLocaleParam );
            vAttrs.addElement(sourceLocaleAttr);
        }

        if (m_targetLocaleParam != null && !(m_targetLocaleParam.trim().equalsIgnoreCase(""))) 
        {
            Attribute targetLocaleAttr = new BasicAttribute(
                    RoleLdapHelper.LDAP_ATTR_TARGET_LOCALE,
                    m_targetLocaleParam );
            vAttrs.addElement(targetLocaleAttr);
        }
        

        if (vAttrs.isEmpty())
        {
            return null;
        }
        
        Attribute[] attrs = new BasicAttribute[vAttrs.size()];
        for (int i = 0; i < vAttrs.size(); i++) 
        {
            attrs[i] = (Attribute) vAttrs.elementAt(i);
        }

        return attrs;
    }

    /**
     * Performs validation on the search parameters.
     *
     * @return <code>true</code> if valid; <code>false</code> otherwise.
     */
    public boolean isValid() 
    {
        boolean retVal = true;

        if (  ( m_nameParam==null || EMPTY_STR.equalsIgnoreCase(m_nameParam.trim()))
            &&(m_sourceLocaleParam==null || EMPTY_STR.equalsIgnoreCase(m_sourceLocaleParam))
            &&(m_targetLocaleParam==null || EMPTY_STR.equalsIgnoreCase(m_targetLocaleParam))
            &&(m_permGroupParam==null || EMPTY_STR.equalsIgnoreCase(m_permGroupParam)))
        {

            retVal = false;
        }

        return retVal;
    }

    // Accessor methods for the various member variables.

    public int getNameFilter() {
        return m_nameFilter;
    }

    public String getNameParam() {
        return m_nameParam;
    }

    public int getNameType() {
        return m_nameType;
    }

    public String getSourceLocaleParam() {
        return m_sourceLocaleParam;
    }

    public String getTargetLocaleParam() {
        return m_targetLocaleParam;
    }

    public void setPermissionSetOfSearcher(PermissionSet p_permsOfSearcher)
    {
    m_permsOfSearcher=p_permsOfSearcher;
    }

    public void setCompanyOfSearcher(String p_companyOfSearcher)
    {
        m_companyOfSearcher = p_companyOfSearcher;
    }
    
    public String getCompanyOfSearcher()
    {
        return m_companyOfSearcher;
    }

    public String getPermissionGroupParam() {

        if (m_permGroupParam==null || m_permGroupParam.trim().equalsIgnoreCase("")) {
            return null;
        }

        return m_permGroupParam;
    }

    public PermissionSet getPermissionSetOfSearcher()
    {
    return m_permsOfSearcher;
    }


    public void setNameFilter(int p_nameFilter) {
        m_nameFilter = p_nameFilter;
    }

    public void setNameParam(String p_nameParam) {
        m_nameParam = p_nameParam;
    }

    public void setNameType(int p_nameType) {
        m_nameType = p_nameType;
    }

    public void setSourceLocaleParam(String p_sourceLocaleParam) {
        if (!"".equals(p_sourceLocaleParam))
            m_sourceLocaleParam = p_sourceLocaleParam;
    }

    public void setTargetLocaleParam(String p_targetLocaleParam) {
        if (!"".equals(p_targetLocaleParam))
            m_targetLocaleParam = p_targetLocaleParam;
    }

    public void setPermissionGroupParam(String p_permGroupParam) {
        if (!"".equals(p_permGroupParam))
            m_permGroupParam = p_permGroupParam;
    }

}
