package com.masscustsoft.service;

public class FieldMapping {
	String attribute;
	String fieldName;
	Boolean serverGenerated;
	
	public String getAttribute() {
		return attribute;
	}
	
	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}
	
	public String getFieldName() {
		return fieldName;
	}
	
	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	public Boolean getServerGenerated() {
		return serverGenerated;
	}

	public void setServerGenerated(Boolean serverGenerated) {
		this.serverGenerated = serverGenerated;
	}

}
