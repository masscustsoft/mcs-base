package com.masscustsoft.xml;

public class BeanProxy<T> {
	String beanId; //full name
	T bean;
	boolean external=false; //for dump to xml, generate ref;
	long lastModified;
	
	public BeanProxy(String bid, T bean, long tm){
		this.beanId=bid;
		this.bean=bean;
		lastModified=tm;
	}
	
	public T getBean() {
		return bean;
	}
	public void setBean(T bean) {
		this.bean = bean;
	}
	public String getBeanId() {
		return beanId;
	}
	public void setBeanId(String beanId) {
		this.beanId = beanId;
	}
	
	public String getFsId(){
		return beanId.substring(0,beanId.indexOf(":"));
	}
	
	public String getShortId(){
		return beanId.replace(':', '-');
	}
	
	public String getName(){
		return beanId.substring(beanId.indexOf(':')+1);
	}
	
	public boolean getExternal(){
		return external;
	}

	public void setExternal(boolean external) {
		this.external = external;
	}

	public long getLastModified() {
		return lastModified;
	}

	public void setLastModified(long lastModified) {
		this.lastModified = lastModified;
	}

}
