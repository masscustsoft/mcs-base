package com.masscustsoft.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class TempItem {
	protected String name;
	
	public abstract OutputStream getOutputStream() throws IOException;
	public abstract InputStream getInputStream() throws IOException;
	public abstract long length();
	public abstract void delete();
	
	public byte[] getBytes() throws IOException{
		InputStream is=getInputStream();
		byte[] buf=new byte[(int)length()];
		is.read(buf);
		is.close();
		return buf;
	}
	
	@Override
	public void finalize(){
		delete();
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
}
