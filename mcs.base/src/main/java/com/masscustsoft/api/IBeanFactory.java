package com.masscustsoft.api;

import com.masscustsoft.xml.BeanProxy;

public interface IBeanFactory {

	public <T> T clone(T from, String newId) throws Exception;
	public String findRealClass(String name);
	public String toXml(Object obj,int op);
	public IRepository getRepository(String fsId);
	public Object loadBean(String xml) throws Exception;
	
	public IDataService getDataService(String dsId);
	public void addDataService(String dsId, IDataService fd);
	
	public Object getBean(String fsId, String beanName) throws Exception;
	public void removeDataService(String dsId) throws Exception;
	public BeanProxy getBeanProxy(String fsId, String beanName) throws Exception;
	public IRepository getRepository(String string, String notifyBodyFile);
	public String getPureBeanName(String notifyBodyFile);
	public void addRepository(String fsId, IRepository repository);
	public String getId();
}
