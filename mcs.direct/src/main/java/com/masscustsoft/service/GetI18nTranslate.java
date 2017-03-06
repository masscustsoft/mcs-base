package com.masscustsoft.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.masscustsoft.model.AbstractResult;
import com.masscustsoft.model.DirectI18n;
import com.masscustsoft.util.GlbUtil;

public class GetI18nTranslate extends DirectAction {
	
	protected String getTranslateBody(String sourceLang, String targetLang, String targetStatus, boolean forValue) throws Exception{
		List<DirectI18n> ll = getDs().getBeanList(DirectI18n.class, "{lang:'"+sourceLang+"'}", "");
		TreeMap<String,String> map=new TreeMap(),raw=new TreeMap();
		for (DirectI18n di:ll){
			map.put(di.getKeyId(), di.getValue());
		}
		ll = getDs().getBeanList(DirectI18n.class, "{lang:'"+targetLang+"'}", "");
		for (DirectI18n di:ll){
			if (map.containsKey(di.getKeyId())){
				raw.put(di.getKeyId(), map.get(di.getKeyId()));	
			}
			map.remove(di.getKeyId());
		}
		System.out.println("MAP left="+map);
		for (String id:map.keySet()){
			String def=GlbUtil.prepareI18n(sourceLang, id, null);
			raw.put(id, def);
		}
		ll = getDs().getBeanList(DirectI18n.class, "{lang:'"+targetLang+"',status:'"+targetStatus+"'}", "", 0, 10000,"keyId");
		
		StringBuffer buf=new StringBuffer();
		for (DirectI18n di:ll){
			String val=di.getValue();
			if (val.equals('['+di.getKeyId()+"]")){
				val=raw.get(di.getKeyId());
			}
			if (forValue) buf.append(val+"\n");
			else buf.append(di.getKeyId()+"\n");
		}
		return buf.toString().trim();
	}
	
	@Override
	protected void run(AbstractResult ret) throws Exception {
		String sourceLang=requiredStr("sourceLang");
		String targetStatus=requiredStr("targetStatus");
		
		String targetLang=requiredStr("targetLang");
		
		String objectId=getStr("objectId","");
		
		Map m=getDefaults(objectId,getStr("fieldId","defaults"));
		m.put("translateBody", getTranslateBody(sourceLang,targetLang,targetStatus,true));
		
		ret.setResult(m);
	}
}
