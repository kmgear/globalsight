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
package com.globalsight.everest.webapp.pagehandler.administration.reports.customize;

// Envoy packages
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Vector;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.globalsight.everest.foundation.L10nProfile;
import com.globalsight.everest.foundation.SearchCriteriaParameters;
import com.globalsight.everest.jobhandler.Job;
import com.globalsight.everest.jobhandler.JobException;
import com.globalsight.everest.jobhandler.JobSearchParameters;
import com.globalsight.everest.projecthandler.Project;
import com.globalsight.everest.servlet.EnvoyServletException;
import com.globalsight.everest.servlet.util.ServerProxy;
import com.globalsight.everest.util.comparator.GlobalSightLocaleComparator;
import com.globalsight.everest.util.comparator.JobComparator;
import com.globalsight.everest.webapp.WebAppConstants;
import com.globalsight.everest.webapp.pagehandler.PageHandler;
import com.globalsight.everest.webapp.pagehandler.administration.reports.util.CurrencyThreadLocal;
import com.globalsight.everest.webapp.pagehandler.administration.vendors.ProjectComparator;
import com.globalsight.everest.webapp.pagehandler.projects.workflows.JobSearchConstants;
import com.globalsight.everest.webapp.webnavigation.WebPageDescriptor;
import com.globalsight.util.GlobalSightLocale;

/**
* Provides ability to add customize report for Excel reports
*/
public class CustomizeReportsMainHandler extends PageHandler
{
    public void invokePageHandler(WebPageDescriptor p_pageDescriptor, 
            HttpServletRequest p_request, HttpServletResponse p_response, 
            ServletContext p_context) 
    throws ServletException, IOException, EnvoyServletException 
    {
        HttpSession session = p_request.getSession();
        ResourceBundle bundle = PageHandler.getBundle(session);
        
        Map paramMap = null;
        
        String action = p_request.getParameter("action");
        if (WebAppConstants.ACTION_JOB_RANGE.equalsIgnoreCase(action))
        {
            paramMap = (Map) session.getAttribute(WebAppConstants.CUSTOMIZE_REPORTS_PARAMMAP);
            if (paramMap == null)
            {
                paramMap = new HashMap();
            }
            
            extractJobRangeParams(p_request, paramMap);
            session.setAttribute(WebAppConstants.CUSTOMIZE_REPORTS_PARAMMAP, paramMap);
            
            // Prepare for job info param in customize report
            String xml = CustomizeReportsParamXmlHandler.getJobInfoParamXml();
            
            xml = CustomizeReportsParamXmlHandler.parseParamXml(bundle, xml);
            
            p_request.setAttribute(WebAppConstants.CUSTOMIZE_REPORTS_JOB_INFO_PARAM_XML, xml);
            
            super.invokePageHandler(p_pageDescriptor, p_request, p_response, p_context);
        }
        else if (WebAppConstants.ACTION_JOB_CANCEL.equalsIgnoreCase(action))
        {
            session.removeAttribute(WebAppConstants.CUSTOMIZE_REPORTS_PARAMMAP);
            
            super.invokePageHandler(p_pageDescriptor, p_request, p_response, p_context);
        }
        else if (WebAppConstants.ACTION_JOB_INFO.equalsIgnoreCase(action))
        {
            paramMap = (Map) session.getAttribute(WebAppConstants.CUSTOMIZE_REPORTS_PARAMMAP);
            //Clean garbage from session.
            // To avoid nullpointer exception.
            //session.removeAttribute(WebAppConstants.CUSTOMIZE_REPORTS_PARAMMAP);
            
            extractJobInfoParams(p_request, paramMap);
            
            //Get label bundle
            paramMap.put(WebAppConstants.LABEL_BUNDLE_PARAM, bundle);
            
            try {
                writeReports(p_request, p_response, paramMap);
            } catch (Exception e) {
                throw new EnvoyServletException(e);
            } 
            
            //Never invoke super.invokePageHandler(...) or something like this here.
        }
        else //Prepare data for customizeReportsJobRange.jsp
        {
            prepareJobRangData(p_request);
            super.invokePageHandler(p_pageDescriptor, p_request, p_response, p_context);
        }
    }
    
