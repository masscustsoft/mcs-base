package com.masscustsoft.service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.masscustsoft.util.TempUtil;

public class FileTempItem extends TempItem {
	File temp;
	boolean keepFile=false;
	
	public FileTempItem(String name){
		this.name=name; 
		if (this.name==null) name=TempUtil.getTempFileName();
	}
	
	public FileTempItem(FileTempService ts, String name){
		this(name);
		File root=ts.getRoot();
		root.mkdirs();
		temp = new File(root,name);
		keepFile=true;
	}

	public FileTempItem(File f){
		this(f.getName());
		temp = f;
		this.keepFile=true;
	}
	
	@Override
	public OutputStream getOutputStream() throws IOException {
		return new FileOutputStream(temp);
	}
	
	@Override
	public InputStream getInputStream() throws IOException{
		BufferedInputStream is=new BufferedInputStream(new FileInputStream(temp));
		return is;
	}
	
	@Override
	public long length(){
		return temp.length();
	}
	
	@Override
	public void delete(){
		if (keepFile==false) temp.delete();
	}
	
	@Override
	public String toString(){
		return temp.getAbsolutePath()+",exist="+temp.exists();
	}
	
	public File getTempFile(){
		return temp;
	}
}
