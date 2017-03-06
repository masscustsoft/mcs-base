package com.masscustsoft.module;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.masscustsoft.Lang.CLASS;
import com.masscustsoft.service.DirectConfig;
import com.masscustsoft.util.GlbUtil;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.ReflectUtil;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.ThreadHelper;
import com.masscustsoft.xml.BeanFactory;
import com.masscustsoft.xml.BeanProxy;

public class ELEMENT {
	String id;
	
	String width, height;
	
	Double flex;
	
	String margin, padding;
	
	String dock;
	
	public Map<String,Object> toJson() {
		//export class name as zType
		//export all String, number, boolean to it
		//export all List/Map to it
		Map<String,Object> m=new HashMap<String,Object>();
		m.put("zType", CLASS.getSimpleName(this.getClass()));
		
		DirectConfig cfg = (DirectConfig)LightUtil.getCfg();
		BeanFactory bf = cfg.getBeanFactory();
		BeanProxy bp = bf.getBeanProxy(this);
		m.put("zId", bp.getBeanId());
		
		List<Field> flds = ReflectUtil.getFieldMap(getClass());
		for (Field f:flds){
			Object val=null;
			try {
				val=ReflectUtil.getProperty(this, f.getName());
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (val==null) continue;
			if (f.getType().equals(String.class)){
				String value=(String)val;
				m.put(f.getName(),GlbUtil.convertI18n(value,true));
			}
			else
			if (LightUtil.isPrimitive(f.getType())){
				m.put(f.getName(), val);
			}
			else
			if (List.class.isAssignableFrom(f.getType())){
				List list=new ArrayList();
				List ll=(List)val;
				for (Object item:ll){
					Class type=item.getClass();
					if (type.equals(String.class)){
						String value=(String)item;
						list.add(LightUtil.macroStr(value));
					}
					else
					if (LightUtil.isPrimitive(type)){
						list.add(val);
					}
					else
					if (ELEMENT.class.isAssignableFrom(type)){
						list.add(((ELEMENT)item).toJson());
					}
				}
				m.put(f.getName(), list);
			}
			else
			if (val instanceof ELEMENT){
				m.put(f.getName(), ((ELEMENT)val).toJson());
			}
			else{
				m.put(f.getName(), LightUtil.toJsonObject(val));
			}
				
		}
		return m;
	}

	public Double getFlex() {
		return flex;
	}

	public void setFlex(Double flex) {
		this.flex = flex;
	}

	public String getWidth() {
		return width;
	}

	public void setWidth(String width) {
		this.width = width;
	}

	public String getHeight() {
		return height;
	}

	public void setHeight(String height) {
		this.height = height;
	}

	public String getMargin() {
		return margin;
	}

	public void setMargin(String margin) {
		this.margin = margin;
	}

	public String getPadding() {
		return padding;
	}

	public void setPadding(String padding) {
		this.padding = padding;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDock() {
		return dock;
	}

	public void setDock(String dock) {
		this.dock = dock;
	}
}
