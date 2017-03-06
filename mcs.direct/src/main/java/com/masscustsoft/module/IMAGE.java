package com.masscustsoft.module;

import java.io.InputStream;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;

import com.masscustsoft.helper.Upload;
import com.masscustsoft.service.DirectConfig;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.xml.BeanFactory;
import com.masscustsoft.xml.BeanProxy;

public class IMAGE extends ELEMENT {
	String src;

	Integer corner, resizeWidth, resizeHeight;
	
	String bgColor;
	
	Boolean stretch;
	
	public String getSrc() {
		return src;
	}

	public void setSrc(String src) {
		this.src = src;
	}

	public Integer getCorner() {
		return corner;
	}

	public void setCorner(Integer corner) {
		this.corner = corner;
	}

	public Integer getResizeWidth() {
		return resizeWidth;
	}

	public void setResizeWidth(Integer resizeWidth) {
		this.resizeWidth = resizeWidth;
	}

	public Integer getResizeHeight() {
		return resizeHeight;
	}

	public void setResizeHeight(Integer resizeHeight) {
		this.resizeHeight = resizeHeight;
	}

	public String getBgColor() {
		return bgColor;
	}

	public void setBgColor(String bgColor) {
		this.bgColor = bgColor;
	}

	public Boolean getStretch() {
		return stretch;
	}

	public void setStretch(Boolean stretch) {
		this.stretch = stretch;
	}
}
