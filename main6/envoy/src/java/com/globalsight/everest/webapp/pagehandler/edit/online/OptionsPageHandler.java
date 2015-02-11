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

package com.globalsight.everest.webapp.pagehandler.edit.online;

import com.globalsight.everest.webapp.pagehandler.edit.online.EditorState;

import com.globalsight.config.UserParameter;
import com.globalsight.config.UserParameterEntityException;
import com.globalsight.config.UserParamNames;
import com.globalsight.everest.servlet.EnvoyServletException;
import com.globalsight.everest.webapp.javabean.NavigationBean;
import com.globalsight.everest.webapp.pagehandler.PageHandler;
import com.globalsight.everest.webapp.webnavigation.WebPageDescriptor;
import com.globalsight.everest.webapp.WebAppConstants;
import com.globalsight.everest.servlet.util.ServerProxy;
import com.globalsight.everest.servlet.util.SessionManager;
import com.globalsight.util.GeneralException;
import com.globalsight.log.GlobalSightCategory;

import java.rmi.RemoteException;

import java.io.IOException;
import java.util.Enumeration;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


public class OptionsPageHandler
    extends PageHandler
    implements UserParamNames, WebAppConstants
{
    private static final GlobalSightCategory CATEGORY =
        (GlobalSightCategory)GlobalSightCategory.getLogger(
            OptionsPageHandler.class);

    /**
     * Invokes this EntryPageHandler object.
     *
     * @param pageDescriptor the description of the page to be produced
     * @param request the original request sent from the browser
     * @param response original response object
     * @param context the Servlet context
     */
    public void invokePageHandler(WebPageDescriptor p_pageDescriptor,
        HttpServletRequest p_request, HttpServletResponse p_response,
        ServletContext p_context)
        throws ServletException, IOException, EnvoyServletException
    {
        HttpSession session = p_request.getSession();
        SessionManager sessionMgr = (SessionManager)session.getAttribute(
            WebAppConstants.SESSION_MANAGER);

        EditorState state = (EditorState)sessionMgr.getAttribute(
            WebAppConstants.EDITORSTATE);

        GeneralException exception = null;

        try
        {
            if (p_request.getParameter("__save") != null)
            {
                try
                {
                    setParameters(session, p_request);
                }
                catch (Exception ex)
                {
                    CATEGORY.error("cannot update user parameters", ex);

                    exception = new GeneralException(
                        "cannot update user parameters", ex);
                }
                finally
                {
                    // Sat Mar 12 01:21:42 2005 CvdL:
                    // initEditorOptions() resets the layout state to
                    // default values.  We must preserve the current
                    // layout for ITERATE_SUBS (set in SE) to work
                    // correctly.  This may break setting the default
                    // layout in ME and expecting the editor to
                    // refresh with the new layout, but then you could
                    // argue that the default layout for new pages
                    // should not affect what the current layout is.
                    EditorState.Layout layout = state.getLayout();
                    state.setLayout(null);

                    EditorHelper.initEditorOptions(state, session);

                    state.setLayout(layout);
                }
            }

            getParameters(session, p_request);

            // null error means everything ok
            p_request.setAttribute(USER_PARAMS_ERROR, "");
        }
        catch (Exception ex)
        {
            CATEGORY.error("cannot read user parameters", ex);

            exception = new GeneralException(
                "cannot read user parameters", ex);
        }
        finally
        {
            if (exception != null)
            {
                // string means show error message
                p_request.setAttribute(USER_PARAMS_ERROR,
                    exception.getTopLevelMessage() + "@@@@@" +
                    GeneralException.getStackTraceString(exception));
            }
        }

        super.invokePageHandler(p_pageDescriptor, p_request,
            p_response, p_context);
    }


    private void getParameters(HttpSession p_session,
        HttpServletRequest p_request)
        throws EnvoyServletException
    {
        p_request.setAttribute(EDITOR_AUTO_SAVE_SEGMENT,
            PageHandler.getUserParameter(
                p_session, EDITOR_AUTO_SAVE_SEGMENT).getValue());

        p_request.setAttribute(EDITOR_AUTO_UNLOCK,
            PageHandler.getUserParameter(
                p_session, EDITOR_AUTO_UNLOCK).getValue());

        p_request.setAttribute(EDITOR_AUTO_SYNC,
            PageHandler.getUserParameter(
                p_session, EDITOR_AUTO_SYNC).getValue()); 

        p_request.setAttribute(EDITOR_AUTO_ADJUST_WHITESPACE,
            PageHandler.getUserParameter(
                p_session, EDITOR_AUTO_ADJUST_WHITESPACE).getValue());

        p_request.setAttribute(EDITOR_LAYOUT,
            PageHandler.getUserParameter(
                p_session, EDITOR_LAYOUT).getValue());

        p_request.setAttribute(EDITOR_VIEWMODE,
            PageHandler.getUserParameter(
                p_session, EDITOR_VIEWMODE).getValue());

        p_request.setAttribute(EDITOR_PTAGMODE,
            PageHandler.getUserParameter(
                p_session, EDITOR_PTAGMODE).getValue());

        p_request.setAttribute(EDITOR_PTAGHILITE,
            PageHandler.getUserParameter(
                p_session, EDITOR_PTAGHILITE).getValue());

        p_request.setAttribute(EDITOR_SHOW_MT,
            PageHandler.getUserParameter(
                p_session, EDITOR_SHOW_MT).getValue());

        p_request.setAttribute(EDITOR_ITERATE_SUBS,
            PageHandler.getUserParameter(
                p_session, EDITOR_ITERATE_SUBS).getValue());

        p_request.setAttribute(TM_MATCHING_THRESHOLD,
            PageHandler.getUserParameter(
                p_session, TM_MATCHING_THRESHOLD).getValue());

        p_request.setAttribute(TB_MATCHING_THRESHOLD,
            PageHandler.getUserParameter(
                p_session, TB_MATCHING_THRESHOLD).getValue());

        p_request.setAttribute(HYPERLINK_COLOR_OVERRIDE,
            PageHandler.getUserParameter(
                p_session, HYPERLINK_COLOR_OVERRIDE).getValue());

        p_request.setAttribute(HYPERLINK_COLOR,
            PageHandler.getUserParameter(
                p_session, HYPERLINK_COLOR).getValue());

        p_request.setAttribute(ACTIVE_HYPERLINK_COLOR,
            PageHandler.getUserParameter(
                p_session, ACTIVE_HYPERLINK_COLOR).getValue());

        p_request.setAttribute(VISITED_HYPERLINK_COLOR,
            PageHandler.getUserParameter(
                p_session, VISITED_HYPERLINK_COLOR).getValue());
        
        p_request.setAttribute(EDITOR_SEGMENTS_MAX_NUM,
                PageHandler.getUserParameter(
                    p_session, EDITOR_SEGMENTS_MAX_NUM).getValue());
    }

    private void setParameters(HttpSession p_session,
        HttpServletRequest p_request)
        throws EnvoyServletException
    {
        String userName = (String)p_session.getAttribute(WebAppConstants.USER_NAME);

        setParameter(p_session, p_request, userName, EDITOR_AUTO_SAVE_SEGMENT);
        setParameter(p_session, p_request, userName, EDITOR_AUTO_UNLOCK);
        setParameter(p_session, p_request, userName, EDITOR_AUTO_SYNC);
        setParameter(p_session, p_request, userName, EDITOR_AUTO_ADJUST_WHITESPACE);
        setParameter(p_session, p_request, userName, EDITOR_LAYOUT);
        setParameter(p_session, p_request, userName, EDITOR_VIEWMODE);
        setParameter(p_session, p_request, userName, EDITOR_PTAGMODE);
        setParameter(p_session, p_request, userName, EDITOR_PTAGHILITE);
        setParameter(p_session, p_request, userName, EDITOR_SHOW_MT);
        setParameter(p_session, p_request, userName, EDITOR_ITERATE_SUBS);
        setParameter(p_session, p_request, userName, TM_MATCHING_THRESHOLD);
        setParameter(p_session, p_request, userName, TB_MATCHING_THRESHOLD);
        setParameter(p_session, p_request, userName, HYPERLINK_COLOR_OVERRIDE);
        setParameter(p_session, p_request, userName, HYPERLINK_COLOR);
        setParameter(p_session, p_request, userName, ACTIVE_HYPERLINK_COLOR);
        setParameter(p_session, p_request, userName, VISITED_HYPERLINK_COLOR);
        setParameter(p_session, p_request, userName, EDITOR_SEGMENTS_MAX_NUM);
    }

    private void setParameter(HttpSession p_session,
        HttpServletRequest p_request, String p_userName, String p_name)
        throws EnvoyServletException
    {
        String newValue = p_request.getParameter(p_name);

        if (newValue != null)
        {
            UserParameter param = PageHandler.getUserParameter(p_session, p_name);

            if (param != null) 
            {
                param.setValue(newValue);
                param = updateParameter(param);

                PageHandler.setUserParameter(p_session, param);            	
            }
        }
    }

    private UserParameter updateParameter(UserParameter p_parameter)
        throws EnvoyServletException
    {
        try
        {
            return ServerProxy.getUserParameterManager().
                updateUserParameter(p_parameter);
        }
        catch (RemoteException re)
        {
            throw new EnvoyServletException(GeneralException.EX_REMOTE, re);
        }
        catch (GeneralException ge)
        {
            throw new EnvoyServletException(ge);
        }
    }
}
