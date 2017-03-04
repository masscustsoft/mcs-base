package com.masscustsoft.api;

import java.util.Map;

import com.masscustsoft.service.PageInfo;

public interface IRefiner {
	public void refineRow(IDataService dataService, Map<String, Object> terms, Map<String,Object> rcd, IEntity en) throws Exception;
	public void refineData(IDataService data, Class[] cs,Map<String,Object> terms,String text, String sort, String facet, boolean raw, PageInfo<Map> pg) throws Exception;
	public void refineSearch(IDataService data, Map<String, Object> spec) throws Exception;
}
