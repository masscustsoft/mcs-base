package com.masscustsoft.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.masscustsoft.helper.Upload;
import com.masscustsoft.model.AbstractResult;
import com.masscustsoft.model.JsonResult;
import com.masscustsoft.util.LightUtil;

public class GetMultiList extends DirectAction {

	@Override
	protected void run(AbstractResult ret) throws Exception {
		List<Map> actions=(List)LightUtil.parseJson(requiredStr("actions"));
		Map attr=null;
		int nn=1;
		List<Map> result=new ArrayList();
		for (int i=0;i<actions.size();i++){
			Map act=actions.get(i);
			System.out.println("act"+i+"="+act);
			Upload.getUpload().add(act);
			String action=(String)act.get("action");
			int index=LightUtil.decodeInt(act.get("index").toString());
			JsonResult resp=new JsonResult();
			call(action, resp);
			System.out.println("resp="+resp.getResult());
			
			if (resp.getSuccess()==false) throw new Exception((String)resp.getResult());
			List<Map> lst=(List)resp.getResult();
			if (attr==null){
				attr=resp.getAttributes();
			}
			String tit=(String)act.get("title");
			if (tit!=null){
				Map m=new HashMap();
				m.put("uuid","_sect-"+index);
				m.put("title", tit);
				m.put("_sectid_", index);
				m.put("_idx_", 0);
				result.add(m);
			}
			for (Map<String,Object> m:lst){
				Map x=new HashMap();
				x.put("_sectid_",index);
				x.put("_idx_",nn); nn++;
				String key;
				for (String f:m.keySet()){
					if (f.equals("uuid")) key=f; else key="_"+index+"__"+f;
					x.put(key, m.get(f));
				}
				
				result.add(x);
			}
		}
		if (attr!=null) ret.setAttributes(attr);
		ret.setResult(result);
	}

}
