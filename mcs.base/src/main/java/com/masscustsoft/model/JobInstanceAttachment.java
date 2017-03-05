package com.masscustsoft.model;

import java.sql.Timestamp;

import com.masscustsoft.api.IndexKey;
import com.masscustsoft.api.SQLSize;
import com.masscustsoft.api.SQLTable;
import com.masscustsoft.api.TimestampIndex;
import com.masscustsoft.util.LightUtil;

@SQLTable("JOBS")
public class JobInstanceAttachment extends BasicFile{
	@IndexKey @SQLSize(64)
	String jobInstanceUuid;

	@TimestampIndex 
	Timestamp attachmentDate;
	
	public String getJobInstanceUuid() {
		return jobInstanceUuid;
	}

	public void setJobInstanceUuid(String jobInstanceUuid) {
		this.jobInstanceUuid = jobInstanceUuid;
	}

	public Timestamp getAttachmentDate() {
		return attachmentDate;
	}

	public void setAttachmentDate(Timestamp attachmentDate) {
		this.attachmentDate = attachmentDate;
	}

	@Override
	public boolean beforeInsert() throws Exception {
		if (attachmentDate==null) attachmentDate=LightUtil.longDate();
		return super.beforeInsert();
	}
}
