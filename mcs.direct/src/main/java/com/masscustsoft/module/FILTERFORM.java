package com.masscustsoft.module;

import java.util.ArrayList;
import java.util.List;

public class FILTERFORM extends FORM {
	Boolean textSearch;
	List<String> sortItems=new ArrayList<String>();
	
	public Boolean getTextSearch() {
		return textSearch;
	}

	public void setTextSearch(Boolean textSearch) {
		this.textSearch = textSearch;
	}

	public List<String> getSortItems() {
		return sortItems;
	}

	public void setSortItems(List<String> sortItems) {
		this.sortItems = sortItems;
	}
	
}
