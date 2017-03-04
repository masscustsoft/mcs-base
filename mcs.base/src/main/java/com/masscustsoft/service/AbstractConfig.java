package com.masscustsoft.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.masscustsoft.api.IClusterService;
import com.masscustsoft.model.AbstractResult;

public abstract class AbstractConfig {
	
	protected String supportedModules;
	
	Map<String, Object> vars = Collections.synchronizedMap(new HashMap<String, Object>());

	protected TempService tempService=new RamTempService();
	
	protected NotifyService notifyService=null;

	public void initThread(){
		
	}
	
	public abstract IClusterService getClusterService();
	
	
	public String getSupportedModules() {
		return supportedModules;
	}

	public void setSupportedModules(String supportedModules) {
		this.supportedModules = supportedModules;
	}

	public Map<String, Object> getVars() {
		return vars;
	}

	public void setVars(Map<String, Object> vars) {
		this.vars = vars;
	}

	public TempService getTempService() {
		return tempService;
	}

	public void setTempService(TempService tempService) {
		this.tempService = tempService;
	}

	public NotifyService getNotifyService() {
		return notifyService;
	}

	public void setNotifyService(NotifyService notifyService) {
		this.notifyService = notifyService;
	}

	public void processSQLException(Throwable e, AbstractResult ret) {
		e.printStackTrace();
	}

}
