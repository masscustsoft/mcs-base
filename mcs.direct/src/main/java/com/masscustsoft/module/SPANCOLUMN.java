package com.masscustsoft.module;

import java.util.ArrayList;
import java.util.List;

public class SPANCOLUMN extends COLUMN {
	List<COLUMN> columns=new ArrayList<COLUMN>();

	public List<COLUMN> getColumns() {
		return columns;
	}

	public void setColumns(List<COLUMN> columns) {
		this.columns = columns;
	}
	
}
