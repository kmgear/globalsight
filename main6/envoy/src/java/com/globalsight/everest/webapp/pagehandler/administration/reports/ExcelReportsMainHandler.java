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
package com.globalsight.everest.webapp.pagehandler.administration.reports;

// Envoy packages
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.globalsight.everest.jobhandler.Job;
import com.globalsight.everest.projecthandler.Project;
import com.globalsight.everest.servlet.EnvoyServletException;
import com.globalsight.everest.servlet.util.ServerProxy;
import com.globalsight.everest.util.comparator.GlobalSightLocaleComparator;
import com.globalsight.everest.util.comparator.JobComparator;
import com.globalsight.everest.webapp.WebAppConstants;
import com.globalsight.everest.webapp.pagehandler.PageHandler;
import com.globalsight.everest.webapp.pagehandler.administration.vendors.ProjectComparator;
import com.globalsight.everest.webapp.webnavigation.WebPageDescriptor;
import com.globalsight.util.GlobalSightLocale;
import com.globalsight.util.SortUtil;

/**
 * Provides ability to add custom stuff for Excel reports
 */
public class ExcelReportsMainHandler extends PageHandler
{
    private final String REPORTNAME = "activityName";
    private ArrayList<Project> projectList = null;
    private ArrayList<GlobalSightLocale> targetLocales = null;
    private Locale uiLocale = null;
    private static Logger LOGGER = Logger
            .getLogger(ExcelReportsMainHandler.class.getName());

    // Some reports are using 6 job states (no "pending"), while the others are
    // using all 7 states, a bit strange. Do not change previous behavior for
    // now.
    private static List<String> reportNameListUsing6States= new ArrayList<String>();
    static
    {
        // Translations Edit Report
        reportNameListUsing6States.add("xlsReportTranslationsEdit");
        // Job Status Report
        reportNameListUsing6States.add("xlsReportJobStatus");
        // Translation Progress Report
        reportNameListUsing6States.add("xlsReportTranslationProgress");
        // Translation SLA Performance
        reportNameListUsing6States.add("xlsReportSlaPerformance");
        // Implemented Comments Check Report
        reportNameListUsing6States.add("implementedCommentsCheck");
        // Online Review Status
        reportNameListUsing6States.add("xlsReportOnlineRevStatus");
        // Activity Duration
        reportNameListUsing6States.add("xlsReportActivityDuration");
        // Comments
        reportNameListUsing6States.add("xlsReportComment");
        // Vendor PO
        reportNameListUsing6States.add("xlsReportVendorPO");
    }

    @Override
    public void invokePageHandler(WebPageDescriptor p_pageDescriptor,
            HttpServletRequest p_request, HttpServletResponse p_response,
            ServletContext p_context) throws ServletException, IOException,
            EnvoyServletException
    {
        String activityName = (String) p_request.getParameter(REPORTNAME);
        HttpSession session = p_request.getSession(false);
        uiLocale = (Locale) session.getAttribute(WebAppConstants.UILOCALE);

        initData();

        List<ReportJobInfo> reportJobInfoList = new ArrayList<ReportJobInfo>();
        if (reportNameListUsing6States.contains(activityName))
        {
            reportJobInfoList = getReportJobInfoForSixStates();
        }
        else
        {
            reportJobInfoList = getReportJobInfoForSevenStates();
        }

        // Set into Request
        p_request.setAttribute(ReportConstants.REPORTJOBINFO_LIST,
                reportJobInfoList);
        p_request.setAttribute(ReportConstants.PROJECT_LIST, projectList);
        p_request.setAttribute(ReportConstants.TARGETLOCALE_LIST, targetLocales);

        super.invokePageHandler(p_pageDescriptor, p_request, p_response,
                p_context);
    }

    /**
     * Initialize some general data most reports need. Note that all queries in
     * this method should be as simple as possible, don't add heavy queries into
     * this !!!
     */
    @SuppressWarnings("unchecked")
    private void initData()
    {
        try
        {
            targetLocales = new ArrayList<GlobalSightLocale>(ServerProxy
                    .getLocaleManager().getAllTargetLocales());
            SortUtil.sort(targetLocales, new GlobalSightLocaleComparator(
                    getUILocale()));

            projectList = new ArrayList<Project>(ServerProxy
                    .getProjectHandler().getAllProjects());
            SortUtil.sort(projectList, new ProjectComparator(getUILocale()));
        }
        catch (Exception e)
        {
            LOGGER.error("Getting target locales or project error", e);
        }
    }

    /**
     * Some reports need jobs with 7 states.
     */
    private List<ReportJobInfo> getReportJobInfoForSevenStates()
    {
        ArrayList<String> stateList = ReportHelper.getAllJobStatusList();
        List<ReportJobInfo> reportJobInfoList = new ArrayList<ReportJobInfo>(
                ReportHelper.getJobInfo(stateList).values());
        if (reportJobInfoList != null && !reportJobInfoList.isEmpty())
        {
            SortUtil.sort(reportJobInfoList, new ReportJobInfoComparator(
                    JobComparator.NAME, getUILocale()));
        }

        return reportJobInfoList;
    }

    /**
     * Some reports need jobs for 6 states, no "pending".
     */
    private List<ReportJobInfo> getReportJobInfoForSixStates()
    {
        ArrayList<String> stateList = ReportHelper.getAllJobStatusList();
        stateList.remove(Job.PENDING);
        List<ReportJobInfo> reportJobInfoList = new ArrayList<ReportJobInfo>(
                ReportHelper.getJobInfo(stateList).values());
        if (reportJobInfoList != null && !reportJobInfoList.isEmpty())
        {
            SortUtil.sort(reportJobInfoList, new ReportJobInfoComparator(
                    JobComparator.NAME, getUILocale()));
        }

        return reportJobInfoList;
    }

    protected Locale getUILocale()
    {
        return uiLocale == null ? Locale.US : uiLocale;
    }
}
