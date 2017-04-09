package com.masscustsoft.module;

import java.io.InputStream;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;

import com.masscustsoft.helper.Upload;
import com.masscustsoft.service.DirectConfig;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.LightStr;
import com.masscustsoft.util.ThreadHelper;
import com.masscustsoft.xml.BeanFactory;
import com.masscustsoft.xml.BeanProxy;

public class FIELD extends ELEMENT {
	String name;
	String label;
	String placeHolder;
	String labelWidth;
	String labelAlign;
	
	String required,visible,enable,readonly;
	Integer min,max;
	String message;
	
	Boolean cache;
	String tag;
	String onChange;
	
	protected String getDefaultLabel(String lbl){
		return lbl+"Lbl";
	}
	
	@Override
	public Map<String,Object> toJson() {
		Map<String,Object> ret=super.toJson();
		FORM form=(FORM)ThreadHelper.get("$$FORM");
		if (label==null){
			String lbl=name; if (lbl==null) lbl=id;
			if (lbl!=null)
			ret.put("label", "#["+LightStr.capitalize(getDefaultLabel(lbl))+"]");
		}
		String lbl=(String)ret.get("label");
		System.out.println("FIELD lbl="+lbl+",ret="+ret);
		if (lbl.startsWith("@")){
			DirectConfig cfg = (DirectConfig)LightUtil.getCfg();
			BeanFactory bf = cfg.getBeanFactory();
			
			try {
				BeanProxy bp = bf.getBeanProxy(this);
				InputStream is = bf.getResource(bp.getFsId(),lbl.substring(1));
				int len=is.available();
				byte[] buf=new byte[len];
				is.read(buf);
				String st=new String(Base64.encodeBase64(buf));
				String ctxType=Upload.getFileContentType(lbl);
				ret.put("label","<img style='width:24px;height:24px;' src=\"data:"+ctxType+";base64,"+st+"\">");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (form!=null){
			if (labelWidth==null && form.getLabelWidth()!=null){
				ret.put("labelWidth", form.getLabelWidth());
			}
			if (labelAlign==null && form.getLabelAlign()!=null){
				ret.put("labelAlign", form.getLabelAlign());
			}
		}
		return ret;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getPlaceHolder() {
		return placeHolder;
	}

	public void setPlaceHolder(String placeHolder) {
		this.placeHolder = placeHolder;
	}

	public String getLabelWidth() {
		return labelWidth;
	}

	public void setLabelWidth(String labelWidth) {
		this.labelWidth = labelWidth;
	}

	public String getLabelAlign() {
		return labelAlign;
	}

	public void setLabelAlign(String labelAlign) {
		this.labelAlign = labelAlign;
	}

	public Integer getMin() {
		return min;
	}

	public void setMin(Integer min) {
		this.min = min;
	}

	public Integer getMax() {
		return max;
	}

	public void setMax(Integer max) {
		this.max = max;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getRequired() {
		return required;
	}

	public void setRequired(String required) {
		this.required = required;
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

	public String getReadonly() {
		return readonly;
	}

	public void setReadonly(String readonly) {
		this.readonly = readonly;
	}

	public Boolean getCache() {
		return cache;
	}

	public void setCache(Boolean cache) {
		this.cache = cache;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public String getOnChange() {
		return onChange;
	}

	public void setOnChange(String onChange) {
		this.onChange = onChange;
	}
	
}
