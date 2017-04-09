package com.masscustsoft.module;

import java.util.ArrayList;
import java.util.List;

public class TABPANEL extends PANEL {
	LoadCfg loadCfg;
	List<BUTTON> sysMenus=new ArrayList();
	
	String tabPosition="bottom";
	
	public LoadCfg getLoadCfg() {
		return loadCfg;
	}

	public void setLoadCfg(LoadCfg loadCfg) {
		this.loadCfg = loadCfg;
	}

	public List<BUTTON> getSysMenus() {
		return sysMenus;
	}

	public void setSysMenus(List<BUTTON> sysMenus) {
		this.sysMenus = sysMenus;
	}

	public String getTabPosition() {
		return tabPosition;
	}

	public void setTabPosition(String tabPosition) {
		this.tabPosition = tabPosition;
	}
	
}
