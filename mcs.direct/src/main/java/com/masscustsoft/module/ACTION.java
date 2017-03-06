package com.masscustsoft.module;

import java.util.HashMap;
import java.util.Map;

public class ACTION extends ELEMENT {
	String module;
	
	String onClick;

	Map params=new HashMap();
	
	public String getModule() {
		return module;
	}

	public void setModule(String module) {
		this.module = module;
	}

	public String getOnClick() {
		return onClick;
	}

	public void setOnClick(String onClick) {
		this.onClick = onClick;
	}

	public Map getParams() {
		return params;
	}

	public void setParams(Map params) {
		this.params = params;
	}
	
	
}
