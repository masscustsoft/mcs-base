package com.masscustsoft.service;

public class Feature{
	String featureId;
	
	String name;

	String exposeTo;
	
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
	
	public boolean updateWith(Feature a) {
		boolean updated=false;
		if (!getName().equals(a.getName())) { setName(a.getName()); updated=true; }
		return updated;
	}

	public String getExposeTo() {
		return exposeTo;
	}

	public void setExposeTo(String exposeTo) {
		this.exposeTo = exposeTo;
	}

}
