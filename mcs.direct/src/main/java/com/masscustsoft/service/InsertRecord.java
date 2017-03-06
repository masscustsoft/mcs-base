package com.masscustsoft.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.fileupload.FileItem;

import com.masscustsoft.helper.Upload;
import com.masscustsoft.model.AbstractResult;
import com.masscustsoft.model.Entity;
import com.masscustsoft.util.MapUtil;
import com.masscustsoft.util.ReflectUtil;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.ThreadHelper;

public class InsertRecord extends RecordAction {
	String returns;

	@Override
	public Map prepare(AbstractResult ret) throws Exception {
		Map filter = new HashMap();
		doFilterIn(filter);
		return filter;
	}
	
	private Entity doInsert(Class<? extends Entity> cls) throws Exception{
		Entity c = cls.newInstance();
		updateEntity(c, true);	
		ThreadHelper.set("$FROMUI$", true);
		getDs().insertBean(c);
		ThreadHelper.set("$FROMUI$", null);
		if (hook != null)
			cfg.setHooks(this, hook, c);
		return c;
	}
	
	@Override
	public void run(AbstractResult ret, Map filter) throws Exception {
		Class<? extends Entity> cls = getModelClass();
		Upload up = Upload.getUpload();
		up.getFieldMap().putAll(filter);
		Entity c=null;
		
		if (up.getAttatchmentCount(cls)>1){
			Map<String, Object> map = up.getFieldMap();
			for (FileItem it:up.getFiles()){
				String fld=up.getField(it);
				if (ReflectUtil.findField(ReflectUtil.getFieldMap(cls), fld)==null) continue;
				map.put(fld, it.getFieldName());
				map.put(fld + ".filename", it.getFieldName());
				map.put(fld + ".fileitem", it);
				c=doInsert(cls);
			}
		}
		else{
			c=doInsert(cls);
		}
		
		Map m = (Map) LightUtil.toJsonObject(c);

		// after insert a record, the primary keys will be set to Upload to
		// allow chain actions

		List<String> keys = MapUtil.getSelectList(returns);
		keys.addAll(getPrimaryKeys(cls));

		for (String s : keys) {
			String v=m.get(s)+"";
			Upload.getUpload().setStr(s, v);
		}

		doFilterOut(c, m);

		ret.setResult(m);
	}

	public String getReturns() {
		return returns;
	}

	public void setReturns(String returns) {
		this.returns = returns;
	}
}
