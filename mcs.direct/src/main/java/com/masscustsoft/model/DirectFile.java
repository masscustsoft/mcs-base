package com.masscustsoft.model;

import java.sql.Timestamp;

import com.masscustsoft.api.FullText;
import com.masscustsoft.api.IndexKey;
import com.masscustsoft.api.SQLTable;
import com.masscustsoft.api.TimestampIndex;
import com.masscustsoft.model.BasicFile;
import com.masscustsoft.model.Entity;

@SQLTable("direct_files")
public class DirectFile extends Entity {
	BasicFile file=new BasicFile();
	
	@IndexKey
	String ownerId;
	
	@FullText
	String name;
	
	@TimestampIndex
	Timestamp createTime;

	public BasicFile getFile() {
		return file;
	}

	public void setFile(BasicFile file) {
		this.file = file;
	}

	public Timestamp getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Timestamp createTime) {
		this.createTime = createTime;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}
}
