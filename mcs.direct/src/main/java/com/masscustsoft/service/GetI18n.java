package com.masscustsoft.service;

import java.util.HashMap;
import java.util.Map;

import com.masscustsoft.model.AbstractResult;
import com.masscustsoft.util.GlbUtil;

public class GetI18n extends DirectAction {
	@Override
	protected synchronized void run(AbstractResult ret) throws Exception {
		String keyId = requiredStr("keyId");
		String lang=getLang();
		String def=getStr("defaultValue","");
		def=GlbUtil.prepareI18n(lang,keyId,def);
		Map map=new HashMap();
		map.put(keyId,def);
		ret.setResult(map);
	}
}
