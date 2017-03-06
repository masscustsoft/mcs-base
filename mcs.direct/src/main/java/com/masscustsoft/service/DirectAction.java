package com.masscustsoft.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.masscustsoft.helper.Upload;
import com.masscustsoft.model.AbstractResult;
import com.masscustsoft.model.DirectUser;
import com.masscustsoft.util.DataUtil;
import com.masscustsoft.util.LightStr;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.LogUtil;
import com.masscustsoft.util.MapUtil;
import com.masscustsoft.util.ThreadHelper;

public class DirectAction extends DirectComponent {
	String id;

	String name;

	String featureId,groupId;

	boolean admin = false;

	String next;

	String ignore;

	boolean saRequired=false;
	
	Boolean cacheable;
	
	Map<String,String> fieldMapping=new HashMap<String,String>();
	
	@Override
	public void init(DirectConfig cfg) throws Exception{
		super.init(cfg);
		if (LightStr.isEmpty(id))
			id = LightStr.decapitalize(getClass().getSimpleName());
		cfg.getActionMap().put(id, this);
	}

	public final void _run(AbstractResult ret) throws Exception {
		ThreadHelper.set("$$me", this);
		ThreadHelper.set("$$ret", ret);
		Map i18n=new HashMap();
		ThreadHelper.set("$$i18n",i18n);
		
		initSession();
		boolean useTransaction=!getStr("useTransaction","").equals("false");
		if (useTransaction){
			DataUtil.startTransaction();
		}
		try{
			if (saRequired){
				cfg.getHooks(this, "SaAuthorize", Upload.getUpload().getFieldMap());
			}
			if (fieldMapping.size()>0){
				Upload up=Upload.getUpload();
				for (String innerId:fieldMapping.keySet()){
					String outerId=fieldMapping.get(innerId);
					String v = up.getStr(outerId, null);
					if (outerId.contains("${")){
						v=LightUtil.macro(outerId, '$', Upload.getUpload().getFieldMap()).toString();
					}
					else{
						up.setStr(outerId, null);
					}
					up.setStr(innerId, v);
				}
			}
			LogUtil.debug("UPLOADED UPMAP=" + Upload.getUpload().getFieldMap());
			if (!LightStr.isEmpty(ignore)) {
				String val = LightUtil.macro(ignore, '$', Upload.getUpload().getFieldMap()).toString();
				if (!"true".equals(val)){
					run(ret);
				}
			} else
				run(ret);
			
			if (i18n.size()>0){
				Object tar=ret.getResult();
				if (tar==null){
					tar=new HashMap();
					ret.setResult(tar);
				}
				if (tar instanceof Map){
					Map map=(Map)tar;
					map.put("i18n", i18n);
				}
				else ret.setAttribute("i18n", i18n);
			}
			
			setFormDefaults();
			
			if (next != null) {
				List<String> chains = MapUtil.getSelectList(next);
				for (String act : chains) {
					AbstractResult res=ret.getClass().newInstance();
					call(act, res);
				}
			}
			if (useTransaction){	
				if (ret.getSuccess()) DataUtil.commitTransaction();
				else DataUtil.rollbackTransaction();
			}
		}
		catch (Exception e){
			if (useTransaction) {
				DataUtil.rollbackTransaction();
			}
			throw e;
		}
	}

	protected void run(AbstractResult ret) throws Exception {

	}

	public void sendMessage(String toId, String title, String body) throws Exception {
		if (getUserId().equals(toId)) return;
		NotifyService ntf = getCfg().getNotifyService();
		if (ntf!=null){
			DirectUser u=getDs().getBean(DirectUser.class, "userId", toId);
			if (u!=null && !LightStr.isEmpty(u.getCellNo()) && u.getCellNo().contains("@")){
				ntf.sendMessage(u.getCellNo(), title, body, null);
			}
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFeatureId() {
		return featureId;
	}

	public void setFeatureId(String featureId) {
		this.featureId = featureId;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public boolean isAdmin() {
		return admin;
	}

	public void setAdmin(boolean admin) {
		this.admin = admin;
	}

	public String getNext() {
		return next;
	}

	public void setNext(String next) {
		this.next = next;
	}

	public boolean isSaRequired() {
		return saRequired;
	}

	public void setSaRequired(boolean saRequired) {
		this.saRequired = saRequired;
	}

	public String getIgnore() {
		return ignore;
	}

	public void setIgnore(String ignore) {
		this.ignore = ignore;
	}

	public Map<String, String> getFieldMapping() {
		return fieldMapping;
	}

	public void setFieldMapping(Map<String, String> fieldMapping) {
		this.fieldMapping = fieldMapping;
	}

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public Boolean getCacheable() {
		return cacheable;
	}

	public void setCacheable(Boolean cacheable) {
		this.cacheable = cacheable;
	}

	public boolean isRelated(Class c) throws Exception {
		return false;
	}

}
