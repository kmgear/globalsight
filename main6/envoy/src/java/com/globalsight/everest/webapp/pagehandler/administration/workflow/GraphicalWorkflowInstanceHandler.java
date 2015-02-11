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
package com.globalsight.everest.webapp.pagehandler.administration.workflow;

// GlobalSight
import java.io.IOException;
import java.io.ObjectInputStream;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TimeZone;
import java.util.Vector;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import CoffeeTable.Grid.GridData;

import com.globalsight.everest.costing.AmountOfWork;
import com.globalsight.everest.costing.Cost;
import com.globalsight.everest.costing.CostCalculator;
import com.globalsight.everest.costing.Currency;
import com.globalsight.everest.costing.Rate;
import com.globalsight.everest.foundation.ContainerRole;
import com.globalsight.everest.foundation.Timestamp;
import com.globalsight.everest.foundation.User;
import com.globalsight.everest.foundation.UserRoleImpl;
import com.globalsight.everest.jobhandler.Job;
import com.globalsight.everest.servlet.EnvoyServletException;
import com.globalsight.everest.servlet.util.ServerProxy;
import com.globalsight.everest.servlet.util.SessionManager;
import com.globalsight.everest.taskmanager.Task;
import com.globalsight.everest.taskmanager.TaskAssignee;
import com.globalsight.everest.taskmanager.TaskInfo;
import com.globalsight.everest.webapp.WebAppConstants;
import com.globalsight.everest.webapp.applet.common.AppletDate;
import com.globalsight.everest.webapp.javabean.TaskInfoBean;
import com.globalsight.everest.webapp.pagehandler.PageHandler;
import com.globalsight.everest.webapp.pagehandler.projects.workflows.JobManagementHandler;
import com.globalsight.everest.webapp.webnavigation.WebPageDescriptor;
import com.globalsight.everest.workflow.WorkflowConstants;
import com.globalsight.everest.workflow.WorkflowInstance;
import com.globalsight.everest.workflow.WorkflowTaskInstance;
import com.globalsight.everest.workflowmanager.Workflow;
import com.globalsight.everest.workflowmanager.WorkflowManagerException;
import com.globalsight.util.GeneralException;
import com.globalsight.util.GlobalSightLocale;
import com.globalsight.util.modules.Modules;

/**
 * GraphicalWorkflowInstanceHandler is responsible for all of the activites of
 * the graphical workflow UI (i.e. displaying an existing workflow, and updating
 * a workflow).
 */

