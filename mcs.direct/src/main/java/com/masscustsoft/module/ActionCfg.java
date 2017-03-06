package com.masscustsoft.module;

import java.util.HashMap;
import java.util.Map;

public class ActionCfg {
	String action;

	String keyFields;
	
	Map<String,String> params=new HashMap<String,String>();

	String actionObject;
	
	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getKeyFields() {
		return keyFields;
	}

	public void setKeyFields(String keyFields) {
		this.keyFields = keyFields;
	}

	public Map<String, String> getParams() {
		return params;
	}

	public void setParams(Map<String, String> params) {
		this.params = params;
	}

	public String getActionObject() {
		return actionObject;
	}

	public void setActionObject(String actionObject) {
		this.actionObject = actionObject;
	}
}
