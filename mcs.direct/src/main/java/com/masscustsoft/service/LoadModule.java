package com.masscustsoft.service;

import java.util.Map;

import com.masscustsoft.model.AbstractResult;
import com.masscustsoft.module.ELEMENT;
import com.masscustsoft.util.ThreadHelper;
import com.masscustsoft.xml.BeanFactory;
import com.masscustsoft.xml.BeanProxy;

public class LoadModule extends DirectAction{

	@Override
	public void init(DirectConfig cfg) throws Exception{
		super.init(cfg);
		if (this.getCacheable()==null) this.setCacheable(true);
	}
	
	@Override
	protected void run(AbstractResult ret) throws Exception {
		String moduleId = requiredStr("moduleId");
		BeanFactory bf = cfg.getBeanFactory();
		
		ELEMENT m=(ELEMENT)bf.getBean(cfg.getDefaultSysId(), moduleId);
		BeanProxy bp = bf.getBeanProxy(m);
		ThreadHelper.set("$$moduleId",bp.getBeanId());
		Map<String, Object> map = m.toJson();
		ret.setResult(map);
		
		ThreadHelper.set("$$i18n",null);
		ThreadHelper.set("$$moduleId",null);
	}
	
	
	
}
