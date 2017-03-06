package com.masscustsoft.service;

import java.util.List;
import java.util.Map;

import com.masscustsoft.model.AbstractResult;
import com.masscustsoft.model.Entity;
import com.masscustsoft.util.LightStr;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.MapUtil;

public class DeleteRecord extends RecordAction {
	int max=1;
	
	String skipPrimaryKey;
	
	@Override
	public Map prepare(AbstractResult ret) throws Exception {
		Map filter = getFilterMap();
		Class<? extends Entity> cls = getModelClass();
		doFilterIn(filter);
		addPrimaryKeys(cls, filter);
		if (!LightStr.isEmpty(skipPrimaryKey)){
			List<String> rms = MapUtil.getSelectList(skipPrimaryKey);
			for (String id:rms){
				filter.remove(id);
			}
		}
		return filter;
	}
	
	@Override
	public void run(AbstractResult ret, Map filter) throws Exception {
		Class<? extends Entity> cls = getModelClass();
		cfg.incVersion(cls);
		
		System.out.println("delete cls="+cls+",filter="+filter);
		PageInfo<Entity> pg = getDs().getBeanList(new Class[] { cls }, LightUtil.toJsonString(filter).toString(),
				"", 0, max, sort, "", false);
		for (Entity c : pg.getList()) {
			if (hook!=null) cfg.clearHooks(this, hook, c);
			getDs().deleteBean(c);
		}
	}

	public int getMax() {
		return max;
	}

	public void setMax(int max) {
		this.max = max;
	}

	public String getSkipPrimaryKey() {
		return skipPrimaryKey;
	}

	public void setSkipPrimaryKey(String skipPrimaryKey) {
		this.skipPrimaryKey = skipPrimaryKey;
	}
}