    /**
     * Generate the new reports and write the excel file back.
     * @throws JobException 
     */
    private void writeReports(HttpServletRequest request, HttpServletResponse p_response, Map p_paramMap) 
    throws IOException, JobException
    {
        //Set response header
        p_response.setHeader("Content-Disposition","inline; filename=CustomizeReports.xls" );
        p_response.setHeader("Expires", "0");
        p_response.setHeader("Cache-Control","must-revalidate, post-check=0,pre-check=0");
        p_response.setHeader("Pragma","public");
        p_response.setContentType("application/vnd.ms-excel");
        ResourceBundle bundle = PageHandler.getBundle(request.getSession());
        ReportWriter reportWriter = new ExcelReportWriter(p_response.getOutputStream(), bundle);
        
        String currency = request.getParameter("currency");
        CurrencyThreadLocal.setCurrency(currency);
        
        CustomizeReportsGenerator generator = new CustomizeReportsGenerator(p_paramMap, reportWriter);
        generator.pupulate();
        
        reportWriter.commit();
    }
    
    /**
     * Extract parameters user choosed in the parameter UI from request and 
     * return a hash map. 
     * @param p_request
     * @return A <code>Map</code> contains the parameters user choosed
     */
    private Map extractJobRangeParams(HttpServletRequest p_request, Map paramMap) 
    {
        //extract parameters from request
        String[] paramJobIds = p_request.getParameterValues("jobId");
        String[] paramProjectIds = p_request.getParameterValues("projectId");
        String[] paramStatus = p_request.getParameterValues("status");
        String[] paramTargetLocales = p_request.getParameterValues("targetLocale");
        
        List jobRangeParam = new ArrayList();
        
        //
        // Get JobSearchParameters
        //
        JobSearchParameters sp = new JobSearchParameters();
        
        //If sepcified job ids, then use job ids only.
        if ((paramJobIds != null) && !("*".equals(paramJobIds[0])))
        {
            //just get the specific jobs they chose
            for (int i = 0; i < paramJobIds.length; i++)
            {
                sp.setJobId(paramJobIds[i]);
                sp.setJobIdCondition(JobSearchParameters.EQUALS);
            }            
        } 
        
        // Get project ids
        if ((paramProjectIds != null) && !("*".equals(paramProjectIds[0]))) 
        {
            for (int i = 0; i < paramProjectIds.length; i++) 
            {
                sp.setProjectId(paramProjectIds[i]);
            }
        } 

        // Get job status.
        List stateList = new ArrayList();
        if ((paramStatus != null) && !("*".equals(paramStatus[0]))) 
        {
            for (int i = 0; i < paramStatus.length; i++) 
            {
                stateList.add(paramStatus[i]);
            }
        } 
        else 
        {
            // just do a query for all in progress jobs, localized, and exported
            stateList.add(Job.DISPATCHED);
            stateList.add(Job.LOCALIZED);
            stateList.add(Job.EXPORTED);
        }
        sp.setJobState(stateList);
        
        // Get creation start
        String paramCreateDateStartCount = p_request
                .getParameter(JobSearchConstants.CREATION_START);
        String paramCreateDateStartOpts = p_request
                .getParameter(JobSearchConstants.CREATION_START_OPTIONS);
        if ("-1".equals(paramCreateDateStartOpts) == false) 
        {
            sp.setCreationStart(new Integer(paramCreateDateStartCount));
            sp.setCreationStartCondition(paramCreateDateStartOpts);
        }

        // Get creation end
        String paramCreateDateEndCount = p_request
                .getParameter(JobSearchConstants.CREATION_END);
        String paramCreateDateEndOpts = p_request
                .getParameter(JobSearchConstants.CREATION_END_OPTIONS);
        if (SearchCriteriaParameters.NOW.equals(paramCreateDateEndOpts)) 
        {
            sp.setCreationEnd(new java.util.Date());
        } 
        else if ("-1".equals(paramCreateDateEndOpts) == false) 
        {
            sp.setCreationEnd(new Integer(paramCreateDateEndCount));
            sp.setCreationEndCondition(paramCreateDateEndOpts);
        }
        
        jobRangeParam.add(sp);
        paramMap.put(WebAppConstants.JOB_RANGE_PARAM, jobRangeParam);
        
        //
        // Get target locales
        //
        List targetLocaleList = new ArrayList();
        if (paramTargetLocales != null && !paramTargetLocales[0].equals("*"))
        {
            for (int i = 0 ; i < paramTargetLocales.length; i++)
            {
                targetLocaleList.add(paramTargetLocales[i]);
            }
        }
        paramMap.put(WebAppConstants.TARGET_LOCALE_PARAM, targetLocaleList);
        
        //
        //Get workflow status
        //
        paramMap.put(WebAppConstants.WORKFLOW_STATUS_PARAM, stateList);
     
        //
        //Get date format
        //
        String dateFormat = p_request.getParameter("dateFormat");
        SimpleDateFormat dateFormatParam = new SimpleDateFormat(dateFormat);
        
        paramMap.put(WebAppConstants.DATE_FORMAT_PARAM, dateFormatParam);
       
        return paramMap;
    }
    
