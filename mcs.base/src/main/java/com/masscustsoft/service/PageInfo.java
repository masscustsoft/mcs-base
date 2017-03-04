package com.masscustsoft.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PageInfo<T> {
	protected int start;
	protected int limit;
	protected int amount;
	protected List<T> list=null;
	protected Map<String,Object> attributes=new HashMap<String,Object>();
	
	public PageInfo(int from, int size){
		this.start=from;
		this.limit=size;
	}

	public int getAmount() {
		return amount;
	}

	public void setAmount(int amount) {
		this.amount = amount;
	}

	public List<T> getList() {
		return list;
	}

	public void setList(List<T> list) {
		this.list = list;
	}
	
	public boolean enableFirst(){
		return (start>0);
	}
	
	public boolean enableLast(){
		return (start+limit<amount);
	}
	
	public boolean enablePrior(){
		return (start-limit>=0);
	}
	
	public boolean enableNext(){
		return (start+limit<amount);
	}
	
	public int page(){
		return (start / limit)+1;
	}
	
	public int pages(){
		int all=amount/limit;
		if (amount%limit==0) return all;
		return all+1;
	}
	
	public int rowFrom(){
		return start+1;
	}
	
	public int rowTo(){
		return start+list.size();
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

	public int getStart() {
		return start;
	}

	public void setStart(int start) {
		this.start = start;
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}
}
