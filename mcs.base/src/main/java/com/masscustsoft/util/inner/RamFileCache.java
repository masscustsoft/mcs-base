package com.masscustsoft.util.inner;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RamFileCache extends FileCache {
	private Map<String,StringBuffer> tcache=Collections.synchronizedMap(new HashMap<String,StringBuffer>());
	
	@Override
	public void reset() throws Exception{
		tcache.clear();
		super.reset();		
	}
	
	@Override
	public void setText(String fn, StringBuffer buf) {
		tcache.put(fn, buf);
	}
	
	@Override
	public StringBuffer getText(String fn) {
		return tcache.get(fn);
	}
}
