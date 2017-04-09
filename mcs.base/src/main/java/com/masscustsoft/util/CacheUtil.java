package com.masscustsoft.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.masscustsoft.service.AbstractConfig;
import com.masscustsoft.service.CacheService;
import com.masscustsoft.service.SysCmdExpireCache;

public class CacheUtil {
	public static void clear(){
		List<String> exps=(List)ThreadHelper.get("_CacheExpires_");
		Object o=ThreadHelper.get("_ProgressInfoThread_");
		if (exps!=null && o==null){
			StringBuffer buf=new StringBuffer();
			for (String s:exps){
				if (buf.length()>0) buf.append("|");
				buf.append("("+s+")");
			}
			CacheUtil.forceExpireCache(buf.toString());
		}
		String[] rsv=new String[]{"beanFactory"};
		Map m=new HashMap();
		for (String s:rsv){
			Object v=ThreadHelper.get(s);
			if (v!=null) m.put(s, v);
		}
		LightUtil.clearup(ThreadHelper.getMap());
		ThreadHelper.getMap().clear();
		ThreadHelper.putAll(m);
			
	}
	
	private static CacheService getCacheService(){
		AbstractConfig cfg = LightUtil.getCfg();
		if (cfg==null) return null;
		return cfg.getCacheService();
	}
	
	public static Object getCache(String id){
		CacheService cs=getCacheService();
		if (cs==null) return null;
		if (id==null || id.contains("Stub")) return null;
		if (!cs.isCachable(id)) return null;
		
		Object o=cs.getCache(id);
		if (o==null) return null;
		System.out.println("Shot "+id);
		try{
			o=LightUtil.fromJsonObject(o);
		}
		catch(Exception e){
			e.printStackTrace();
			return null;
		}
		return o;
	}
	
	public static void setCache(String id, Object value){
		setCache(id, value, null);
	}
	
	public static void setCache(String id, Object value, Boolean single){
		CacheService cs=getCacheService();
		if (cs==null) return;
		if (id==null || id.contains("Stub")) return;
		if (!cs.isCachable(id)) return;
		
		Object o=LightUtil.toJsonObject(value, 1);
		cs.setCache(id, o, single);
	}
	
	public static void expireCache(String pattern){
		CacheService cs=getCacheService();
		if (cs!=null){
			//clear local first
			cs.expireCache(pattern);
			//prepare to network clear
			List<String> exps=(List)ThreadHelper.get("_CacheExpires_");
			if (exps==null) {
				exps=new ArrayList<String>();
				ThreadHelper.set("_CacheExpires_",exps);
			}
			if (!exps.contains(pattern)) exps.add(pattern);
		}
	}
	
	public static void forceExpireCache(String pattern){
		CacheService cs=getCacheService();
		if (cs!=null){
			try {
				SysCmdExpireCache dc = new SysCmdExpireCache();
				dc.setPattern(pattern);
				ClusterUtil.broadcast(null,null,dc);
			} catch (Exception e) {
				LogUtil.dumpStackTrace(e);
			}
			//System.out.println("Clear Cache "+pattern+", size="+((Map)cs.getCache(null)).size());
		}

	}
}
