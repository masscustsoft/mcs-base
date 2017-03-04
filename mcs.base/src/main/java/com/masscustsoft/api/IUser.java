package com.masscustsoft.api;

import java.io.Serializable;
import java.security.Principal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

public interface IUser extends Principal, Serializable{
	public String getUserId();
	public void setUserId(String userId);
	public String getUserName();
	public void setUserName(String name);
	public String getUuid();
	public String getPassword();
	public String getEmail();
	public void setEmail(String email);
	public void setPassword(String pwd);
	public List<IRole> getRoleList(IDataService data) throws Exception;
	public void setRoleList(IDataService data, List<IRole> roles) throws Exception;
	public Boolean getExpired() throws Exception;
	public void setExpired(boolean expired) throws Exception;
	public void setFailedAttempts(int attemps) throws Exception;
	public int getFailedAttempts();
	public Timestamp getFailedAttemptDate();
	public void setFailedAttemptDate(Timestamp failedAttemptDate);
	public void setVar(String var, Object val);
	public Object getVar(String var);
	public Map<String,Object> getVars();
}
