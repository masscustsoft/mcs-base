package com.masscustsoft.api;

import java.util.List;
import java.util.Map;

public interface ISecurityService {
	public IUser getUser(IDataService data,String uid) throws Exception;
	public boolean verify(IDataService data,String pwd, IUser u) throws Exception;
	public void refineAccessList(IDataService data, IUser u, Map<String,String> vars, IRole r, List<String> acc) throws Exception;
}
