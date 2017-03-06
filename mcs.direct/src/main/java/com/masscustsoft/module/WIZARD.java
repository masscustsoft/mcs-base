package com.masscustsoft.module;

import java.util.ArrayList;
import java.util.List;

public class WIZARD extends PANEL{
	Boolean transMode;
	SaveCfg saveCfg;
	Boolean insert;
	
	List<PANEL> steps=new ArrayList<PANEL>();

	public List<PANEL> getSteps() {
		return steps;
	}

	public void setSteps(List<PANEL> steps) {
		this.steps = steps;
	}

	public Boolean getTransMode() {
		return transMode;
	}

	public void setTransMode(Boolean transMode) {
		this.transMode = transMode;
	}

	public SaveCfg getSaveCfg() {
		return saveCfg;
	}

	public void setSaveCfg(SaveCfg saveCfg) {
		this.saveCfg = saveCfg;
	}

	public Boolean getInsert() {
		return insert;
	}

	public void setInsert(Boolean insert) {
		this.insert = insert;
	}
	
}
