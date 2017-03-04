package com.masscustsoft.api;

public interface IBeanFactory {

	public <T> T clone(T from, String newId) throws Exception;
	public String findRealClass(String name);
	public String toXml(Object obj,int op);
}
