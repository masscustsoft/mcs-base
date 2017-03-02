package com.masscustsoft.util;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.masscustsoft.service.TempItem;

public class ThreadHelper {
	private static ThreadLocal<Map<String, Object>> tMap = new ThreadLocal<Map<String, Object>>() {
		@Override
		protected Map<String, Object> initialValue() {
			return new HashMap<String, Object>();
		}
	};

	public static Map<String, Object> getMap() {
		return tMap.get();
	}

	public static Object get(String key) {
		if (key == null)
			return getMap();
		return getMap().get(key);
	}

	public static void putAll(Map m) {
		getMap().putAll(m);
	}

	public static void set(String key, Object obj) {
		if (obj == null) {
			getMap().remove(key);
		} else {
			getMap().put(key, obj);
		}
	}

	public static void remove(String key) {
		getMap().remove(key);
	}
	
	public static void postponeDelete(File f){
		set(LightUtil.getHashCode(),f);
	}
	
	public static void postponeDelete(TempItem f){
		set(LightUtil.getHashCode(),f);
	}
}
