package com.masscustsoft.api;

import java.util.Map;

public interface IJoin {
	public void doJoin(IDataService data, Map m) throws Exception;
	public String getModel();
	
}
