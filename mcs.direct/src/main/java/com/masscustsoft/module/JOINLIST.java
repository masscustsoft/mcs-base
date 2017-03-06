package com.masscustsoft.module;

import java.util.ArrayList;
import java.util.List;

public class JOINLIST extends PANEL {
	List<LIST> sections=new ArrayList<LIST>();
	String itemWidth;
	
	public List<LIST> getSections() {
		return sections;
	}

	public void setSections(List<LIST> sections) {
		this.sections = sections;
	}

	public String getItemWidth() {
		return itemWidth;
	}

	public void setItemWidth(String itemWidth) {
		this.itemWidth = itemWidth;
	}
	
}
