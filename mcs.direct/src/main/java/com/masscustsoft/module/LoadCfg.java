package com.masscustsoft.module;

import java.util.ArrayList;
import java.util.List;

public class LoadCfg extends ActionCfg {
	
	String onSuccess,onFailure;
	
	List fields=new ArrayList();
	List data=new ArrayList();
	
	Integer pageSize;
	
	public String getOnSuccess() {
		return onSuccess;
	}

	public void setOnSuccess(String onSuccess) {
		this.onSuccess = onSuccess;
	}

	public String getOnFailure() {
		return onFailure;
	}

	public void setOnFailure(String onFailure) {
		this.onFailure = onFailure;
	}

	public List getData() {
		return data;
	}

	public void setData(List data) {
		this.data = data;
	}

	public List getFields() {
		return fields;
	}

	public void setFields(List fields) {
		this.fields = fields;
	}

	public Integer getPageSize() {
		return pageSize;
	}

	public void setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
	}
	
}
