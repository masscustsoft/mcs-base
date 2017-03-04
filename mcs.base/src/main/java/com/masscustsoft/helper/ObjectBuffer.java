package com.masscustsoft.helper;

import java.util.Map;
import java.util.TreeMap;

public class ObjectBuffer {
	StringBuffer o=new StringBuffer();
	boolean comma=false;
	String prefix,postfix;
	boolean isArray;
	Map<String,ObjectBuffer> map=new TreeMap<String,ObjectBuffer>();
	
	public ObjectBuffer(String prefix,String postfix){
		if (prefix==null) prefix="";
		if (postfix==null) postfix="";
		isArray=prefix.endsWith("["); 
		this.prefix=prefix;
		this.postfix=postfix;
	}
	
	public ObjectBuffer getItem(String name, String prefix, String postfix){
		ObjectBuffer val=map.get(name);
		if (val==null){
			val=new ObjectBuffer(prefix,postfix);
			map.put(name, val);
		}
		return val;
	}
	
	public void append(String val, String text){
		if (val==null) return;
		if (comma) o.append(",");
		o.append(text.replace("{0}", val));
		comma=true;
	}
	
	public void append(Boolean val, String text){
		if (val==null) return;
		append(val.toString(),text);
	}
	
	public void append(Integer val, String text){
		if (val==null) return;
		append(val.toString(),text);
	}
	
	public void append(Double val, String text){
		if (val==null) return;
		append(val.toString(),text);
	}
	
	@Override
	public String toString(){
		for (String name:map.keySet()){
			ObjectBuffer cli=map.get(name);
			append("",name+":"+cli.toString());
		}
		return prefix+o+postfix;
	}
}
