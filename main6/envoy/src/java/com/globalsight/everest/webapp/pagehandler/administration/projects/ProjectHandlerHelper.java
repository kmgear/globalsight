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
package com.globalsight.everest.webapp.pagehandler.administration.projects;

/* Copyright (c) 2000, GlobalSight Corporation.  All rights reserved.
 * THIS DOCUMENT CONTAINS TRADE SECRET DATA WHICH IS THE PROPERTY OF
 * GLOBALSIGHT CORPORATION. THIS DOCUMENT IS SUBMITTED TO RECIPIENT
 * IN CONFIDENCE. INFORMATION CONTAINED HEREIN MAY NOT BE USED, COPIED
 * OR DISCLOSED IN WHOLE OR IN PART EXCEPT AS PERMITTED BY WRITTEN
 * AGREEMENT SIGNED BY AN OFFICER OF GLOBALSIGHT CORPORATION.
 *
 * THIS MATERIAL IS ALSO COPYRIGHTED AS AN UNPUBLISHED WORK UNDER
 * SECTIONS 104 AND 408 OF TITLE 17 OF THE UNITED STATES CODE.
 * UNAUTHORIZED USE, COPYING OR OTHER REPRODUCTION IS PROHIBITED
 * BY LAW.
 */
// java
import java.util.Collection;

import java.rmi.RemoteException;

// javax
import javax.naming.NamingException;
// com.globalsight
import com.globalsight.everest.foundation.User;
import com.globalsight.everest.projecthandler.Project;
import com.globalsight.everest.projecthandler.ProjectHandlerException;
import com.globalsight.everest.servlet.EnvoyServletException;
import com.globalsight.everest.servlet.util.ServerProxy;
import com.globalsight.everest.servlet.util.SessionManager;
import com.globalsight.everest.usermgr.UserInfo;
import com.globalsight.everest.usermgr.UserManager;
import com.globalsight.everest.usermgr.UserManagerException;
import com.globalsight.util.GeneralException;

