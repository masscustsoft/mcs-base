package com.masscustsoft.service;

import com.masscustsoft.model.AbstractResult;
import com.masscustsoft.model.DeviceLogin;

public class SyncNotification extends DirectAction {

	@Override
	protected void run(AbstractResult ret) throws Exception {
		DirectSession ses = getSession();
		String regId = requiredStr("registrationId");
		if (regId.equals(ses.getRegistrationId()))
			return;
		String vendorType= getStr("vendorType","Gcm");
		DeviceLogin dl = getDs().getBean(DeviceLogin.class, "deviceId", ses.getDeviceId());
		if (dl != null) {
			if (dl.getRegistrationId().equals(regId))
				return;
			dl.setRegistrationId(regId);
			dl.setVendorType(vendorType);
			dl.setDeviceType(ses.getDeviceType());
			getDs().updateBean(dl);
		} else {
			dl = new DeviceLogin();
			dl.setUid(ses.getUserId());
			dl.setDeviceId(ses.getDeviceId());
			dl.setRegistrationId(regId);
			dl.setDeviceType(ses.getDeviceType());
			dl.setVendorType(vendorType);	
			getDs().insertBean(dl);
		}
	}

}
