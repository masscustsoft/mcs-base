package com.masscustsoft.util;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import com.masscustsoft.api.IDataService;
import com.masscustsoft.model.DirectI18n;
import com.masscustsoft.service.DirectConfig;

public class GlbUtil {

	public static String prepareI18n(String lang,String key,String def){
		DirectConfig cfg=(DirectConfig)LightUtil.getCfg();
		Map<String, String> langs = cfg.getLanguages();
		Map<String,String> list=new HashMap<String,String>();
		for (String loc:langs.keySet()){
			String l=langs.get(loc);
			if (list.get(l)==null){
				list.put(l,l);
				if (l.equals(lang)){
					def=getI18n(l,key,def);
				}
				else
					getI18n(l,key,null);
			}
		}
		return def;
//		return getI18n(lang,key,def);
	}
	
	public static String i18n(String key){
		return getI18n(LightUtil.getLang(),key, null);
	}
	
	public static String getI18n(String lang, String key, String def){
		int k=key.indexOf(":");
		if (LightStr.isEmpty(def)) def="["+key+"]";
		if (k>0){
			def=key.substring(k+1);
			key=key.substring(0, k);
		}
		if (key.contains("{")){
			return key;
		}
			
		DirectConfig cfg=(DirectConfig)LightUtil.getCfg();
		IDataService ds = cfg.getDs(null);
		try {
			DirectI18n di = ds.getBean(DirectI18n.class, "lang", lang, "keyId", key);	
			if (di==null){
				di=new DirectI18n();
				di.setKeyId(key);
				di.setValue(def);
				di.setLang(lang);
				di.setStatus("auto");
				di.setModuleId("Global");
				ds.insertBean(di);
			}
			else def=di.getValue();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return def;
	}
	
	public static String convertI18n(String str, boolean keep){
		String lang=(String)ThreadHelper.get("$$lang");
		for (int i=str.indexOf("#[");i>=0;i=str.indexOf("#[")){
			int j=str.indexOf("]",i+2);
			String key=str.substring(i+2,j);
			if (keep){
				str=str.substring(0,i)+"$["+key+"]"+str.substring(j+1);
				if (key.contains("{")==false) prepareI18n(lang,key,null);
			}
			else{
				String def=getI18n(lang,key,null);	
				str=str.substring(0,i)+def+str.substring(j+1);
			}
		}
		return str.replace("$[", "#[");
	}
	
	public static String decimal(Object num, int dec) {
		return LightUtil.toNativeNumber(num,dec);
	}
	
	public static String shortDate(Object val) {
		if (val==null) return "";
		if (val instanceof Date){
			return LightUtil.toNativeShortDate((Date)val);
		}
		return "";
	}
	
	public static String longDate(Object val) {
		if (val==null) return "";
		if (val instanceof Timestamp){
			return LightUtil.toNativeLongDate((Timestamp)val);
		}
		return "";
	}
}
