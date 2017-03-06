package com.masscustsoft.model;

import com.masscustsoft.api.IndexKey;
import com.masscustsoft.api.SQLSize;

public class Sequence extends Entity {
	@IndexKey @SQLSize(64)
	String seqId,ancestorId;
	
	long value;

	public String getSeqId() {
		return seqId;
	}

	public void setSeqId(String seqId) {
		this.seqId = seqId;
	}

	public long getValue() {
		return value;
	}

	public void setValue(long value) {
		this.value = value;
	}

	public String getAncestorId() {
		return ancestorId;
	}

	public void setAncestorId(String ancestorId) {
		this.ancestorId = ancestorId;
	}

}
