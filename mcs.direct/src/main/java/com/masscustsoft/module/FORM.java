package com.masscustsoft.module;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.masscustsoft.util.ThreadHelper;

public class FORM extends PANEL {
	String labelWidth,labelAlign;

	LoadCfg loadCfg;
	SaveCfg saveCfg;
	DeleteCfg deleteCfg;
	
	Boolean viewOnly, loadable, insert;
	
	List<Map> validators=new ArrayList<Map>();
	
	public String getLabelWidth() {
		return labelWidth;
	}

	public void setLabelWidth(String labelWidth) {
		this.labelWidth = labelWidth;
	}

	public String getLabelAlign() {
		return labelAlign;
	}

	public void setLabelAlign(String labelAlign) {
		this.labelAlign = labelAlign;
	}
	
	@Override
	public Map<String,Object> toJson() {
		ThreadHelper.set("$$FORM", this);
		Map<String,Object> ret=super.toJson();
		ThreadHelper.set("$$FORM", null);
		return ret;
	}

	public LoadCfg getLoadCfg() {
		return loadCfg;
	}

	public void setLoadCfg(LoadCfg loadCfg) {
		this.loadCfg = loadCfg;
	}

	public SaveCfg getSaveCfg() {
		return saveCfg;
	}

	public void setSaveCfg(SaveCfg saveCfg) {
		this.saveCfg = saveCfg;
	}

	public DeleteCfg getDeleteCfg() {
		return deleteCfg;
	}

	public void setDeleteCfg(DeleteCfg deleteCfg) {
		this.deleteCfg = deleteCfg;
	}

	public Boolean getViewOnly() {
		return viewOnly;
	}

	public void setViewOnly(Boolean viewOnly) {
		this.viewOnly = viewOnly;
	}

	public Boolean getLoadable() {
		return loadable;
	}

	public void setLoadable(Boolean loadable) {
		this.loadable = loadable;
	}

	public Boolean getInsert() {
		return insert;
	}

	public void setInsert(Boolean insert) {
		this.insert = insert;
	}

	public List<Map> getValidators() {
		return validators;
	}

	public void setValidators(List<Map> validators) {
		this.validators = validators;
	}
}
