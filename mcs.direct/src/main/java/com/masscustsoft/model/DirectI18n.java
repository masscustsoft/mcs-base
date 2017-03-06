package com.masscustsoft.model;

import com.masscustsoft.api.FullText;
import com.masscustsoft.api.IndexKey;
import com.masscustsoft.api.PrimaryKey;
import com.masscustsoft.api.SQLTable;
import com.masscustsoft.model.Entity;

@SQLTable("direct_i18n")
public class DirectI18n extends Entity {
	@IndexKey @PrimaryKey
	String keyId;
	
	@IndexKey @PrimaryKey
	String lang;
	
	@FullText
	String value;
	
	@IndexKey
	String status;
	
	@IndexKey
	String moduleId;
	
	public String getKeyId() {
		return keyId;
	}

	public void setKeyId(String keyId) {
		this.keyId = keyId;
	}

	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getModuleId() {
		return moduleId;
	}

	public void setModuleId(String moduleId) {
		this.moduleId = moduleId;
	}
	
}
