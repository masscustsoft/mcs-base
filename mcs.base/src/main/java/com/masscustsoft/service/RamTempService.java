package com.masscustsoft.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class RamTempService extends TempService {
	Map<String, TempItem> map=new HashMap<String, TempItem>();
	
	@Override
	public TempItem newTempItem(String name){
		RamTempItem it = new RamTempItem(this, name);
		map.put(it.getName(),it);
		return it;
	}
	
	@Override
	public TempItem getTempItem(String name) throws IOException{
		return map.get(name);
	}

	public void deleteTempItem(TempItem it){
		map.remove(it.getName());
	}
}
