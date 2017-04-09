package com.masscustsoft.service;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.masscustsoft.Lang.CLASS;
import com.masscustsoft.api.AutoInc;
import com.masscustsoft.api.IFile;
import com.masscustsoft.api.PrimaryKey;
import com.masscustsoft.helper.Upload;
import com.masscustsoft.model.AbstractResult;
import com.masscustsoft.model.Entity;
import com.masscustsoft.util.MapUtil;
import com.masscustsoft.util.ReflectUtil;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.LightStr;

public class RecordAction extends DirectAction {
	String model;

	String keyFields;
	
	String sort;
	
	String keepIfEmpty;
	
	String removes;
	
	String hook;
	
	String modelTypeField;
	
	Map<String,String> modelTypeMapping=new HashMap<String,String>();
	
	Map<String,Object> filter=new HashMap();
	
	@Override
	final protected void run(AbstractResult ret) throws Exception {
		Map filter=prepare(ret);
		run(ret,filter);
	}
	
	public Map prepare(AbstractResult ret) throws Exception {
		return new HashMap();
	}
	
	protected void run(AbstractResult ret, Map filter) throws Exception {

	}
	
	protected Class<? extends Entity> getModelClass() throws Exception{
		return getModelClass(this.model);
	}
	
	protected Class<? extends Entity> getModelClass(String model) throws Exception{
		if (LightStr.isEmpty(model)) {
			if (!LightStr.isEmpty(modelTypeField)){
				String type=getStr(modelTypeField,"");
				model=modelTypeMapping.get(type);
				if (LightStr.isEmpty(model)) model=type;
				if (LightStr.isEmpty(model)) {
					throw new Exception("ObjectNotFound");
				}
			}
			else{
				throw new Exception("ObjectNotFound");
			}
		}
		return CLASS.forName(cfg.getBeanFactory().findRealClass(model));
	}
	
	protected List<String> getPrimaryKeys(Class c,boolean insert) throws Exception{
		return getPrimaryKeys(c,keyFields,insert);
	}
	protected List<String> getPrimaryKeys(Class c, String keyFields, boolean insert) throws Exception{
		List<String> keys=new ArrayList<String>();
		if (c==null) c=getModelClass();
		List<Field> flds = ReflectUtil.getFieldMap(c);
		
		if (LightStr.isEmpty(keyFields)){
			for (Field f : flds) {
				PrimaryKey pk = (PrimaryKey) f.getAnnotation(PrimaryKey.class);
				if (pk!=null){
					if (insert){
						if (f.getAnnotation(AutoInc.class)!=null) continue;
					}
					keys.add(f.getName());
				}
			}
			
		}
		else{
			List<String> items=MapUtil.getSelectList(keyFields);
			for (Field f : flds) {
				if (!items.contains(f.getName())) continue;
				if (insert){
					if (f.getAnnotation(AutoInc.class)!=null) continue;
				}
				keys.add(f.getName());
			}
		}
		
		return keys;
	}
	
	protected void doFilterIn(Map filter) throws Exception {
	}
	
	protected void doFilterOut(Entity en, Map rec) throws Exception {
		if (fieldMapping.size()>0){
			for (String innerId:fieldMapping.keySet()){
				String outerId=fieldMapping.get(innerId);
				Object v = rec.get(innerId);
				rec.remove(innerId);
				rec.put(outerId,v);
			}
		}
		if (removes!=null){
			List<String> mm = MapUtil.getSelectList(removes);
			for (String ss:mm){
				rec.remove(ss);
			}
		}
	}

	protected void addPrimaryKeys(Class c, Map filter) throws Exception{
		List<String> keys = getPrimaryKeys(c,false);
		for (String key:keys){
			if (filter.containsKey(key)) continue;
			String id=key,uploadId=key;
			int i=key.indexOf("=");
			if (i>0){
				id=key.substring(0,i);
				uploadId=key.substring(i+1);
			}
			String val=requiredStr(uploadId);
			filter.put(id,val);
		}
		if (keys.size()==0){
			filter.put("uuid", requiredStr("uuid"));
		}
	}
	