public class GraphicalWorkflowInstanceHandler extends PageHandler implements
		WorkflowTemplateConstants
{

	// ////////////////////////////////////////////////////////////////////
	// Begin: Constructor
	// ////////////////////////////////////////////////////////////////////
	public GraphicalWorkflowInstanceHandler()
	{
		super();
	}

	// ////////////////////////////////////////////////////////////////////
	// End: Constructor
	// ////////////////////////////////////////////////////////////////////

	// ////////////////////////////////////////////////////////////////////
	// Begin: Override Methods
	// ////////////////////////////////////////////////////////////////////
	/**
     * Invokes this PageHandler
     * 
     * @param p_pageDescriptor
     *            the page desciptor
     * @param p_request
     *            the original request sent from the browser
     * @param p_response
     *            the original response object
     * @param p_context
     *            context the Servlet context
     */
	public void invokePageHandler(WebPageDescriptor p_pageDescriptor,
			HttpServletRequest p_request, HttpServletResponse p_response,
			ServletContext p_context) throws ServletException, IOException,
			EnvoyServletException
	{
		// first store the basic info
		storeInfoInSessionManager(p_request);
		// Call parent invokePageHandler() to set link beans and invoke JSP
		super.invokePageHandler(p_pageDescriptor, p_request, p_response,
				p_context);
	}

	/**
     * Invokes this page handler for an applet request object.
     * 
     * @param p_isGet -
     *            Determines whether the request is a get or post.
     * @param thePageDescriptor
     *            the description of the page to be produced
     * @param theRequest
     *            the original request sent from the browser
     * @param theResponse
     *            the original response object
     * @param context
     *            the Servlet context
     * @return A vector of serializable objects to be passed to applet.
     */
	public Vector invokePageHandlerForApplet(boolean p_isDoGet,
			WebPageDescriptor p_thePageDescriptor,
			HttpServletRequest p_theRequest, HttpServletResponse p_theResponse,
			ServletContext p_context, HttpSession p_session)
			throws ServletException, IOException, EnvoyServletException
	{
		Vector retVal = null;
		if (p_isDoGet)
		{
			retVal = getDisplayData(p_theRequest, p_session);
		}
		else
		{
			retVal = saveData(p_theRequest, p_session);
		}
		return retVal;
	}

	// ////////////////////////////////////////////////////////////////////
	// End: Override Methods
	// ////////////////////////////////////////////////////////////////////

	// ////////////////////////////////////////////////////////////////////
	// Begin: Local Methods
	// ////////////////////////////////////////////////////////////////////

	// store the info from the first page in the session manager
	private void storeInfoInSessionManager(HttpServletRequest p_request)
			throws EnvoyServletException
	{
		try
		{
			SessionManager sessionMgr = WorkflowTemplateHandlerHelper
					.getSessionManager(p_request);

			HttpSession session = p_request.getSession(false);
			String action = p_request.getParameter("action");
			String wfId = p_request.getParameter(JobManagementHandler.WF_ID);
			sessionMgr.setAttribute(JobManagementHandler.MODIFY_WF_PARAM, wfId);
			String jobId = p_request.getParameter(JobManagementHandler.JOB_ID);
			sessionMgr.setAttribute(JobManagementHandler.JOB_ID,
					new Long(jobId));
			if (wfId != null)
			{
				Workflow workflow = ServerProxy.getWorkflowManager()
						.getWorkflowById(session.getId(), Long.parseLong(wfId));

				GlobalSightLocale source = workflow.getJob().getSourceLocale();
				GlobalSightLocale target = workflow.getTargetLocale();

				sessionMgr.setAttribute(WF_INSTANCE, workflow);
				sessionMgr.setAttribute(SOURCE_LOCALE, source);
				sessionMgr.setAttribute(TARGET_LOCALE, target);
				sessionMgr.setAttribute(TASK_HASH,
						getTaskInstanceHashTable(workflow));

				// also set the locale pair in the request for displaying
				// the workflow name
				Locale uiLocale = (Locale) session.getAttribute(UILOCALE);
				sessionMgr.setAttribute(
						WorkflowTemplateConstants.WF_INSTANCE_NAME, source
								.getDisplayName(uiLocale)
								+ " -> " + target.getDisplayName(uiLocale));

				if (action != null && action.equals("view"))
				{
					List taskInfos = (List) sessionMgr
							.getAttribute(WorkflowConstants.TASK_INFOS);
					String taskIdParam = p_request.getParameter("taskId");
					long taskId = Long.parseLong(taskIdParam);

					TaskInfo taskInfo = ServerProxy.getWorkflowManager()
							.getTaskInfoByTaskId(workflow, taskInfos, taskId,
									true);
					p_request.setAttribute(WorkflowConstants.TASK_INFO,
							taskInfo);
				}
				else
				{
					ArrayList taskInfos = (ArrayList) ServerProxy
							.getWorkflowServer().timeDurationsInDefaultPath(
									null, Long.parseLong(wfId), -1);
					sessionMgr.setAttribute(WorkflowConstants.TASK_INFOS,
							taskInfos);
				}
				p_request.setAttribute(JobManagementHandler.WF_ID, wfId);
				p_request.setAttribute(JobManagementHandler.JOB_ID, jobId);
			}
		}
		catch (Exception e)
		{
			throw new EnvoyServletException(e);
		}
	}

	private Hashtable getTaskInstanceHashTable(Workflow workflow)
	{
		WorkflowInstance wfi = workflow.getIflowInstance();

		if (wfi == null)
		{
			/* This judgement is used to avoid the jsp page display issue */
			/*
             * The way of solve this problem is unregular and risky, we should
             * remove this code after we find the root cause of this problem
             */
			// TODO
			return new Hashtable(1);
		}

		Vector tasks = wfi.getWorkflowInstanceTasks();
		Hashtable ht = new Hashtable(tasks.size());
		for (int i = 0; i < tasks.size(); i++)
		{
			WorkflowTaskInstance wfti = (WorkflowTaskInstance) tasks
					.elementAt(i);
			ht.put(new Long(wfti.getTaskId()), wfti);
		}
		return ht;
	}

	// Get all the info required to be displayed on the graphical workflow UI.
	// The info required for the dialog boxes for each node should also be
	// included.
	private Vector getDisplayData(HttpServletRequest p_request,
			HttpSession p_session) throws WorkflowManagerException,
			RemoteException, GeneralException
	{
		SessionManager sm = WorkflowTemplateHandlerHelper
				.getSessionManager(p_request);
		ResourceBundle bundle = getBundle(p_session);
		Vector objs = new Vector();

		boolean isReady = false;
		WorkflowInstance wfi = null;
		GlobalSightLocale srcLocale = null;
		GlobalSightLocale targetLocale = null;
		boolean hasCosting = WorkflowTemplateHandlerHelper.isCostingEnabled();
		boolean hasRevenue = WorkflowTemplateHandlerHelper.isRevenueEnabled();
		Workflow wf = (Workflow) sm.getAttribute(WF_INSTANCE);
		/*
         * Reget the workflow object because the object in the sessin is getted
         * by lazy initlize by hibernate
         */
		wf = ServerProxy.getWorkflowManager().getWorkflowById(null,
				Long.valueOf(wf.getId()));

		if (wf != null)
		{
			srcLocale = wf.getJob().getSourceLocale();
			targetLocale = wf.getTargetLocale();
			wfi = wf.getIflowInstance();
			isReady = Workflow.READY_TO_BE_DISPATCHED.equals(wf.getState());
		}

		// all images and the flag that determines whether AND/OR nodes should
		// be visible
		Hashtable imageHash = new Hashtable();
		imageHash.put("gpact", "/images/graphicalworkflow/gpact.gif");
		imageHash.put("gpexit", "/images/graphicalworkflow/gpexit.gif");
		imageHash.put("gpcond", "/images/graphicalworkflow/gpcond.gif");
		imageHash.put("gpand", "/images/graphicalworkflow/gpand.gif");
		imageHash.put("gparrow", "/images/graphicalworkflow/gparrow.gif");
		imageHash.put("pointer", "/images/graphicalworkflow/pointer.gif");
		imageHash.put("gpor", "/images/graphicalworkflow/gpor.gif");
		imageHash.put("gpsub", "/images/graphicalworkflow/gpsub.gif");
		imageHash.put("gpcancel", "/images/graphicalworkflow/gpcancel.gif");
		imageHash.put("gpsave", "/images/graphicalworkflow/gpsave.gif");
		imageHash.put("gpprint", "/images/graphicalworkflow/print.gif");
		imageHash.put("visible", WorkflowTemplateHandlerHelper
				.areAndOrNodesEnabled());
		// default data item ref for contional node.
		String dataItemRefName = bundle
				.getString(WorkflowConstants.CONDITION_UDA);

		objs.addElement(imageHash); // 0
		objs.addElement(dataItemRefName); // 1
		// activity dialog info
		objs.addElement(getDataForDialog(p_request, p_session, srcLocale,
				targetLocale)); // 2
		objs.addElement(wfi); // 3
		objs.addElement(p_session.getAttribute(UILOCALE));// 4
		objs.addElement(new Boolean(isReady)); // 5
		objs.addElement(getTaskInfoMap(wf, hasCosting, hasRevenue)); // 6
		return objs;
	}

	// save the newly created workflow template
	private Vector saveData(HttpServletRequest p_request, HttpSession p_session)
			throws EnvoyServletException, IOException
	{
		Vector outData = null;
		SessionManager sessionMgr = (SessionManager) p_session
				.getAttribute(SESSION_MANAGER);
		try
		{
			ObjectInputStream inputFromApplet = new ObjectInputStream(p_request
					.getInputStream());
			Vector inData = (Vector) inputFromApplet.readObject();
			if (inData != null) // if this is null the command is cancel.
			{
				String command = (String) inData.elementAt(0);
				boolean isUserRole = command.equals(USER_ROLE);
				// return data in order to populate user or role.
				if (isUserRole || command.equals(ROLE))
				{
					outData = new Vector();
					outData.addElement(getDataForRole((Long) inData
							.elementAt(2), p_session, (String) inData
							.elementAt(1), isUserRole, sessionMgr,
							(GlobalSightLocale) sessionMgr
									.getAttribute(SOURCE_LOCALE),
							(GlobalSightLocale) sessionMgr
									.getAttribute(TARGET_LOCALE)));
				}
				else if (command.equals(SAVE_ACTION))
				{
					// save the modified workflows.
					saveWorkflow(p_session, sessionMgr, inData.elementAt(1),
							(Hashtable) inData.elementAt(2));
				}
			}
		}
		catch (ClassNotFoundException ex)
		{
			throw new EnvoyServletException(EnvoyServletException.EX_GENERAL,
					ex);

		}
		catch (Exception e)
		{
			throw new EnvoyServletException(EnvoyServletException.EX_GENERAL, e);
		}
		return outData;
	}

	// get grid data for role table of the dialog
	private GridData getDataForRole(Long taskId, HttpSession session,
			String p_activityName, boolean p_isUser,
			SessionManager p_sessionMgr, GlobalSightLocale p_srcLocale,
			GlobalSightLocale p_targetLocale) throws EnvoyServletException
	{
		GridData gridData = null;
		Workflow workflow = (Workflow) p_sessionMgr.getAttribute(WF_INSTANCE);

		if (p_isUser)
		{
			// obtain the roles to be turned into griddata.
			Collection usersCollection = WorkflowTemplateHandlerHelper
					.getUserRoles(p_activityName, p_srcLocale.toString(),
							p_targetLocale.toString());
			int cols = 3;
			if (Modules.isCalendaringInstalled())
			{
				cols = 4;
			}
			if (usersCollection != null)
			{
				TaskInfo taskInfo = null;
				TimeZone timeZone = (TimeZone) session
						.getAttribute(WebAppConstants.USER_TIME_ZONE);
				Timestamp ts = new Timestamp(timeZone);
				Locale uiLocale = (Locale) session.getAttribute(UILOCALE);
				ts.setLocale(uiLocale);
				ArrayList assignees = new ArrayList();
				ResourceBundle bundle = getBundle(session);

				if (Modules.isCalendaringInstalled())
				{
					List taskInfos = (List) p_sessionMgr
							.getAttribute(WorkflowConstants.TASK_INFOS);
					try
					{
						taskInfo = ServerProxy.getWorkflowManager()
								.getTaskInfoByTaskId(workflow, taskInfos,
										taskId.longValue(), false);
					}
					catch (Exception e)
					{
						throw new EnvoyServletException(e);
					}
					if (taskInfo != null)
					{
						assignees = (ArrayList) taskInfo.getTaskAssignees();
					}
				}

				Set projectUserIds = workflow.getJob().getL10nProfile()
						.getProject().getUserIds();
				Vector usersInProject = new Vector();

				// filter out the users that aren't in the project
				for (Iterator i = usersCollection.iterator(); i.hasNext();)
				{
					UserRoleImpl userRole = (UserRoleImpl) i.next();
					if (projectUserIds.contains(userRole.getUser()))
					{
						usersInProject.add(userRole);
					}
				}
				gridData = new GridData(usersInProject == null ? 0
						: usersInProject.size(), cols);

				for (int i = 0; i < usersInProject.size(); i++)
				{

					Vector row = new Vector();
					UserRoleImpl userRole = (UserRoleImpl) usersInProject
							.get(i);
					User user = WorkflowTemplateHandlerHelper.getUser(userRole
							.getUser());
					row.addElement(user.getFirstName()); // 0
					row.addElement(user.getLastName()); // 1
					row.addElement(user.getUserId()); // 2

					if (Modules.isCalendaringInstalled())
					{
						if (assignees.size() == 0)
						{
							row.addElement(bundle
									.getString("msg_not_in_default_path")); // 3
						}
						else
						{
							// Find the user in the list
							boolean found = false;
							for (int j = 0; j < assignees.size(); j++)
							{
								TaskAssignee ta = (TaskAssignee) assignees
										.get(j);
								if (user.getUserId().equals(ta.getUserId()))
								{
									ts.setDate(ta.getEstimatedCompletionDate());
									row
											.addElement(new AppletDate(ts
													.getDate())); // 3
									found = true;
									break;
								}
							}
							if (!found) row.addElement(null); // 3
						}
					}
					else
					{
						row.addElement(null); // 3
					}

					row.addElement(userRole.getName()); // 4
					row.addElement(userRole.getRate()); // 5

					gridData.setRowHeaderData(i + 1, userRole.getName());
					gridData.setRowData(i + 1, row);
				}
			}
			else
			{
				// no user roles are found so create an empty grid data
				// but the columns can't be "0" - will cause arithmetic
				// exception later down the line
				gridData = new GridData(0, cols);
			}
		}
		else
		{
			ContainerRole containerRole = WorkflowTemplateHandlerHelper
					.getContainerRole(p_activityName, p_srcLocale.toString(),
							p_targetLocale.toString(), workflow.getJob()
									.getL10nProfile().getProject().getId());

			gridData = new GridData(containerRole == null ? 0 : 1, 3);
			if (containerRole != null)
			{
				Vector row = new Vector();
				row.addElement(containerRole.getName());
				row.addElement("");
				row.addElement("");
				gridData.setRowHeaderData(1, containerRole.getName());
				gridData.setRowData(1, row);

			}
		}
		return gridData;
	}

	private WorkflowTaskInstance getTaskInstance(SessionManager sessionMgr,
			Long taskId)
	{
		Hashtable tasks = (Hashtable) sessionMgr.getAttribute(TASK_HASH);
		return (WorkflowTaskInstance) tasks.get(taskId);
	}

	/*
     * private String[] dialogLabels(ResourceBundle p_bundle) }
     */
	// get the data needed for the dialog
	private Hashtable getDataForDialog(HttpServletRequest p_request,
			HttpSession p_session, GlobalSightLocale p_srcLocale,
			GlobalSightLocale p_targetLocale) throws EnvoyServletException
	{
		ResourceBundle bundle = getBundle(p_session);
		Locale uiLocale = (Locale) p_session.getAttribute(UILOCALE);

		return WorkflowTemplateHandlerHelper.getDataForDialog(bundle, uiLocale,
				p_srcLocale, p_targetLocale);
	}

	// save the workflow...
	private void saveWorkflow(HttpSession p_session,
			SessionManager p_sessionMgr, Object p_workflowObject,
			Hashtable p_modifiedTaskInfoMap) throws EnvoyServletException
	{
		try
		{
			Workflow wf = (Workflow) p_sessionMgr.getAttribute(WF_INSTANCE);
			long jobId = wf.getJob().getId();
			boolean hasCosting = false;
			boolean hasRevenue = false;

			String userId = (String) p_session.getAttribute(USER_NAME);
			ServerProxy.getWorkflowManager().modifyWorkflow(p_session.getId(),
					(WorkflowInstance) p_workflowObject, userId,
					p_modifiedTaskInfoMap);

			// now calculate costing
			if (wf != null)
			{
				hasCosting = WorkflowTemplateHandlerHelper.isCostingEnabled();
				hasRevenue = WorkflowTemplateHandlerHelper.isRevenueEnabled();
			}
			if (hasCosting)
			{
				String curr = (String) p_sessionMgr
						.getAttribute(JobManagementHandler.CURRENCY);
				Job j = ServerProxy.getJobHandler().getJobById(jobId);
				if (curr == null)
				{
					// Get the pivot currency;
					Currency c = ServerProxy.getCostingEngine()
							.getPivotCurrency();
					curr = c.getIsoCode();
				}
				Currency oCurrency = ServerProxy.getCostingEngine()
						.getCurrency(curr);
				// Calculate Expenses
				CostCalculator calculator = new CostCalculator(j.getId(),
						oCurrency, true, Cost.EXPENSE);
				calculator.sendToCalculateCost();
				if (hasRevenue)
				{
					// Calculate Revenue
					calculator = new CostCalculator(j.getId(), oCurrency, true,
							Cost.REVENUE);
					calculator.sendToCalculateCost();
				}
			}
		}
		catch (Exception e)
		{
			throw new EnvoyServletException(e);
		}

		// after successful save, cleanup session manager...
		// all except for the job id
		Long jobid = (Long) p_sessionMgr
				.getAttribute(JobManagementHandler.JOB_ID);
		clearSessionExceptTableInfo(p_session, KEY);
		p_sessionMgr.setAttribute(JobManagementHandler.JOB_ID, jobid);
	}

	/**
     * Return a Hashtable of all tasks associated with the WorkflowInstance. The
     * key is the task id and the value is the TaskInfoBean. This is only needed
     * for costing purposes.
     */
	private Hashtable getTaskInfoMap(Workflow p_wf, boolean p_hasCosting,
			boolean p_hasRevenue) throws EnvoyServletException
	{
		Hashtable ht = new Hashtable();

		if (p_hasCosting && p_wf != null)
		{
			try
			{
				Collection tasks = p_wf.getTasks().values();
				int size = tasks == null ? 0 : tasks.size();
				// iterate through the tasks and get the appropriate info
				// to populate the TaskInfoBean
				for (Iterator taskI = tasks.iterator(); taskI.hasNext();)
				{
					Rate expenseRate = null;
					Rate revenueRate = null;
					Task t = (Task) taskI.next();
					int rateSelectionCriteria = t.getRateSelectionCriteria();
					String estimatedHourAmount = "0.0";
					String actualHourAmount = null;
					expenseRate = t.getExpenseRate();
					if (expenseRate != null
							&& expenseRate.getRateType().equals(
									Rate.UnitOfWork.HOURLY))
					{
						AmountOfWork aow = t
								.getAmountOfWork(Rate.UnitOfWork.HOURLY);
						if (aow != null)
						{
							estimatedHourAmount = Double.toString(aow
									.getEstimatedAmount());
							// if completed - also grab the actual amount
							if (t.getState() == Task.STATE_COMPLETED
									&& t.getCompletedDate() != null)
							{
								actualHourAmount = Double.toString(aow
										.getActualAmount());
							}
						}
						else
						{
							// the task is complete so set the actual
							// hours to 0.0 rather than NULL.
							if (t.getState() == Task.STATE_COMPLETED
									&& t.getCompletedDate() != null)
							{
								actualHourAmount = "0.0";
							}
						}

					}
					if (p_hasRevenue)
					{
						revenueRate = t.getRevenueRate();
						if (!(expenseRate != null && expenseRate.getRateType()
								.equals(Rate.UnitOfWork.HOURLY))
								&& (revenueRate != null && revenueRate
										.getRateType().equals(
												Rate.UnitOfWork.HOURLY)))
						{
							AmountOfWork aow = t
									.getAmountOfWork(Rate.UnitOfWork.HOURLY);
							if (aow != null)
							{
								estimatedHourAmount = Double.toString(aow
										.getEstimatedAmount());
								// if completed - also grab the actual amount
								if (t.getState() == Task.STATE_COMPLETED
										&& t.getCompletedDate() != null)
								{
									actualHourAmount = Double.toString(aow
											.getActualAmount());
								}
							}
							else
							{
								// the task is complete so set the actual
								// hours to 0.0 rather than NULL.
								if (t.getState() == Task.STATE_COMPLETED
										&& t.getCompletedDate() != null)
								{
									actualHourAmount = "0.0";
								}
							}

						}
					}
					TaskInfoBean tib = new TaskInfoBean(t.getId(),
							estimatedHourAmount, actualHourAmount, expenseRate,
							revenueRate, rateSelectionCriteria);
					ht.put(new Long(t.getId()), tib);
				}
			}
			catch (Exception e)
			{
				throw new EnvoyServletException(e);
			} // end of TBR
		}

		return ht;
	}

	// ////////////////////////////////////////////////////////////////////
	// End: Local Methods
	// ////////////////////////////////////////////////////////////////////
}
