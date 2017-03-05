package com.masscustsoft.api;

import java.io.InputStream;
import java.util.Collection;

public interface IRepository {
	public String getFsId();
	public Boolean getExternal();
	public long getLastModified(String name) throws Exception;
	public Boolean getRestricted();
	public boolean isCached();
	public boolean isRestricted();
	
	public InputStream getResource(String name) throws Exception;
	public String saveResource(String name,InputStream is) throws Exception;
	public void removeResource(String name) throws Exception;
	public Collection<String> listResources(String folder, String ext) throws Exception;
	public void destroy() throws Exception;
	public void setFsId(String fsId);
	public void initialize();
	public void saveXml(String beanName, String xml) throws Exception;
	public String getXml(String name) throws Exception;
	public String existResource(String name) throws Exception;
	public void setFolderName(String folderName);
	public String getFolderName();
}