import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class ProjectHandlerHelper 
{
    /**
     * Add a project to the system.
     * <p>
     * @param p_project The project to add.
     * @exception EnvoyServletException.  Failed to add the profile;
     *		  the cause is indicated by the exception code.
     */
    public static void addProject(Project p_project) throws EnvoyServletException
    {
        try
        {
            ServerProxy.getProjectHandler().addProject(p_project);
        }
        catch (ProjectHandlerException phe)
        {
            throw new EnvoyServletException(phe.getExceptionId(), phe);
        }
        catch (NamingException ne)
        {
            throw new EnvoyServletException(GeneralException.EX_NAMING, ne);
        }
        catch (GeneralException ge)
        {
            throw new EnvoyServletException(ge.getExceptionId(), ge);
        }
        catch (RemoteException re)
        {
            throw new EnvoyServletException(GeneralException.EX_REMOTE, re);
        }
    }

    /**
     * Create a project.
     * <p>
     * @return Return the project that is created.
     * @exception EnvoyServletException.  Failed to create the project; the cause
     *		  is indicated by the exception code.
     */
    public static Project createProject() throws EnvoyServletException
    {
        try
        {
            return ServerProxy.getProjectHandler().createProject();
        }
        catch (ProjectHandlerException phe)
        {
            throw new EnvoyServletException(phe.getExceptionId(), phe);
        }
        catch (NamingException ne)
        {
            throw new EnvoyServletException(GeneralException.EX_NAMING, ne);
        }
        catch (GeneralException ge)
        {
            throw new EnvoyServletException(ge.getExceptionId(), ge);
        }
        catch (RemoteException re)
        {
            throw new EnvoyServletException(GeneralException.EX_REMOTE, re);
        }
    }

    /**
     * Delete a project from the system.
     * <p>
     * @param p_project The project to be deleted.
     * @exception EnvoyServletException.  Failed to delete the project; the cause
     * 		  is indicated by the exception code.
     */
    public static void deleteProject(Project p_project) throws EnvoyServletException
    {
        try
        {
            ServerProxy.getProjectHandler().deleteProject(p_project);
        }
        catch (ProjectHandlerException phe)
        {
            throw new EnvoyServletException(phe.getExceptionId(), phe);
        }
        catch (NamingException ne)
        {
            throw new EnvoyServletException(GeneralException.EX_NAMING, ne);
        }
        catch (GeneralException ge)
        {
            throw new EnvoyServletException(ge.getExceptionId(), ge);
        }
        catch (RemoteException re)
        {
            throw new EnvoyServletException(GeneralException.EX_REMOTE, re);
        }
    }

    /**
     * Returns all the projects in the system.
     * <p>
     * @return Return all the projects in the system.
     * @exception EnvoyServletException.  Miscellaneous exception, most likely
     *            occuring in the persistence component.
     */
    public static Collection getAllProjects() throws EnvoyServletException
    {
        try
        {
            return ServerProxy.getProjectHandler().getAllProjects();
        }
        catch (ProjectHandlerException phe)
        {
            throw new EnvoyServletException(phe.getExceptionId(), phe);
        }
        catch (NamingException ne)
        {
            throw new EnvoyServletException(GeneralException.EX_NAMING, ne);
        }
        catch (GeneralException ge)
        {
            throw new EnvoyServletException(ge.getExceptionId(), ge);
        }
        catch (RemoteException re)
        {
            throw new EnvoyServletException(GeneralException.EX_REMOTE, re);
        }
    }

    /**
     * Returns all the projects (as ProjectInfo) in the system.
     * <p>
     * @return Return all the projects (as ProjectInfo) in the system.
     * @exception EnvoyServletException.  Miscellaneous exception, most likely
     *            occuring in the persistence component.
     */
    public static Collection getAllProjectsForGUI() throws EnvoyServletException
    {
        try
        {
            return ServerProxy.getProjectHandler().getAllProjectInfosForGUI();
        }
        catch (ProjectHandlerException phe)
        {
            throw new EnvoyServletException(phe.getExceptionId(), phe);
        }
        catch (NamingException ne)
        {
            throw new EnvoyServletException(GeneralException.EX_NAMING, ne);
        }
        catch (GeneralException ge)
        {
            throw new EnvoyServletException(ge.getExceptionId(), ge);
        }
        catch (RemoteException re)
        {
            throw new EnvoyServletException(GeneralException.EX_REMOTE, re);
        }
    }

    /**
     * Get user matched the given uid.
     * <p>
     * @param p_userId - The user id
     * @return The user associated with the user id.
     * @exception EnvoyServletException
     */
    public static User getUser(String p_userId) throws EnvoyServletException
    {
        try
        {
            return ServerProxy.getUserManager().getUser(p_userId);
        }
        catch (GeneralException ge)
        {
            throw new EnvoyServletException(ge.getExceptionId(), ge);
        }
        catch (RemoteException re)
        {
            throw new EnvoyServletException(GeneralException.EX_REMOTE, re);
        }
    }

    /**
     * Get the Tm objects in the system.
     * <p>
     * @return A collection of Tms.
     * @exception EnvoyServletException.
     */
    public static Collection getAllTms() throws EnvoyServletException
    {
        try
        {
            return ServerProxy.getTmManager().getAllTms();
        }
        catch (GeneralException ge)
        {
            throw new EnvoyServletException(ge.getExceptionId(), ge);
        }
        catch (RemoteException re)
        {
            throw new EnvoyServletException(GeneralException.EX_REMOTE, re);
        }
    }

    /**
     * Modify a project in the system.
     * <p>
     * @param p_project The project to be modified.
     * @exception EnvoyServletException.  Componenet related exception.
     */
    public static void modifyProject(Project p_project, 
                                     String p_modifierId)
        throws EnvoyServletException
    {
	try
        {
            ServerProxy.getProjectHandler().modifyProject(
                p_project, p_modifierId);
        }
        catch (ProjectHandlerException phe)
        {
            throw new EnvoyServletException(phe.getExceptionId(), phe);
        }
        catch (NamingException ne)
        {
            throw new EnvoyServletException(GeneralException.EX_NAMING, ne);
        }
        catch (GeneralException ge)
        {
            throw new EnvoyServletException(ge.getExceptionId(), ge);
        }
        catch (RemoteException re)
        {
            throw new EnvoyServletException(GeneralException.EX_REMOTE, re);
        }
    }

    /**
     * Retrieve an existing project.
     * <p>
     * @return Return an existing project.
     * @exception EnvoyServletException.  Failed to access the project; the cause
     *		  is indicated by the exception code.
     */
    public static Project getProjectById(long p_id) throws EnvoyServletException
    {
        try
        {
            return ServerProxy.getProjectHandler().getProjectById(p_id);
        }
        catch (ProjectHandlerException phe)
        {
            throw new EnvoyServletException(phe.getExceptionId(), phe);
        }
        catch (NamingException ne)
        {
            throw new EnvoyServletException(GeneralException.EX_NAMING, ne);
        }
        catch (GeneralException ge)
        {
            throw new EnvoyServletException(ge.getExceptionId(), ge);
        }
        catch (RemoteException re)
        {
            throw new EnvoyServletException(GeneralException.EX_REMOTE, re);
        }
    }
    
    /**
     * 
     * Returns all the projects (as Project) in the system.
     * <p>
     * @return Return all the projects in the system.
     * @exception EnvoyServletException. 
     * 
     */
    public static List getProjectByUser(String p_id) throws EnvoyServletException
    {
        try
        {
            return ServerProxy.getProjectHandler().getProjectsByUser((p_id));
        }
        catch (ProjectHandlerException phe)
        {
            throw new EnvoyServletException(phe.getExceptionId(), phe);
        }
        catch (NamingException ne)
        {
            throw new EnvoyServletException(GeneralException.EX_NAMING, ne);
        }
        catch (GeneralException ge)
        {
            throw new EnvoyServletException(ge.getExceptionId(), ge);
        }
        catch (RemoteException re)
        {
            throw new EnvoyServletException(GeneralException.EX_REMOTE, re);
        }
    }

    /**
     * Retrieve possible users for a project that a particular pm owns
     * <p>
     * @return Return an list of users.
     */
    public static List getPossibleUsersForProject(User pm)
         throws EnvoyServletException
    {
        try
        {
             return ServerProxy.getProjectHandler().getAllPossibleUserInfos(pm);
        }
        catch (GeneralException ge)
        {
            throw new EnvoyServletException(ge.getExceptionId(), ge);
        }
        catch (RemoteException re)
        {
            throw new EnvoyServletException(GeneralException.EX_REMOTE, re);
        }
        catch (NamingException ne)
        {
            throw new EnvoyServletException(GeneralException.EX_NAMING, ne);
        }
    }

    /**
     * Extract data from users page and set in project.
     */
    public static void extractUsers(Project project, HttpServletRequest request,
                                    SessionManager sessionMgr)
        throws EnvoyServletException
    {
        // Set users
        String toField = (String)request.getParameter("toField");
        // First, make sure default users are in the list
        ArrayList defUsers = (ArrayList)sessionMgr.getAttribute("defUsers");
        TreeSet addedUsers = new TreeSet();
        for (int i = 0; i < defUsers.size(); i++)
        {
            addedUsers.add(((UserInfo)defUsers.get(i)).getUserId());
        }

        if (toField != null && !toField.equals(""))
        {
            String[] userids = toField.split(",");
            for (int i=0; i < userids.length; i++)
            {
                addedUsers.add(userids[i]);
            }
        }
        project.setUserIds(addedUsers);
    }

    private static UserManager getUserManager()
        throws EnvoyServletException
    {
        try
        {
            return ServerProxy.getUserManager();
        }
        catch (GeneralException ge)
        {
            throw new EnvoyServletException(ge.getExceptionId(), ge);
        }
    }
}
