package com.masscustsoft.service;

import java.util.HashMap;
import java.util.Map;

import com.masscustsoft.model.AbstractResult;
import com.masscustsoft.model.DirectDefault;
import com.masscustsoft.util.LightUtil;

public class GetDefaults extends DirectAction {

	@Override
	protected void run(AbstractResult ret) throws Exception {
		String objectId = requiredStr("objectId");
		String fieldId = requiredStr("fieldId");
		Map map;
		DirectDefault def = getDs().getBean(DirectDefault.class, "userId", getUserId(), "objectId", objectId, "fieldId", fieldId);
		if (def != null)
			map = (Map) LightUtil.parseJson(def.getValue());
		else
			map = new HashMap();
		ret.setResult(map);
	}

}
