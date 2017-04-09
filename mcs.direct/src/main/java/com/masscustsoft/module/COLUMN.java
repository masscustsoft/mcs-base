package com.masscustsoft.module;

import java.util.Map;

import com.masscustsoft.util.LightStr;

public class COLUMN extends ELEMENT {
	String name;
	String label;
	Double minWidth;
	Boolean locked;
	
	@Override
	public Map<String,Object> toJson() {
		Map<String,Object> ret=super.toJson();
		if (label==null){
			String lbl=name; if (lbl==null) lbl=id;
			if (lbl!=null)
			ret.put("label", "#["+LightStr.capitalize(lbl)+"Lbl]");
		}
		return ret;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getLabel() {
		return label;
	}
	
	public void setLabel(String label) {
		this.label = label;
	}

	public Double getMinWidth() {
		return minWidth;
	}

	public void setMinWidth(Double minWidth) {
		this.minWidth = minWidth;
	}

	public Boolean getLocked() {
		return locked;
	}

	public void setLocked(Boolean locked) {
		this.locked = locked;
	}
	
}
