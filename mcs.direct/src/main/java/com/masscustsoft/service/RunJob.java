package com.masscustsoft.service;

import com.masscustsoft.model.AbstractResult;
import com.masscustsoft.model.Job;
import com.masscustsoft.model.JobInstance;
import com.masscustsoft.service.JobGroup;
import com.masscustsoft.service.JobThread;
import com.masscustsoft.util.ThreadHelper;

public class RunJob extends DirectAction {
	private Job findJob(String jobUuid){
		for (JobGroup g:cfg.getJobService().getGroups()){
			for (Job j:g.getJobs()){
				if (j.getUuid().equals(jobUuid)) return j;
			}
		}
		return null;
	}
	
	@Override
	protected void run(AbstractResult ret) throws Exception {
		if (ThreadHelper.get("$MockBackDays$")==null) throw new Exception("Job action is not supproted.");
		String jobUuid=requiredStr("jobUuid");
		System.out.println("mock Job="+jobUuid);
		Job j=findJob(jobUuid);
		System.out.println("mock find Job="+j);
		if (j==null) return;
		JobInstance inst = cfg.getJobService().launchJob(getDs(), j, null,"running","");
		JobThread thread=new JobThread();
		System.out.println("mock exec Job="+j);
		j.execute(thread, getDs(), inst);
		getDs().deleteBean(inst);
	}
}
