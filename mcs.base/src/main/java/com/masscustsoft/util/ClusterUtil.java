package com.masscustsoft.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.masscustsoft.service.ClusterCmd;
import com.masscustsoft.service.ClusterService;

public class ClusterUtil {
	static Map<String,Map<String,Object>> nodes=new HashMap<String,Map<String,Object>>();
	static Map<String,Map<String,Map<String,Object>>> clusters=new HashMap<String,Map<String,Map<String,Object>>>();
	
	public static void broadcast(String group, Comparable dst,ClusterCmd cmd) throws Exception{ //support "cloud" 
		ClusterService svc = LightUtil.getCfg().getClusterService();
		if (svc==null || !svc.isEnable()){
			cmd.run(svc,null,dst);
		}
		else
		svc.broadcast(group,dst,cmd);
	}
	
	public static void updateMembers(List list) throws Exception{
		clusters.clear();
		List<String> all=new ArrayList<String>();
		for (String n:nodes.keySet()){
			all.add(n);
		}
		for (Object a:list){
			String name=a.toString();
			Map<String,Object> m=(Map)LightUtil.parseJson(name);
			String clu=(String)m.get("clusterId");
			String nod=(String)m.get("nodeId");
			String nm=clu+"."+nod;
			Map<String,Object> map=nodes.get(nm);
			if (map==null){
				nodes.put(nm, m);
			}
			else {
				map.putAll(m);
				all.remove(nm);
			}
		}
		for (String n:all){
			nodes.remove(n);
		}
		ClusterService svc = LightUtil.getCfg().getClusterService();
		System.out.println("Load group:"+svc.getCloudId()+" Members: "+nodes);
	}
	
	public static String getCluster(String clusterId){
		Map<String, Map<String,Object>> m = clusters.get(clusterId);
		if (m==null){
			m=new HashMap<String,Map<String,Object>>();
			clusters.put(clusterId, m);
			for (Map<String,Object> s:nodes.values()){
				if (clusterId.equals(s.get("clusterId"))){
					m.put((String)s.get("nodeId"), s);
				}
			}
		}
		StringBuffer ss=new StringBuffer();
		for (Map x:m.values()){
			if (ss.length()>0) ss.append(",");
			ss.append(x.get("bindIp")+":"+x.get("bindPort"));
		}
		return ss.toString();
	}
}
