package com.masscustsoft.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.masscustsoft.model.AbstractResult;

public class GetLangItems extends DirectAction {
	@Override
	protected void run(AbstractResult ret) throws Exception {
		List<Map> result = new ArrayList<Map>();
		int start=getInt("start");
		if (start==0){
			Map<String, String> langs = cfg.getLanguages();
			List<String> list=new ArrayList<String>();
			for (String loc:langs.keySet()){
				String lang=langs.get(loc);
				if (!list.contains(lang)){
					list.add(lang);
					Map m=new HashMap();
					m.put("id", lang);
					m.put("name", "#["+lang+"]");
					result.add(m);
				}
			}
		}
		ret.setAmount(result.size());
		ret.setResult(result);
	}
}
