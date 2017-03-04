package com.masscustsoft.api;

import java.sql.Connection;

public interface IDatabaseAccess{
	public Connection connect() throws Exception;
	public void disconnect(Connection conn) throws Exception;
}
