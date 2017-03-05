package com.masscustsoft.model;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import com.masscustsoft.api.FullText;
import com.masscustsoft.api.IndexKey;
import com.masscustsoft.api.SQLTable;
import com.masscustsoft.api.TimestampIndex;
import com.masscustsoft.util.LightUtil;

@SQLTable("DeviceLogin")
public class DeviceLogin extends Entity {
	@IndexKey
	protected String deviceId; // type-uuid ex: ios-aaaa-bbbb-cccc-dddd

	@FullText
	String deviceName; // user agent or device name

	@IndexKey
	protected String uid;

	@IndexKey
	String status; // registering, ready

	@FullText
	String note;

	@TimestampIndex
	Timestamp lastVisit;

	String deviceType; // ios, android or windows

	String vendorType; // Apn, Gcm, Baidu
	
	int failCount = 0;
	String rememberMe, autoReconnect;

	Map<String, Integer> badges = new HashMap<String, Integer>();

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

	String registrationId;

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public Timestamp getLastVisit() {
		return lastVisit;
	}

	public void setLastVisit(Timestamp lastVisit) {
		this.lastVisit = lastVisit;
	}

	public int getFailCount() {
		return failCount;
	}

	public void setFailCount(int failCount) {
		this.failCount = failCount;
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

	public Map<String, Integer> getBadges() {
		return badges;
	}

	public void setBadges(Map<String, Integer> badges) {
		this.badges = badges;
	}

	public boolean expired() {
		if (lastVisit == null)
			return true;
		Calendar c = LightUtil.getCalendar();
		c.add(Calendar.DATE, -30);
		if (c.getTime().after(lastVisit))
			return true;
		return false;
	}

	public void addBadges(String[] ids) {
		for (String id : ids) {
			Integer n = badges.get(id);
			if (n == null)
				n = 0;
			badges.put(id, n + 1);
		}
	}

	public String getVendorType() {
		return vendorType;
	}

	public void setVendorType(String vendorType) {
		this.vendorType = vendorType;
	}
}
