package com.masscustsoft.service;

import com.masscustsoft.model.AbstractResult;
import com.masscustsoft.model.DirectUser;
import com.masscustsoft.service.NotifyService;

public class SendCaptcha extends DirectAction {

	@Override
	protected void run(AbstractResult ret) throws Exception {
		DirectSession ses = getSession();
		String cellNo = getStr("cellNo", "");
		NotifyService ns = cfg.getNotifyService();
		String title = getStr("text", "Verify Code");
		if (cellNo.length() == 0) {
			String userId = getStr("userId", "");
			if (userId.length() > 0) {
				DirectUser u = getDs().getBean(DirectUser.class, "userId", userId);
				if (u != null) {
					cellNo = u.getCellNo();
				}
			}
		}
		if (ns != null && cellNo.length() > 0) {
			String code = (1000 + (int) (Math.random() * 9000)) + "";
			ns.sendMessage(cellNo, title, code, null);
			ses.setCaptcha(code);
		}
	}

}
