package com.masscustsoft.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.masscustsoft.api.IDataService;
import com.masscustsoft.api.IRepository;
import com.masscustsoft.model.AbstractResult;
import com.masscustsoft.util.GlbHelper;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.LogUtil;

public abstract class AbstractConfig {
	
	protected String supportedModules;
	
	Map<String, Object> vars = Collections.synchronizedMap(new HashMap<String, Object>());

	protected TempService tempService=new RamTempService();
	
	protected NotifyService notifyService=null;

	protected LogService logService;
	
	List<BeanInterceptor> beanInterceptors=new ArrayList<BeanInterceptor>();

	protected CacheService cacheService=null;

	ClusterService clusterService;
	
	JobService jobService;
	
	public void initThread(){
		
	}
	
	public void initCluster(){//install jobService
		if (clusterService==null) return;
		try {
			if (clusterService.init(LightUtil.getBeanFactory())) clusterService.start();
		} catch (Exception e) {
			LogUtil.info("Cluster Start failed!"+e.getMessage());
		}
	}
	
	public void removeCluster(){
		if (clusterService==null) return;
		try {
			clusterService.stop();
		} catch (Exception e) {
			LogUtil.info("Cluster Stop failed!"+e.getMessage());
		}	
	}
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

	public void processSQLException(Throwable e, AbstractResult ret) throws Exception {
		e.printStackTrace();
	}

	public LogService getLogService() {
		return logService;
	}

	public void setLogService(LogService logService) {
		this.logService = logService;
	}

	public <T> T getVar(String var, T def){
		return (T)vars.get(var);
	}

	public abstract IDataService getDs();

	public abstract IRepository getFs();

	public List<BeanInterceptor> getBeanInterceptors() {
		return beanInterceptors;
	}

	public void setBeanInterceptors(List<BeanInterceptor> beanInterceptors) {
		this.beanInterceptors = beanInterceptors;
	}

	public CacheService getCacheService() {
		return cacheService;
	}

	public void setCacheService(CacheService cacheService) {
		this.cacheService = cacheService;
	}

	public ClusterService getClusterService() {
		return clusterService;
	}

	public void setClusterService(ClusterService clusterService) {
		this.clusterService = clusterService;
	}

	public JobService getJobService() {
		return jobService;
	}

	public void setJobService(JobService jobService) {
		this.jobService = jobService;
	}

}
