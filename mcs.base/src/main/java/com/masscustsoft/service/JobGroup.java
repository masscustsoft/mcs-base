package com.masscustsoft.service;

import java.util.ArrayList;
import java.util.List;

import com.masscustsoft.api.IDataService;
import com.masscustsoft.api.IRepository;
import com.masscustsoft.model.Job;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.xml.BeanFactory;

public class JobGroup {
	String dsId;
	String fsId;
	String groupId; //redefined as a unique group id to avoid duplicate.
	String groupFilter; // a json structure.
	
	transient IDataService dataService=null;
	transient IRepository fileService=null;
	
	List<Job> jobs=new ArrayList<Job>();

	public IDataService getDataService() {
		if (dataService==null){
			if (dsId!=null){
				dataService=BeanFactory.getBeanFactory().getDataService(dsId);	
			}
			else {
				dataService=LightUtil.getDataService();
			}
		}
		return dataService;
	}

	public IRepository getFileService() {
		if (fileService==null){
			if (fsId!=null){
				fileService=BeanFactory.getBeanFactory().getRepository(fsId);
			}
			else{
				fileService=LightUtil.getRepository();
			}
		}
		return fileService;
	}
	
	public void setDataService(IDataService dataService) {
		this.dataService = dataService;
	}

	public List<Job> getJobs() {
		return jobs;
	}

	public void setJobs(List<Job> jobs) {
		this.jobs = jobs;
	}

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String id) {
		this.groupId = id;
	}

	public String getDsId() {
		return dsId;
	}

	public void setDsId(String dsId) {
		this.dsId = dsId;
	}

	public String getFsId() {
		return fsId;
	}

	public void setFsId(String fsId) {
		this.fsId = fsId;
	}

	public void setFileService(Repository fileService) {
		this.fileService = fileService;
	}

	public String getGroupFilter() {
		return groupFilter;
	}

	public void setGroupFilter(String groupFilter) {
		this.groupFilter = groupFilter;
	}
	
	@Override
	public String toString(){
		return "{id:'"+groupId+"',jobs:"+jobs+"}";
	}

}
