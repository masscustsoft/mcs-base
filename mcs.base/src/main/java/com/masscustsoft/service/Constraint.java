package com.masscustsoft.service;

import java.util.ArrayList;
import java.util.List;

import com.masscustsoft.api.IReferentItem;

public class Constraint {
	String model;
	List<IReferentItem> refers=new ArrayList<IReferentItem>();
	List<IReferentItem> descendants=new ArrayList<IReferentItem>();
	
	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public List<IReferentItem> getRefers() {
		return refers;
	}

	public void setRefers(List<IReferentItem> refers) {
		this.refers = refers;
	}

	public List<IReferentItem> getDescendants() {
		return descendants;
	}

	public void setDescendants(List<IReferentItem> descendants) {
		this.descendants = descendants;
	}

	@Override
	public String toString(){
		return "{model:"+model+",refers:"+refers+",descendants:"+descendants+"}";
	}
}
