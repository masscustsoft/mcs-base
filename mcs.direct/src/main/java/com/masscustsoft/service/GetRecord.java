package com.masscustsoft.service;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.masscustsoft.api.IFile;
import com.masscustsoft.api.JsonField;
import com.masscustsoft.helper.Upload;
import com.masscustsoft.model.AbstractResult;
import com.masscustsoft.model.Entity;
import com.masscustsoft.model.JsonResult;
import com.masscustsoft.service.PageInfo;
import com.masscustsoft.util.MapUtil;
import com.masscustsoft.util.ReflectUtil;
import com.masscustsoft.util.LightUtil;

public class GetRecord extends RecordAction {
	
	@Override
	protected void doFilterOut(Entity en, Map rec) throws Exception {
		super.doFilterOut(en, rec);
		for (Field f : ReflectUtil.getFieldMap(en.getClass())) {
			if (IFile.class.isAssignableFrom(f.getType())) {
				rec.put(f.getName(), getImageUrl((IFile) ReflectUtil.getProperty(en, f.getName())));
			}
			if (Map.class.isAssignableFrom(f.getType())){
				JsonField jf=f.getAnnotation(JsonField.class);
				if (jf!=null && jf.output()){
					rec.put(f.getName(),LightUtil.toJsonString(ReflectUtil.getProperty(en, f.getName())).toString());
				}
			}
		}
	}

	@Override
	public Map prepare(AbstractResult ret) throws Exception {
		Map filter = getFilterMap();
		Class<? extends Entity> cls = getModelClass();
		doFilterIn(filter);
		addPrimaryKeys(cls, filter);
		return filter;
	}
	
	@Override
	public void run(AbstractResult ret, Map filter) throws Exception {
		Class<? extends Entity> cls = getModelClass();
		PageInfo<Entity> pg = getDs().getBeanList(new Class[] { cls }, LightUtil.toJsonString(filter).toString(), "",
				0, 1, sort, "", false);
		if (pg.getList().size() > 0) {
			Entity c = pg.getList().get(0);
			Map m = (Map) LightUtil.toJsonObject(c);
			
			doFilterOut(c, m);
			if (hook!=null) cfg.getHooks(this, hook, m);
			
			ret.setResult(m);
		} else {
			ret.setResult(new HashMap());
		}
	}

	public Map<String, Object> getFilter() {
		return filter;
	}

	public void setFilter(Map<String, Object> filter) {
		this.filter = filter;
	}
	
	public void runActions(List<Map> actions) throws Exception{
		Upload up=Upload.getUpload();
		for (Map m:actions){
			up.getFieldMap().putAll(m);
			
			JsonResult ret = new JsonResult();
			String cmd = up.getStr("action", "");
			call(cmd, ret);
		}
	}
}
