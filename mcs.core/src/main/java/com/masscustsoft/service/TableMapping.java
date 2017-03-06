package com.masscustsoft.service;

import java.util.ArrayList;
import java.util.List;

public class TableMapping {
	String entity; //Bean
	String tableName;
	//Boolean supportFullText=true;
	//Boolean uniqueTableName=false;
	
	List<FieldMapping> fields=new ArrayList<FieldMapping>();
	
	public List<FieldMapping> getFields() {
		return fields;
	}

	public void setFields(List<FieldMapping> fields) {
		this.fields = fields;
	}

	public String getEntity() {
		return entity;
	}
	
	public void setEntity(String entity) {
		this.entity = entity;
	}
	
	public String getTableName() {
		return tableName;
	}
	
	public void setTableName(String tableName) {
		this.tableName = tableName;
	} 
	
}
