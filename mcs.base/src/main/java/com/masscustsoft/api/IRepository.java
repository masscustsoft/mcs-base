package com.masscustsoft.api;

import java.io.InputStream;

public interface IRepository {
	public String getFsId();
	public Boolean getExternal();
	public long getLastModified(String name) throws Exception;
	public Boolean getRestricted();
	public boolean isCached();
	public InputStream getResource(String name) throws Exception;
	public boolean isRestricted();
}
