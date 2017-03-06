package com.masscustsoft.model;

import com.masscustsoft.api.FullText;
import com.masscustsoft.api.IndexKey;
import com.masscustsoft.api.PrimaryKey;
import com.masscustsoft.api.SQLTable;
import com.masscustsoft.model.Entity;

@SQLTable("direct_users")
public class DirectRole extends Entity {
	@IndexKey
	@PrimaryKey
	String roleId;

	@FullText
	String name;

	String accessId;

	@IndexKey
	String active="yes";

	public String getRoleId() {
		return roleId;
	}

	public void setRoleId(String roleId) {
		this.roleId = roleId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAccessId() {
		return accessId;
	}

	public void setAccessId(String accessId) {
		this.accessId = accessId;
	}

	@Override
	public void validate() throws Exception {
		roleId = roleId.toLowerCase();
		super.validate();
	}

	public String getActive() {
		return active;
	}

	public void setActive(String active) {
		this.active = active;
	}

}
