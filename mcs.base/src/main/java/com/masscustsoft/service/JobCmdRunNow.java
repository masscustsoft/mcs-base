package com.masscustsoft.service;

import com.masscustsoft.util.LightUtil;

public class JobCmdRunNow extends ClusterCmd {
	@Override
	public void run(ClusterService clu,Comparable me,Comparable src) throws Exception{
		JobService svc = LightUtil.getCfg().getJobService();
		if (svc!=null) svc.addWaitingJob();
	}
}
