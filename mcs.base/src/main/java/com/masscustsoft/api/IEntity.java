package com.masscustsoft.api;

import java.util.List;
import java.util.Map;

import com.masscustsoft.model.AbstractResult;

public interface IEntity {
	public void setDataService(IDataService dataService);
	public IDataService getDataService();
		
	public <T> T getOldValue(String fld,T def);
	public String getUuid();
	public void validate() throws Exception;
	public Class getPrimaryClass();
	public boolean beforeInsert() throws Exception;
	public void afterInsert() throws Exception;
	public void validateDelete() throws Exception;
	public boolean beforeDelete() throws Exception;
	public void afterDelete() throws Exception;
	public boolean beforeUpdate() throws Exception;
	public void afterUpdate() throws Exception;
	public IEntity getOld();
	
	public List<IReferentItem> getReferList() throws Exception;
	public List<IReferentItem> getDescendantList() throws Exception;
	
	public void getBeanList(AbstractResult result, Map<String, Object> terms, String text, String sortBy, int from, int size)  throws Exception;
	
}