    /**
     * Extract parameters user choosed in the job related info UI from request 
     * and return a hash map. 
     * @param p_request
     * @return A <code>Map</code> contains the parameters user choosed
     */
    private Map extractJobInfoParams(HttpServletRequest p_request, Map paramMap) {
        //extract parameters from request
        
        Enumeration item = p_request.getParameterNames();
        
        CustomizeReportParamInitiator paramManager = new CustomizeReportParamInitiator();
        
        while (item.hasMoreElements()) 
        {
            String name = (String)item.nextElement();
            if (name.startsWith("param."))
            {
                // strip off "param."
                paramManager.setParamByName(name.substring(6));
            }
            else if (name.startsWith("cat."))
            {
                // strip off "cat."
                paramManager.setParamByName(name.substring(4));
            }
        }
        paramMap.put(WebAppConstants.JOB_INFO_PARAM, paramManager.getRootParam());
        
        return paramMap;
    }
    
    private void prepareJobRangData(HttpServletRequest p_request) 
    throws EnvoyServletException
    {
        HttpSession session = p_request.getSession();

        //Now we support only these three job status. Keep these synchronize with 
        //the one in customizeReportsJobRangParam.jsp
        Vector statusList = new Vector(3);
        statusList.add(Job.DISPATCHED);
        statusList.add(Job.LOCALIZED);
        statusList.add(Job.EXPORTED);
        
        //Gets job list
        List jobList = null;
        try {
            Collection jobs =  ServerProxy.getJobHandler().getJobsByStateList(statusList);
            jobList = new ArrayList(jobs);
        } catch (Exception e) {
            throw new EnvoyServletException(e);
        } 
        Locale uiLocale = (Locale)session.getAttribute(WebAppConstants.UILOCALE);
        Collections.sort(jobList, new JobComparator(JobComparator.NAME, uiLocale));
        p_request.setAttribute(WebAppConstants.CUSTOMIZE_REPORTS_JOB_LIST, jobList);
        
        //Gets project list and target locale list
        List projectList = new ArrayList();
        List targetLocaleList = new ArrayList();
        for (Iterator iter = jobList.iterator(); iter.hasNext();)
        {
            L10nProfile l10nProfile = ((Job) iter.next()).getL10nProfile();
            GlobalSightLocale [] targetLocales = l10nProfile.getTargetLocales();
            for (int i = 0; i < targetLocales.length; i++)
            {
                if (!targetLocaleList.contains(targetLocales[i]))
                {
                    targetLocaleList.add(targetLocales[i]);
                }
            }
            Project project = l10nProfile.getProject();
            if (!projectList.contains(project))
            {
                projectList.add(project);
            }
        }
        Collections.sort(targetLocaleList, new GlobalSightLocaleComparator(Locale.US));
        Collections.sort(projectList, new ProjectComparator(Locale.US));
        p_request.setAttribute(WebAppConstants.CUSTOMIZE_REPORTS_TARGETLOCALE_LIST, targetLocaleList);
        p_request.setAttribute(WebAppConstants.CUSTOMIZE_REPORTS_PROJECT_LIST, projectList);
    }
}

