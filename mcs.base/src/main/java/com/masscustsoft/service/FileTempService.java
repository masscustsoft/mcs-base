package com.masscustsoft.service;

import java.io.File;
import java.io.IOException;

import com.masscustsoft.util.StreamUtil;
import com.masscustsoft.util.TempUtil;


public class FileTempService extends TempService {
	String rootFolder;
	
	public FileTempService(){
	}
	
	public FileTempService(String root){
		rootFolder=root;
	}
	
	@Override
	public FileTempItem newTempItem(String name){
		if (name==null) name=TempUtil.getTempFileName();
		return new FileTempItem(this, name);
	}
	
	public File getRoot(){
		String tmpdir=System.getProperty("java.io.tmpdir");
		File root=new File(tmpdir);
		if (rootFolder!=null) {
			if (rootFolder.contains("/") || rootFolder.contains("\\"))
				root=new File(rootFolder);
			else 
				root=new File(root,rootFolder);
		}
		return root;
	}
	
	@Override
	public TempItem getTempItem(String name) throws IOException{
		return new FileTempItem(new File(getRoot(),name));
	}
	
	@Override
	public void clear() throws IOException{
		if (rootFolder==null) return;
		File root=getRoot();
		StreamUtil.xdelete(root);
	}

	public String getRootFolder() {
		return rootFolder;
	}

	public void setRootFolder(String rootFolder) {
		this.rootFolder = rootFolder;
	}

}
