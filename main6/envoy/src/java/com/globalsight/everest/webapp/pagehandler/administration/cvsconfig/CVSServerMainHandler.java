package com.globalsight.everest.webapp.pagehandler.administration.cvsconfig;

import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Vector;

import javax.jms.JMSException;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hibernate.HibernateException;

import com.globalsight.cxe.engine.util.FileUtils;
import com.globalsight.everest.cvsconfig.CVSConfigException;
import com.globalsight.everest.cvsconfig.CVSModule;
import com.globalsight.everest.cvsconfig.CVSRepository;
import com.globalsight.everest.cvsconfig.CVSServer;
import com.globalsight.everest.cvsconfig.CVSServerManagerLocal;
import com.globalsight.everest.cvsconfig.CVSUtil;
import com.globalsight.everest.servlet.EnvoyServletException;
import com.globalsight.everest.servlet.util.SessionManager;
import com.globalsight.everest.util.comparator.CVSServerComparator;
import com.globalsight.everest.util.comparator.CompanyComparator;
import com.globalsight.everest.webapp.WebAppConstants;
import com.globalsight.everest.webapp.pagehandler.PageHandler;
import com.globalsight.everest.webapp.webnavigation.WebPageDescriptor;
import com.globalsight.log.GlobalSightCategory;
import com.globalsight.util.GeneralException;

public class CVSServerMainHandler extends PageHandler {
	private CVSServerManagerLocal manager = new CVSServerManagerLocal();
    private static final GlobalSightCategory logger = (GlobalSightCategory)GlobalSightCategory.getLogger(CVSServerMainHandler.class.getName());

	public void invokePageHandler(WebPageDescriptor p_pageDescriptor,
			HttpServletRequest p_request, HttpServletResponse p_response,
			ServletContext p_context) throws ServletException, IOException,
			EnvoyServletException {
		try {
			HttpSession session = p_request.getSession(false);
			SessionManager sessionMgr = (SessionManager) session.getAttribute(WebAppConstants.SESSION_MANAGER);
			
			ResourceBundle bundle = getBundle(session);

			String action = p_request.getParameter("action");
			if (CVSConfigConstants.CREATE.equals(action)) {
				//Add new CVS server configuration
				createCVSServer(p_request); 
			} else if (CVSConfigConstants.UPDATE.equals(action)) {
				updateCVSServer(p_request);
			} else if (CVSConfigConstants.REMOVE.equals(action)) {
				removeCVSServer(p_request);
			} else {
				try {
					List servers = (List)manager.getAllServer();
					sessionMgr.setAttribute(CVSConfigConstants.CVS_SERVERS, servers);
				} catch (Exception e) {
					System.out.println(e.toString());
				}
			}
			if (!CVSUtil.isCVSEnabled()) {
				sessionMgr.setAttribute("cvsmsg", bundle.getString("msg_no_exec_cvs"));
			}
			dataForTable(p_request, session);
			
		} catch (NamingException ne)
	        {
	            throw new EnvoyServletException(EnvoyServletException.EX_GENERAL, ne);
	        }
	        catch (RemoteException re)
	        {
	            throw new EnvoyServletException(EnvoyServletException.EX_GENERAL, re);
	        }
	        catch (GeneralException ge)
	        {
	            throw new EnvoyServletException(EnvoyServletException.EX_GENERAL, ge);
	        }
	        catch (HibernateException e)
	        {
	            throw new EnvoyServletException(e);
	        }

	        super.invokePageHandler(p_pageDescriptor, p_request, p_response, p_context);
	}

    private void dataForTable(HttpServletRequest p_request, HttpSession p_session) throws RemoteException, NamingException, GeneralException
	{
	    Vector servers = vectorizedCollection(manager.getAllServer());
	    Locale uiLocale = (Locale)p_session.getAttribute(
	                                WebAppConstants.UILOCALE);
	
	    setTableNavigation(p_request, p_session, servers,
	                   new CVSServerComparator(uiLocale),
	                   10,
	                   CVSConfigConstants.CVS_SERVER_LIST, CVSConfigConstants.CVS_SERVER_KEY);
	}

