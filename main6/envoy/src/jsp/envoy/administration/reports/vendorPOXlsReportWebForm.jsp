<%@ taglib uri="/WEB-INF/tlds/globalsight.tld" prefix="amb" %>
<%@ page contentType="text/html; charset=UTF-8"
         errorPage="/envoy/common/activityError.jsp"
         import="java.util.*,com.globalsight.everest.servlet.util.SessionManager,com.globalsight.everest.webapp.WebAppConstants,com.globalsight.util.GlobalSightLocale,com.globalsight.everest.webapp.javabean.NavigationBean,com.globalsight.everest.webapp.pagehandler.PageHandler,com.globalsight.everest.webapp.pagehandler.administration.users.UserHandlerHelper,com.globalsight.everest.webapp.pagehandler.projects.workflows.JobSearchConstants,com.globalsight.util.resourcebundle.ResourceBundleConstants,com.globalsight.util.resourcebundle.SystemResourceBundle,com.globalsight.everest.foundation.SearchCriteriaParameters,com.globalsight.everest.foundation.User,com.globalsight.everest.util.comparator.JobComparator,com.globalsight.everest.jobhandler.Job,com.globalsight.everest.jobhandler.JobSearchParameters,com.globalsight.everest.projecthandler.ProjectInfo,com.globalsight.everest.webapp.webnavigation.LinkHelper,com.globalsight.everest.permission.Permission,com.globalsight.everest.servlet.util.ServerProxy,com.globalsight.everest.projecthandler.Project,com.globalsight.everest.webapp.pagehandler.administration.vendors.ProjectComparator,com.globalsight.everest.servlet.EnvoyServletException,com.globalsight.everest.util.system.SystemConfigParamNames,com.globalsight.everest.util.system.SystemConfiguration,com.globalsight.util.GeneralException,com.globalsight.util.GlobalSightLocale,com.globalsight.everest.company.CompanyWrapper,com.globalsight.everest.costing.Currency,java.text.MessageFormat,java.util.Locale,java.util.ResourceBundle,java.util.List,java.util.HashSet"
          session="true"
%>
<%  String EMEA = CompanyWrapper.getCurrentCompanyName();
    ResourceBundle bundle = PageHandler.getBundle(session);
    SessionManager sessionMgr = (SessionManager)session.getAttribute(WebAppConstants.SESSION_MANAGER);
    Locale uiLocale = (Locale)session.getAttribute(WebAppConstants.UILOCALE);
    String userName = (String)session.getAttribute(WebAppConstants.USER_NAME);
    String creationStart = JobSearchConstants.CREATION_START;
    String creationStartOptions = JobSearchConstants.CREATION_START_OPTIONS;
    String creationEnd = JobSearchConstants.CREATION_END;
    String creationEndOptions = JobSearchConstants.CREATION_END_OPTIONS;
    
    
%>
<html>
<!-- This JSP is: /envoy/administration/reports/vendorPOXlsReportWebForm.jsp-->
<head>
<title><%= EMEA%> <%=bundle.getString("vendor_po_report_web_form")%></title>
</head>
<body leftmargin="0" rightrmargin="0" topmargin="0" marginwidth="0" marginheight="0"
bgcolor="LIGHTGREY">
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
<TR><TD ALIGN="CENTER"><IMG SRC="/globalsight/images/logo_header.gif"></TD></TR>
</TABLE><BR>
<span class="mainHeading"><B><%= EMEA%> <%=bundle.getString("vendor_po_report_web_form")%></B></span>
<BR><BR>
<TABLE WIDTH="80%">
<TR><TD>
<SPAN CLASS="smallText">
<%=bundle.getString("select_the_appropriate")%>
</SPAN>
</TD></TR></TABLE>

<form name="searchForm" method="post" action="/globalsight/envoy/administration/reports/vendorPOXlsReport.jsp">

<table border="0" cellspacing="2" cellpadding="2" class="standardText">
<tr>
<td class="standardText"><%=bundle.getString("lb_project")%>:</td>
<td class="standardText" VALIGN="BOTTOM">
<select name="projectId" multiple="true" size=4>
<OPTION value="*" selected>&lt;<%=bundle.getString("all")%>&gt;</OPTION>
<%
         ArrayList projects =new ArrayList( ServerProxy.getProjectHandler().getAllProjects() );
         Collections.sort(projects,new ProjectComparator(Locale.US));
         Iterator iter = projects.iterator();
         while (iter.hasNext())
         {
             Project p = (Project) iter.next();
%>
<option VALUE="<%=p.getId()%>"><%=p.getName()%></OPTION>
<%
         }
%>
</select>
</td>
</tr>

