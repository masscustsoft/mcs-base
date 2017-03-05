package com.masscustsoft.service;

import com.masscustsoft.util.GlbHelper;
import com.masscustsoft.util.LightUtil;

public class JobCmdRestartJobs extends ClusterCmd{
	@Override
	public void run(ClusterService clu,Comparable me,Comparable src) throws Exception{
		System.out.println("\n\n\n########restartService requested####\n\n\n");
		JobService svc = LightUtil.getCfg().getJobService();
		if (svc!=null) svc.restartJobs();
	}
}
