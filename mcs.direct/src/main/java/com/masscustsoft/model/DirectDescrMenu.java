package com.masscustsoft.model;

public class DirectDescrMenu extends DirectDescr {
	String text;
	
	String targetOwnerId;

	String targetTitle;
	
	String targetWidth,targetHeight;
	
	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getTargetOwnerId() {
		return targetOwnerId;
	}

	public void setTargetOwnerId(String targetOwnerId) {
		this.targetOwnerId = targetOwnerId;
	}

	public String getTargetTitle() {
		return targetTitle;
	}

	public void setTargetTitle(String targetTitle) {
		this.targetTitle = targetTitle;
	}
}
