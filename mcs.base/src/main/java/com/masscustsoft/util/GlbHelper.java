package com.masscustsoft.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class GlbHelper {
	private static Map<String,Object> gMap=Collections.synchronizedMap(new HashMap<String,Object>());
	
	public static Map<String,Object> getMap(){
		return gMap;
	}
	
	public static Object get(String key){
		synchronized(gMap){return gMap.get(key);}
	}
	
	public static void set(String key,Object obj){
		synchronized(gMap){gMap.put(key, obj);}
	}
	
	public static void remove(String key){
		synchronized(gMap){gMap.remove(key);}
	}
	
	public static void installJob(Object js, Thread job){
//		if (ServerUtil.isGae()){
//			Map map=getInnerJobs();
//			map.put(js.hashCode()+"."+job.hashCode(), job);
//		}
//		else{
			job.start();
//		}
	}
	
	public static void installJob(Object js, Runnable run){
//		if (ServerUtil.isGae()){
//			Map map=getInnerJobs();
//			map.put(js.hashCode()+"."+run.hashCode(), run);
//		}
//		else{
			Thread th = new Thread(run);
			GlbHelper.set(js.hashCode()+"."+run.hashCode()+".thread", th);
			th.start();
//		}
	}
	
	public static void scheduleAtFixedRate(TimerTask task, long delay, long interval){
//		if (ServerUtil.isGae()){
//			Map map=getInnerJobs();
//			map.put(task.hashCode(), task);
//		}
//		else{
			Timer timer= new Timer(false);
			GlbHelper.set(task.hashCode()+".timer", timer);
			timer.scheduleAtFixedRate(task, 1000, 1000);
//		}
	}
	
	public static void cancelTimer(TimerTask task){
//		if (ServerUtil.isGae()){
//			Map map=getInnerJobs();
//			map.remove(task.hashCode());
//		}
//		else{
			Timer timer=(Timer)GlbHelper.get(task.hashCode()+".timer");
			if (timer!=null) timer.cancel();
			GlbHelper.remove(task.hashCode()+".timer");
//		}
	}
	public static void uninstallJob(Object js, Thread job) throws Exception{
		job.interrupt();
//		if (ServerUtil.isGae()){
//			Map map=getInnerJobs();
//			map.remove(js.hashCode()+"."+job.hashCode());
//		}
//		else{
			if (job.isAlive()) job.join();
//		}
	}
	
	public static void uninstallJob(Object js,Runnable run) throws Exception{
//		if (ServerUtil.isGae()){
//			Map map=getInnerJobs();
//			map.remove(js.hashCode()+"."+run.hashCode());
//		}
//		else{
			String id=js.hashCode()+"."+run.hashCode()+".thread";
			Thread job=(Thread)GlbHelper.get(id);
			if (job!=null){
				job.interrupt();
				if (job.isAlive()) job.join();
			}
			GlbHelper.remove(id);
//		}
	}
	
	public static Map<String,Object> getInnerJobs(){
		Map map=(Map)gMap.get("innerJobs");
		if (map==null){
			map=new HashMap();
			gMap.put("innerJobs",map);
		}
		return map;
	}
}
