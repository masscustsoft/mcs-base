package com.masscustsoft.model;

import com.masscustsoft.model.AbstractResult;

public class JsonResult extends AbstractResult {
	
	public JsonResult(){
		super();
		setType(ResultType.Json);
	}

	public JsonResult(Exception e){
		this();
		this.setError(e);
	}

}
