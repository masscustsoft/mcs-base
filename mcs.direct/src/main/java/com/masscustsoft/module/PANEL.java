package com.masscustsoft.module;

import java.util.ArrayList;
import java.util.List;

public class PANEL extends CONTAINER {
	String title, titleAlign, titleIcon, scrollable, backable;
	Boolean hideOnMaskTap;
	
	List<BUTTON> leftButtons=new ArrayList<BUTTON>();
	List<BUTTON> rightButtons=new ArrayList<BUTTON>();
	
	String beforeLoad, afterLoad, beforeClose, afterClose;
	
	String actionObject, keyFields;
	
	String visible;
	
	public String getActionObject() {
		return actionObject;
	}

	public void setActionObject(String actionObject) {
		this.actionObject = actionObject;
	}

	public String getAfterClose() {
		return afterClose;
	}

	public void setAfterClose(String afterClose) {
		this.afterClose = afterClose;
	}

	public String getBeforeClose() {
		return beforeClose;
	}

	public void setBeforeClose(String beforeClose) {
		this.beforeClose = beforeClose;
	}

	public String getScrollable() {
		return scrollable;
	}

	public void setScrollable(String scrollable) {
		this.scrollable = scrollable;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getTitleAlign() {
		return titleAlign;
	}

	public void setTitleAlign(String titleAlign) {
		this.titleAlign = titleAlign;
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

	public String getBeforeLoad() {
		return beforeLoad;
	}

	public void setBeforeLoad(String beforeLoad) {
		this.beforeLoad = beforeLoad;
	}

	public String getAfterLoad() {
		return afterLoad;
	}

	public void setAfterLoad(String afterLoad) {
		this.afterLoad = afterLoad;
	}	

	public String getBackable() {
		return backable;
	}

	public void setBackable(String backable) {
		this.backable = backable;
	}

	public String getTitleIcon() {
		return titleIcon;
	}

	public void setTitleIcon(String titleIcon) {
		this.titleIcon = titleIcon;
	}

	public String getKeyFields() {
		return keyFields;
	}

	public void setKeyFields(String keyFields) {
		this.keyFields = keyFields;
	}

	public String getVisible() {
		return visible;
	}

	public void setVisible(String visible) {
		this.visible = visible;
	}

	public Boolean getHideOnMaskTap() {
		return hideOnMaskTap;
	}

	public void setHideOnMaskTap(Boolean hideOnMaskTap) {
		this.hideOnMaskTap = hideOnMaskTap;
	}
	
}
