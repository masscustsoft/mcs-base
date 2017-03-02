package com.masscustsoft.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

import com.masscustsoft.util.LightUtil;

public class RamTempItem extends TempItem {

	ByteArrayOutputStream buf=new ByteArrayOutputStream();
	RamTempService temp;
	
	public RamTempItem(String name){
		this.name=name; 
		if (this.name==null) name=getTempName();
	}
	
	public RamTempItem(RamTempService temp, String name){
		this(name);
		this.temp=temp;
	}
	
	@Override
	public OutputStream getOutputStream() throws FileNotFoundException {
		return buf;
	}

	@Override
	public InputStream getInputStream() throws FileNotFoundException{
		return new ByteArrayInputStream(buf.toByteArray());
	}
	
	@Override
	public long length(){
		return buf.toByteArray().length;
	}

	@Override
	public void delete(){
		temp.deleteTempItem(this);
	}
	
	public byte[] getBytes(){
		return buf.toByteArray();
	}

	private String getTempName(){
		Random r=new Random();
		String prefix="tmp";
		return prefix+"_" + LightUtil.getCalendar().getTime().getTime() + r.nextInt(1000)+".tmp";
	}

	
}
