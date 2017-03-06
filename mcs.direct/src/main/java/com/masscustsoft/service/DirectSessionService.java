package com.masscustsoft.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.ThreadHelper;

public class DirectSessionService {
	int timeoutSeconds = 30000;

	Map<String, DirectSession> sessions = Collections.synchronizedMap(new HashMap<String, DirectSession>());

	public DirectSession newSession(DirectConfig cfg,String deviceId, String deviceType, String deviceName) {
		removeSameDevices(deviceId);
		DirectSession ses = new DirectSession();
		ses.setDeviceId(deviceId);
		ses.setDeviceName(deviceName);
		ses.setDeviceType(deviceType);
		ses.setRegistrationId("");
		ses.setSessionId(LightUtil.getHashCode());
		ses.setStartTime(LightUtil.longDate());
		ses.setLastAccessTime(LightUtil.longDate());
		ses.setUserId("guest");
		ses.setCaptcha((1000 + (int) (Math.random() * 9000)) + "");
		ses.setCfgId(cfg.getId());
		ses.setPingable(cfg.getPingable());
		sessions.put(ses.getSessionId(), ses);
		removeTimeout();
		ThreadHelper.set("$$session",ses);
		return ses;
	}

	private synchronized void removeTimeout() {
		List<String> invals = new ArrayList<String>();
		for (DirectSession ses : sessions.values()) {
			if (ses.expired(this.timeoutSeconds)) {
				invals.add(ses.getSessionId());
			}
		}

		for (String id : invals) {
			sessions.remove(id);
		}
	}

	private synchronized void removeSameDevices(String deviceId) {
		List<String> invals = new ArrayList<String>();
		for (DirectSession ses : sessions.values()) {
			if (ses.getDeviceId().equals(deviceId))
				invals.add(ses.getSessionId());
		}

		for (String id : invals) {
			sessions.remove(id);
		}
	}

	public synchronized DirectSession getSession(String sesId) {
		DirectSession ses = sessions.get(sesId);
		if (ses != null) {
			if (ses.expired(this.timeoutSeconds)) {
				removeTimeout();
				return null;
			}
			ses.updateSession();
		}
		return ses;
	}

	public synchronized void expireSession(String sesId) {
		sessions.remove(sesId);
	}

	public int getTimeoutSeconds() {
		return timeoutSeconds;
	}

	public void setTimeoutSeconds(int timeoutSeconds) {
		this.timeoutSeconds = timeoutSeconds;
	}
}
