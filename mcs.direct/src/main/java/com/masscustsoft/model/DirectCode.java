package com.masscustsoft.model;

import java.util.List;

import com.masscustsoft.api.FullText;
import com.masscustsoft.api.IndexKey;
import com.masscustsoft.api.SQLTable;
import com.masscustsoft.model.BasicFile;
import com.masscustsoft.model.Entity;

@SQLTable("direct_codes")
public class DirectCode extends Entity {
	@IndexKey
	String grpId;

	@IndexKey
	String codeId;

	@FullText
	String name;

	@IndexKey
	String parentId;

	BasicFile logo;

	public String getGrpId() {
		return grpId;
	}

	public void setGrpId(String grpId) {
		this.grpId = grpId;
	}

	public String getCodeId() {
		return codeId;
	}

	public void setCodeId(String codeId) {
		this.codeId = codeId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public BasicFile getLogo() {
		return logo;
	}

	public void setLogo(BasicFile logo) {
		this.logo = logo;
	}
}
