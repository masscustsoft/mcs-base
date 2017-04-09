package com.masscustsoft.module;

import java.util.ArrayList;
import java.util.List;

public class NAVIGATOR extends PANEL {
	List<BUTTON> menus=new ArrayList<BUTTON>();

	String navType; //default, tree, accordion, accordiontree, dropdownmenu
	
	String navSize; //set how big the nav
	
	String navDock; //
	
	public List<BUTTON> getMenus() {
		return menus;
	}

	public void setMenus(List<BUTTON> menus) {
		this.menus = menus;
	}

	public String getNavType() {
		return navType;
	}

	public void setNavType(String navType) {
		this.navType = navType;
	}

	public String getNavSize() {
		return navSize;
	}

	public void setNavSize(String navSize) {
		this.navSize = navSize;
	}

	public String getNavDock() {
		return navDock;
	}

	public void setNavDock(String navDock) {
		this.navDock = navDock;
	}
	
}
