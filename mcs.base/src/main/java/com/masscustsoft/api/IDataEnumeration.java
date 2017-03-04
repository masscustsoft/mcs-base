package com.masscustsoft.api;

import java.util.Map;

import com.masscustsoft.service.PageInfo;

public interface IDataEnumeration<T> {
	public boolean hasMoreElements() throws Exception;
	public T nextElement() throws Exception;
	public int getAmount();
	public Map getAttributes();
	public PageInfo getPage();
}

