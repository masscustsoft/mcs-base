package com.masscustsoft.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.masscustsoft.Lang.CLASS;
import com.masscustsoft.api.IDataService;
import com.masscustsoft.api.IEntity;
import com.masscustsoft.model.SearchResult;
import com.masscustsoft.util.LightStr;
import com.masscustsoft.util.LightUtil;

/**
 * AbstractDataService exposes all needed methods that must be implemented for new data connectors but need to extend DataService to be compatible to DataService.
 * 
 * @author JSong
 *
 */
public abstract class AbstractDataService implements IDataService {

	/**
	 * {@inheritDoc}
	 */
	public void _insertBean(String clusterId,String tbl,String uniqueId, String xml) throws Exception{
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void _deleteBean(String clusterId, String tbl, String uniqueId) throws Exception{
		
	}
	
	private transient List<String> locks=Collections.synchronizedList(new ArrayList<String>());
	
	/**
	 * Lock an Entity by it's unique uuid.
	 * 
	 * @param uuid
	 * @throws Exception
	 */
	protected void _lock(String uuid) throws Exception {
		for (int i=0;i<100;i++){
			if (locks.contains(uuid)){
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			}
			else{
				locks.add(uuid);
				return;		
			}		
		}
		throw new Exception("GetLockTimeout");
	}
	
	/**
	 * {@inheritDoc}.
	 */
	public void _updateBean(String clusterId, String tbl, String uniqueId,
			String old, String xml) throws Exception {
		_deleteBean(clusterId,tbl,uniqueId);
		_insertBean(clusterId,tbl,uniqueId,xml);
	}
	
	protected String getPureBean(String beans){
		String bean=beans; // ENTITY USER
		int x=bean.lastIndexOf(" ");
		if (x>0){
			bean=bean.substring(x+1);
		}
		return bean;
	}

	protected String getSearchBean(String names){
		if (names.length()==0) return null;
		String [] nameList=names.split(",");
		String bean=nameList[0];
		if (LightStr.isEmpty(bean)) bean="Entity";
		return bean;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	protected SearchResult _preSearch(String names, Map<String,Object> terms, String fields,
			String text, String sortBy, int from, int size, String facet)
			throws Exception {
		SearchResult res = new SearchResult();
		if (names.length()>0){
			try{
				String bean=getSearchBean(names);
				Class cls=CLASS.forName(LightUtil.getBeanFactory().findRealClass(getPureBean(bean)));
				IEntity en=(IEntity)cls.newInstance();
				en.setDataService((IDataService)this);
				en.getBeanList(res, terms, text, sortBy, from, size);
			}
			catch(InstantiationException e){
				
			}
		}
		
		return res;
	}
	
	@Override
	public SearchResult _doSearch(String names, Map<String,Object> terms, String fields,
			String text, String sortBy, int from, int size, String facet)
			throws Exception {
		
		return null;
	}
	

	@Override
	public SearchResult _doSearchBySql(String sql, Map<String,Object> terms, int from, int size, String sort, String facet) throws Exception {
		throw new Exception("Not supported!");
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public void destroy() throws Exception{
	}

}
