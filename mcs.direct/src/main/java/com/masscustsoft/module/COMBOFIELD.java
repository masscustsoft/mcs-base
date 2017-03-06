package com.masscustsoft.module;

import java.util.ArrayList;
import java.util.List;

public class COMBOFIELD extends FIELD {
	List<String> items=new ArrayList<String>();

	String prefix;
	
	Boolean convert;
	
	public List<String> getItems() {
		return items;
	}

	public void setItems(List<String> items) {
		this.items = items;
	}

	public Boolean getConvert() {
		return convert;
	}

	public void setConvert(Boolean convert) {
		this.convert = convert;
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}
	
}
