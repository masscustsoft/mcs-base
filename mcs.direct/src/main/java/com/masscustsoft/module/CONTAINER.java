package com.masscustsoft.module;

import java.util.ArrayList;
import java.util.List;

public class CONTAINER extends ELEMENT {
	String layout, align, pack;
	
	List<ELEMENT> items=new ArrayList<ELEMENT>();
	
	String visible;
	
	String excludeTag;
	
	public String getVisible() {
		return visible;
	}

	public void setVisible(String visible) {
		this.visible = visible;
	}

	public List<ELEMENT> getItems() {
		return items;
	}

	public void setItems(List<ELEMENT> items) {
		this.items = items;
	}

	public String getLayout() {
		return layout;
	}

	public void setLayout(String layout) {
		this.layout = layout;
	}

	public String getAlign() {
		return align;
	}

	public void setAlign(String align) {
		this.align = align;
	}

	public String getPack() {
		return pack;
	}

	public void setPack(String pack) {
		this.pack = pack;
	}

	public String getExcludeTag() {
		return excludeTag;
	}

	public void setExcludeTag(String excludeTag) {
		this.excludeTag = excludeTag;
	}
}
