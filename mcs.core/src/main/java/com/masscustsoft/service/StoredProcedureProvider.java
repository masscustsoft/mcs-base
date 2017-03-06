package com.masscustsoft.service;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.masscustsoft.api.IDataService;
import com.masscustsoft.util.LightStr;
import com.masscustsoft.util.ReflectUtil;

public class StoredProcedureProvider {
	Map<String,String> effectingTables=new HashMap<String,String>();
	
	public Object runStoredProcedure(IDataService data, String name, Integer resultType, Object... params) throws Exception{
		Method m=ReflectUtil.getMethod(null, this, "do"+LightStr.capitalize(name), Object[].class);
		Object ret=m.invoke(this, params);
		return ret;
	}

	public Map<String, String> getEffectingTables() {
		return effectingTables;
	}

	public void setEffectingTables(Map<String, String> effectingTables) {
		this.effectingTables = effectingTables;
	}
}