<tr>
<td class="standardText" colspan=2>
<%=bundle.getString("lb_creation_date_range")%>:
</td>
</tr>
<tr>
<td class="standardText" style="padding-left:70px" colspan=2 VALIGN="BOTTOM">
<%=bundle.getString("lb_starts")%>:
<input type="text" name="<%=creationStart%>" size="3" maxlength="9">
<select name="<%=creationStartOptions%>">
<option value='-1'></option>
<option value='<%=SearchCriteriaParameters.HOURS_AGO%>'><%=bundle.getString("lb_hours_ago")%></option>
<option value='<%=SearchCriteriaParameters.DAYS_AGO%>'><%=bundle.getString("lb_days_ago")%></option>
<option value='<%=SearchCriteriaParameters.WEEKS_AGO%>'><%=bundle.getString("lb_weeks_ago")%></option>
<option value='<%=SearchCriteriaParameters.MONTHS_AGO%>'><%=bundle.getString("lb_months_ago")%></option>
</select>
<%=bundle.getString("lb_ends")%>:
<input type="text" name="<%=creationEnd%>" size="3" maxlength="9">
<select name="<%=creationEndOptions%>" onChange="checkNow(this, searchForm.<%=creationEnd%>)">
<option value='-1'></option>
<option value='<%=SearchCriteriaParameters.NOW%>'><%=bundle.getString("lb_now")%></option>
<option value='<%=SearchCriteriaParameters.HOURS_AGO%>'><%=bundle.getString("lb_hours_ago")%></option>
<option value='<%=SearchCriteriaParameters.DAYS_AGO%>'><%=bundle.getString("lb_days_ago")%></option>
<option value='<%=SearchCriteriaParameters.WEEKS_AGO%>'><%=bundle.getString("lb_weeks_ago")%></option>
<option value='<%=SearchCriteriaParameters.MONTHS_AGO%>'><%=bundle.getString("lb_months_ago")%></option>
</select>
</td>
</tr>

<tr>
<td class="standardText"><%=bundle.getString("lb_target_language")%>:</td>
<td class="standardText" VALIGN="BOTTOM">
<select name="targetLang" multiple="true" size=4>
<OPTION value="*" selected>&lt;<%=bundle.getString("all")%>&gt;</OPTION>
<%
         Vector targetLocales = ServerProxy.getLocaleManager().getAllTargetLocales();
         HashMap targetLangs = new HashMap();
         ArrayList showLangs = new ArrayList();
         iter = targetLocales.iterator();
         while (iter.hasNext())
         {
             GlobalSightLocale gsl = (GlobalSightLocale) iter.next();
             showLangs.add(gsl.getDisplayName(uiLocale));
             targetLangs.put(gsl.getDisplayName(uiLocale), gsl.toString());
		 }
         Collections.sort(showLangs);
         iter = showLangs.iterator();
         while (iter.hasNext())
		 {
             String lang = (String) iter.next();
%>
<option VALUE="<%=targetLangs.get(lang).toString()%>"><%=lang%></OPTION>
<%
         }
%>
</select>
</td>
</tr>

	<tr>
		<td class="standardText"><%=bundle.getString("lb_currency")%>:</td>
		<td><SELECT NAME="currency">
			<%
        Collection<?> currencies = ServerProxy.getCostingEngine().getCurrencies();
        Currency pivotCurrency = ServerProxy.getCostingEngine().getPivotCurrency();

        ArrayList<String> labeledCurrencies = new ArrayList<String>();
        ArrayList<String> valueCurrencies = new ArrayList<String>();
        iter = currencies.iterator();

        while ( iter.hasNext() ) 
        {
            Currency c = (Currency) iter.next();
            if (!labeledCurrencies.contains(c.getDisplayName())) 
            {
                labeledCurrencies.add(c.getDisplayName(uiLocale));
            	valueCurrencies.add(c.getDisplayName());
            }
        }
		
        for (int j = 0; j < labeledCurrencies.size(); j++)
        {
            String currencyLabel = labeledCurrencies.get(j);
            String currencyText = valueCurrencies.get(j);
           %>
		<OPTION VALUE="<%=currencyText%>"><%=currencyLabel%> <%  }  %>
			
		</SELECT></td>
	</tr>

	<tr>
<amb:permission name="<%=Permission.REPORTS_DELL_ONLINE_JOBS_RECALC%>" >
<TR><TD><%=bundle.getString("re_calculate_costs")%>?<br></TD>
<TD><SELECT NAME="recalc">
<OPTION name="false" VALUE="false" SELECTED><%=bundle.getString("lb_false")%></OPTION>
<OPTION name="true" VALUE="true"><%=bundle.getString("lb_true")%></OPTION>
</SELECT></TD>
</TR>
</amb:permission>
<td><input type="BUTTON" VALUE="<%=bundle.getString("lb_shutdownSubmit")%>" onClick="submitForm()"></td>
<TD><INPUT type="BUTTON" VALUE="<%=bundle.getString("lb_cancel")%>" onClick="window.close()"></TD>
</tr>
</table>
</form>
<BODY>
</HTML>

