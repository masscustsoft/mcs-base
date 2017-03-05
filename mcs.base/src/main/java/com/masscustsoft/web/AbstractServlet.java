package com.masscustsoft.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.masscustsoft.helper.Upload;
import com.masscustsoft.util.CacheUtil;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.ThreadHelper;
import com.masscustsoft.xml.BeanFactory;

public class AbstractServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	protected static List<String> servlets=new ArrayList<String>();
    
	protected BeanFactory beanFactory;
	
	@Override
	public void init() throws ServletException {
		super.init();

		String root=this.getInitParameter("root");
		System.out.println("servlet init..."+getClass().getName()+", cpath="+this.getServletContext().getContextPath()+", name="+this.getServletName()+", root="+root);
		if (root!=null){
			if (!servlets.contains(root)) servlets.add(root);
		}
		
		beanFactory=BeanFactory.initBeanFactory(getInitParameter("beanFactoryId"));
    }
    
	private void parseContextPath(HttpServletRequest req){
    	String croot=this.getServletContext().getContextPath();
    	String uri0=req.getRequestURI();
    	uri0=uri0.substring(croot.length());
    	int len=0;
    	String uri=uri0,cpath="";
    	for (String serv:servlets){
    		if (uri0.startsWith(serv)){
    			if (len<serv.length()){
    				len=serv.length();
    				cpath=serv;
    				uri=uri0.substring(serv.length());
    			}
    		}
    	}
    	if (cpath.equals("/")) cpath="";
    	//if (!uri.startsWith("/")) uri="/"+uri;
    	ThreadHelper.set("relativePath",uri);
    	ThreadHelper.set("contextPath",croot+cpath);
    	ThreadHelper.set("contextRelativePath",cpath);
    	//System.out.println("uri="+uri+",cpath="+cpath+", cRoot="+croot);
    }
    
	//DAVServlet need the options to work
	protected boolean allowsCheck(HttpServletRequest req, HttpServletResponse resp) throws IOException{
		resp.setHeader("Access-Control-Allow-Origin", "*");
    	//resp.setHeader("Access-Control-Allow-Method","POST， GET, OPTIONS");
    	resp.setHeader("Access-Control-Allow-Headers", "sessionId, X-Requested-With, Content-Type, Accept, x-cors-access, CRC");
    	
    	if (req.getMethod().equals("OPTIONS")){
			return false;
		}
		if (req.getMethod().equals("TRACE")){
			resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
			return false;
		}
		return true;
	}
	
	final protected void service(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {
    	
    	CacheUtil.clear();
    	ThreadHelper.set("beanFactory", beanFactory);
    	
    	if (allowsCheck(req,resp)==false) return;

    	ThreadHelper.set("servlet", this);
    	
    	req.setCharacterEncoding(LightUtil.UTF8);
    	resp.setCharacterEncoding(LightUtil.UTF8);
	
    	parseContextPath(req);
    	
    	try{
    		if (prepare(req,resp)) super.service(req, resp);
    	}
    	catch(Exception e){
    		e.printStackTrace();
    		resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
    		return;
    	}
    	finally{
    		Upload up=Upload.getUpload();
    		if (up!=null) up.destroy();
    		CacheUtil.clear();
    	}
	}

	//return true terminates
	protected boolean  prepare(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		return false;
	}
}
