<%@ page contentType="text/html; charset=UTF-8"
	errorPage="/envoy/common/activityError.jsp"
	import="java.util.*,
	com.globalsight.everest.webapp.pagehandler.PageHandler,
	com.globalsight.everest.webapp.pagehandler.projects.workflows.JobSearchConstants,
	com.globalsight.everest.foundation.SearchCriteriaParameters,
    com.globalsight.everest.webapp.pagehandler.administration.reports.ReportConstants,
    com.globalsight.everest.webapp.pagehandler.administration.reports.ReportJobInfo,
	com.globalsight.everest.projecthandler.Project,
	com.globalsight.everest.util.comparator.GlobalSightLocaleComparator,
	com.globalsight.everest.jobhandler.Job,
	com.globalsight.everest.jobhandler.JobSearchParameters,
	com.globalsight.everest.servlet.util.ServerProxy,
	com.globalsight.util.GlobalSightLocale,
	com.globalsight.util.SortUtil,
	com.globalsight.everest.company.CompanyWrapper,
	java.util.Locale,java.util.ResourceBundle,
	com.globalsight.everest.company.CompanyThreadLocal,
	com.globalsight.everest.webapp.pagehandler.administration.users.UserUtil,
	com.globalsight.everest.usermgr.UserLdapHelper,
	com.globalsight.everest.webapp.WebAppConstants"
	session="true"%>
<%
	String EMEA = CompanyWrapper.getCurrentCompanyName();
	//Multi-Company: get current user's company from the session
	HttpSession userSession = request.getSession(false);
	String companyName = (String) userSession
			.getAttribute(WebAppConstants.SELECTED_COMPANY_NAME_FOR_SUPER_PM);
	if (UserUtil.isBlank(companyName))
	{
		companyName = (String) userSession
				.getAttribute(UserLdapHelper.LDAP_ATTR_COMPANY);
	}
	if (companyName != null)
	{
		CompanyThreadLocal.getInstance().setValue(companyName);
	}

	ResourceBundle bundle = PageHandler.getBundle(session);
	Locale uiLocale = (Locale) session
			.getAttribute(WebAppConstants.UILOCALE);

	// Field names
	String creationStart = JobSearchConstants.CREATION_START;
	String creationStartOptions = JobSearchConstants.CREATION_START_OPTIONS;
	String creationEnd = JobSearchConstants.CREATION_END;
	String creationEndOptions = JobSearchConstants.CREATION_END_OPTIONS;

    List<ReportJobInfo> jobList = (ArrayList<ReportJobInfo>)
        request.getAttribute(ReportConstants.REPORTJOBINFO_LIST);
    List<Project> projectList = (ArrayList<Project>)
        request.getAttribute(ReportConstants.PROJECT_LIST);
   List<GlobalSightLocale> targetLocales = (ArrayList<GlobalSightLocale>)
       request.getAttribute(ReportConstants.TARGETLOCALE_LIST);
%>
<html>
<!--  This JSP is: /envoy/administration/reports/translationProgressXlsReportWebForm.jsp-->
<head>
<title><%=EMEA%> <%=bundle.getString("review_translation_progress_report")%></title>
</head>
<body leftmargin="0" rightrmargin="0" topmargin="0" marginwidth="0"
	marginheight="0" bgcolor="LIGHTGREY">
<SCRIPT LANGUAGE="JAVASCRIPT">
// If user selected "now", then blank out the preceeding numeric field.
function checkNow(field, text)
{
    if (field.options[1].selected)
        text.value = "";
}

function isInteger(value)
{
    if (value == "") return true;
    return (parseInt(value) == value);
}

function validateForm()
{
    if ((-1 != searchForm.<%=creationStartOptions%>.value) &&
        (searchForm.<%=creationStart%>.value == ""))
        return ('<%=bundle.getString("jsmsg_job_search_bad_date")%>');
    if ((-1 != searchForm.<%=creationEndOptions%>.value) &&
    	("<%=SearchCriteriaParameters.NOW%>" != searchForm.<%=creationEndOptions%>.value) &&
        (searchForm.<%=creationEnd%>.value == ""))
        return ('<%=bundle.getString("jsmsg_job_search_bad_date")%>');
    if (!isInteger(searchForm.<%=creationStart%>.value))
        return ('<%=bundle.getString("jsmsg_job_search_bad_date")%>');
    if (!isInteger(searchForm.<%=creationEnd%>.value))
        return ('<%=bundle.getString("jsmsg_job_search_bad_date")%>');
    return "";
}

function submitForm()
{
   var msg = validateForm();
   if (msg != "")
   {
    alert(msg);
    return;
   }
   else
    searchForm.submit();
}

</script>
<TABLE WIDTH="100%" BGCOLOR="WHITE">
	<TR>
		<TD ALIGN="CENTER"><IMG SRC="/globalsight/images/logo_header.gif"></TD>
	</TR>
</TABLE>
<BR>
<span class="mainHeading"><B><%=EMEA%> <%=bundle.getString("review_translation_progress_report")%></B></span>
<BR>
<BR>
<TABLE WIDTH="80%">
	<TR>
		<TD><SPAN CLASS="smallText"><%=bundle.getString("optionally_submit_generate")%> <%=bundle.getString("hold_the_shift")%></SPAN></TD>
	</TR>
