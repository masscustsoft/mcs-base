package com.masscustsoft.module;

import java.util.ArrayList;
import java.util.List;

public class LOOKUPFIELD extends FIELD {
	LoadCfg loadCfg;
	List<ELEMENT> docks=new ArrayList<ELEMENT>();
	String filterField;
	List<BUTTON> buttons=new ArrayList<BUTTON>();
	
	String valueField,displayField,itemWidth;
	
	@Override
	protected String getDefaultLabel(String lbl){
		if (lbl.endsWith("Id")||lbl.endsWith("Code")) return lbl+"Name";
		return lbl+"Lbl";
	}
	
	public List<ELEMENT> getDocks() {
		return docks;
	}

	public void setDocks(List<ELEMENT> docks) {
		this.docks = docks;
	}

	public String getFilterField() {
		return filterField;
	}

	public void setFilterField(String filterField) {
		this.filterField = filterField;
	}

	public List<BUTTON> getButtons() {
		return buttons;
	}

	public void setButtons(List<BUTTON> buttons) {
		this.buttons = buttons;
	}

	public LoadCfg getLoadCfg() {
		return loadCfg;
	}

	public void setLoadCfg(LoadCfg loadCfg) {
		this.loadCfg = loadCfg;
	}

	public String getValueField() {
		return valueField;
	}

	public void setValueField(String valueField) {
		this.valueField = valueField;
	}

	public String getDisplayField() {
		return displayField;
	}

	public void setDisplayField(String displayField) {
		this.displayField = displayField;
	}

	public String getItemWidth() {
		return itemWidth;
	}

	public void setItemWidth(String itemWidth) {
		this.itemWidth = itemWidth;
	}

}
