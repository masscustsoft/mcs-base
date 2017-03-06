package com.masscustsoft.web;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.masscustsoft.api.IDataService;
import com.masscustsoft.helper.Upload;
import com.masscustsoft.model.AbstractResult;
import com.masscustsoft.model.AbstractResult.ResultType;
import com.masscustsoft.model.DeviceLogin;
import com.masscustsoft.model.JsonResult;
import com.masscustsoft.service.DirectAction;
import com.masscustsoft.service.DirectConfig;
import com.masscustsoft.service.DirectData;
import com.masscustsoft.service.DirectSession;
import com.masscustsoft.util.GlbHelper;
import com.masscustsoft.util.LightStr;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.MapUtil;
import com.masscustsoft.util.MessageUtil;
import com.masscustsoft.util.StreamUtil;
import com.masscustsoft.util.ThreadHelper;

public class DirectServlet extends AbstractServlet {
	private static final long serialVersionUID = 1L;

	DirectConfig directCfg;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		String direct = this.getServletName();
		
		String configFile = this.getInitParameter("config");
		if (configFile == null)
			configFile = direct;

		String appId=this.getServletContext().getContextPath();
		if (appId!=null && appId.startsWith("/")) appId=appId.substring(1);
		if (LightStr.isEmpty(appId)) appId="ROOT"; 
		
		String baseDir = getServletContext().getRealPath("/WEB-INF");
		beanFactory.addFileRepository(direct, baseDir, 0, true);

		try {
			// load prop to BF
			String propList = getInitParameter("propertyFileList");
			if (LightStr.isEmpty(propList)) {
				propList = configFile + ".properties";
			}
			Map vars=new HashMap();
			List<String> files = MapUtil.getSelectList(propList);
			for (String fn : files) {
				File f = new File(baseDir, fn.trim());
				Properties p = StreamUtil.loadProperties(f);
				Enumeration e = p.propertyNames();
				while (e.hasMoreElements()) {
					String key = (String) e.nextElement();
					vars.put(key, p.get(key));
				}
			}
			String cfgSiteUrl=(String)vars.get("CfgSiteUrl");
			try {
				Map m=LightUtil.loadFromCfgSite(cfgSiteUrl, appId);
				vars.putAll(m);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			// initaliize direct.xml
			directCfg = (DirectConfig) beanFactory.getBean(direct, configFile);
			directCfg.setId(LightUtil.encodeHashCode(direct));
			directCfg.setBeanFactory(beanFactory);
			GlbHelper.set("$DirectCfg$" + directCfg.getId(), directCfg);

			directCfg.getVars().putAll(vars);
			
			directCfg.initThread();
			
			directCfg.init();
			
			// try insert or remove/insert initial data if resetOnBoot if set or
			// non exist
			if (!LightStr.isEmpty(directCfg.getInitId())){
				List<DirectData> list=(List)beanFactory.getBean(direct, directCfg.getInitId());
				for (DirectData dd : list) {
					dd.init(directCfg);
					dd.updateTestData(directCfg);
				}
			}
			

			directCfg.initCluster();
			
			if (directCfg.getJobService()!=null){
				directCfg.getJobService().startJobs(directCfg);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	

	@Override
	protected boolean prepare(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		directCfg.initThread();

		Upload up = new Upload(req, resp);
		
		String action = up.getStr("action", "");
		String verb=(String)ThreadHelper.get(Upload.ATTRVERB);
		if (verb.endsWith(".appcache")){
			OutputStream os=resp.getOutputStream();
			Calendar c=LightUtil.getCalendar();
			c.setTimeInMillis(LightUtil.bootupTime);
			String ver=directCfg.getAppVersion();
			LightUtil.noCache(resp);
			StringBuffer buf=new StringBuffer("CACHE MANIFEST\n#Version: "+ver+"\nNETWORK:\n*\n");
			resp.setContentType("text/cache-manifest");
			StreamUtil.saveStream(os, buf, LightUtil.UTF8);
			return false;
		}
		
		up.parseContent();
		
		if (!verb.equals("") && action.equals("")){
			up.setStr("action", verb);
			action=verb;
			if (action.equals("index.html") || action.equals("index-debug.html")){
				up.setFilePath("index.html");
				action="resource";
			}
			else
			if (beanFactory.getRepository(action)!=null){
				up.setFilePath(up.getUri());
				action="resource";
			}
			else
			if (action.endsWith(".module")){
				String st=action.substring(0, action.length()-7);
				action="loadModule";
				up.setStr("moduleId",st);
			}
		}
		else
		if (action.equals("") && verb.equals("")&&up.getUri().equals("")){
			up.setFilePath("index.html");
			action="resource";
		}
		JsonResult ret = new JsonResult();
		try {
			DirectAction a = directCfg.doProcess(action, ret);
			if (a.getCacheable()!=null && a.getCacheable() && req.getMethod().equals("GET")) LightUtil.doCache(resp);
		} catch (Exception e) {
			e.printStackTrace();
			ret.setError(e);
		}
		writeOut(up, ret);
		return false;
	}
	
	private void writeOut(Upload up, AbstractResult pr) throws IOException {
		if (pr.getType() == ResultType.Stream)
			return;

		HttpServletResponse resp = up.getResponse();
		resp.setContentType("text/html"); // rather than text/json
		String callback = up.getStr("callback", "");

		PrintWriter writer = resp.getWriter();

		if (!LightStr.isEmpty(callback))
			writer.write(callback + "(");
		Map mm = (Map) LightUtil.toJsonObject(pr);
		if (pr.getAttributes().size() == 0)
			mm.remove("attributes");
		if (pr.getCats().size() == 0)
			mm.remove("cats");
		if (pr.getList().size() == 0)
			mm.remove("list");
		
		DirectSession ses=(DirectSession)ThreadHelper.get("$$session");
		IDataService ds = directCfg.getDs(null);
		if (ses!=null){
			try {
				DeviceLogin dl = ds.getBean(DeviceLogin.class, "deviceId", ses.getDeviceId());
				if (dl!=null && dl.getBadges()!=null && dl.getBadges().size()>0){
					mm.put("badges", new HashMap(dl.getBadges()));
					dl.getBadges().clear();
					ds.updateBean(dl);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			List<Map<String, Object>> news = MessageUtil.getNews(ses.getLastAccessTime().getTime());
			if (news.size()>0){
				mm.put("news",news);
			}
		}
		
		StringBuffer json = LightUtil.toJsonString(mm);
		writer.write(json.toString());
		if (!LightStr.isEmpty(callback))
			writer.write(");");
	}

	public DirectConfig getDirectCfg() {
		return directCfg;
	}

	public void setDirectCfg(DirectConfig directCfg) {
		this.directCfg = directCfg;
	}



	@Override
	public void destroy() {
		if (directCfg!=null && directCfg.getJobService()!=null){
			try {
				directCfg.getJobService().stopJobs();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		super.destroy();
	}

}
