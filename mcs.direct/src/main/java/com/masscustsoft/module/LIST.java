package com.masscustsoft.module;

import java.util.ArrayList;
import java.util.List;

public class LIST extends PANEL {
	LoadCfg loadCfg;
	
	CONTAINER itemContainer;

	List<BUTTON> itemButtons=new ArrayList<BUTTON>();
	
	ACTION itemTap;
	
	FORM form;
	
	FILTERFORM filter;
	
	String addable, editable, deletable, pagable;
	
	WIZARD wizard;
	
	REPORT report;
	
	String itemWidth;
	
	JOINLIST detail;
	
	public LoadCfg getLoadCfg() {
		return loadCfg;
	}

	public void setLoadCfg(LoadCfg loadCfg) {
		this.loadCfg = loadCfg;
	}

	public CONTAINER getItemContainer() {
		return itemContainer;
	}

	public void setItemContainer(CONTAINER itemContainer) {
		this.itemContainer = itemContainer;
	}

	public ACTION getItemTap() {
		return itemTap;
	}

	public void setItemTap(ACTION itemTap) {
		this.itemTap = itemTap;
	}

	public FORM getForm() {
		return form;
	}

	public void setForm(FORM form) {
		this.form = form;
	}

	public FILTERFORM getFilter() {
		return filter;
	}

	public void setFilter(FILTERFORM filter) {
		this.filter = filter;
	}

	public List<BUTTON> getItemButtons() {
		return itemButtons;
	}

	public void setItemButtons(List<BUTTON> itemButtons) {
		this.itemButtons = itemButtons;
	}

	public WIZARD getWizard() {
		return wizard;
	}

	public void setWizard(WIZARD wizard) {
		this.wizard = wizard;
	}

	public REPORT getReport() {
		return report;
	}

	public void setReport(REPORT report) {
		this.report = report;
	}

	public String getAddable() {
		return addable;
	}

	public void setAddable(String addable) {
		this.addable = addable;
	}

	public String getEditable() {
		return editable;
	}

	public void setEditable(String editable) {
		this.editable = editable;
	}

	public String getDeletable() {
		return deletable;
	}

	public void setDeletable(String deletable) {
		this.deletable = deletable;
	}

	public String getPagable() {
		return pagable;
	}

	public void setPagable(String pagable) {
		this.pagable = pagable;
	}

	public String getItemWidth() {
		return itemWidth;
	}

	public void setItemWidth(String itemWidth) {
		this.itemWidth = itemWidth;
	}

	public JOINLIST getDetail() {
		return detail;
	}

	public void setDetail(JOINLIST detail) {
		this.detail = detail;
	}

	
}
