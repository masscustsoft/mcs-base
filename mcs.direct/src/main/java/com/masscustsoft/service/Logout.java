package com.masscustsoft.service;

import com.masscustsoft.model.AbstractResult;
import com.masscustsoft.model.DeviceLogin;

public class Logout extends DirectAction {

	@Override
	protected void run(AbstractResult ret) throws Exception {
		DirectSession ses = getSession();
		if (ses == null)
			return;
		DeviceLogin dl = getDs().getBean(DeviceLogin.class, "deviceId", ses.getDeviceId());
		if (dl != null) {
			dl.setAutoReconnect("");
			getDs().updateBean(dl);
		}
		cfg.getSessionService().expireSession(getStr("sessionId", ""));
	}

	
}
