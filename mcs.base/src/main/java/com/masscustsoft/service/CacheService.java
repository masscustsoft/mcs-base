package com.masscustsoft.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.masscustsoft.util.LightStr;
import com.masscustsoft.util.LightUtil;

public class CacheService {
	int max=5000;
	
	boolean singleOnly=false;
	
	String cacheRegex,noCacheRegex;
	
	private transient Map<String, Object> _map=null;
	private Pattern cachePattern=null, noCachePattern=null;
	
	public Map<String,Object> getMap(){
		if (_map==null){
			_map=new LinkedHashMap<String,Object>(max+1,0.75F,true){

				@Override
				protected boolean removeEldestEntry(Entry<String, Object> eldest) {
					return this.size()>max;
				}
				
			};
			
			String exp=LightUtil.macroStr(cacheRegex);
			if (!LightStr.isEmpty(exp)){
				cachePattern = Pattern.compile(exp, Pattern.CASE_INSENSITIVE);
			}
			exp=LightUtil.macroStr(noCacheRegex);
			if (!LightStr.isEmpty(exp)){
				noCachePattern = Pattern.compile(exp, Pattern.CASE_INSENSITIVE);
			}
		}
		return _map;
	}
	
	public Object getCache(String id){
		getMap();
		synchronized(_map){
			return _map.get(id);
		}
	}
	
	public boolean isCachable(String id){
		if (cachePattern==null && noCachePattern==null) return false;
		getMap();
		boolean ret=true;
		synchronized(_map){
			if (cachePattern!=null){
				Matcher m = cachePattern.matcher(id);
				if (!m.find()) ret=false;
			}
			if (noCachePattern!=null){
				Matcher m = noCachePattern.matcher(id);
			    if (m.find()) ret=false;
			}
		}
		return ret;
	}
	
	public void setCache(String id, Object value, Boolean single){
		getMap();
		synchronized(_map){
			if (singleOnly){
				if (single==null || single==false) return;
			}
			getMap().put(id, value);
		}
	}
	
	public synchronized void expireCache(String pattern){
		getMap();
		synchronized(_map){
			Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
			List<String> ids=new ArrayList<String>();
			for (Map.Entry<String, Object> e:getMap().entrySet()){
				 Matcher m = p.matcher(e.getKey());
			     if (m.find()) ids.add(e.getKey());
			}
			for (String id:ids){
				getMap().remove(id);
			}
		}
	}

	public int getMax() {
		return max;
	}

	public void setMax(int max) {
		this.max = max;
	}

	public boolean isSingleOnly() {
		return singleOnly;
	}

	public void setSingleOnly(boolean singleOnly) {
		this.singleOnly = singleOnly;
	}

	public String getCacheRegex() {
		return cacheRegex;
	}

	public void setCacheRegex(String cacheRegex) {
		this.cacheRegex = cacheRegex;
	}

	public String getNoCacheRegex() {
		return noCacheRegex;
	}

	public void setNoCacheRegex(String noCacheRegex) {
		this.noCacheRegex = noCacheRegex;
	}
}
