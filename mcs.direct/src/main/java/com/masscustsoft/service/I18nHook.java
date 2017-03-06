package com.masscustsoft.service;

import java.util.HashMap;
import java.util.Map;

import com.masscustsoft.model.AbstractResult;
import com.masscustsoft.model.DirectI18n;
import com.masscustsoft.model.Entity;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.ThreadHelper;

/**
 * This file used in I18nSettings
 * @author Jason
 *
 */
public class I18nHook extends HOOK {

	@Override
	public void doSet(DirectComponent dd, Entity en) throws Exception {
		DirectI18n di=(DirectI18n)en;
		
		DirectSession ses = dd.getSession();
		
		AbstractResult ret=(AbstractResult)ThreadHelper.get("$$ret");
		Map map=new HashMap();
		Map iMap=new HashMap();
		if (di.getLang().equals(dd.getLang())){
			iMap.put(di.getKeyId(), di.getValue());
			map.put("i18n", iMap);
		}
		ret.setResult(map);
	}

	@Override
	public void doClear(DirectComponent dd, Entity en) throws Exception {
	
	}

}
