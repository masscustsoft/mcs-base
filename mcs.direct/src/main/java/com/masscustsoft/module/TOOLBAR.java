package com.masscustsoft.module;

import java.util.ArrayList;
import java.util.List;

public class TOOLBAR extends ELEMENT {
	String scrollable, dock, ui, visible;
	
	List<BUTTON> leftButtons=new ArrayList<BUTTON>();
	List<BUTTON> rightButtons=new ArrayList<BUTTON>();
	List<BUTTON> centerButtons=new ArrayList<BUTTON>();
	
	public String getScrollable() {
		return scrollable;
	}
	public void setScrollable(String scrollable) {
		this.scrollable = scrollable;
	}
	public List<BUTTON> getLeftButtons() {
		return leftButtons;
	}
	public void setLeftButtons(List<BUTTON> leftButtons) {
		this.leftButtons = leftButtons;
	}
	public List<BUTTON> getRightButtons() {
		return rightButtons;
	}
	public void setRightButtons(List<BUTTON> rightButtons) {
		this.rightButtons = rightButtons;
	}
	public List<BUTTON> getCenterButtons() {
		return centerButtons;
	}
	public void setCenterButtons(List<BUTTON> centerButtons) {
		this.centerButtons = centerButtons;
	}
	public String getDock() {
		return dock;
	}
	public void setDock(String dock) {
		this.dock = dock;
	}
	public String getUi() {
		return ui;
	}
	public void setUi(String ui) {
		this.ui = ui;
	}
	public String getVisible() {
		return visible;
	}
	public void setVisible(String visible) {
		this.visible = visible;
	}
	
}
