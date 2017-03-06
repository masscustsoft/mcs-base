package com.masscustsoft.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.masscustsoft.api.FullBody;
import com.masscustsoft.api.ICatalog;
import com.masscustsoft.api.IndexKey;
import com.masscustsoft.model.Entity;
import com.masscustsoft.util.MapUtil;

public class DirectClassifyItem extends Entity implements ICatalog{
	String catalog;

	@FullBody
	String catalogKeys, catalogGrps;

	@IndexKey
	String publishStatus; //online, offline
	
	public String getCatalog() {
		return catalog;
	}

	public void setCatalog(String catalog) {
		this.catalog = catalog;
	}

	public String getCatalogGrps() {
		return catalogGrps;
	}

	public void setCatalogGrps(String catalogGrps) {
		this.catalogGrps = catalogGrps;
	}

	//Catalog owner
	protected String getClassifyOwnerId(){
		return "~";
	}
	
	protected String getClassifyType(){
		return "~";
	}
	
	//Descr Owner Id
	protected String getDescrOwnerId(){
		return "~";
	}
	
	protected void resetCatalog() throws Exception {
		List<String> cats=MapUtil.getSelectList(getCatalogKeys());
		for (String keyId:cats){
			List<DirectClassifyCatalog> cc=dataService.getBeanList(DirectClassifyCatalog.class, "{ownerId:'"+ getClassifyOwnerId()+"',classifyType:'"+ getClassifyType()+"', keyId:'"+ keyId+"'}", "");
			for (DirectClassifyCatalog c:cc){
				c.resetStatus();
				while (!"ROOT".equals(c.getParentId())){
					c=dataService.getBean(DirectClassifyCatalog.class, "uuid", c.getParentId());
					if (c!=null){
						c.resetStatus();
					}
					else break;
				}
			}
		}
		cats=MapUtil.getSelectList(getCatalogGrps());
		for (String groupId:cats){
			List<DirectClassifyCatalog> cc=dataService.getBeanList(DirectClassifyCatalog.class, "{ownerId:'"+ getClassifyOwnerId()+"',classifyType:'"+ getClassifyType()+"', groupId:'"+ groupId+"'}", "");
			for (DirectClassifyCatalog c:cc){
				c.resetStatus();
				while (!"ROOT".equals(c.getParentId())){
					c=dataService.getBean(DirectClassifyCatalog.class, "uuid", c.getParentId());
					if (c!=null){
						c.resetStatus();
					}
					else break;
				}
			}
		}
	}
	
	@Override
	public void validateDelete() throws Exception {
		resetCatalog();
		super.validateDelete();
	}

	public String getCatalogKeys() {
		return catalogKeys;
	}

	public void setCatalogKeys(String catalogKeys) {
		this.catalogKeys = catalogKeys;
	}

	@Override
	public void validate() throws Exception {
		List<String> keys=MapUtil.getSelectList(getCatalog());
		List<String> grps=new ArrayList<String>();
		for (String keyId:keys){
			DirectClassifyKey ck=dataService.getBean(DirectClassifyKey.class, "ownerId", getClassifyOwnerId(), "classifyType", getClassifyType(), "keyId", keyId);
			if (ck!=null && !grps.contains(ck.getGroupId())) grps.add(ck.getGroupId());
		}
		setCatalogKeys(MapUtil.mergeSelectList(keys));
		setCatalogGrps(MapUtil.mergeSelectList(grps));
		if (old!=null){
			DirectClassifyItem ci=(DirectClassifyItem)old;
			ci.setDataService(dataService);
			ci.resetCatalog();
		}
		resetCatalog();
		super.validate();
	}
	
	public void processFilter(Map filter){
		filter.put("publicStatus", "visible");
	}

	@Override
	public boolean beforeDelete() throws Exception {
		dataService.deleteBeanList(DirectDescr.class, "{ownerId:'"+this.getDescrOwnerId()+"'}");
		return super.beforeDelete();
	}

	@Override
	public boolean beforeInsert() throws Exception {
		if (publishStatus==null) setPublishStatus("invisible");
		return super.beforeInsert();
	}

	public String getPublishStatus() {
		return publishStatus;
	}

	public void setPublishStatus(String publishStatus) {
		this.publishStatus = publishStatus;
	}

}
