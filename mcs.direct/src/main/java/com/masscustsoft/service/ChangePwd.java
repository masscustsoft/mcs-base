package com.masscustsoft.service;

import com.masscustsoft.model.AbstractResult;
import com.masscustsoft.model.DirectUser;
import com.masscustsoft.util.EncryptUtil;

public class ChangePwd extends DirectAction {

	@Override
	protected void run(AbstractResult ret) throws Exception {
		DirectSession ses = getSession();
		String pwd = requiredStr("oldPassword");
		String password = requiredStr("password");

		DirectUser du = getDs().getBean(DirectUser.class, "userId", ses.getUserId());
		if (du == null)
			throw new Exception("UserNotFound");
		if (!EncryptUtil.saltPassword(pwd, du.getUserId()).equals(du.getPassword())) {
			throw new Exception("InvalidPassword");
		}
		du.setPassword(password);
		getDs().updateBean(du);
	}

}
