package com.masscustsoft.model;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.masscustsoft.util.LightUtil;

public class AbstractResult {
	public enum ResultType{
		Xml,Json,Stream,Soap
	}
	
	/*
	 * result==null, it's a stream
	 * result instanceof String, it's a json string
	 * result is a map, convert to json needed.
	 */
	private boolean success;
	private Object result;
	private Map attributes=new HashMap();
	
	private Integer amount;
	private List<Map<String,Object>> list=new ArrayList<Map<String,Object>>();
	private Map<String,Integer> cats=new TreeMap<String,Integer>();
	
	transient private ResultType type=ResultType.Xml; //json,xml,stream
	transient private Object tag;
	
	public AbstractResult(){
		result=null;
		success=true;
	}

	public Object getResult() {
		return result;
	}

	public void setResult(Object result) {
		this.result = result;
	}

	public void setError(Exception e) {
		this.success=false;
		String msg=e.getMessage();
		if (e instanceof InvocationTargetException){
			InvocationTargetException ee=(InvocationTargetException)e;
			msg=ee.getTargetException().getMessage();
		}
		this.result = msg;
		try {
			LightUtil.getCfg().processSQLException(e,this);
		} catch (Exception e1) {
		}
	}

	public boolean getSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public ResultType getType() {
		return type;
	}

	public void setType(ResultType type) {
		this.type = type;
	}

	public Object getTag() {
		return tag;
	}

	public void setTag(Object tag) {
		this.tag = tag;
	}

	public Map getAttributes() {
		return attributes;
	}

	public void setAttributes(Map attributes) {
		this.attributes = attributes;
	}
	
	public void setAttribute(String attr, Object val){
		this.attributes.put(attr, val);
	}
	
	public Object getAttribute(String attr){
		return this.attributes.get(attr);
	}
	
	public Integer getAmount() {
		return amount;
	}

	public void setAmount(Integer amount) {
		this.amount = amount;
	}

	public List<Map<String, Object>> getList() {
		return list;
	}

	public void setList(List<Map<String, Object>> list) {
		this.list = list;
	}

	public Map<String, Integer> getCats() {
		return cats;
	}

	public void setCats(Map<String, Integer> cats) {
		this.cats = cats;
	}
	
	public void reset(AbstractResult ret){
		this.success=ret.success;
		this.amount=ret.amount;
		this.attributes=ret.attributes;
		this.cats=ret.cats;
		this.list=ret.list;
		this.result=ret.result;
		this.type=ret.type;
		this.tag=ret.tag;
	}
}
