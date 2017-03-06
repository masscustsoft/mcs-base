package com.masscustsoft.util;

import java.util.HashMap;
import java.util.Map;

import com.masscustsoft.api.IDataService;

public class DataUtil {
	public static void startTransaction() throws Exception{
		String transId=(String)ThreadHelper.get("transaction");
		if (transId==null){
			transId=LightUtil.getHashCode();
			Map<String,Object> trans=new HashMap<String,Object>();
			trans.put("_nest_", 1);
			ThreadHelper.set("transaction", transId);
			GlbHelper.set("trans-"+transId,trans);
		}
		else{
			Map<String,Object> trans=(Map)GlbHelper.get("trans-"+transId);
			trans.put("_nest_",(Integer)trans.get("_nest_")+1);
		}
	}
	
	public static void commitTransaction() throws Exception {
		String transId=(String)ThreadHelper.get("transaction");
		if (LightStr.isEmpty(transId)) return;
		Map<String,Object> trans=(Map)GlbHelper.get("trans-"+transId);
		if (trans!=null){
			Integer nest=(Integer)trans.get("_nest_"); if (nest==null) nest=1;
			trans.put("_nest_",--nest);
			if (nest>0) return;
			trans.remove("_nest_");
		}
		ThreadHelper.remove("transaction");
		if (trans!=null){
			for (String name:trans.keySet()){
				String dsId=(String)trans.get(name);
				IDataService data=LightUtil.getBeanFactory().getDataService(dsId);
				//System.out.println("COMMITTRANS name="+name+",dsId="+dsId+",ds="+data);
				if (data==null) continue;
				data._commitTransaction(transId);
			}
		}
		GlbHelper.remove("trans-"+transId);
	}
	
	public static void rollbackTransaction() throws Exception {
		String transId=(String)ThreadHelper.get("transaction");
		if (LightStr.isEmpty(transId)) return;
		Map<String,Object> trans=(Map)GlbHelper.get("trans-"+transId);
		if (trans!=null){
			Integer nest=(Integer)trans.get("_nest_"); if (nest==null) nest=1;
			trans.put("_nest_",--nest);
			if (nest>0) return;
			trans.remove("_nest_");
		}
		ThreadHelper.remove("transaction");
		for (String name:trans.keySet()){
			String dsId=(String)trans.get(name);
			IDataService data=LightUtil.getBeanFactory().getDataService(dsId);
			if (data==null) continue;
			data._rollbackTransaction(transId);
		}
		GlbHelper.remove("trans-"+transId);
	}

}