</TABLE>

<form name="searchForm" method="post"
	action="/globalsight/envoy/administration/reports/translationProgressXlsReport.jsp">

<table border="0" cellspacing="2" cellpadding="2" class="standardText">
	<tr>
		<td class="standardText"><%=bundle.getString("lb_job_name")%>:</td>
		<td class="standardText" VALIGN="BOTTOM"><select name="jobId"
			MULTIPLE size="6" style="width:300px">
			<option value="*" SELECTED><B>&lt;<%=bundle.getString("all")%>&gt;</B></OPTION>
			<%
				for (ReportJobInfo j : jobList)
				{
			%>
			<option title="<%=j.getJobName()%>" VALUE="<%=j.getJobId()%>"><%=j.getJobName()%></OPTION>
			<%
				}
			%>
		</select></td>
	</tr>

	<tr>
		<td class="standardText"><%=bundle.getString("lb_project")%>:</td>
		<td class="standardText" VALIGN="BOTTOM"><select name="projectId"
			MULTIPLE size=4>
			<option VALUE="*" SELECTED>&lt;<%=bundle.getString("all")%>&gt;</OPTION>
			<%
			    for (Project p : projectList)
				{
			%>
			<option VALUE="<%=p.getId()%>"><%=p.getName()%></OPTION>
			<%
				}
			%>
		</select></td>
	</tr>

	<tr>
		<td class="standardText"><%=bundle.getString("source_locales")%>:</td>
		<td class="standardText" VALIGN="BOTTOM"><select
			name="sourceLocalesList" size=4>
			<%
				ArrayList sourceLocales = new ArrayList(ServerProxy
						.getLocaleManager().getAllSourceLocales());
				SortUtil.sort(sourceLocales, new GlobalSightLocaleComparator(Locale.getDefault()));

				for (int i = 0; i < sourceLocales.size(); i++)
				{
					GlobalSightLocale gsLocale = (GlobalSightLocale) sourceLocales
							.get(i);
			%>
			<option VALUE="<%=gsLocale.toString()%>" <%=(i==0)?"SELECTED":"" %>><%=gsLocale.getDisplayName(uiLocale)%></OPTION>
			<%
				}
			%>
		</select></td>
	</tr>

	<tr>
		<td class="standardText"><%=bundle.getString("lb_target_locales")%>:</td>
		<td class="standardText" VALIGN="BOTTOM"><select
			name="targetLocalesList" size=4>
			<%
				for (int i = 0; i < targetLocales.size(); i++)
				{
					GlobalSightLocale gsLocale = targetLocales.get(i);
			%>
			<option VALUE="<%=gsLocale.toString()%>" <%=(i==0)?"SELECTED":"" %>><%=gsLocale.getDisplayName(uiLocale)%></OPTION>
			<%
				}
			%>
		</select></td>
	</tr>
    <tr>
		<td class="standardText" colspan=2><%=bundle.getString("lb_creation_date_range")%>:
		</td>
	</tr>
	<tr>
		<td class="standardText" style="padding-left: 70px" colspan=2
			VALIGN="BOTTOM"><%=bundle.getString("lb_starts")%>: <input
			type="text" name="<%=creationStart%>" size="3" maxlength="9">
		<select name="<%=creationStartOptions%>">
			<option value='-1'></option>
			<option value='<%=SearchCriteriaParameters.HOURS_AGO%>'><%=bundle.getString("lb_hours_ago")%></option>
			<option value='<%=SearchCriteriaParameters.DAYS_AGO%>'><%=bundle.getString("lb_days_ago")%></option>
			<option value='<%=SearchCriteriaParameters.WEEKS_AGO%>'><%=bundle.getString("lb_weeks_ago")%></option>
			<option value='<%=SearchCriteriaParameters.MONTHS_AGO%>'><%=bundle.getString("lb_months_ago")%></option>
		</select> <%=bundle.getString("lb_ends")%>: <input type="text"
			name="<%=creationEnd%>" size="3" maxlength="9"> <select
			name="<%=creationEndOptions%>"
			onChange="checkNow(this, searchForm.<%=creationEnd%>)">
			<option value='-1'></option>
			<option value='<%=SearchCriteriaParameters.NOW%>'><%=bundle.getString("lb_now")%></option>
			<option value='<%=SearchCriteriaParameters.HOURS_AGO%>'><%=bundle.getString("lb_hours_ago")%></option>
			<option value='<%=SearchCriteriaParameters.DAYS_AGO%>'><%=bundle.getString("lb_days_ago")%></option>
			<option value='<%=SearchCriteriaParameters.WEEKS_AGO%>'><%=bundle.getString("lb_weeks_ago")%></option>
			<option value='<%=SearchCriteriaParameters.MONTHS_AGO%>'><%=bundle.getString("lb_months_ago")%></option>
		</select></td>
	</tr>
	<tr>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td><input type="BUTTON" VALUE="<%=bundle.getString("lb_shutdownSubmit")%>" onClick="submitForm()"></td>
		<td><INPUT type="BUTTON" VALUE="<%=bundle.getString("lb_cancel")%>" onClick="window.close()"></td>
	</tr>
</table>
</form>
<BODY>
</HTML>

