package com.masscustsoft.api;

import java.util.List;
import java.util.Map;

public interface IReferentItem {
	public String getModel();
	public Map<String, String> getKeyMapping();
	public String getTable();
	public List<IEntity> getReferentData(IEntity me, int max) throws Exception;
	public void checkReference(IEntity me) throws Exception;
	public void cascadeDelete(IEntity me) throws Exception;
}
