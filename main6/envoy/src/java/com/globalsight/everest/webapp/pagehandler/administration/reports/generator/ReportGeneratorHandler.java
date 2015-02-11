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
package com.globalsight.everest.webapp.pagehandler.administration.reports.generator;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.globalsight.everest.foundation.SearchCriteriaParameters;
import com.globalsight.everest.jobhandler.Job;
import com.globalsight.everest.jobhandler.JobException;
import com.globalsight.everest.jobhandler.JobSearchParameters;
import com.globalsight.everest.servlet.EnvoyServletException;
import com.globalsight.everest.servlet.util.ServerProxy;
import com.globalsight.everest.webapp.WebAppConstants;
import com.globalsight.everest.webapp.javabean.ErrorBean;
import com.globalsight.everest.webapp.pagehandler.ActionHandler;
import com.globalsight.everest.webapp.pagehandler.PageHandler;
import com.globalsight.everest.webapp.pagehandler.administration.reports.ReportConstants;
import com.globalsight.everest.webapp.pagehandler.administration.reports.ReportDBUtil;
import com.globalsight.everest.webapp.pagehandler.administration.reports.ReportHelper;
import com.globalsight.everest.webapp.pagehandler.administration.reports.bo.ReportSearchOptions;
import com.globalsight.everest.webapp.pagehandler.administration.reports.bo.ReportsData;
import com.globalsight.everest.webapp.pagehandler.projects.workflows.JobSearchConstants;
import com.globalsight.everest.webapp.pagehandler.projects.workflows.JobSummaryHelper;
import com.globalsight.everest.webapp.webnavigation.WebPageDescriptor;
import com.globalsight.everest.workflowmanager.Workflow;
import com.globalsight.util.GeneralException;
import com.globalsight.util.GlobalSightLocale;

/**
 * Generate Report handler. The handle can be used for generating reports,
 * getting the percent of reports, cancelling the reports.
 */