    private void createCVSServer(HttpServletRequest p_request) throws CVSConfigException, RemoteException {
		CVSServerManagerLocal manager = new CVSServerManagerLocal();
		CVSServer server = new CVSServer();
		String name = p_request.getParameter(CVSConfigConstants.SERVER_NAME);
		if (!manager.isServerExist(name)) {
			server.setName(name);
			server.setHostIP(p_request.getParameter(CVSConfigConstants.HOST_IP));
			String hostPort = p_request.getParameter(CVSConfigConstants.HOST_PORT);
			try {
				server.setHostPort(Integer.parseInt(hostPort));
			} catch (NumberFormatException nfe) {
				server.setHostPort(2401);
			}
			String protocol = p_request.getParameter(CVSConfigConstants.PROTOCOL);
			try {
				server.setProtocol(Integer.parseInt(protocol));
			} catch (Exception e) {
				server.setProtocol(0);
			}
			server.setSandbox(p_request.getParameter(CVSConfigConstants.SANDBOX));
			String repositoryName = p_request.getParameter(CVSConfigConstants.REPOSITORY_CVS);
			String loginUser = p_request.getParameter(CVSConfigConstants.CVS_REPOSITORY_LOGIN_USER);
			String loginPwd = p_request.getParameter(CVSConfigConstants.CVS_REPOSITORY_LOGIN_PASSWORD);
			server.setRepository(repositoryName);
			server.setLoginUser(loginUser);
			server.setLoginPwd(loginPwd);
			protocol = server.getProtocol() == 0 ? ":pserver:" : ":ext:";
	        StringBuilder sb = new StringBuilder(protocol);
	        sb.append(loginUser);
	        if (server.getProtocol()==0) {
	        	sb.append(":").append(loginPwd);
	        }
	        sb.append("@").append(server.getHostIP()).append(":");
	        if (repositoryName.charAt(0) != '/')
	        	sb.append("/");
        	sb.append(repositoryName);
	        server.setCVSRootEnv(sb.toString());

			manager.addServer(server);
		}
	}
    
    private void updateCVSServer(HttpServletRequest p_request) throws CVSConfigException, RemoteException {
    	CVSServer server = null;
    	HttpSession session = null;
    	try {
    		session = p_request.getSession(false);
			SessionManager sessionMgr = (SessionManager) session.getAttribute(WebAppConstants.SESSION_MANAGER); 
			server = (CVSServer)sessionMgr.getAttribute(CVSConfigConstants.CVS_SERVER);
			
			//set parameter
			server.setName(p_request.getParameter(CVSConfigConstants.SERVER_NAME));
			server.setHostIP(p_request.getParameter(CVSConfigConstants.HOST_IP));
			server.setHostPort(Integer.parseInt(p_request.getParameter(CVSConfigConstants.HOST_PORT)));
			server.setProtocol(Integer.parseInt(p_request.getParameter(CVSConfigConstants.PROTOCOL)));
			server.setSandbox(p_request.getParameter(CVSConfigConstants.SANDBOX));

			String repositoryName = p_request.getParameter(CVSConfigConstants.REPOSITORY_CVS);
			String loginUser = p_request.getParameter(CVSConfigConstants.CVS_REPOSITORY_LOGIN_USER);
			String loginPwd = p_request.getParameter(CVSConfigConstants.CVS_REPOSITORY_LOGIN_PASSWORD);
			server.setRepository(repositoryName);
			server.setLoginUser(loginUser);
			server.setLoginPwd(loginPwd);
			String protocol = server.getProtocol() == 0 ? ":pserver:" : ":ext:";
	        StringBuilder sb = new StringBuilder(protocol);
	        sb.append(loginUser);
	        if (server.getProtocol()==0) {
	        	sb.append(":").append(loginPwd);
	        }
	        sb.append("@").append(server.getHostIP()).append(":");
	        if (repositoryName.charAt(0) != '/')
	        	sb.append("/");
	        sb.append(repositoryName);
	        server.setCVSRootEnv(sb.toString());

			CVSServerManagerLocal manager = new CVSServerManagerLocal();
			manager.updateServer(server);
		} catch (Exception e) {
			throw new EnvoyServletException(EnvoyServletException.EX_GENERAL, e);
		}
    }
    
    private void removeCVSServer(HttpServletRequest p_request) throws CVSConfigException, RemoteException {
    	try {
    		HttpSession session = p_request.getSession(false);
			SessionManager sessionMgr = (SessionManager) session.getAttribute(WebAppConstants.SESSION_MANAGER);
			ResourceBundle bundle = getBundle(session);
			
			CVSServerManagerLocal manager = new CVSServerManagerLocal();
			long id = Long.parseLong(p_request.getParameter("id"));
			CVSServer server = manager.getServer(id);
			ArrayList<String> existModules = new ArrayList<String>();
			for (CVSModule m : server.getModuleSet()) {
				if (m.isActive()) {
					existModules.add(m.getName());
				}
			}
			Collections.sort(existModules);
			if (existModules.size()>0) {
				sessionMgr.setAttribute("cvsmsg", bundle.getString("msg_cnd_remove_server"));
				sessionMgr.setAttribute("existModules", existModules);
				return;
			}
				
			String sandbox = manager.getServer(id).getSandbox();
			manager.removeServer(id);

			//Delete the sandbox
			FileUtils.deleteAllFilesSilently(CVSUtil.getBaseDocRoot().concat(sandbox)); 
		} catch (Exception e) {
			throw new EnvoyServletException(EnvoyServletException.EX_GENERAL, e);
		}
    }

}
