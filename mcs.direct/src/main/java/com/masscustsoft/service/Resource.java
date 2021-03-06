package com.masscustsoft.service;

import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import com.masscustsoft.api.IRepository;
import com.masscustsoft.helper.Upload;
import com.masscustsoft.model.AbstractResult;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.StreamUtil;

public class Resource extends DirectAction {
	
	@Override
	protected void run(AbstractResult ret) throws Exception {
		Upload up=Upload.getUpload();
		String f = requiredStr("f");
		ret.setType(AbstractResult.ResultType.Stream);
		System.out.println("RESOURCE "+up.getRequest().getMethod()+" f="+f+", locale="+up.getRequest().getLocale().toString());
		
		IRepository fs=null;
		int i=f.indexOf("/");
		if (i>0){
			String rep=f.substring(0, i);
			fs=this.getCfg().getBeanFactory().getRepository(rep);
			if (fs!=null) f=f.substring(i+1);
		}
		if (fs==null) fs=this.getCfg().getBeanFactory().getRepository(this.getCfg().getResourceId());
		
		HttpServletResponse resp = up.getResponse();
		resp.setContentType(Upload.getFileContentType(f));
		
		Map<String,Object> map=Upload.getUpload().getFieldMap();
		map.put("resourceId", this.getCfg().getResourceId());
		map.put("versionId", this.getCfg().getAppVersion());
		map.put("appTitle", this.getCfg().getAppTitle());
		map.put("appDebug", f.contains("-debug")?"-debug":"");
		map.put("locale", getLocale());
		
		if (f.equals("index.html")||f.equals("index-debug.html")){
			LightUtil.noCache(resp);
			InputStream gs=fs.getResource("index.json");
			StringBuffer buf=new StringBuffer();
			StreamUtil.loadStream(gs, buf, LightUtil.UTF8);
			gs.close();
			String st=LightUtil.macro(buf.toString(), '$', map).toString();
			map.put("resourceCfg",st);
			f="index.html";
		}
		else {
			LightUtil.doCache(resp);
		}
		
		if (f.equals("index-debug.json")) f="index.json";
		
		InputStream is = fs.getResource(f);
		if (f.endsWith(".css")||f.endsWith(".js")||f.endsWith(".html")||f.endsWith(".svg")||f.endsWith(".json")){
			StringBuffer buf=new StringBuffer();
			StreamUtil.loadStream(is, buf, LightUtil.UTF8);
			
			
			String st=LightUtil.macro(buf.toString(), '$', map).toString();
			PrintWriter w = resp.getWriter();
			w.append(st);
			w.close();
		}
		else
		StreamUtil.copyStream(is, resp.getOutputStream(), 0);
		is.close();
	}
}
