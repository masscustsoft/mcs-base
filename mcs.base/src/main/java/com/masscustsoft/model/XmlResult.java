package com.masscustsoft.model;

public class XmlResult extends AbstractResult {
	public XmlResult(){
		super();
		setType(ResultType.Xml);
	}
	
	public XmlResult(Exception e){
		this();
		this.setError(e);
	}
		
}
