package com.masscustsoft.model;

import com.masscustsoft.api.FullBody;
import com.masscustsoft.api.IndexKey;
import com.masscustsoft.api.PrimaryKey;
import com.masscustsoft.api.SQLTable;
import com.masscustsoft.model.Entity;

@SQLTable("direct_svg")
public class DirectSvg extends Entity {
	@IndexKey @PrimaryKey
	String svgId;
	
	@FullBody
	String content;
	
	@FullBody
	String refers;

	public String getRefers() {
		return refers;
	}

	public void setRefers(String refers) {
		this.refers = refers;
	}

	public String getSvgId() {
		return svgId;
	}

	public void setSvgId(String svgId) {
		this.svgId = svgId;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}
	
}
