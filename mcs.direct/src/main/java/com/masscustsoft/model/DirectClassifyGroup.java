package com.masscustsoft.model;

import java.util.ArrayList;
import java.util.List;

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
public class DirectClassifyGroup extends Entity{
	@IndexKey @PrimaryKey
	String ownerId="default";
	
	@IndexKey @PrimaryKey @AutoInc("G")
	String groupId;
	
	@IndexKey
	String classifyType;
	
	@NumIndex @SequenceId()
	Long sequenceId;
	
	@FullText
	String name;

	@JsonField
	BasicFile image=new BasicFile();
	
	@NumIndex
	int keyCount=0;
	
	@FullText
	String parentKeys;
	
	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public boolean beforeDelete() throws Exception {
		dataService.deleteBeanList(DirectClassifyCatalog.class, "{ownerId:'"+ownerId+"',catalogType:'group',groupId:'"+groupId+"'}");
		dataService.deleteBeanList(DirectClassifyCatalog.class, "{ownerId:'"+ownerId+"',catalogType:'key',keyGrpId:'"+groupId+"'}");
		dataService.deleteBeanList(DirectClassifyKey.class, "{ownerId:'"+ownerId+"',groupId:'"+groupId+"'}");
		return super.beforeDelete();
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

	public String getClassifyType() {
		return classifyType;
	}

	public void setClassifyType(String classifyType) {
		this.classifyType = classifyType;
	}

	public int getKeyCount() {
		return keyCount;
	}

	public void setKeyCount(int keyCount) {
		this.keyCount = keyCount;
	}

	@Override
	public boolean beforeInsert() throws Exception {
		keyCount=0;
		parentKeys="default";
		return super.beforeInsert();
	}

	@Override
	public boolean beforeUpdate() throws Exception {
		if (LightStr.isEmpty(parentKeys)) parentKeys="default";
		if (changed("name")){
			List<DirectClassifyKey> keys = dataService.getBeanList(DirectClassifyKey.class, "{ownerId:'"+ownerId+"',groupId:'"+groupId+"'}", "");
			for (DirectClassifyKey key:keys){
				key.setGroupName(name);
				key.setClassifyType(classifyType);
				dataService.updateBean(key);
			}
		}
		return super.beforeUpdate();
	}

	public String getParentKeys() {
		return parentKeys;
	}

	public void setParentKeys(String parentKeys) {
		this.parentKeys = parentKeys;
	}
	
	public void recalcKeys() throws Exception{
		List<String> klist=new ArrayList<String>();
		List<DirectClassifyKey> keys = dataService.getBeanList(DirectClassifyKey.class, "{ownerId:'"+ownerId+"',classifyType:'"+classifyType+"',groupId:'"+groupId+"'}", "");
		for (DirectClassifyKey key:keys){
			List<String> kk=MapUtil.getSelectList(key.getParentKeys());
			for (String k:kk){
				if (!klist.contains(k)) klist.add(k);
			}
		}
		if (klist.size()==0) parentKeys="default"; else parentKeys=MapUtil.mergeSelectList(klist);
		dataService.updateBean(this);

	}
}
