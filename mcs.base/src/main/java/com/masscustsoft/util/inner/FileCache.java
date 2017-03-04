package com.masscustsoft.util.inner;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class FileCache {

	private static Map<String,String> crcs=Collections.synchronizedMap(new HashMap<String,String>());
	
	public void reset() throws Exception{
		crcs.clear();
	}

	public String getCrc(String fn){
		return crcs.get(fn);
	}
	
	public void setCrc(String fn, String crc) {
		crcs.put(fn, crc);
	}

	public void setText(String fn, StringBuffer buf) throws Exception{
		
	}
	
	public StringBuffer getText(String fn) throws Exception{
		return null;
	}
}
