package com.masscustsoft.model;

import java.util.HashMap;
import java.util.Map;

import com.masscustsoft.api.FullText;
import com.masscustsoft.api.IndexKey;
import com.masscustsoft.api.JsonField;
import com.masscustsoft.api.PrimaryKey;
import com.masscustsoft.api.SQLTable;
import com.masscustsoft.model.BasicFile;
import com.masscustsoft.model.Entity;
import com.masscustsoft.util.EncryptUtil;

@SQLTable("direct_users")
public class DirectUser extends Entity {
	@IndexKey
	@PrimaryKey
	String userId;

	@JsonField(output = false)
	String password;

	@FullText
	String userName;

	@IndexKey
	String cellNo;

	BasicFile avatar = new BasicFile();

	@IndexKey
	String active = "yes"; // active, inactive: you can set inactive only if
							// after endDate

	protected Map<String, String> vars = new HashMap<String, String>();

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getPassword() {
		if (password!=null && password.startsWith("\0x1f")){
			password="\u001f"+password.substring("\0x1f".length());
		}
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	public void validate() throws Exception {
		userId = userId.toLowerCase();
		password = EncryptUtil.saltPassword(password, userId);
		super.validate();
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getCellNo() {
		return cellNo;
	}

	public void setCellNo(String cellNo) {
		this.cellNo = cellNo;
	}

	public BasicFile getAvatar() {
		return avatar;
	}

	public void setAvatar(BasicFile avatar) {
		this.avatar = avatar;
	}

	public String getActive() {
		return active;
	}

	public void setActive(String active) {
		this.active = active;
	}

	public Map<String, String> getVars() {
		return vars;
	}

	public void setVars(Map<String, String> vars) {
		this.vars = vars;
	}
}
