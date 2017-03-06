package com.masscustsoft.service;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.masscustsoft.api.DateIndex;
import com.masscustsoft.api.FullText;
import com.masscustsoft.api.IndexKey;
import com.masscustsoft.api.NumIndex;
import com.masscustsoft.api.TimestampIndex;
import com.masscustsoft.model.AbstractResult;
import com.masscustsoft.model.Entity;
import com.masscustsoft.service.PageInfo;
import com.masscustsoft.util.ReflectUtil;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.LightStr;

public class GetItems extends GetRecordList {

	String idField, nameField, parentField;
	String idPrefix;
	

	@Override
	public void init(DirectConfig cfg) throws Exception{
		super.init(cfg);
		if (this.getCacheable()==null) this.setCacheable(true);
	}
	
	@Override
	protected void doFilterOut(Entity en, Map rec) throws Exception {
		String id = ReflectUtil.getProperty(en, idField).toString();
		String name = (String) ReflectUtil.getProperty(en, nameField);
		rec.clear();
		if (!LightStr.isEmpty(idPrefix)) id=idPrefix+id;
		rec.put("id", id);
		rec.put("name", name);
		processExtraFields(en,rec);
	}

	@Override
	public Map prepare(AbstractResult ret) throws Exception {
		Class<? extends Entity> cls = getModelClass();

		if (LightStr.isEmpty(idField)) {
			List<String> keys = this.getPrimaryKeys(null);
			idField = keys.get(keys.size() - 1);
		}
		if (LightStr.isEmpty(nameField)) {
			nameField = "name";
		}

		Map filter = getFilterMap();
		doFilterIn(filter);
		if (!LightStr.isEmpty(parentField) && !LightStr.isEmpty(getStr("parentId",""))) {
			filter.put(parentField,getStr("parentId",""));
		}
		
		//automatically add extra fields for search
		List<Field> flds = ReflectUtil.getFieldMap(cls);
		for (Field f:flds){
			if (filter.containsKey(f.getName())) continue;
			if (f.getName().equals("text")) continue;
			String v=getStr(f.getName(),"");
			if (LightStr.isEmpty(v)) continue;
			if (f.isAnnotationPresent(IndexKey.class) || f.isAnnotationPresent(FullText.class) || f.isAnnotationPresent(DateIndex.class) || f.isAnnotationPresent(TimestampIndex.class) || f.isAnnotationPresent(NumIndex.class)){
				filter.put(f.getName(),v);
			}
		}
		//
		return filter;
	}
	
	@Override
	public void run(AbstractResult ret, Map filter) throws Exception {
		Class<? extends Entity> cls = getModelClass();
		List<Map> result = new ArrayList<Map>();
		System.out.println("getItems cls="+cls+" .filter="+filter);
		PageInfo<Entity> pg = getDs().getBeanList(new Class[] { cls }, LightUtil.toJsonString(filter).toString(), getStr("text", ""), getInt("start"),
				getInt("limit", 5), sort, "", false);
		for (Entity c : pg.getList()) {
			Map m = (Map) LightUtil.toJsonObject(c);
			doFilterOut(c, m);
			result.add(m);
		}

		String cur = getStr("currentValue", "");
		if (!LightStr.isEmpty(cur)) {
			if (!LightStr.isEmpty(idPrefix)){
				if (cur.startsWith(idPrefix)) cur=cur.substring(idPrefix.length());
			}
			filter.put(idField, cur);
			pg = getDs().getBeanList(new Class[] { cls }, LightUtil.toJsonString(filter).toString(), "", 0, 1, sort, "", false);
			for (Entity c : pg.getList()) {
				Map m = (Map) LightUtil.toJsonObject(c);
				doFilterOut(c, m);
				result.add(m);
			}
		}
		ret.setResult(result);
	}

	@Override
	public boolean isRelated(Class c) throws Exception {
		Class<? extends Entity> cls = getModelClass();
		return cls.isAssignableFrom(c);
	}
	
	public String getIdField() {
		return idField;
	}

	public void setIdField(String idField) {
		this.idField = idField;
	}

	public String getNameField() {
		return nameField;
	}

	public void setNameField(String nameField) {
		this.nameField = nameField;
	}

	public Map getFilter() {
		return filter;
	}

	public void setFilter(Map filter) {
		this.filter = filter;
	}

	public String getParentField() {
		return parentField;
	}

	public void setParentField(String parentField) {
		this.parentField = parentField;
	}

	public String getIdPrefix() {
		return idPrefix;
	}

	public void setIdPrefix(String idPrefix) {
		this.idPrefix = idPrefix;
	}
}
