package com.masscustsoft.model;

import java.sql.Timestamp;

import com.masscustsoft.api.IndexKey;
import com.masscustsoft.api.JsonField;
import com.masscustsoft.api.NumIndex;
import com.masscustsoft.api.SQLTable;
import com.masscustsoft.api.SequenceId;
import com.masscustsoft.api.TimestampIndex;
import com.masscustsoft.util.LightStr;

@SQLTable("Classifies")
public class DirectClassifyCatalog extends ParentalEntity {
	@IndexKey
	String ownerId;
	
	@IndexKey
	String classifyType; //the Entity to calculate the count. Such as Indivual, Group, stands for a ClassifyItem
	
	@NumIndex @SequenceId({"ownerId","parentId"})
	Long sequenceId;
	
	@IndexKey
	String catalogType; //group/key
	
	@IndexKey
	String keyId,keyGrpId; //keyGrpId is a convinent value to seek for leader
	
	@IndexKey
	String groupId;
	
	@IndexKey
	String asLeader; // if it's key and is leader, all its brothers will be followed here.
	
	@IndexKey
	String asTemplate; // yes, it's defined from user, no it's generated to comply 
	
	@TimestampIndex
	Timestamp updateTime;
	
	@IndexKey
	String status; //updated
	
	@JsonField
	transient String name;
	
	@JsonField
	transient BasicFile image;
	
	@NumIndex
	int count;
	
	@Override
	public Class getPrimaryClass() {
		return DirectClassifyCatalog.class;
	}
	
	public Long getSequenceId() {
		return sequenceId;
	}

	public void setSequenceId(Long sequenceId) {
		this.sequenceId = sequenceId;
	}

	public String getKeyId() {
		return keyId;
	}

	public void setKeyId(String keyId) {
		this.keyId = keyId;
	}

	public Timestamp getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Timestamp updateTime) {
		this.updateTime = updateTime;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public void resetStatus() throws Exception {
		if ("updated".equals(status)){
			status="updating";
			dataService.updateBean(this);
		}
	}

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	@Override
	public void validate() throws Exception {
		if ("group".equals(catalogType)){
			asLeader="no";
			asTemplate="no";
			keyId="";
			keyGrpId="";
		}
		else{
			groupId="";
			
		}
		super.validate();
	}

	public boolean isLeader(){
		return !LightStr.isEmpty(keyId) && "yes".equals(asLeader);
	}
	
	public boolean isTemplate(){
		return "yes".equals(asTemplate);
	}
	
	public String getId(){
		return (LightStr.isEmpty(groupId)?"":groupId)+"-"+(LightStr.isEmpty(keyId)?"":keyId);
	}

	public String getKeyGrpId() {
		return keyGrpId;
	}

	public void setKeyGrpId(String keyGrpId) {
		this.keyGrpId = keyGrpId;
	}

	public String getAsLeader() {
		return asLeader;
	}

	public void setAsLeader(String asLeader) {
		this.asLeader = asLeader;
	}

	public String getAsTemplate() {
		return asTemplate;
	}

	public void setAsTemplate(String asTemplate) {
		this.asTemplate = asTemplate;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getClassifyType() {
		return classifyType;
	}

	public void setClassifyType(String classifyType) {
		this.classifyType = classifyType;
	}

	public String getCatalogType() {
		return catalogType;
	}

	public void setCatalogType(String catalogType) {
		this.catalogType = catalogType;
	}
}
