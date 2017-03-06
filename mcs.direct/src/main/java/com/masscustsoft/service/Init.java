package com.masscustsoft.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.masscustsoft.api.IDataService;
import com.masscustsoft.model.AbstractResult;
import com.masscustsoft.model.DeviceLogin;
import com.masscustsoft.model.DirectI18n;
import com.masscustsoft.model.DirectUser;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.MapUtil;

public class Init extends DirectAction {
	boolean verifyCaptcha = true;

	boolean allowAutoReconnect = true;

	boolean allowRegister = true;

	@Override
	protected void run(AbstractResult ret) throws Exception {
		String deviceId = requiredStr("deviceId");
		String deviceType = requiredStr("deviceType");
		String deviceName = requiredStr("deviceName");

		long pulse = getLong("pulse");
		long delta = System.currentTimeMillis() - pulse;

		DirectSession ses = cfg.getSessionService().newSession(cfg, deviceId, deviceType, deviceName);
		ses.setDelta(delta);
		DeviceLogin dl = getDs().getBean(DeviceLogin.class, "deviceId", deviceId);
		DirectUser u = null;
		if (dl != null) {
			u = getDs().getBean(DirectUser.class, "userId", dl.getUid());
			if (u != null) {
				ses.setAutoReconnect(dl.getAutoReconnect());
				ses.setRememberMe(dl.getRememberMe());
				if (LightUtil.decodeBoolean(ses.getRememberMe()))
					ses.setUserId(dl.getUid());
				if (dl.getFailCount() == 0 || !verifyCaptcha)
					ses.setInTrust("true");
			}
		}
		Map m = (Map) LightUtil.toJsonObject(ses);
		m.remove("captcha");
		m.put("allowAutoReconnect", allowAutoReconnect + "");
		if (u != null && LightUtil.decodeBoolean(ses.getInTrust())
				&& LightUtil.decodeBoolean(ses.getAutoReconnect())) {
			ses.getVars().putAll(u.getVars());
			m.put("roles", getRoleList(u.getUserId()));
			cfg.getHooks(this, "Login", m);
			ses.updateSession();

		}
		
		m.put("mainEntry", cfg.getMainEntry());
		m.put("guestEntry", cfg.getGuestEntry());
		m.put("secureSessionId", cfg.getSecureSessionId());
		m.put("heartBeat", cfg.getHeartBeat());
		m.put("allowRegister", allowRegister);
		m.put("pushVendors", MapUtil.getSelectList((String) cfg.getVars().get("pushVendors")));
		m.put("appVersion", cfg.getAppVersion());
		m.put("resourceId", cfg.getResourceId());
		m.put("itemCaches", cfg.getItemCaches());
		m.put("i18n", getI18nMap(getLang()));
		
		String pingUrl = (String) cfg.getVars().get("pingUrl");
		if (pingUrl != null)
			m.put("pingUrl", pingUrl);

		m.put("lang", getLang());
		m.put("locale", getLocale());
		m.put("numFmt", getNumFmt());
		m.put("dateFmt", getDateFmt());
		m.put("timeFmt", getTimeFmt());
		
		ret.setResult(m);

	}

	Map getI18nMap(String lang) throws Exception{
		IDataService ds = this.getDs();
		List<DirectI18n> lst = ds.getBeanList(DirectI18n.class, "{lang:'"+getLang()+"'}", "");
		Map map=new HashMap();
		for (DirectI18n d:lst){
			map.put(d.getKeyId(), d.getValue());
		}
		return map;
	}
	
	public boolean isVerifyCaptcha() {
		return verifyCaptcha;
	}

	public void setVerifyCaptcha(boolean verifyCaptcha) {
		this.verifyCaptcha = verifyCaptcha;
	}

	public boolean isAllowAutoReconnect() {
		return allowAutoReconnect;
	}

	public void setAllowAutoReconnect(boolean allowAutoReconnect) {
		this.allowAutoReconnect = allowAutoReconnect;
	}

	public boolean isAllowRegister() {
		return allowRegister;
	}

	public void setAllowRegister(boolean allowRegister) {
		this.allowRegister = allowRegister;
	}
}
