package com.masscustsoft.service;

import java.util.ArrayList;
import java.util.List;

import com.masscustsoft.api.IDataService;
import com.masscustsoft.api.IRepository;
import com.masscustsoft.service.PLUGIN;

public class UIPLUGIN{
	List<DirectDispatch> requestDispatches = new ArrayList<DirectDispatch>();
	List<UIENTRY> uiEntries=new ArrayList<UIENTRY>();
	List<HOOK> hooks=new ArrayList<HOOK>();
	List<DirectAction> actions=new ArrayList<DirectAction>();
	List<Feature> features=new ArrayList<Feature>();
	
	public List<UIENTRY> getUiEntries() {
		return uiEntries;
	}

	public void setUiEntries(List<UIENTRY> uiEntries) {
		this.uiEntries = uiEntries;
	}

	public List<DirectDispatch> getRequestDispatches() {
		return requestDispatches;
	}

	public void setRequestDispatches(List<DirectDispatch> requestDispatches) {
		this.requestDispatches = requestDispatches;
	}

	public List<HOOK> getHooks() {
		return hooks;
	}

	public void setHooks(List<HOOK> hooks) {
		this.hooks = hooks;
	}

	public List<DirectAction> getActions() {
		return actions;
	}

	public void setActions(List<DirectAction> actions) {
		this.actions = actions;
	}

	public List<Feature> getFeatures() {
		return features;
	}

	public void setFeatures(List<Feature> features) {
		this.features = features;
	}

}
