package com.masscustsoft.model;

import com.masscustsoft.api.FullText;
import com.masscustsoft.api.IndexKey;
import com.masscustsoft.api.SQLSize;

public class Variable extends Entity {
	@IndexKey @SQLSize(64)
	String varId;
	
	@FullText @SQLSize(255)
	String value;

	public String getVarId() {
		return varId;
	}

	public void setVarId(String varId) {
		this.varId = varId;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

}
