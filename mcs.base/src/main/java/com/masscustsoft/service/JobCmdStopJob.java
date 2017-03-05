package com.masscustsoft.service;

import com.masscustsoft.util.LightUtil;

public class JobCmdStopJob extends ClusterCmd{
	String jobUuid;
	
	@Override
	public void run(ClusterService clu,Comparable me,Comparable src){
		JobService svc = LightUtil.getCfg().getJobService();
		if (svc!=null) svc.stopJob(jobUuid);
	}

	public String getJobUuid() {
		return jobUuid;
	}

	public void setJobUuid(String jobUuid) {
		this.jobUuid = jobUuid;
	}
}
