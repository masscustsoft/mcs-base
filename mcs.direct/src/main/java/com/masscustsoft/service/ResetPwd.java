package com.masscustsoft.service;

import com.masscustsoft.model.AbstractResult;
import com.masscustsoft.model.DeviceLogin;
import com.masscustsoft.model.DirectUser;

public class ResetPwd extends DirectAction {

	@Override
	protected void run(AbstractResult ret) throws Exception {
		DirectSession ses = getSession();
		String userId = requiredStr("userId");
		String password = requiredStr("password");
		String captcha = requiredStr("captcha");

		DirectUser du = getDs().getBean(DirectUser.class, "userId", userId);
		if (du == null)
			throw new Exception("UserNotFound");
		if (!captcha.equals(ses.getCaptcha()))
			throw new Exception("InvalidCaptcha");
		du.setPassword(password);
		getDs().updateBean(du);

		// once resetPassword
		getDs().deleteBeanList(DeviceLogin.class, "{deviceId:'" + ses.getDeviceId() + "'}");
	}

}
