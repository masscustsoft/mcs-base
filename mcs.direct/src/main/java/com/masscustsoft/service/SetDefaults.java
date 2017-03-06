package com.masscustsoft.service;

import com.masscustsoft.model.AbstractResult;
import com.masscustsoft.model.DirectDefault;

public class SetDefaults extends DirectAction {

	@Override
	protected void run(AbstractResult ret) throws Exception {
		String objectId = requiredStr("objectId");
		String fieldId = requiredStr("fieldId");
		String value = requiredStr("value");
		DirectDefault def = getDs().getBean(DirectDefault.class, "userId", getUserId(), "objectId", objectId, "fieldId", fieldId);
		if (def != null) {
			def.setValue(value);
			getDs().updateBean(def);
		} else {
			def = new DirectDefault();
			def.setUserId(getSession().getUserId());
			def.setObjectId(objectId);
			def.setFieldId(fieldId);
			def.setValue(value);
			getDs().insertBean(def);
		}
		ret.setResult(def);
	}

}
