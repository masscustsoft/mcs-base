package com.masscustsoft.service;

import java.util.HashMap;
import java.util.Map;

import com.masscustsoft.Lang.CLASS;
import com.masscustsoft.api.IDataEnumeration;
import com.masscustsoft.api.IDataService;
import com.masscustsoft.model.DirectClassifyCatalog;
import com.masscustsoft.model.DirectClassifyItem;
import com.masscustsoft.model.Job;
import com.masscustsoft.model.JobAction;
import com.masscustsoft.model.JobInstance;
import com.masscustsoft.model.JobInstanceAction;
import com.masscustsoft.util.LightStr;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.xml.BeanFactory;

public class DirectClassifyJob extends JobAction{
	public static Map getCatalogKeys(IDataService dataService, String parentId) throws Exception{
		Map ret=new HashMap();
		StringBuffer keys=new StringBuffer();
		StringBuffer grps=new StringBuffer();
		String classifyType=null;
		for(String pid=parentId;;){
			DirectClassifyCatalog cat=dataService.getBean(DirectClassifyCatalog.class, "uuid", pid);
			if (cat!=null){
				if (classifyType==null) classifyType=cat.getClassifyType();
				if (!LightStr.isEmpty(cat.getKeyId())) keys.append(cat.getKeyId()+" ");
				else{
					grps.append(cat.getGroupId()+" ");
				}
				pid=cat.getParentId();
			}
			else break;
		}
		if (keys.length()>0){
			ret.put("catalogKeys", "#"+keys.toString().trim());
		}
		if (grps.length()>0){
			ret.put("catalogGrps","#"+grps.toString().trim());
		}
		if (classifyType!=null){
			String cls = BeanFactory.getBeanFactory().findRealClass(classifyType);
			DirectClassifyItem ci=(DirectClassifyItem)CLASS.forName(cls).newInstance();
			ci.processFilter(ret);
		}
		return ret;
	}
	
	@Override
	public void run(JobThread jobThread, Job job, JobInstance inst, JobInstanceAction a)
			throws Exception {
		//update all
		IDataEnumeration<DirectClassifyCatalog> it = dataService.enumeration(DirectClassifyCatalog.class,"{status:'!updated'}","",50,false);
		while(it.hasMoreElements()){
			DirectClassifyCatalog ca=it.nextElement();
			Map m=getCatalogKeys(dataService, ca.getUuid());
			m.put("active","yes");
			//ca.doCalc(m);
			ca.setStatus("updated");
			ca.setUpdateTime(LightUtil.longDate());
			dataService.updateBean(ca);
		}
	}
	
}
