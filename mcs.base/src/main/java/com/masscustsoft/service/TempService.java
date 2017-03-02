package com.masscustsoft.service;

import java.io.IOException;

public class TempService{

	protected String rootFolder;
	
	public TempItem newTempItem(String name) throws IOException{
		return null;
	}

	public TempItem getTempItem(String name) throws IOException{
		return null;
	}
	
	public void clear() throws IOException{
	}

	public String getRootFolder() {
		return rootFolder;
	}

	public void setRootFolder(String rootFolder) {
		this.rootFolder = rootFolder;
	}
}
