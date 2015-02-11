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
package com.globalsight.everest.servlet;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.globalsight.everest.webapp.WebAppConstants;
import com.globalsight.everest.webapp.pagehandler.PageHandler;
import com.globalsight.log.GlobalSightCategory;
import com.sun.jndi.toolkit.url.UrlUtil;

/**
 * The ImageServlet can be used to view images contained in imported
 * documents, i.e. images that are stored in the CXEDOCS directory.
 */
public class ImageFileServlet2
    extends UncacheableFileServlet
{
    private static final long serialVersionUID = -555209420906242066L;
    public GlobalSightCategory CATEGORY =
        (GlobalSightCategory)GlobalSightCategory.getLogger("Images");

    /**
     * Write out the image to the response's buffered stream.
     *
     * @param p_request -- the request
     * @param p_response -- the response
     * @throws ServletException
     * @throws IOException
     */
    public void service(HttpServletRequest p_request,
        HttpServletResponse p_response)
        throws ServletException, IOException
    {
        HttpSession userSession = p_request.getSession(false);
        // if there is no session in the browser, forward to login page.
        if (userSession == null) 
        {
            p_response.sendRedirect("/globalsight");
            return;
        }
        
        String docHome = getInitParameter("docHome");

        // strip off the first CXEDOCS part of the url since it is not
        // part of the directory structure
        String url = p_request.getRequestURI().toString();
        String decodedUrl = UrlUtil.decode(url, "utf-8");
        String fileName = decodedUrl;

        int index = decodedUrl.indexOf(WebAppConstants.VIRTUALDIR_CXEDOCS2);
        if (index >= 0)
        {
            fileName = decodedUrl.substring(
                index + WebAppConstants.VIRTUALDIR_CXEDOCS.length());
        }
        else
        {
            // invalid or incorrect use of this servlet
            CATEGORY.warn("Invalid request for " + fileName +
                ", not under " + WebAppConstants.VIRTUALDIR_CXEDOCS);
            p_response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        File file = new File(docHome, fileName);
        if (!file.exists())
        {
            if (CATEGORY.isDebugEnabled())
            {
                CATEGORY.debug("Requested image `" + fileName +
                    "' does not exist.");
            }
            p_response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try
        {
            if (CATEGORY.isDebugEnabled())
            {
                CATEGORY.debug("Sending image " + fileName);
            }
            
            String contentType = "application/octet-stream";
            // PO File is used in Linux/Unix
            if(fileName!=null && fileName.toLowerCase().endsWith(".po"))
            {
                contentType = "text/x-gettext-translation";
            }
            
            //set the content type appropriately
            p_response.setContentType(contentType);
            String attachment = "attachment; filename=\""
                    + UrlUtil.encode(file.getName(), "utf-8") + "\";";
            p_response.setHeader("Content-Disposition", attachment);
            
            if (p_request.isSecure())
            {
                PageHandler.setHeaderForHTTPSDownload(p_response);
            }
            
            writeOutFile(file, p_response, false);
        }
        catch (Throwable ignore)
        {
            // client may have closed the connection, ignore
        }
    }
}