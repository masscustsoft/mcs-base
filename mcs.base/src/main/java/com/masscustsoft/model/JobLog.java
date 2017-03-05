package com.masscustsoft.model;

import java.sql.Timestamp;

import com.masscustsoft.api.FullBody;
import com.masscustsoft.api.FullText;
import com.masscustsoft.api.IndexKey;
import com.masscustsoft.api.SQLField;
import com.masscustsoft.api.SQLSize;
import com.masscustsoft.api.SQLTable;
import com.masscustsoft.api.TimestampIndex;

@SQLTable("JOBS")
public class JobLog extends Entity {
	@IndexKey @SQLField("JOB_INSTANCE_ID") @SQLSize(64)
	String jobInstanceUuid;
	
	@IndexKey @SQLField("JOB_ACTION_ID") @SQLSize(64)
	String jobActionUuid;
	
	@FullText @SQLField("LOG_TYPE") @SQLSize(1)
	String logLevel;

	@TimestampIndex @SQLField("LOG_DATE")
	Timestamp logDate;
	
	@FullBody @SQLField("MESSAGE") @SQLSize(255)
	String message;

	@Override
	public boolean supportFullText(){
		return false;
	}
	
	public String getJobInstanceUuid() {
		return jobInstanceUuid;
	}

	public void setJobInstanceUuid(String jobInstanceUuid) {
		this.jobInstanceUuid = jobInstanceUuid;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getLogLevel() {
		return logLevel;
	}

	public void setLogLevel(String logLevel) {
		this.logLevel = logLevel;
	}

	public Timestamp getLogDate() {
		return logDate;
	}

	public void setLogDate(Timestamp logDate) {
		this.logDate = logDate;
	}

	public String getJobActionUuid() {
		return jobActionUuid;
	}

	public void setJobActionUuid(String jobActionUuid) {
		this.jobActionUuid = jobActionUuid;
	}
}
