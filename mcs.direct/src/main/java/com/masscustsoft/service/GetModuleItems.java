package com.masscustsoft.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.masscustsoft.model.AbstractResult;
import com.masscustsoft.util.MapUtil;

public class GetModuleItems extends DirectAction {
	@Override
	protected void run(AbstractResult ret) throws Exception {
		List<Map> result = new ArrayList<Map>();
		int start=getInt("start");
		if (start==0){
			{
				String modu="Global";
				Map m=new HashMap();
				m.put("id", modu);
				m.put("name", "#["+modu+"]");
				result.add(m);
			}
			List<String> modules = MapUtil.getSelectList(cfg.getSupportedModules());
			for (String modu:modules){
				Map m=new HashMap();
				m.put("id", modu);
				m.put("name", "#["+modu+"]");
				result.add(m);
			}
		}
		ret.setAmount(result.size());
		ret.setResult(result);
	}
}
