package com.masscustsoft.service;

public class OutputParameter {
	int type;
	int id;
	Object value;
	
	public OutputParameter(int type){
		this.type=type;
		id=0;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
}
