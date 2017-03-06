package com.masscustsoft.model;

import com.masscustsoft.api.FullText;
import com.masscustsoft.api.IndexKey;
import com.masscustsoft.api.SQLSize;

public class FacetItem extends Entity {
	@IndexKey @SQLSize(64)
	String queryId;
	
	@FullText @SQLSize(255)
	String item;
	
	@FullText @SQLSize(255)
	String names;
	
	@IndexKey @SQLSize(64)
	Integer freq;

	public String getQueryId() {
		return queryId;
	}

	public void setQueryId(String queryId) {
		this.queryId = queryId;
	}

	public String getItem() {
		return item;
	}

	public void setItem(String item) {
		this.item = item;
	}

	public Integer getFreq() {
		return freq;
	}

	public void setFreq(Integer freq) {
		this.freq = freq;
	}

	public String getNames() {
		return names;
	}

	public void setNames(String names) {
		this.names = names;
	}
	
	public String toString(){
		return "{FacetItem queryId="+queryId+", item="+item+", freq="+freq+",names="+names+"}";
	}
}
