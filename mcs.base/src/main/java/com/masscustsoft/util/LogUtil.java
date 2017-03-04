package com.masscustsoft.util;

import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

import com.masscustsoft.service.AbstractConfig;
import com.masscustsoft.service.LogService;


public class LogUtil {
	private static LogService _service=null;
	private static long _bootup=new Date().getTime();
	
	//private static PrintStream out;
	
	private static LogService getService(){
		AbstractConfig config=LightUtil.getCfg();
		if (config!=null){
			_service=config.getLogService();
			//if (out==null && !"false".equals(config.getVars().get("_consoleRedirect"))){
			//	out=System.out;
			//	System.setOut(new PrintStream(new LogStream("debug"),true));
			//	System.setErr(new PrintStream(new LogStream("info"),true));
			//}
		}
		return _service;
	}
	
	private static ThreadLocal<Integer>logger_layer=new ThreadLocal<Integer>(){
		@Override
		protected Integer initialValue() {
			return 0;
		}
	};

	public static void Inc(int lay){
		int layer=logger_layer.get();
		layer+=lay;
		logger_layer.set(layer);
	}

	public static void info(Object... msgs){
		log("info", msgs);
	}
	
	public static void debug(Object... msgs){
		log("debug", msgs);
	}
	
	public static void error(Object... msgs){
		log("error", msgs);
	}
	
	private static void addSpace(StringBuffer buf){
		int layer=logger_layer.get();
		for (int i=0;i<layer;i++) buf.append(' ');
	}
	
	public static void log(String level, Object... msgs){
		StringBuffer buf=new StringBuffer();
		addSpace(buf);
		for (Object err:msgs) {
			if (err instanceof Throwable){
				Throwable th=(Throwable)err;
				StringWriter w=new StringWriter();
				th.printStackTrace(new PrintWriter(w));
				buf.append("\n"+w.getBuffer());
			}
			else{
				buf.append(err+" ");
			}
		}
		
		Throwable th=new Throwable();
    	StackTraceElement[] stacks =th.getStackTrace();
    	StackTraceElement cur=null;
    	
    	for (int i=0;i<stacks.length;i++){
    		StackTraceElement s=stacks[i];
    		if (s==null) continue;
    		if (!s.getClassName().equals(LogUtil.class.getName()) && !s.getClassName().equals(LogStream.class.getName())  && !s.getClassName().equals(PrintStream.class.getName()) 
    				&& !s.getClassName().equals("sun.nio.cs.StreamEncoder") && !s.getClassName().equals(OutputStreamWriter.class.getName())
    				&& !s.getClassName().equals(Throwable.class.getName())){
    			cur=stacks[i];
    			break;
    		}
    	}
    	
    	String msg=buf.toString();
    	int idx=msg.indexOf(":");
    	if (idx>=0 && idx<10){
    		level=msg.substring(0,idx);
    		msg=msg.substring(idx+1);
    	}
    	String source="";
    	if (cur!=null){
    		source=" "+cur.toString();
    	}
    	//String clusterId=(String)GlbHelper.get("clusterId");
    	String nodeId=(String)GlbHelper.get("nodeId");
    	String ID="";
    	//if (!StrUtil.isEmpty(clusterId)) ID=clusterId;
    	if (!LightStr.isEmpty(nodeId)) ID=nodeId;
    	if (ID!=null) msg="["+ID+"] "+msg;
    	
    	if (getService()!=null) _service.log(level,source,msg);
    	//if (out!=null) out.println((new Date().getTime()-_bootup)+" "+level.toUpperCase()+source+"\n - "+msg);
    	//else
    	if ("true".equals(GlbHelper.get("logToConsole"))||getService()==null){
    		boolean log=true;
    		if (_service!=null) {
    			int l=_service.getLevelId(level);
    			int l0=_service.getLevelId(_service.getLevel());
        		if (l<l0) log=false;
    		}
    		if (log)
    		System.out.println((new Date().getTime()-_bootup)+" "+level.toUpperCase()+source+"\n - "+msg);
    	}
	}
	
	public static void dumpStackTrace(Throwable e){
		log("error",e);
	}
}
