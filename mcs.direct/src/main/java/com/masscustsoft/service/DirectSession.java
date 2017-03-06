package com.masscustsoft.service;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import com.masscustsoft.util.GlbHelper;
import com.masscustsoft.util.LightUtil;

public class DirectSession {
	String sessionId;
	
	String deviceId;
	
	String deviceName;
	
	String deviceType;
	
	String registrationId;
	
	String userId;
	
	String captcha;
	
	Timestamp startTime;
	
	Timestamp lastAccessTime;

	String rememberMe,autoReconnect,inTrust;
	
	String cfgId;
	
	String pingable;
	
	long delta;
	
	Map<String,String> vars=new HashMap<String,String>();
	
	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getCaptcha() {
		return captcha;
	}

	public void setCaptcha(String captcha) {
		this.captcha = captcha;
	}

	public Timestamp getStartTime() {
		return startTime;
	}

	public void setStartTime(Timestamp startTime) {
		this.startTime = startTime;
	}

	public Timestamp getLastAccessTime() {
		return lastAccessTime;
	}

	public void setLastAccessTime(Timestamp lastAccessTime) {
		this.lastAccessTime = lastAccessTime;
	}
	
	public boolean expired(int timeoutSeconds){
		Calendar c=LightUtil.getCalendar();
		c.add(Calendar.SECOND,-timeoutSeconds);
		Timestamp now=LightUtil.longDate(c);
		return (getLastAccessTime().before(now));
	}

	public void updateSession() {
		setLastAccessTime(LightUtil.longDate());
	}

	public String getDeviceName() {
		return deviceName;
	}

	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}

	public String getDeviceType() {
		return deviceType;
	}

	public void setDeviceType(String deviceType) {
		this.deviceType = deviceType;
	}

	public String getRegistrationId() {
		return registrationId;
	}

	public void setRegistrationId(String registrationId) {
		this.registrationId = registrationId;
	}

	public String getRememberMe() {
		return rememberMe;
	}

	public void setRememberMe(String rememberMe) {
		this.rememberMe = rememberMe;
	}

	public String getAutoReconnect() {
		return autoReconnect;
	}

	public void setAutoReconnect(String autoReconnect) {
		this.autoReconnect = autoReconnect;
	}

	public String getInTrust() {
		return inTrust;
	}

	public void setInTrust(String inTrust) {
		this.inTrust = inTrust;
	}

	public String getCfgId() {
		return cfgId;
	}

	public void setCfgId(String cfgId) {
		this.cfgId = cfgId;
	}

	public DirectConfig getDirectConfig(){
		return (DirectConfig)GlbHelper.get("$DirectCfg$"+cfgId);
	}

	public String getPingable() {
		return pingable;
	}

	public void setPingable(String pingable) {
		this.pingable = pingable;
	}

	public long getDelta() {
		return delta;
	}

	public void setDelta(long delta) {
		this.delta = delta;
	}

	public Map<String, String> getVars() {
		return vars;
	}

	public void setVars(Map<String, String> vars) {
		this.vars = vars;
	}

	public String getVar(String var){
		return vars.get(var);
	}
	
	public String setVar(String var, String val){
		return vars.put(var,val);
	}
	
}
