package com.masscustsoft.model;

import java.sql.Timestamp;

import com.masscustsoft.api.FullText;
import com.masscustsoft.api.IndexKey;
import com.masscustsoft.api.NumIndex;
import com.masscustsoft.api.SQLSize;
import com.masscustsoft.api.SQLTable;
import com.masscustsoft.api.TimestampIndex;
import com.masscustsoft.util.ThreadHelper;

@SQLTable("JOBS")
public class JobInstanceAction extends Entity {
	@IndexKey @SQLSize(64)
	String jobInstanceUuid;
	
	@IndexKey
	Integer seqId;
	
	@IndexKey @SQLSize(64)
	String action;
	
	@FullText @SQLSize(64)
	String ifException; //default false, if set, if exception, we just ignore it.
	
	@FullText @SQLSize(64)
	String actionStatus;
	
	@TimestampIndex
	Timestamp runTime;	//execute time
	
	@NumIndex
	Double runSeconds;
	
	@NumIndex
	int warns=0,infos=0,errors=0;
	
	@IndexKey @SQLSize(64)
	String jobActionUuid;
	
	@Override
	public boolean supportFullText(){
		return false;
	}
	
	public Object getVar(String var){
		return ThreadHelper.get(var);
	}
	
	public void setVar(String var, Object obj){
		ThreadHelper.set(var, obj);
	}

	public Integer getSeqId() {
		return seqId;
	}

	public void setSeqId(Integer seqId) {
		this.seqId = seqId;
	}

	public String getIfException() {
		return ifException;
	}

	public void setIfException(String ifException) {
		this.ifException = ifException;
	}

	public String getActionStatus() {
		return actionStatus;
	}

	public void setActionStatus(String actionStatus) {
		this.actionStatus = actionStatus;
	}

	public Timestamp getRunTime() {
		return runTime;
	}

	public void setRunTime(Timestamp runTime) {
		this.runTime = runTime;
	}

	public Double getRunSeconds() {
		return runSeconds;
	}

	public void setRunSeconds(Double runSeconds) {
		this.runSeconds = runSeconds;
	}

	public int getWarns() {
		return warns;
	}

	public void setWarns(int warns) {
		this.warns = warns;
	}

	public int getInfos() {
		return infos;
	}

	public void setInfos(int infos) {
		this.infos = infos;
	}

	public int getErrors() {
		return errors;
	}

	public void setErrors(int errors) {
		this.errors = errors;
	}
	
	@Override
	public boolean beforeDelete() throws Exception {
		dataService.deleteBeanList(JobLog.class, "{jobActionUuid:'"+uuid+"'}");
		return super.beforeDelete();
	}

	public String getJobInstanceUuid() {
		return jobInstanceUuid;
	}

	public void setJobInstanceUuid(String jobInstanceUuid) {
		this.jobInstanceUuid = jobInstanceUuid;
	}

	public String getJobActionUuid() {
		return jobActionUuid;
	}

	public void setJobActionUuid(String jobActionUuid) {
		this.jobActionUuid = jobActionUuid;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getAction() {
		return action;
	}

}
