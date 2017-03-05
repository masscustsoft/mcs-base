package com.masscustsoft.service;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import com.masscustsoft.api.IRepository;
import com.masscustsoft.util.LightStr;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.LogUtil;
import com.masscustsoft.util.ReflectUtil;
import com.masscustsoft.util.StreamUtil;

/**
 * File storage service.
 * 
 * @author JSong
 *
 */
public class Repository implements IRepository{
	/**
	 * A unique Id which will register to BeanFactory once created.
	 */
	protected String fsId;
	
	/**
	 * Flag to set if this Repository is session-sensitive, default is true, you must set to false if you want it be expose publicly.
	 */
	protected Boolean restricted; //set if session-sensitive.
	
	/**
	 * Flag to indicate if allow browser to cache this resource, by default it's true. 
	 */
	protected Boolean cache;
	
	/**
	 * The working folder name.
	 */
	protected String folderName;
	
	/**
	 * By default true, will be converted via the resource processors, you can set to false to skip the conversion.
	 */
	protected Boolean external;
	
	private String parentFsId;
	
	public void initialize(){
		if (fsId==null) {
			fsId="defaultFs";
			if (LightUtil.getBeanFactory().getRepository(fsId)!=null){
				LogUtil.error("fsId `"+fsId+"` defined twice!",new Throwable());
			}
		}
		LightUtil.getBeanFactory().addRepository(fsId, this);
		System.out.println("add Repository "+fsId+", "+LightUtil.getBeanFactory().getId());
	}
	
	public String getFsId() {
		return fsId;
	}

	public void setFsId(String fsId) {
		this.fsId = fsId;
		
	}

	@Override
	public final String getXml(String name) throws Exception{
		InputStream res=getResource(name+".xml");
		StringBuffer sb=new StringBuffer();
		try {
			InputStream is=new BufferedInputStream(res);
			StreamUtil.loadStream(is, sb, LightUtil.UTF8);
			is.close();
		} catch (Exception e) {
			LogUtil.info("getXml:"+name+" Err:"+e);
		}
		return sb.toString();
	}
	
	@Override
	public final void saveXml(String name,String xml) throws Exception{
		ByteArrayInputStream is = new ByteArrayInputStream(xml.getBytes(LightUtil.UTF8));
		saveResource(name+".xml",is);
	}
	
	@Override
	public final String existResource(String name) throws Exception{
		String res=_existResource(name);
		if (LightStr.isEmpty(res) && !LightStr.isEmpty(parentFsId)){
			//Map<String,Object> map=(Map)LightUtil.toJsonObject(LightUtil.getCfg(),128);
			//String pId=LightUtil.macro(parentFsId, '%', map).toString();
			String pid=parentFsId;
			if (parentFsId.equals("@resourceId")){
				pid=(String)ReflectUtil.getProperty(LightUtil.getCfg(),"resourceId");
			}
			IRepository fs = LightUtil.getBeanFactory().getRepository(pid);
			if (fs!=null) return fs.existResource(name);
		}
		return res;
	}
	
	public String _existResource(String name) throws Exception{
		throw new IOException("Not support!");
	}
	
	public final InputStream getResource(String name) throws Exception{
		InputStream is=_getResource(name);
		if (is==null && !LightStr.isEmpty(parentFsId)){
			String pid=parentFsId;
			if (parentFsId.equals("@resourceId")){
				pid=(String)ReflectUtil.getProperty(LightUtil.getCfg(),"resourceId");
			}
			IRepository fs = LightUtil.getBeanFactory().getRepository(pid);
			if (fs!=null) return fs.getResource(name);
		}
		return is;
	}
	
	public InputStream _getResource(String name) throws Exception{
		throw new IOException("Not support!");
	}
	
	//return path info
	public String saveResource(String name,InputStream is) throws Exception{
		throw new IOException("Not support!");
	}
	
	public void removeResource(String name) throws Exception{
		throw new IOException("Not support!");
	}
	
	public Collection<String> listResources(String folder, String ext) throws Exception{
		throw new IOException("Not support!");
	}
	
	public Boolean getRestricted() {
		return restricted;
	}

	public void setRestricted(Boolean restricted) {
		this.restricted = restricted;
	}

	public Boolean getCache() {
		return cache;
	}

	public void setCache(Boolean cache) {
		this.cache = cache;
	}
	
	public boolean isCached(){
		return cache==null || cache;
	}

	public long getLastModified(String name) throws Exception{
		return LightUtil.bootupTime;
	}

	public String getFolderName() {
		return folderName;
	}

	public void setFolderName(String folderName) {
		this.folderName = folderName;
	}

	@Override
	public void destroy() throws Exception{
		
	}

	/**
	 * Not set as same as set to true
	 * @return
	 */
	public boolean isRestricted() {
		return (restricted==null|| restricted);
	}

	public Boolean getExternal() {
		return external;
	}

	public void setExternal(Boolean external) {
		this.external = external;
	}

	public String getParentFsId() {
		return parentFsId;
	}

	public void setParentFsId(String parentFsId) {
		this.parentFsId = parentFsId;
	}
}
