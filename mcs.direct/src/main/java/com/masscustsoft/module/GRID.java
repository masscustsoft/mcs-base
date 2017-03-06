package com.masscustsoft.module;

import java.util.ArrayList;
import java.util.List;

public class GRID extends LIST {
	
	Double tableWidth, minCellWidth;
	
	List<COLUMN> columns=new ArrayList<COLUMN>();
	
	public List<COLUMN> getColumns() {
		return columns;
	}

	public void setColumns(List<COLUMN> columns) {
		this.columns = columns;
	}

	public Double getTableWidth() {
		return tableWidth;
	}

	public void setTableWidth(Double tableWidth) {
		this.tableWidth = tableWidth;
	}

	public Double getMinCellWidth() {
		return minCellWidth;
	}

	public void setMinCellWidth(Double minCellWidth) {
		this.minCellWidth = minCellWidth;
	}
	
}
