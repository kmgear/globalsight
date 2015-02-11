<%@ page contentType="text/html; charset=UTF-8" errorPage="/envoy/common/error.jsp"
         import="java.util.ArrayList,
         		 java.text.SimpleDateFormat,
         		 java.util.Date,
         		 java.util.ResourceBundle,
                 com.globalsight.everest.webapp.pagehandler.PageHandler,
         	     com.globalsight.reports.Constants" session="true"
%>
<%
	SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	Date now = new Date();
    long yest = now.getTime() - (long) (24 * 60 * 60 * 1000);
    Date yesterday = new Date(yest);
    ResourceBundle bundle = PageHandler.getBundle(session);
%>
<html>
<head>
    <title><%=bundle.getString("report_parameters")%></title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <script language="JavaScript" src="/globalsight/includes/report/calendar.js"></script>
    <script language="JavaScript">
            function showCalendar1() {
                var cal1 = new calendar2(document.forms['request'].elements['startDate']);
                cal1.year_scroll = true;
                cal1.time_comp = true;
                cal1.popup();
            }
            function showCalendar2() {
                var cal2 = new calendar2(document.forms['request'].elements['endDate']);
                cal2.year_scroll = true;
                cal2.time_comp = true;
                cal2.popup();
            }
    </script>
</head>

<body bgcolor="#FFFFFF" text="#000000" leftmargin="0" topmargin="0" marginwidth="0" marginheight="0">
    <table border="0" cellspacing="0" cellpadding="5" height="543" width="100%">
    <tr bgcolor="#ABB0D3" valign="top"> 
        <td height="40" colspan="2"><b><font face="Verdana, Arial, Helvetica, sans-serif"><%=bundle.getString("report")%></font></b>: 
        <font face="Verdana, Arial, Helvetica, sans-serif"><%=bundle.getString("term_audit")%></font></td>
    </tr>
    <tr bgcolor="ccffff"> 
        <td height="6" colspan="2"></td>
    </tr>
    <tr> 
        <td height="452" valign="top" align="left" width="30%" background="/globalsight/images/parambar.jpg"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"></font></td>
        <td height="452" valign="top" align="left" width="70%" bgcolor="#E9E9E9"><font face="Verdana, Arial, Helvetica, sans-serif" size="3">
    <FORM name=request action="/globalsight/TranswareReports?reportPageName=TermAudit&act=create" method=POST>
        <CENTER><TABLE BORDER=0 CELLSPACING=10>
        <TR>
            <TD ALIGN=RIGHT><%=(String)request.getAttribute(Constants.PARAM_STARTDATE_LABEL)%></TD>
            <TD><INPUT type=text name="startDate" value="<%=(String)request.getAttribute(Constants.PARAM_STARTDATE)%>" READONLY></TD>
            <TD><IMG style='cursor:hand' align=top border=0 src="/globalsight/includes/Calendar.gif"  onclick="showCalendar1()"></td>
        </TR><TR>
            <TD ALIGN=RIGHT><%=(String)request.getAttribute(Constants.PARAM_ENDDATE_LABEL)%></TD>
            <TD><INPUT type=text name="endDate" value="<%=(String)request.getAttribute(Constants.PARAM_ENDDATE)%>" READONLY></TD>
            <TD><IMG style='cursor:hand' align=top border=0 src="/globalsight/includes/Calendar.gif"  onclick="showCalendar2()"></td>
        </TR><TR>
            <TD ALIGN=RIGHT><%=(String)request.getAttribute(Constants.PARAM_LANGUAGE_LABEL)%></TD>
            <TD>
            	<SELECT name="selectedLang">
                <% ArrayList termbaseLangs = (ArrayList)request.getAttribute(Constants.PARAM_LANGUAGE);
                   ArrayList termbaseLangLabels = (ArrayList)request.getAttribute(Constants.PARAM_LANGUAGE_LABELS);
                   for(int i=0; i<termbaseLangs.size(); i++) {
                       if(i==0) {
                %>
            	<OPTION value="<%=(String)termbaseLangs.get(i)%>" selected><%=(String)termbaseLangLabels.get(i)%></option>
            	<% }
            	   else {
            	%>
            	<OPTION value="<%=(String)termbaseLangs.get(i)%>"><%=(String)termbaseLangLabels.get(i)%></option>
            	<% }
            	 }
            	%>
            </SELECT></TD>
         </TR><TR>
            <TD colspan=2><CENTER><input type=submit value="<%=bundle.getString("lb_shutdownSubmit")%>" >
            &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
            <input type=button value="<%=bundle.getString("lb_cancel")%>" onclick="window.close()"></CENTER></TD>
          </TR></TABLE>
      </CENTER>
      </FORM></font></td>
  </tr>
  <tr> 
    <td width="27%">&nbsp;</td>
    <td width="73%">&nbsp;</td>
  </tr>
</table>
</body>
</html>
