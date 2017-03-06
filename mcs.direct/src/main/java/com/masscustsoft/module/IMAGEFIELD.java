package com.masscustsoft.module;

public class IMAGEFIELD extends FIELD {
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
