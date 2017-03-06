package com.masscustsoft.service;

import java.util.ArrayList;
import java.util.List;

import com.masscustsoft.api.IDataService;
import com.masscustsoft.api.IRepository;

public class PLUGIN {
	List<IDataService> dataServices=new ArrayList<IDataService>();
	List<IRepository> repositories=new ArrayList<IRepository>();
	List<String> packages=new ArrayList<String>();
	
	public List<IDataService> getDataServices() {
		return dataServices;
	}
	public void setDataServices(List<IDataService> dataServices) {
		this.dataServices = dataServices;
	}
	public List<IRepository> getRepositories() {
		return repositories;
	}
	public void setRepositories(List<IRepository> repositories) {
		this.repositories = repositories;
	}
	public List<String> getPackages() {
		return packages;
	}
	public void setPackages(List<String> packages) {
		this.packages = packages;
	}
	
}
