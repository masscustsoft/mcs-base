package com.masscustsoft.model;

import com.masscustsoft.api.IndexKey;
import com.masscustsoft.api.SQLTable;
import com.masscustsoft.model.Entity;

@SQLTable("direct_users")
public class DirectUserRole extends Entity {
	@IndexKey
	String userId;
	
	@IndexKey
	String roleId;
	
	String extraAccesses;
	
	@IndexKey
	String proxyId;

	@IndexKey
	String active = "yes";
	
	
	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getRoleId() {
		return roleId;
	}

	public void setRoleId(String roleId) {
		this.roleId = roleId;
	}

	public String getExtraAccesses() {
		return extraAccesses;
	}

	public void setExtraAccesses(String extraAccesses) {
		this.extraAccesses = extraAccesses;
	}

	public String getProxyId() {
		return proxyId;
	}

	public void setProxyId(String proxyId) {
		this.proxyId = proxyId;
	}

	public String getActive() {
		return active;
	}

	public void setActive(String active) {
		this.active = active;
	}
	
}