public class ReportGeneratorHandler extends PageHandler implements
        ReportConstants
{
    static private final Logger logger = Logger
            .getLogger(ReportGeneratorHandler.class);

    private static Map<String, ReportGenerator> m_generatorMap = 
            new ConcurrentHashMap<String, ReportGenerator>();
    private static Map<String, ReportInfo> m_reportResultMap = 
            new ConcurrentHashMap<String, ReportInfo>();

    @ActionHandler(action = ACTION_CANCEL_REPORT, formClass = "")
    public void cancelGenerateReport(HttpServletRequest p_request,
            HttpServletResponse p_response) throws Exception
    {
    	HttpSession userSession = p_request.getSession();
        String userId = (String) userSession
                .getAttribute(WebAppConstants.USER_NAME);
        List<Long> reportJobIDS = ReportHelper.getListOfLong(p_request
                .getParameter(ReportConstants.JOB_IDS));
        String reportType = p_request.getParameter(ReportConstants.REPORT_TYPE);
        
        String key = ReportHelper.getKey(userId, reportJobIDS, reportType);
        ReportGenerator generator = m_generatorMap.get(key);
        
        if (generator != null)
        {
        	Cancelable cancelable = (Cancelable) generator;
        	cancelable.cancel();
        }
    }
    
    /**
     * Gets Reports
     */
    @ActionHandler(action = GET_REPORT, formClass = "")
    public void getReport(HttpServletRequest p_request,
            HttpServletResponse p_response) throws Exception
    {
        HttpSession userSession = p_request.getSession();
        String userId = (String) userSession
                .getAttribute(WebAppConstants.USER_NAME);
        List<Long> reportJobIDS = ReportHelper.getListOfLong(p_request
                .getParameter(ReportConstants.JOB_IDS));
        String reportType = p_request.getParameter(ReportConstants.REPORT_TYPE);
        
        String key = ReportHelper.getKey(userId, reportJobIDS, reportType);
        ReportInfo info = m_reportResultMap.get(key);
    	if (info != null)
    	{
    		File[] reports = info.getFiles();
    		ReportHelper.sendFiles(reports, null, p_response);
    	}
    }
    
    /**
     * Operate Generate Report Request from Activity Download Report Page.
     * The reports are TranslationsEditReport and ReviewersCommentsReport.
     */
    @ActionHandler(action = GENERATE_REPORT, formClass = "")
    public void generateOneReport(HttpServletRequest p_request,
            HttpServletResponse p_response) throws Exception
    {
        HttpSession userSession = p_request.getSession();
        String userId = (String) userSession
                .getAttribute(WebAppConstants.USER_NAME);
        List<Long> reportJobIDS = ReportHelper.getListOfLong(p_request
                .getParameter(ReportConstants.JOB_IDS));
        List<GlobalSightLocale> reportTargetLocales = ReportHelper
                .getTargetLocaleList(p_request
                        .getParameterValues(ReportConstants.TARGETLOCALE_LIST),
                        null);
        String reportType = p_request.getParameter(ReportConstants.REPORT_TYPE);
        
        String key = ReportHelper.getKey(userId, reportJobIDS, reportType);
        ReportInfo info = new ReportInfo();
    	m_reportResultMap.put(key, info);
    	
        try 
        {
            // Initial Reports percent and status for m_reportsDataMap.
            ReportHelper.setReportsData(userId, reportJobIDS, reportType, 0, ReportsData.STATUS_INPROGRESS);
            
            ReportGenerator generator = ReportGeneratorFactory
                    .getReportGenerator(reportType, p_request, p_response);
	        
	        if (generator instanceof Cancelable) 
	        {
				m_generatorMap.put(key, generator);
			}
	        
	        File[] files = generator.generateReports(reportJobIDS,
	                reportTargetLocales);
	        info.setFiles(files);
	        
	        // Set Reports percent and status for m_reportsDataMap.
	        ReportHelper.setReportsData(userId, reportJobIDS, reportType, 100, ReportsData.STATUS_FINISHED);
        }
        finally
        {
        	info.setFinished(true);
		}
    }
    
    /**
     * Generate Reports
     */
    @ActionHandler(action = GENERATE_REPORTS, formClass = "")
    public void generateReport(HttpServletRequest p_request,
            HttpServletResponse p_response) throws Exception
    {
        HttpSession userSession = p_request.getSession();
        String userId = (String) userSession
                .getAttribute(WebAppConstants.USER_NAME);
        List<Long> reportJobIDS = filterByDateRang(p_request);
       
        List<GlobalSightLocale> reportTargetLocales = ReportHelper
                .getTargetLocaleList(p_request
                        .getParameterValues(ReportConstants.TARGETLOCALE_LIST),
                        null);
        List<String> reportTypeList = ReportHelper.getListOfStr(
                p_request.getParameter(ReportConstants.REPORT_TYPE), ",");
        ReportGenerator generator;

        // Cancel Duplicate Request
        if (ReportHelper.checkReportsDataInProgressStatus(userId, reportJobIDS, reportTypeList))
        {
            String message = "Ignore the request, due the report is generating, userID/reportJobIDS/reportTypeList:"
                    + userId + ", " + reportJobIDS + ", " + reportTypeList;
            logger.info(message);
            p_response.sendError(p_response.SC_NO_CONTENT);
            return;
        }
        // Initial Reports percent and status for m_reportsDataMap.
        ReportHelper.setReportsData(userId, reportJobIDS, reportTypeList, 
                0, ReportsData.STATUS_INPROGRESS);

        List<File> reports = new ArrayList<File>();
        String zipFileName = null;
        if (reportTypeList.size() == 1)
        {
            String reportType = reportTypeList.get(0);
            generator = ReportGeneratorFactory.getReportGenerator(reportType,
                    p_request, p_response);
            
            if (generator instanceof Cancelable)  
            {
				String key = ReportHelper.getKey(userId, reportJobIDS, reportType);
				m_generatorMap.put(key, generator);
			}
            
            File[] files = generator.generateReports(reportJobIDS,
                    reportTargetLocales);
            ReportHelper.addFiles(reports, files);
        }
        else if (reportTypeList.size() > 1)
        {
            for (String reportType : reportTypeList)
            {
                if (isCancelled(userId, reportJobIDS, reportType))
                {
                    logger.debug("cancelGenerateReports:" + userId
                            + reportJobIDS);
                    generator = null;
                    return;
                }
                generator = ReportGeneratorFactory.getReportGenerator(
                        reportType, p_request, p_response);
                File[] files = generator.generateReports(reportJobIDS,
                        reportTargetLocales);
                ReportHelper.addFiles(reports, files);
            }
            zipFileName = getReportName(reportJobIDS);
        }

        // Set Reports percent and status for m_reportsDataMap.
        ReportHelper.setReportsData(userId, reportJobIDS, reportTypeList, 
                100, ReportsData.STATUS_FINISHED);
        generator = null;        
        if (reports == null || reports.size() == 0)
        {
            StringBuffer msg = new StringBuffer();
            msg.append("Can't create the report. Please check the options.");
            ErrorBean errorBean = new ErrorBean(0, msg.toString());
            p_request.setAttribute(WebAppConstants.ERROR_BEAN_NAME, errorBean);
            p_request.getRequestDispatcher(ReportConstants.ERROR_PAGE).forward(p_request, p_response);
        }
        
        ReportHelper.sendFiles(reports, zipFileName, p_response);
    }

    public List<Long> filterByDateRang(HttpServletRequest p_request)
            throws JobException, RemoteException,
            GeneralException, NamingException
    {
        // TODO VILADATE filterByDateRang
        List<Long> reportJobIDS = ReportHelper.getListOfLong(p_request
                .getParameter(ReportConstants.JOB_IDS));
        String dateRange = p_request.getParameter("dateRange");
        if (!"Y".equals(dateRange))
        {
            return reportJobIDS;
        }
        JobSearchParameters searchParams = new JobSearchParameters();
        String paramCreateDateStartCount = p_request
                .getParameter(JobSearchConstants.CREATION_START);
        String paramCreateDateStartOpts = p_request
                .getParameter(JobSearchConstants.CREATION_START_OPTIONS);
        if ("-1".equals(paramCreateDateStartOpts) == false)
        {
            searchParams
                    .setCreationStart(new Integer(paramCreateDateStartCount));
            searchParams.setCreationStartCondition(paramCreateDateStartOpts);
        }

        String paramCreateDateEndCount = p_request
                .getParameter(JobSearchConstants.CREATION_END);
        String paramCreateDateEndOpts = p_request
                .getParameter(JobSearchConstants.CREATION_END_OPTIONS);
        if (SearchCriteriaParameters.NOW.equals(paramCreateDateEndOpts))
        {
            searchParams.setCreationEnd(new java.util.Date());
        }
        else if ("-1".equals(paramCreateDateEndOpts) == false)
        {
            searchParams.setCreationEnd(new Integer(paramCreateDateEndCount));
            searchParams.setCreationEndCondition(paramCreateDateEndOpts);
        }
        ArrayList<Job> queriedJobs = new ArrayList<Job>(ServerProxy
                .getJobHandler().getJobs(searchParams));
        List<Long> queriedIds = new ArrayList<Long>();
        for (Job job : queriedJobs)
        {
            queriedIds.add(job.getId());
        }
        reportJobIDS.retainAll(queriedIds);
        return reportJobIDS;
    }

    
    /**
     * Generate Summary Report
     */
    @ActionHandler(action = ACTION_GENERATE_SUMMARY_PERCENT, formClass = "")
    public void generateSummaryReport(HttpServletRequest p_request,
            HttpServletResponse p_response) throws Exception
    {
        SummaryReportGenerator generator = new SummaryReportGenerator();
        ReportSearchOptions searchOptions = generator.getSearchOptions(p_request);
        File[] file = generator.generateReports(searchOptions);
        
        generator = null;
        ReportHelper.sendFiles(file, null, p_response);
    }
    
    /**
     * Check if selected jobs are using same source locale.
     */
    @ActionHandler(action = ACTION_CHECK_SOURCE_LOCALE, formClass = "")
    public void checkSourceLocale(HttpServletRequest p_request,
            HttpServletResponse p_response) throws Exception
    {   
    	List<Long> reportJobIDS = filterByDateRang(p_request);   	
    	JSONObject jsonObj = new JSONObject();
    	List<GlobalSightLocale> reportTargetLocales = ReportHelper
        .getTargetLocaleList(p_request
                .getParameterValues(ReportConstants.TARGETLOCALE_LIST),
                null);
    	if(checkSourceLocale(reportJobIDS, reportTargetLocales))
    	{   		
    		jsonObj.put("differentSource", true);
    		jsonObj.put("info", "Selected jobs are not using same source locale, please reset.");
    	}
    	else
    	{
    		jsonObj.put("differentSource", false);
		}
    	p_response.getWriter().write(jsonObj.toString());
    }

    /**
     * Gets the ReportsData.
     */
    @ActionHandler(action = ACTION_GET_REPORTSDATA, formClass = "")
    public void getReportsData(HttpServletRequest p_request,
            HttpServletResponse p_response) throws Exception
    {
        String json = "";
        HttpSession userSession = p_request.getSession();
        String userId = (String) userSession
                .getAttribute(WebAppConstants.USER_NAME);
        List<Long> reportJobIDS = filterByDateRang(p_request);
        // just for LisaQACommentsAnalysisReportWebForm.jsp
        String dateRange = p_request.getParameter("dateRange");
        if ("Y".equals(dateRange) && reportJobIDS.size() == 0)
        {
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("status", "inProgress");
            jsonObj.put("info", "No specified job in this date range, please reset.");
            p_response.getWriter().write(jsonObj.toString());
            return;
        }

        List<String> reportTypeList = ReportHelper.getListOfStr(
                p_request.getParameter(ReportConstants.REPORT_TYPE), ",");
        
        ReportsData data = ReportDBUtil.getReportsData(userId, reportJobIDS, reportTypeList);
        if (data != null)
        {
            json = data.toJSON();
            logInfo("GETPERCENT METHOD:" + json);
        }
        
        p_response.getWriter().write(json);
    }
    
    /**
     * Gets the percent of Reports.
     */
    @ActionHandler(action = ACTION_GET_PERCENT, formClass = "")
    public void getPercent(HttpServletRequest p_request,
            HttpServletResponse p_response) throws Exception
    {
        HttpSession userSession = p_request.getSession();
        String userId = (String) userSession
                .getAttribute(WebAppConstants.USER_NAME);
        List<Long> reportJobIDS = ReportHelper.getListOfLong(p_request
                .getParameter(ReportConstants.JOB_IDS));
        List<String> reportTypeList = ReportHelper.getListOfStr(
                p_request.getParameter(ReportConstants.REPORT_TYPE), ",");

        ReportsData data = ReportDBUtil.getReportsData(userId, reportJobIDS, reportTypeList);
        if (data != null)
        {
            String json = data.toJSON();
            logger.debug("GETPERCENT METHOD:" + json);
            if (data.getPercent() >= 100)
            {
                ReportDBUtil.delReportsData(userId, reportJobIDS, reportTypeList);
            }
            p_response.getWriter().write(json);
        }
    }

    /**
     * Set the cancel status for the Reports.
     */
    @ActionHandler(action = ACTION_CANCEL_REPORTS, formClass = "")
    public void cancelReports(HttpServletRequest p_request,
            HttpServletResponse p_response) throws Exception
    {
        HttpSession userSession = p_request.getSession();
        String userId = (String) userSession
                .getAttribute(WebAppConstants.USER_NAME);
        List<Long> reportJobIDS = ReportHelper.getListOfLong(p_request
                .getParameter(ReportConstants.JOB_IDS));
        List<String> reportTypeList = ReportHelper.getListOfStr(
                p_request.getParameter(ReportConstants.REPORT_TYPE), ",");

        ReportsData data = ReportDBUtil.getReportsData(userId, reportJobIDS, reportTypeList);
        if (data != null)
        {
            data.setStatus(ReportsData.STATUS_CANCEL);
        }
    }


    /**
     * Set the Report percent by Report Generator.
     */
    public static void setReportsMapByGenerator(String p_userId,
            List<Long> p_reportJobIDS, double p_percent, String p_reportType)
    {
        List<String> reportTypeList = new ArrayList<String>();
        reportTypeList.add(p_reportType);
        ReportDBUtil.saveOrUpdateReportsData(p_userId, p_reportJobIDS, reportTypeList, null, p_percent);
    }

    /**
     * Gets ReportsData from m_reportsDataMap, for detect whether canceled the
     * reports.
     */
    public static ReportsData getReportsMap(String p_userId,
            List<Long> p_reportJobIDS, String p_reportType)
    {
        List<String> reportTypeList = new ArrayList<String>();
        reportTypeList.add(p_reportType);
        ReportsData data = ReportDBUtil.getReportsData(p_userId, p_reportJobIDS, reportTypeList);
        return data;
    }

    public static boolean isCancelled(String p_userId, List<Long> p_reportJobIDS, String p_reportType)
    {
        ReportsData data = getReportsMap(p_userId, p_reportJobIDS, p_reportType);        
        if (data != null)
        {
            return data.isCancle();
        }

        return false;
    }
    
    /**
     * Invokes this PageHandler
     * 
     * @param p_pageDescriptor
     *            the page descriptor
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
        beforeAction(p_request, p_response);

        callAction(p_request, p_response);

        afterAction(p_request, p_response);

        if (!GENERATE_REPORTS.equalsIgnoreCase(p_request
                .getParameter("linkName")))
        {
            super.invokePageHandler(p_pageDescriptor, p_request, p_response,
                    p_context);
        }
    }

    private void callAction(HttpServletRequest p_request,
            HttpServletResponse p_response)
    {
        String action = p_request.getParameter("action");
        if (action == null)
        {
            //Job Details Page rewrite.Job Summary need.
            JobSummaryHelper jobSummaryHelper = new JobSummaryHelper();
            Job job = jobSummaryHelper.getJobByRequest(p_request);
            jobSummaryHelper.packJobSummaryInfoView(p_request, job);
            return;
        }

        Method[] ms = this.getClass().getMethods();
        for (Method m : ms)
        {
            if (m.isAnnotationPresent(ActionHandler.class))
            {
                ActionHandler handler = m.getAnnotation(ActionHandler.class);
                if (action.matches(handler.action()))
                {
                    try
                    {
                        m.invoke(this, p_request, p_response);
                    }
                    catch (Exception e)
                    {
                        logger.error(e.getMessage(), e);
                    }

                    break;
                }
            }
        }
    }

    public void beforeAction(HttpServletRequest p_request,
            HttpServletResponse response)
    {
    }

    public void afterAction(HttpServletRequest request,
            HttpServletResponse response)
    {
    }

    public static String getReportName(List<Long> p_reportJobIDS)
            throws JobException, RemoteException, GeneralException,
            NamingException
    {
        if (p_reportJobIDS != null && p_reportJobIDS.size() == 1)
        {
            long jobId = p_reportJobIDS.get(0);
            Job job = ServerProxy.getJobHandler().getJobById(jobId);
            return ReportConstants.REPORTS_NAME + "-[" + job.getJobName()
                    + "][" + jobId + "]";
        }
        else
        {
            return ReportConstants.REPORTS_NAME;
        }
    }
    
    private boolean checkSourceLocale(List<Long> p_jobIDS, List<GlobalSightLocale> reportTargetLocales) throws Exception
    {
    	boolean differentSource = false;
    	if(reportTargetLocales == null)
    	{
    		reportTargetLocales = new ArrayList<GlobalSightLocale>(ServerProxy
                    .getLocaleManager().getAllTargetLocales());
    	}
    	for(GlobalSightLocale targetLocale :reportTargetLocales)
    	{
    		if(differentSource)
    		{
    			break;
    		}
        	GlobalSightLocale sourceLocale = null;
        	for (long jobID : p_jobIDS)
        	{
        		Job job = ServerProxy.getJobHandler().getJobById(jobID);
        		if (job == null)
        		{
                    continue;
        		}
        		Set<Workflow> wfSet = (Set<Workflow>) job.getWorkflows();
        		boolean containTargetLocale = false;
        		for(Workflow wf:wfSet)
        		{
        			if(wf.getTargetLocale().equals(targetLocale))
        			{
        				containTargetLocale = true;
        			}
        		}
        		if(!containTargetLocale)
        		{
        			continue;
        		}
        		if(sourceLocale == null)
        		{
        			sourceLocale = job.getSourceLocale();
        			continue;
        		}
        		if(!sourceLocale.equals(job.getSourceLocale())){
        			differentSource = true;
        			break;
        		}
        	}
    	}
    	return differentSource;
    }
    
    private static void logInfo(String p_msg)
    {
        //logger.info(p_msg);
    }
}
