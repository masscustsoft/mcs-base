package com.masscustsoft.api;

import java.io.InputStream;

public interface IFile {
	public String getName();
	public void setName(String name);
	public long getSize();
	public void setSize(long size);
	public String getExternalId();
	public void setExternalId(String externalPath);
	public InputStream getResource(String fsId0) throws Exception;
}
