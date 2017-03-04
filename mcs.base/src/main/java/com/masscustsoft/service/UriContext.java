package com.masscustsoft.service;

import java.io.UnsupportedEncodingException;

import com.masscustsoft.api.IBeanFactory;
import com.masscustsoft.api.IRepository;
import com.masscustsoft.util.LightStr;
import com.masscustsoft.util.LightUtil;

public class UriContext{
	public IRepository repository;
	public String uri0; 	//  Req.requestURI(), 				/res/extjs4/app/Loader.js
	public String uri; 		//	ResourceName in Repository, 	extjs4/app/Loader.js
	public String path;		//									extjs4/app
	public String name;		//									Loader.js
	
	public UriContext(String uri0) throws UnsupportedEncodingException{
		this.uri0=uri0;
		String ss=LightStr.decodeUrl(uri0);
		
		IBeanFactory bf=LightUtil.getBeanFactory();
	    if (ss.startsWith("/")) ss=ss.substring(1);
    	if (ss.length()==0) ss="index.html";
    	//for sso
    	int idx=ss.indexOf("?");
    	if (idx>=0) ss=ss.substring(0,idx);
    	//
    	idx=ss.indexOf("/");
    	if (idx>0){
    		repository=bf.getRepository(ss.substring(0,idx));
    		if (repository!=null) ss=ss.substring(idx+1); //if rep exists, cut it
    	}
    	else repository=null;
    	
    	uri=ss;
    	idx=ss.lastIndexOf("/");
    	if (idx>0){
    		path=ss.substring(0,idx);
    		name=ss.substring(idx+1);
    	}
    	else {
    		path="";
    		name=ss;
    	}
	}
	
	public String getPkg(){
		String pkg=path; //for static resource, pkg=path, but for module-binded resource, only last part after the last $
		int i=pkg.lastIndexOf("$");
		if (i>=0) pkg=pkg.substring(i+1);
		return pkg;
	}
	
	public UriContext(UriContext ctx,String name){
		this(ctx,ctx.path,name);
	}
	
	public UriContext(UriContext ctx,String path,String name){
		repository=ctx.repository;
		this.name=name;
		this.path=path;
		if (path.length()>0) uri=path+"/"+name; else uri=name;
		uri0="/"+repository.getFsId()+"/"+uri;
	}
	
	public String getResId(){
		//System.out.println("ctx.key="+uri0);
		//if (uri0.endsWith(".jnlp")){
		//	return ServerUtil.getContextPath()+uri0;
		//}
		//else 
		return uri0;
	}
	
	public boolean isCachable(){
		if (isIndex()) return false;
		if (name.endsWith(".jnlp") || name.endsWith(".html")) return false;
		return true;
	}
	
	public boolean isIndex(){
		if ("index.js".equals(name)) return true;
		if ("index-debug.js".equals(name)) return true;
		if ("index.html".equals(name)) return true;
		if ("index-debug.html".equals(name)) return true;
		return false;
	}
}
