package com.masscustsoft.service;

import java.util.Map;

import com.masscustsoft.model.DirectUser;
import com.masscustsoft.util.EncryptUtil;

public class SaHook extends HOOK {

	@Override
	public void doGet(DirectComponent dd, Map resp) throws Exception {
		String sapass=(String)resp.get("sapass");
		String userId=dd.getUserId();
		DirectUser u=dd.getDs().getBean(DirectUser.class, "userId", userId);
		if (u == null)
			throw new Exception("InvalidSaPassword");
		String enpass = EncryptUtil.saltPassword(sapass, userId);
		if (!enpass.equals(u.getPassword())) {
			throw new Exception("InvalidSaPassword");
		}
	}
}
