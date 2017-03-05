package com.masscustsoft.model;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Map;

import com.masscustsoft.api.FullBody;
import com.masscustsoft.api.FullText;
import com.masscustsoft.api.IBeanFactory;
import com.masscustsoft.api.IDataService;
import com.masscustsoft.api.IndexKey;
import com.masscustsoft.api.SQLSize;
import com.masscustsoft.api.SQLTable;
import com.masscustsoft.api.TimestampIndex;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.LogUtil;

@SQLTable("ChangeLogs")
public class ChangeLog extends Entity {
	@IndexKey @SQLSize(64)
	String beanId;

	@TimestampIndex 
	Timestamp traceDate;
	
	@IndexKey @SQLSize(64)
	String userId;
	
	@IndexKey @SQLSize(64)
	String action;	//insert, update, delete
	
	@FullText @SQLSize(255)
	String beanType;
	
	@FullBody 
	String summary;
	
	String beanData;
	
	public String getBeanId() {
		return beanId;
	}

	public void setBeanId(String beanId) {
		this.beanId = beanId;
	}

	public Timestamp getTraceDate() {
		return traceDate;
	}

	public void setTraceDate(Timestamp traceDate) {
		this.traceDate = traceDate;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getBeanType() {
		return beanType;
	}

	public void setBeanType(String beanType) {
		this.beanType = beanType;
	}

	public String getBeanData() {
		return beanData;
	}

	public void setBeanData(String beanData) {
		this.beanData = beanData;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public void append(IDataService dataService, String action, String summary, Map<String,Object> map) throws IOException{
		IBeanFactory bf=LightUtil.getBeanFactory();
		ChangeLog bt=this;
		String uuid=(String)map.get("rowId"); if (uuid==null) uuid="";
		bt.setBeanId(uuid);
		bt.setBeanType(LightUtil.getCascadeName(Map.class));
		bt.setAction(action);
		bt.setBeanData(bf.toXml(map, 1));
		bt.setTraceDate(LightUtil.longDate());
		bt.setSummary(summary);
		String userId=LightUtil.getUserId();
		bt.setUserId(userId);
		try {
			dataService.insertBean(bt);
		} catch (Exception e) {
			LogUtil.dumpStackTrace(e);
		}
	}
}
