package com.masscustsoft.model;

import com.masscustsoft.api.AutoInc;
import com.masscustsoft.api.FullText;
import com.masscustsoft.api.IndexKey;
import com.masscustsoft.api.JsonField;
import com.masscustsoft.api.NumIndex;
import com.masscustsoft.api.PrimaryKey;
import com.masscustsoft.api.SQLTable;
import com.masscustsoft.api.SequenceId;
import com.masscustsoft.util.LightStr;
import com.masscustsoft.util.MapUtil;

@SQLTable("Classifies")
public class DirectClassifyKey extends Entity {
	@IndexKey @PrimaryKey
	String ownerId="default";
	
	@IndexKey @PrimaryKey @AutoInc("C")
	String keyId;
	
	@NumIndex @SequenceId({"ownerId","groupId"})
	Long sequenceId;
	
	@FullText
	String name;
	
	@IndexKey
	String groupId;

	@JsonField
	BasicFile image=new BasicFile();
	
	@FullText
	String parentKeys;
	
	@FullText
	String groupName;
	
	@IndexKey
	String classifyType;
	
	String parentCatalog;
	
	public String getKeyId() {
		return keyId;
	}

	public void setKeyId(String keyId) {
		this.keyId = keyId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}
	
	public BasicFile getImage() {
		return image;
	}

	public void setImage(BasicFile image) {
		this.image = image;
	}

	public Long getSequenceId() {
		return sequenceId;
	}

	public void setSequenceId(Long sequenceId) {
		this.sequenceId = sequenceId;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}

	public String getParentKeys() {
		return parentKeys;
	}

	public void setParentKeys(String parentKeys) {
		this.parentKeys = parentKeys;
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public String getClassifyType() {
		return classifyType;
	}

	public void setClassifyType(String classifyType) {
		this.classifyType = classifyType;
	}

	public String getParentCatalog() {
		return parentCatalog;
	}

	public void setParentCatalog(String parentCatalog) {
		this.parentCatalog = parentCatalog;
	}

	@Override
	public void validate() throws Exception {
		parentKeys=MapUtil.mergeSelectList(MapUtil.getSelectList(parentCatalog));
		if (LightStr.isEmpty(parentKeys)) parentKeys="default";
		super.validate();
	}

}
