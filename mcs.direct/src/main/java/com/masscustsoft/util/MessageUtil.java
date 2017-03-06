package com.masscustsoft.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.masscustsoft.api.IDataService;
import com.masscustsoft.model.DeviceLogin;
import com.masscustsoft.model.DirectMessage;
import com.masscustsoft.model.DirectUser;

public class MessageUtil {
	
	public static void postMessage(IDataService ds, String ownerId, String type, String sender, String title, String body) throws Exception {
		DirectMessage m = new DirectMessage();
		m.setOwnerId(ownerId);
		m.setMessageType(type);
		m.setTitle(title);
		m.setMessage(body);
		m.setHasContent("no");
		m.setSenderId(sender);
		m.setSentTime(LightUtil.longDate());
		ds.insertBean(m);
		
		addBadge(ds, ownerId, "messages");
	}
	
	public static void addBadge(IDataService ds, String ownerId, String... ids) throws Exception{
		List<DeviceLogin> lst=ds.getBeanList(DeviceLogin.class, "{uid:'"+ ownerId+"'}","");
		for (DeviceLogin d:lst){
			if (d.expired()){
				ds.deleteBean(d);
			}
			else{
				d.addBadges(ids);
				ds.updateBean(d);
			}
		}
	}

	public static void broadcastMessage(IDataService ds, String type, String sender, String title, String body) throws Exception {
		List<DirectUser> devs = ds.getBeanList(DirectUser.class, "{}", "");
		for (DirectUser u:devs){
			if (u.getUserId().equals(sender)) continue;
			postMessage(ds,u.getUserId(),type,sender,title,body);
		}
	}
	
	public static void broadcastBadges(IDataService ds, String... ids) throws Exception {
		List<DirectUser> devs = ds.getBeanList(DirectUser.class, "{}", "");
		for (DirectUser u:devs){
			addBadge(ds, u.getUserId(), ids);
		}
	}
	
	private static List<Map<String, Object>> news = Collections.synchronizedList(new ArrayList<Map<String, Object>>());
	
	public static void broardNews(String message) {
		if (news.size()>20){
			news.remove(0);
			news.remove(0);
		}
		Map<String, Object> m=new HashMap();
		m.put("text", message);
		m.put("seconds", 300);
		m.put("time",System.currentTimeMillis());
		news.add(m);
	}

	public static List<Map<String,Object>> getNews(long last){
		List<Map<String, Object>> ret = new ArrayList<Map<String,Object>>();
		for (int i=0;i<news.size();i++){
			Map<String, Object> m=news.get(i);
			long tm=(Long)m.get("time");
			if (tm>last){
				ret.add(m);
			}
		}
		return ret;
	}
}
