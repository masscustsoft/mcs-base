package com.masscustsoft.xml.inner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.masscustsoft.util.inner.FileCache;
import com.masscustsoft.util.inner.RamFileCache;
import com.masscustsoft.xml.BeanProxy;

public class CacheFactory {
	private Map<String, BeanProxy> beans=Collections.synchronizedMap(new HashMap<String,BeanProxy>());
	
	private Map<Object,BeanProxy> objects=Collections.synchronizedMap(new HashMap<Object,BeanProxy>());
	
	public Map<String, String> runnings=Collections.synchronizedMap(new HashMap<String,String>());
	
	transient FileCache fileCache=new RamFileCache();
	transient Map<String,Object> objectCache=Collections.synchronizedMap(new HashMap<String,Object>());
	transient List<String> objectCaching=Collections.synchronizedList(new ArrayList<String>());
	
	public void clearBeans(String prefix){
		List<String> list=new ArrayList<String>();
		for (String bean:beans.keySet()){
			if (bean.startsWith(prefix) || bean.startsWith("mem:") && bean.indexOf("/"+prefix)>0) list.add(bean);
		}
		for (String bean:list){
			beans.remove(bean);
		}
	}
	
	public BeanProxy get(String id){
		return beans.get(id);
	}
	
	public void put(String id, Object obj, long tm){
		BeanProxy bp=new BeanProxy(id,obj,tm);
		beans.put(id, bp);
		objects.put(obj,bp);
	}
	
	public void remove(String name){
		BeanProxy b=beans.get(name);
		beans.remove(name);
		if (b!=null){
			objects.remove(b.getBean());
		}
	}
	
	public BeanProxy getByObj(Object obj){
		return objects.get(obj);
	}
	
	public void clear(){
		beans.clear();
		runnings.clear();
	}

	public FileCache getFileCache() {
		return fileCache;
	}

	public <T> Map<String, T> getObjectCache() {
		return (Map<String, T>) objectCache;
	}

	public List<String> getObjectCaching() {
		return objectCaching;
	}
}
