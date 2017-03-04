package com.masscustsoft.service;

import java.lang.reflect.Method;
import java.util.Map;

import com.masscustsoft.Lang.CLASS;
import com.masscustsoft.Lang.DynamicClassInfo;
import com.masscustsoft.api.IVariant;
import com.masscustsoft.util.AsmHelper;
import com.masscustsoft.util.LightUtil;

/**
 * Entity Wrapper implements IVariant. 
 * 
 * @author JSong
 *
 */
public class ProxyVariantWrapper implements IVariant{
	
	/**
	 * To generate dynamic byte code for a EntityWrapper
	 */
	@Override
	public byte[] getClassBytes(DynamicClassInfo info) throws Exception {
		String targetClass=info.getTargetClass();
		AsmHelper asm=new AsmHelper(Object.class,targetClass);
		String cls=LightUtil.getBeanFactory().findRealClass(info.getSubId());
		System.out.println("TargetEntity="+targetClass+", subId="+info.getSubId()+",cls="+cls);
		
		//create a _getProxy_ method
		
		Map<String,Method> methods=CLASS.getMethodMap(CLASS.forName(cls));
		for (Method m:methods.values()){
			asm.addProxyMethod(m);
		}
		
		return asm.toByteArray();
	}
}
