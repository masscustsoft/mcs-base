package com.masscustsoft.service;

import java.util.List;
import java.util.Map;

import com.masscustsoft.api.IDataService;
import com.masscustsoft.api.IEntity;
import com.masscustsoft.api.IRefiner;
import com.masscustsoft.service.PageInfo;

/**
 * Data refiner installed to DataStore allow you do some adjustment to the retrieve data before showing to the user.
 * 
 * @author JSong
 *
 */
public class Refiner implements IRefiner{
	/**
	 * Refiner for each record
	 * 
	 * @param dataService The DataService
	 * @param terms The search criteria
	 * @param rcd Current Record in map
	 * @param en Current {@link Entity} if available.
	 */
	public void refineRow(IDataService dataService, Map<String, Object> terms, Map<String,Object> rcd, IEntity en) throws Exception{
	}

	/**
	 * Refine data for each page of data
	 * 
	 * @param data The dataService
	 * @param terms The search criteria
	 * @param list The search result 
	 * @param pg The {@link PageInfo} object.
	 */
	public void refineData(IDataService data, Class[] cs,Map<String,Object> terms,String text, String sort, String facet, boolean raw, PageInfo<Map> pg) throws Exception{
	}

	/**
	 * Refine Search Criteria.
	 */
	public void refineSearch(IDataService data, Map<String, Object> spec) throws Exception{
	}
}
