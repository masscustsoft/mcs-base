package com.masscustsoft.model;

import com.masscustsoft.model.BasicFile;

public class DirectDescrImage extends DirectDescr {
	BasicFile image=new BasicFile();

	String width;
	
	String allowZoom;
	
	public BasicFile getImage() {
		return image;
	}

	public void setImage(BasicFile image) {
		this.image = image;
	}
	public String getWidth() {
		return width;
	}

	public void setWidth(String width) {
		this.width = width;
	}

	public String getAllowZoom() {
		return allowZoom;
	}

	public void setAllowZoom(String allowZoom) {
		this.allowZoom = allowZoom;
	}
	
}
