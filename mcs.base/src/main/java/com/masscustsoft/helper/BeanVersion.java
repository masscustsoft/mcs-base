package com.masscustsoft.helper;

public class BeanVersion {
	long lastModified;

	public BeanVersion(long tm){
		lastModified=tm;
	}
	
	public long getLastModified() {
		return lastModified;
	}

	public void setLastModified(long lastModified) {
		this.lastModified = lastModified;
	}
	
}
