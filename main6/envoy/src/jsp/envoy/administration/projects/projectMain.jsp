<%@ taglib uri="/WEB-INF/tlds/globalsight.tld" prefix="amb" %>
<%@ page contentType="text/html; charset=UTF-8"
    errorPage="/envoy/common/error.jsp"
    import="java.util.*,com.globalsight.everest.webapp.javabean.NavigationBean,
         com.globalsight.util.resourcebundle.ResourceBundleConstants,
         com.globalsight.util.resourcebundle.SystemResourceBundle,
         com.globalsight.everest.permission.Permission,
         com.globalsight.everest.permission.PermissionSet,
         com.globalsight.everest.util.comparator.ProjectComparator,
         com.globalsight.everest.webapp.pagehandler.PageHandler,
         com.globalsight.everest.webapp.pagehandler.administration.projects.ProjectMainHandler,
         com.globalsight.everest.projecthandler.ProjectInfo,
         com.globalsight.everest.foundation.User,
         com.globalsight.everest.servlet.util.ServerProxy,
         com.globalsight.util.edit.EditUtil,
         java.text.MessageFormat,
         java.util.Locale,
         java.util.ResourceBundle,
         java.util.List,
         java.util.ArrayList" 
         session="true" %>
<jsp:useBean id="skinbean" scope="application"
 class="com.globalsight.everest.webapp.javabean.SkinBean" />
<jsp:useBean id="self" scope="request"
 class="com.globalsight.everest.webapp.javabean.NavigationBean" />
<jsp:useBean id="new1" scope="request"
 class="com.globalsight.everest.webapp.javabean.NavigationBean" />
<jsp:useBean id="modify" scope="request"
 class="com.globalsight.everest.webapp.javabean.NavigationBean" />
<jsp:useBean id="_import" scope="request"
 class="com.globalsight.everest.webapp.javabean.NavigationBean" />
<jsp:useBean id="_export" scope="request"
 class="com.globalsight.everest.webapp.javabean.NavigationBean" />
<jsp:useBean id="remove" scope="request"
 class="com.globalsight.everest.webapp.javabean.NavigationBean" />
<jsp:useBean id="projects" scope="request" class="java.util.ArrayList" />

<% 
    ResourceBundle bundle = PageHandler.getBundle(session);
    SessionManager sessionManager =
      (SessionManager)session.getAttribute(WebAppConstants.SESSION_MANAGER);
    PermissionSet perms=(PermissionSet)session.getAttribute(WebAppConstants.PERMISSIONS);

    String selfUrl = self.getPageURL();
    String newUrl = new1.getPageURL()+"&action=new";
    String editUrl = modify.getPageURL()+"&action=edit";
    String importSchedUrl = _import.getPageURL();
    String exportUrl = _export.getPageURL();
    String removeUrl = remove.getPageURL()+"&action=remove";

    String subTitle = "";
    String title= bundle.getString("lb_projects");
    String moduleLink="/globalsight/ControlServlet?activityName=";

    // Button names
    String newButton = bundle.getString("lb_new1");
    String editButton = bundle.getString("lb_edit1");
    String importButton = bundle.getString("lb_import_schedules");
    String exportButton = bundle.getString("lb_export_schedules");
    String removeButton = bundle.getString("lb_remove");

    // user info
    User user = (User)sessionManager.getAttribute(WebAppConstants.USER);
    String pmName = user.getUserName();

    boolean isSuperAdmin = ((Boolean) session.getAttribute(WebAppConstants.IS_SUPER_ADMIN)).booleanValue();
    
    String error = (String) sessionManager.getAttribute(WebAppConstants.PROJECT_ERROR);

%>
<HTML XMLNS:gs>
<!-- This JSP is envoy/administration/projects/projectMain.jsp -->
<HEAD>
<META HTTP-EQUIV="content-type" CONTENT="text/html;charset=UTF-8">
<TITLE><%= title %></TITLE>
<SCRIPT LANGUAGE="JavaScript" SRC="/globalsight/includes/setStyleSheet.js"></SCRIPT>
<%@ include file="/envoy/wizards/guidesJavascript.jspIncl" %>
<%@ include file="/envoy/common/warning.jspIncl" %>
<SCRIPT LANGUAGE="JavaScript">
    var needWarning = false;
    var objectName = "";
    var guideNode = "projects";
    var helpFile = "<%=bundle.getString("help_projects_main_screen")%>";

   
