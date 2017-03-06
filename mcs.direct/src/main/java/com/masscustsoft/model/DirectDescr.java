package com.masscustsoft.model;

import com.masscustsoft.api.AutoInc;
import com.masscustsoft.api.IndexKey;
import com.masscustsoft.api.PrimaryKey;
import com.masscustsoft.api.SQLTable;
import com.masscustsoft.api.SequenceId;
import com.masscustsoft.model.Entity;

@SQLTable("Descrs")
public class DirectDescr extends Entity{
	@IndexKey @PrimaryKey
	String ownerId; //productId or any id
	
	@IndexKey @PrimaryKey @AutoInc
	Integer descrId;

	@IndexKey @SequenceId
	Integer sequenceId;
	
	@Override
	public Class getPrimaryClass() {
		return DirectDescr.class;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}

	public Integer getDescrId() {
		return descrId;
	}

	public void setDescrId(Integer descrId) {
		this.descrId = descrId;
	}

	public Integer getSequenceId() {
		return sequenceId;
	}

	public void setSequenceId(Integer sequenceId) {
		this.sequenceId = sequenceId;
	}

}
