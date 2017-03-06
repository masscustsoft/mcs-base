package com.masscustsoft.service;

import com.masscustsoft.model.AbstractResult;
import com.masscustsoft.model.DirectI18n;

public class SaveI18nTranslate extends GetI18nTranslate {
	@Override
	protected void run(AbstractResult ret) throws Exception {
		String sourceLang=requiredStr("sourceLang");
		String targetStatus=requiredStr("targetStatus");
		String resetStatus=requiredStr("resetStatus");
		
		String targetLang=requiredStr("targetLang");
		String setModuleId=getStr("setModuleId","");
		String moduleId=getStr("moduleId","");
		
		String[] old = getTranslateBody(sourceLang,targetLang,targetStatus,false).split("\n");
		String[] body = getStr("translateBody","").split("\n");
		
		if (old.length!=body.length) throw new Exception("#[MismatchTranslateBody]");
		
		for (int i=0;i<old.length;i++){
			String keyId=old[i];
			String text=body[i];
			if (text.equals("["+keyId+"]")) continue;
			DirectI18n di = getDs().getBean(DirectI18n.class,"lang",targetLang,"keyId",keyId);
			if (di==null) continue;
			di.setStatus(resetStatus);
			di.setValue(text);
			if (setModuleId.equals("yes")) di.setModuleId(moduleId);
			else{
				DirectI18n dd = getDs().getBean(DirectI18n.class,"lang",sourceLang,"keyId",keyId);
				if (dd!=null) di.setModuleId(dd.getModuleId());	
			}
			getDs().updateBean(di);
		}
	}
}
