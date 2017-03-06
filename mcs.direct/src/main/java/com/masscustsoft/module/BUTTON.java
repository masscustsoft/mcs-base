package com.masscustsoft.module;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.masscustsoft.service.DirectConfig;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.xml.BeanFactory;
import com.masscustsoft.xml.BeanProxy;

public class BUTTON extends ACTION {
	String icon;
	
	String text;

	String ui="bare";
	
	String visible, enable;
	
	String style;
	
	Boolean cover;
	
	String side;
	
	List<BUTTON> menus=new ArrayList<BUTTON>();
	
	@Override
	public Map<String,Object> toJson() {
		Map<String,Object> ret=super.toJson();
		DirectConfig cfg = (DirectConfig)LightUtil.getCfg();
		BeanFactory bf = cfg.getBeanFactory();
		BeanProxy bp = bf.getBeanProxy(this);
		
		
		String m=(String)ret.get("module");
		if (m!=null && m.indexOf(':')==-1){
			ret.put("module",bp.getFsId()+":"+m);
		}
		String icon=(String)ret.get("icon");
		if (icon!=null && !icon.substring(0,1).equals("@") && icon.indexOf(':')==-1){
			ret.put("icon",bp.getFsId()+":"+icon);
		}
		
		return ret;
	}
	
	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
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

	public String getEnable() {
		return enable;
	}

	public void setEnable(String enable) {
		this.enable = enable;
	}

	public String getStyle() {
		return style;
	}

	public void setStyle(String style) {
		this.style = style;
	}

	public List<BUTTON> getMenus() {
		return menus;
	}

	public void setMenus(List<BUTTON> menus) {
		this.menus = menus;
	}

	public Boolean getCover() {
		return cover;
	}

	public void setCover(Boolean cover) {
		this.cover = cover;
	}

	public String getSide() {
		return side;
	}

	public void setSide(String side) {
		this.side = side;
	}
	
}
