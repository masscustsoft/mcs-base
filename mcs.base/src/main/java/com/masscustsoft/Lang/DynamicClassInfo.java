package com.masscustsoft.Lang;

import com.masscustsoft.api.IVariantConfig;

/**
 * A dynamic class info used for dynamic class loader
 * 
 * @author JSong
 *
 */
public class DynamicClassInfo {
	/**
	 * the categoryId or simple name of the service class if it's webservice. 
	 */
	String categoryId;
	
	/**
	 * Simple type class name if it's webservice. Or not used.
	 */
	String subId;
	
	/**
	 * the full name of the class
	 */
	String targetClass;
	
	/**
	 * The base class need to inherit from. A template class must implement the interface IVariant to be qualified for dynamic class load.
	 */
	String template;
	
	/**
	 * the Dynamic Class Configuration
	 */
	IVariantConfig config;

	public String getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(String categoryId) {
		this.categoryId = categoryId;
	}

	public String getTargetClass() {
		return targetClass;
	}

	public void setTargetClass(String targetClass) {
		this.targetClass = targetClass;
	}

	public String getTemplate() {
		return template;
	}

	public void setTemplate(String template) {
		this.template = template;
	}

	public IVariantConfig getConfig() {
		return config;
	}

	public void setConfig(IVariantConfig config) {
		this.config = config;
	}

	public String getSubId() {
		return subId;
	}

	public void setSubId(String subId) {
		this.subId = subId;
	}
	
}
