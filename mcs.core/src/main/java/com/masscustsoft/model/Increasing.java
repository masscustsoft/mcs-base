package com.masscustsoft.model;

import com.masscustsoft.api.IndexKey;
import com.masscustsoft.api.SQLSize;

public class Increasing extends Entity {
	@IndexKey @SQLSize(64)
	String incId;
	
	long longValue;
	double doubleValue;
	int intValue;
	
	public String getIncId() {
		return incId;
	}
	public void setIncId(String incId) {
		this.incId = incId;
	}
	public long getLongValue() {
		return longValue;
	}
	public void setLongValue(long longValue) {
		this.longValue = longValue;
	}
	public double getDoubleValue() {
		return doubleValue;
	}
	public void setDoubleValue(double doubleValue) {
		this.doubleValue = doubleValue;
	}
	public int getIntValue() {
		return intValue;
	}
	public void setIntValue(int intValue) {
		this.intValue = intValue;
	}

	
}