	protected void updateEntity(Entity en, boolean withPk) throws Exception{
		List<Field> flds = ReflectUtil.getFieldMap(en.getClass());
		List<String> keeps = MapUtil.getSelectList(keepIfEmpty);
//		if (en instanceof IFile){
//			Upload up=Upload.getUpload();
//			IFile file=(IFile)en;
//			updateFile(en,file,file.getName(),"");
//			up.remove("size");
//			up.remove("externalId");
//		}
		for (Field f : flds) {
			if (!withPk){
				PrimaryKey pk = (PrimaryKey) f.getAnnotation(PrimaryKey.class);
				if (pk!=null) continue;
			}
			AutoInc inc=(AutoInc) f.getAnnotation(AutoInc.class);
			if (inc!=null) continue;
			
			if (IFile.class.isAssignableFrom(f.getType())){
				updateAttachment(en, f.getName());
				continue;
			}
			String val=getStr(f.getName(),null);
			if (val==null) continue;
			
			if (LightStr.isEmpty(val)){
				if (keeps.contains(f.getName())) continue;
			}
//			if (IFile.class.isAssignableFrom(f.getType())){
//				IFile file=(IFile)ReflectUtil.getProperty(en, f.getName());
//				if (file==null){
//					file=(IFile)f.getType().newInstance();
//					ReflectUtil.setProperty(en, f.getName(),file);
//				}
//				updateFile(en,file,f.getName(),"");
//			}
			if (Map.class.isAssignableFrom(f.getType())){
				Map m=(Map)LightUtil.parseJson(val);
				ReflectUtil.setProperty(en, f.getName(), m);
				continue;
			}
			
			Object obj=LightUtil.decodeObject(val, f.getType(),true);
			ReflectUtil.setProperty(en, f.getName(), obj);
		}
		cfg.incVersion(en.getClass());
	}
	
//	private void updateFile(Entity en, IFile file, String fldName, String prefix) throws Exception{
//		Upload up=Upload.getUpload();
//		if (!LightStr.isEmpty(file.getExternalId())){
//			en.deleteFile(file);
//		}
//		String sizes=up.getStr(prefix+fldName+"_image", null);
//		String remove=up.getStr(prefix+fldName+"_remove", null);
//		
//		if ("yes".equals(remove)||"Y".equals(remove)) return;
//		
//		FileItem it = up.getFileItem(prefix+fldName);
//		if (sizes==null||sizes.indexOf(':')<0){
//			 ExternalFile.newExternalFile(getDs(), getFs(),file, it);
//		}
//		else{
//			String[] ss=sizes.split(":");
//			int imgWidth=LightUtil.decodeInt(ss[0]);
//			int imgHeight=LightUtil.decodeInt(ss[1]);
//			
//			TempItem temp=FileUtil.getResizedImage(it.getInputStream(), imgWidth, imgHeight, null, 0);
//			ExternalFile.newExternalFile(getDs(), getFs(), file, temp, it.getName());
//			temp.delete();
//		}
//	}
	
	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public String getSort() {
		return sort;
	}

	public void setSort(String sort) {
		this.sort = sort;
	}

	public String getKeyFields() {
		return keyFields;
	}

	public void setKeyFields(String keyFields) {
		this.keyFields = keyFields;
	}

	public String getKeepIfEmpty() {
		return keepIfEmpty;
	}

	public void setKeepIfEmpty(String keepIfEmpty) {
		this.keepIfEmpty = keepIfEmpty;
	}

	public String getRemoves() {
		return removes;
	}

	public void setRemoves(String removes) {
		this.removes = removes;
	}

	public String getHook() {
		return hook;
	}

	public void setHook(String hook) {
		this.hook = hook;
	}

	public String getModelTypeField() {
		return modelTypeField;
	}

	public void setModelTypeField(String modelTypeField) {
		this.modelTypeField = modelTypeField;
	}

	public Map<String, String> getModelTypeMapping() {
		return modelTypeMapping;
	}

	public void setModelTypeMapping(Map<String, String> modelTypeMapping) {
		this.modelTypeMapping = modelTypeMapping;
	}

	protected Map getFilterMap(){
		return MapUtil.getMacroMap(filter);
	}

	public Map<String, Object> getFilter() {
		return filter;
	}

	public void setFilter(Map<String, Object> filter) {
		this.filter = filter;
	}
	
}