function enableButtons()
{
    if (ProjectForm.editBtn) {
        ProjectForm.editBtn.disabled = false;
    }
    if (ProjectForm.removeBtn) {
		ProjectForm.removeBtn.disabled = false;
    }
    <% if(b_calendaring) { %>
        if (ProjectForm.exportBtn) {
            ProjectForm.exportBtn.disabled = false;
        }
        if (ProjectForm.importBtn) {
            ProjectForm.importBtn.disabled = false;
        }
    <% } %>
}

function submitForm(selectedButton)
{
    var checked = false;
    var selectedRadioBtn = null;
    if (ProjectForm.radioBtn != null)
    {
        // If more than one radio button is displayed, the length attribute of
        // the radio button array will be non-zero, so find which one is checked
        if (ProjectForm.radioBtn.length)
        {
            for (i = 0; !checked && i < ProjectForm.radioBtn.length; i++)
            {
                if (ProjectForm.radioBtn[i].checked == true)
                {
                    checked = true;
                    selectedRadioBtn = ProjectForm.radioBtn[i].value;
                }
             }
        }
        // If only one is displayed, there is no radio button array, so
        // just check if the single radio button is checked
        else
        {
            if (ProjectForm.radioBtn.checked == true)
            {
                checked = true;
                selectedRadioBtn = ProjectForm.radioBtn.value;
            }
        }
    }
    // otherwise do the following
    if (selectedButton == 'New')
    {
        ProjectForm.action = "<%=newUrl%>";
        ProjectForm.submit();
        return;
    }
    else if (!checked)
    {
        alert("<%= bundle.getString("jsmsg_select_project") %>");
        return false;
    }

    values = selectedRadioBtn.split(",");
    if (selectedButton == 'Edit')
    {
        if (!<%=perms.getPermissionFor(Permission.PROJECTS_EDIT)%> && values[1] != "<%=pmName%>")
        {
            alert("<%=bundle.getString("jsmsg_cannot_edit_project") %>");
            return;
        }
        ProjectForm.action = "<%=editUrl %>";
    }
    else if (selectedButton == 'Import')
    {       
        if (!<%=perms.getPermissionFor(Permission.PROJECTS_IMPORT)%> && values[1] != "<%=pmName%>")
        {
            alert("<%=bundle.getString("jsmsg_cannot_import_project") %>");
            return;
        }
        ProjectForm.action = "<%=importSchedUrl%>" +
        "&<%=WebAppConstants.TM_ACTION%>=<%=WebAppConstants.TM_ACTION_IMPORT%>";
    }
    else if (selectedButton == 'Export')
    {
        if (!<%=perms.getPermissionFor(Permission.PROJECTS_EXPORT)%> && values[1] != "<%=pmName%>")
        {
            alert("<%=bundle.getString("jsmsg_cannot_export_project") %>");
            return;
        }
        ProjectForm.action = "<%=exportUrl%>" +
        "&<%=WebAppConstants.TM_ACTION%>=<%=WebAppConstants.TM_ACTION_EXPORT%>";
    }
    else if (selectedButton == 'Remove')
    {
        if (!<%=perms.getPermissionFor(Permission.PROJECTS_REMOVE)%> && values[1] != "<%=pmName%>")
        {
        	alert("<%=bundle.getString("jsmsg_cannot_remove_project") %>");
        	return;
        }
        ProjectForm.action = "<%=removeUrl%>";
    }

    ProjectForm.submit();
}
</SCRIPT>
<style type="text/css">
.list {
    border: 1px solid <%=skinbean.getProperty("skin.list.borderColor")%>;
}
</style>
</HEAD>
<BODY LEFTMARGIN="0" RIGHTMARGIN="0" TOPMARGIN="0" MARGINWIDTH="0" MARGINHEIGHT="0"
    ONLOAD="loadGuides()">
