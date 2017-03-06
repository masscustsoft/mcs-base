package com.masscustsoft.service;

import java.util.Map;

import com.masscustsoft.model.Entity;

public abstract class HOOK<T extends Entity> {
	String name;

	protected String featureId;
	
	public boolean accept(DirectComponent dd) throws Exception{
		return true;
	}
		
	public void doGet(DirectComponent dd, Map resp) throws Exception{
		
	}

	public void doSet(DirectComponent dd, T en) throws Exception{
		
	}

	public void doClear(DirectComponent dd, T en) throws Exception{
		
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFeatureId() {
		return featureId;
	}

	public void setFeatureId(String featureId) {
		this.featureId = featureId;
	}
}
