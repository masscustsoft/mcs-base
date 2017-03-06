package com.masscustsoft.service;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import com.masscustsoft.model.AbstractResult;
import com.masscustsoft.model.DeviceLogin;
import com.masscustsoft.model.DirectUser;
import com.masscustsoft.util.EncryptUtil;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.LightStr;

public class Login extends DirectAction {
	int maxTry = 5;

	int autoUnlockSeconds = 30000;

	@Override
	protected void run(AbstractResult ret) throws Exception {
		DirectSession ses = getSession();
		String userId = getStr("usrId", "").toLowerCase();
		if (LightStr.isEmpty(userId))
			throw new Exception("InvalidUserPassword");
		String rememberMe = getStr("rememberMe", "");
		String autoReconnect = getStr("autoReconnect", "");
		DeviceLogin dl = getDs().getBean(DeviceLogin.class, "deviceId", ses.getDeviceId());
		if (dl == null) {
			dl = new DeviceLogin();
			dl.setDeviceId(ses.getDeviceId());
			dl.setDeviceName(ses.getDeviceName());
			dl.setDeviceType(ses.getDeviceType());
			dl.setUid(userId);
			dl.setStatus("new");
			dl.setRegistrationId(ses.registrationId);
			dl.setLastVisit(LightUtil.longDate());
			dl.setFailCount(0);
			dl.setRememberMe("");
			dl.setAutoReconnect("");
			getDs().insertBean(dl);
		}
		dl.setUid(userId);
		Calendar c = LightUtil.getCalendar();
		c.add(Calendar.SECOND, -autoUnlockSeconds);
		// auto unlcok
		if (dl.getFailCount() > maxTry && dl.getLastVisit().before(LightUtil.longDate(c))) {
			dl.setLastVisit(LightUtil.longDate());
			dl.setFailCount(0);
		}
		dl.setFailCount(dl.getFailCount() + 1);
		getDs().updateBean(dl);
		if (dl.getFailCount() > maxTry)
			throw new Exception("#[UserLocked]:" + (LightUtil.longDate().getTime() - dl.getLastVisit().getTime()));

		String password = getStr("password", "");
		String capcha = getStr("captcha", "");

		if (!"true".equals(ses.getInTrust()) && !capcha.equals(ses.getCaptcha()))
			throw new Exception("#[InvalidCaptcha]");
		DirectUser u = getDs().getBean(DirectUser.class, "userId", userId, "active", "yes");
		if (u == null)
			throw new Exception("#[InvalidUserPassword] "+userId);
		String enpass = EncryptUtil.saltPassword(password, userId);
		if (!enpass.equals(u.getPassword())) {
			throw new Exception("#[InvalidUserPassword]");
		}
		ses.setUserId(userId);
		ses.getVars().putAll(u.getVars());
		ses.updateSession();
		Map m = new HashMap();
		m.put("userId", u.getUserId());
		m.put("userName", u.getUserName());
		m.put("roles", getRoleList(u.getUserId()));
		cfg.getHooks(this, "Login", m);
		ret.setResult(m);

		dl.setRememberMe(rememberMe);
		dl.setAutoReconnect(autoReconnect);
		dl.setLastVisit(LightUtil.longDate());
		dl.setFailCount(0);
		dl.setStatus("ready");
		getDs().updateBean(dl);

	}

	public int getMaxTry() {
		return maxTry;
	}

	public void setMaxTry(int maxTry) {
		this.maxTry = maxTry;
	}

	public int getAutoUnlockSeconds() {
		return autoUnlockSeconds;
	}

	public void setAutoUnlockSeconds(int autoUnlockSeconds) {
		this.autoUnlockSeconds = autoUnlockSeconds;
	}

}
