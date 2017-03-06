package com.masscustsoft.module;

import java.util.ArrayList;
import java.util.List;

public class TABPANEL extends PANEL {
	LoadCfg loadCfg;
	List<BUTTON> sysMenus=new ArrayList();
	
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
	
}