<%@ include file="/envoy/common/header.jspIncl" %>
<%@ include file="/envoy/common/navigation.jspIncl" %>
<%@ include file="/envoy/wizards/guides.jspIncl" %>
    <DIV ID="contentLayer" STYLE=" POSITION: ABSOLUTE; Z-INDEX: 9; TOP: 108; LEFT: 20px; RIGHT: 20px;">

<% if (error != null) {
	sessionManager.removeElement(WebAppConstants.PROJECT_ERROR);
%>
    <amb:header title="<%=title%>" helperText="<%=error%>" />
<%   } else {  %>
    <amb:header title="<%=title%>"/>
<% }  %>
    
    <form name="ProjectForm" method="post">
    <TABLE CELLPADDING=0 CELLSPACING=0 BORDER=0 CLASS="standardText">
      <TR VALIGN="TOP">
        <TD ALIGN="RIGHT">
          <amb:tableNav bean="projects" key="<%=ProjectMainHandler.PROJECT_KEY%>"
                     pageUrl="self" />
        </td>
      </tr>
      <tr>
        <td>
          <amb:table bean="projects" id="proj" key="<%=ProjectMainHandler.PROJECT_KEY%>"
           dataClass="com.globalsight.everest.projecthandler.ProjectInfo" pageUrl="self"
           emptyTableMsg="msg_no_projects" >
            <amb:column label="" width="20px">
              <input type="radio" name="radioBtn" onclick="enableButtons()"
                 value="<%=proj.getProjectId()%>">
            </amb:column>
            <amb:column label="lb_name" sortBy="<%=ProjectComparator.PROJECTNAME%>">
              <%=proj.getName()%>
            </amb:column>
            <amb:column label="lb_project_manager"
             sortBy="<%=ProjectComparator.PROJECTMANAGER%>">
              <%=proj.getProjectManagerName()%>
            </amb:column>
            <amb:column label="lb_termbase" sortBy="<%=ProjectComparator.TERMBASE%>">
              <% out.print(proj.getTermbaseName() == null ? "" : proj.getTermbaseName()); %>
            </amb:column>
            <% if (isSuperAdmin) { %>
            <amb:column label="lb_company_name" sortBy="<%=ProjectComparator.ASC_COMPANY%>">
              <%=ServerProxy.getJobHandler().getCompanyById(Long.parseLong(proj.getCompanyId())).getCompanyName()%>
            </amb:column>
            <% } %>
          </amb:table>
</TD>
</TR>
</DIV>
<TR><TD>&nbsp;</TD></TR>

<TR>
<TD>
<DIV ID="DownloadButtonLayer" ALIGN="RIGHT" STYLE="visibility: visible">
    <P>
    <%if(b_calendaring) { %>
<amb:permission name="<%=Permission.PROJECTS_IMPORT%>" >
    <INPUT TYPE="BUTTON" VALUE="<%=importButton%>" onClick="submitForm('Import');"
        name="importBtn" disabled />
</amb:permission>
<amb:permission name="<%=Permission.PROJECTS_EXPORT%>" >
    <INPUT TYPE="BUTTON" VALUE="<%=exportButton%>" onClick="submitForm('Export');"
        name="exportBtn" disabled />
</amb:permission>
    <% } %>
<amb:permission name="<%=Permission.PROJECTS_EDIT%>" >
    <INPUT TYPE="BUTTON" VALUE="<%=editButton%>" onClick="submitForm('Edit');"
        name="editBtn" disabled />
</amb:permission>
<amb:permission name="<%=Permission.PROJECTS_NEW%>" >
    <INPUT TYPE="BUTTON" VALUE="<%=newButton%>" onClick="submitForm('New');" />
</amb:permission>
<amb:permission name="<%=Permission.PROJECTS_REMOVE%>" >
    <INPUT TYPE="BUTTON" VALUE="<%=removeButton%>" onClick="submitForm('Remove');"
    	name="removeBtn" disabled />
</amb:permission>
</DIV>
</TD>
</TR>
</TABLE>
</FORM>
</BODY>
</HTML>
