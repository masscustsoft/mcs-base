package com.masscustsoft.service;

import java.util.ArrayList;
import java.util.List;

import com.masscustsoft.api.ISecurityService;
import com.masscustsoft.api.IVariantConfig;

/**
 * Configuration Item to expose an Entity as a Web Service.
 * 
 * @author JSong
 *
 */
public class WebServiceEntry implements IVariantConfig{
	/**
	 * Web Service Name. Ex: AAAService
	 */
	String name;

	/**
	 * Template for the web service, by default it's WebServiceBase
	 */
	String template="WebServiceBase";
	
	/**
	 * Web Service can have its own security service
	 */
	ISecurityService securityService;
	
	/**
	 * Detail Entities exposed for this named web service.
	 */
	List<WebServiceEntity> entities=new ArrayList<WebServiceEntity>();
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<WebServiceEntity> getEntities() {
		return entities;
	}

	public void setEntities(List<WebServiceEntity> entities) {
		this.entities = entities;
	}

	public String getTemplate() {
		return template;
	}

	public void setTemplate(String template) {
		this.template = template;
	}
	
	public WebServiceEntity getWebServiceEntity(String entity){
		for (WebServiceEntity ent:entities){
			if (entity.equals(ent.getName())) return ent;
		}
		return null;
	}

	public ISecurityService getSecurityService() {
		return securityService;
	}

	public void setSecurityService(ISecurityService securityService) {
		this.securityService = securityService;
	}
}
