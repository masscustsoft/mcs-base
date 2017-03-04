package com.masscustsoft.xml;

import com.masscustsoft.helper.BeanVersion;

public interface XStreamListener {
	//to object
	public Object onExternalBean(String parentId,Object parent,String ref, BeanVersion ver) throws Exception;
	
	public void cacheBean(String id,Object bean,BeanVersion ver) throws Exception;
	
	//to xml/
	public boolean onToNode(Object parent,Object bean,XmlNode node,boolean fullPath);
}
