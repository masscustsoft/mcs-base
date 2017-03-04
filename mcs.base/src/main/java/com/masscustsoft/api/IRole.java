package com.masscustsoft.api;

import java.io.Serializable;
import java.util.List;

/**
 * A user can act as one role each time.
 * 
 * @author JSong
 */
public interface IRole extends Serializable{
	/**
	 * Get the Role id
	 */
	public String getRoleId();
	
	/**
	 * Get Access List
	 */
	public List<String> getAccessList(IDataService data) throws Exception;
	
	/**
	 * Get Readonly setting, value is yes or no.
	 */
	public String getViewOnly();
	
	/**
	 * Set Readonly, value must be yest or no.
	 */
	public void setViewOnly(String value);
}
