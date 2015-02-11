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
package com.globalsight.everest.webapp.pagehandler.administration.vendors;

import com.globalsight.everest.foundation.User;
import com.globalsight.everest.vendormanagement.Vendor;
import com.globalsight.everest.securitymgr.FieldSecurity;
import com.globalsight.everest.servlet.EnvoyServletException;
import com.globalsight.everest.servlet.util.ServerProxy;
import com.globalsight.everest.servlet.util.SessionManager;
import com.globalsight.everest.webapp.WebAppConstants;
import com.globalsight.everest.webapp.pagehandler.ControlFlowHelper;
import com.globalsight.everest.webapp.pagehandler.PageHandler;
import com.globalsight.everest.webapp.webnavigation.WebPageDescriptor;
import com.globalsight.everest.webapp.pagehandler.administration.permission.PermissionHelper;
import com.globalsight.util.GeneralException;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


public class VendorPermissionHandler extends PageHandler
{
    
    /**
     * Invokes this PageHandler
     *
     * @param p_pageDescriptor the page desciptor
     * @param p_request the original request sent from the browser
     * @param p_response the original response object
     * @param p_context context the Servlet context
     */
    public void invokePageHandler(WebPageDescriptor pageDescriptor,
                                  HttpServletRequest request,
                                  HttpServletResponse response,
                                  ServletContext context)
    throws ServletException, IOException,
        EnvoyServletException
    {

        HttpSession session = request.getSession(false);
        SessionManager sessionMgr =
            (SessionManager)session.getAttribute(SESSION_MANAGER);
        User user = (User)sessionMgr.getAttribute(WebAppConstants.USER);
        Vendor vendor = (Vendor)sessionMgr.getAttribute("vendor");
        String action = (String)request.getParameter("action");

        
        if ("next".equals(action))
        {
            // save security info
            FieldSecurity fs = (FieldSecurity)
                sessionMgr.getAttribute(VendorConstants.FIELD_SECURITY_NOCHECK);
            VendorHelper.saveSecurity(fs, request);
        }
        else if ("perms".equals(action))
        {
            // Save the data from the basic info page
            VendorHelper.saveBasicInfo(vendor, request);

        }

        // Get data for page
        request.setAttribute("allPerms", PermissionHelper.getAllPermissionGroups());
        Collection userPerms = (Collection)sessionMgr.getAttribute("userPerms");
        if (userPerms == null)
        {
            String vendorUserId = vendor.getUserId();
            if (vendorUserId != null)
            {
                userPerms =
                    PermissionHelper.getAllPermissionGroupsForUser(vendor.getUserId());
                sessionMgr.setAttribute("userPerms",
                                        PermissionHelper.getAllPermissionGroupsForUser(vendor.getUserId()));
            }
        }

        // Call parent invokePageHandler() to set link beans and invoke JSP
        super.invokePageHandler(pageDescriptor, request,
                                response, context);
    }

}
