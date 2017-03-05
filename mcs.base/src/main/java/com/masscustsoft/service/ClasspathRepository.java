package com.masscustsoft.service;

import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Properties;

import com.masscustsoft.Lang.CLASS;
import com.masscustsoft.util.LightStr;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.StreamUtil;

public class ClasspathRepository extends Repository {
	String prefix="";

	transient Properties lastModi=null;
	
	public ClasspathRepository(){
	}

	@Override
	public InputStream _getResource(String name) throws Exception {
		InputStream is=CLASS.getResourceAsStream(prefix+name);
		return is;
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		prefix=prefix.replace(".", "/");
		if (!prefix.endsWith("/")) prefix+="/";
		this.prefix = prefix;
	}
	
	@Override
	public String _existResource(String name) throws Exception{
		if (CLASS.getResource(prefix+name)!=null) return name;
		return null;
	}
	
	@Override
	public Collection<String> listResources(String folder, String ext) throws Exception{
		String pat=folder+".*";
		if (!LightStr.isEmpty(ext)) pat+=ext.replace(".", "\\.");
		Collection<String> list = StreamUtil.getResources(pat);	
		return list;
	}
	
	@Override
	public long getLastModified(String name) throws Exception{
		if (lastModi==null){
			lastModi=new Properties();
			ClassLoader cl = CLASS.getLoader();
			Enumeration<URL> all = cl.getResources("META-INF/CRC-"+prefix+".MAPPING");
			for (;all.hasMoreElements();){
				URL u=all.nextElement();
				InputStream is = u.openStream();
				Properties p=new Properties();
				p.load(is);
				is.close();
				lastModi.putAll(p);
			}
		}
		String ver=lastModi.getProperty(prefix+name);
		if (LightStr.isEmpty(ver)) return LightUtil.bootupTime; 
		return LightUtil.decodeLong(ver);
	}
}
