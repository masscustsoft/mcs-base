package com.masscustsoft.service;

import java.util.HashMap;
import java.util.Map;

import com.masscustsoft.model.AbstractResult;
import com.masscustsoft.model.Entity;
import com.masscustsoft.service.PageInfo;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.ThreadHelper;

public class UpdateRecord extends RecordAction {
	@Override
	public Map prepare(AbstractResult ret) throws Exception {
		Map filter = new HashMap();
		Class<? extends Entity> cls = getModelClass();
		doFilterIn(filter);
		addPrimaryKeys(cls,filter);
		return filter;
	}
	
	@Override
	public void run(AbstractResult ret, Map filter) throws Exception {
		DirectSession ses = getSession();
		Class<? extends Entity> cls = getModelClass();
		
		System.out.println("Update Filter="+filter);
		PageInfo<Entity> pg = getDs().getBeanList(new Class[] { cls }, LightUtil.toJsonString(filter).toString(),
				"", 0, 1, sort, "", false);
		for (Entity c : pg.getList()) {
			updateEntity(c,false);
			ThreadHelper.set("$FROMUI$", true);
			getDs().updateBean(c);
			ThreadHelper.set("$FROMUI$", null);
			if (hook!=null) cfg.setHooks(this, hook, c);
		}
	}
	
	
}
