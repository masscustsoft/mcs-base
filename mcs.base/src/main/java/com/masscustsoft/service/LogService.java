package com.masscustsoft.service;

public class LogService {
	protected String logFile;
	protected String level; //for log
	
	protected transient boolean initialized=false;
		
	public void log(String level,String source,String msg) {
	}
	
	public String getLogFile() {
		return logFile;
	}

	public void init(){
		if (initialized) return;
		if (level==null) level="debug";
		initialized=true;
	}
	
	public void setLogFile(String logFile) {
		this.logFile = logFile;
		
	}

	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}
	
	public int getLevelId(String level){
		if ("info".equals(level)) return 1;
		if ("error".equals(level)) return 2;
		return 0;
	}
}
