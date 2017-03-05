package com.masscustsoft.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import com.masscustsoft.api.IDataService;
import com.masscustsoft.api.IRepository;
import com.masscustsoft.helper.HttpClient;
import com.masscustsoft.model.Job;
import com.masscustsoft.model.JobInstance;
import com.masscustsoft.model.JobInstanceAttachment;
import com.masscustsoft.util.CacheUtil;
import com.masscustsoft.util.GlbHelper;
import com.masscustsoft.util.LightStr;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.LogUtil;
import com.masscustsoft.util.ThreadHelper;

public class JobThread implements Runnable{
	List<Job> list=new ArrayList<Job>();
	transient IDataService dataService;
	transient IRepository fileService;
	
	transient long lastTick=System.currentTimeMillis();
	transient boolean idle=true;
	
	transient JobService jobService;
	
	transient String threadId;
	transient JobInstance jobInstance=null;
	transient boolean needReload=false;
	transient List<Job> waitings=new ArrayList<Job>();
	
	private void runJob(final Job agent,JobInstance inst) throws Exception{
		ThreadHelper.set("lang", agent.getLang());
		ThreadHelper.set("numfmt", agent.getNumFmt());
		ThreadHelper.set("longdtfmt", agent.getLongDtFmt());
		ThreadHelper.set("shortdtfmt", agent.getShortDtFmt());
		ThreadHelper.set("userId", "jobAgent");
		
		if (!LightStr.isEmpty(agent.getTimezoneId())) ThreadHelper.set("timezone",agent.getTimezoneId());
		Calendar c=LightUtil.getCalendar(agent.getTimezoneId());
		long tick=c.getTimeInMillis();
		
		inst.setRunTime(LightUtil.longDate());
		inst.setStatus("running");
		dataService.updateBean(inst);	
		
		ThreadHelper.set("_ignoreJobReload","1");
		agent.setLastRunTime(LightUtil.longDate(c));
		dataService.updateBean(agent);
		ThreadHelper.set("_ignoreJobReload",null);
		
		//LogUtil.debug("run job "+agent.getName());
		
		String status=agent.execute(this,dataService,inst);
		inst.setStatus(status);
		
		c.setTime(LightUtil.longDate());
		inst.setEndTime(LightUtil.longDate());
		inst.setRunSeconds((c.getTimeInMillis()-tick)/1000D);
		dataService.updateBean(inst);
		
		if (!LightStr.isEmpty(agent.getNotifyee())){
			if (agent.hasEvent("R") || inst.getErrors()>0 && agent.hasEvent("E") || inst.getWarns()>0 && agent.hasEvent("W") || inst.getInfos()>0 && agent.hasEvent("I")){
				
				List<JobInstanceAttachment> atts=new ArrayList<JobInstanceAttachment>();
				if ("yes".equals(agent.getWithAttachments())){
					atts=dataService.getBeanList(JobInstanceAttachment.class,"{jobInstanceUuid:'"+inst.getUuid()+"'}",""); 
				}
				Map<String,Object> env=(Map)LightUtil.toJsonObject(agent);
				env.putAll((Map)LightUtil.toJsonObject(inst));
				String sub=jobService.getNotifySubject();
				String body=jobService.getNotifyBody();
				if (!LightStr.isEmpty(sub)){
					notify(agent.getNotifyee(),LightUtil.macro(sub,'%', env).toString(),
							LightUtil.macro(body,'%', env).toString()
							,atts);
				}
			}
		}
		if (!LightStr.isEmpty(inst.getCallback())){
			HttpClient wc=new HttpClient();
			StringBuffer buf=new StringBuffer();
			try{
				String cb=inst.getCallback();
				if (cb.indexOf('?')>=0) cb+="&"; else cb+="?";
				cb+="jobInstanceId="+inst.getJobInstanceId();
				wc.doGet(cb, buf);
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	private long runScheduledJobs(long min) throws Exception{
		for (Job job:list){
			Calendar c=LightUtil.getCalendar(job.getTimezoneId());
			if (interrupted()) return -1;
			boolean skip=false;
			if ("no".equals(job.getActive())) skip=true;
			
			if (job.getStartDate()!=null){
				if (c.getTime().before(job.getStartDate())) skip=true;
			}
			if (job.getEndDate()!=null){
				if (c.getTime().after(job.getEndDate())) skip=true;
			}
			if (skip){
				if(job.getNextRunTime()!=null){
					job.setNextRunTime(null);
					ThreadHelper.set("_ignoreJobReload","1");
					dataService.updateBean(job);
					ThreadHelper.set("_ignoreJobReload",null);
				}
				continue;
			}
			if (job.getNextRunTime()==null){
	 			ThreadHelper.set("_ignoreJobReload","1");
	 			job.setNextRunTime(LightUtil.longDate(job.predictNextTime(c)));
	 			if (!job.getInner()) dataService.updateBean(job);
	 			ThreadHelper.set("_ignoreJobReload",null);
	 		}
			if (LightUtil.longDate(c).after(job.getNextRunTime())){
				//LogUtil.info("launch job by "+Thread.currentThread().getId()+" :"+ job);
				ThreadHelper.set("_ignoreJobReload","1");
				c=LightUtil.getCalendar(job.getTimezoneId());
				job.setLastRunTime(LightUtil.longDate(c));
				Calendar next=job.predictNextTime(c);
				job.setNextRunTime(LightUtil.longDate(next));
				dataService.updateBean(job);
				ThreadHelper.set("_ignoreJobReload",null);
				JobInstance inst = jobService.launchJob(dataService, job, null,"running","");
				runJob(job,inst);
			}
			long elapse = (job.getNextRunTime().getTime()-c.getTime().getTime());
			if (elapse<=0) min=0;
			else
			if (elapse<min) min=elapse;
			//System.out.println("JOB "+threadId+"="+job.getName()+",int="+job.getInterval()+",next="+job.getNextRunTime().toGMTString()+", ela="+elapse);
		}
//		System.out.println("MIN "+threadId+"="+min);
//		for (JobThread v:jobService.getThreadList()){
//			System.out.println(" "+v.getThreadId()+", "+v.interrupted()+", "+v.getList());
//		}
		return min;
	}
	
	public long executeJob(){
		long min=60000*60*8;
		if (!needReload){
			idle=false;
			lastTick=System.currentTimeMillis();
			try{
				min=runScheduledJobs(min);
			}
			catch (InterruptedException e){
				return -1;
			}
			catch (Exception e){
				LogUtil.dumpStackTrace(e);
			}
			idle=true;
		}
		return min;
	}
	
	public void execute(){
		if (jobInstance==null){
			executeJob();		
		}
		else{
			try {
				//System.out.println("RUNONCE "+jobInstance);
				runJob(jobInstance.getJob(), jobInstance);
				this.jobService.endThread(this, dataService, fileService);
			} catch (Exception e) {
				LogUtil.dumpStackTrace(e);
			}
		}
	}
	
	@Override
	public void run() {
		System.out.println("JOB-run "+this.getThreadId()+", "+jobService.getCfg());
		final String jobUser="job@"+this.getThreadId();
		if (jobInstance==null){
			while (true){
				
				jobService.initJobUser(jobUser);
				
				long min=executeJob();
				if (min==-1) {
					break; //interrupted
				}
				try{
					if (min>0) Thread.sleep(min); 
				}
				catch (InterruptedException e) {
					break;
				}
				finally{
					CacheUtil.clear();
				}
			}
		}
		else{
			try {
				jobService.initJobUser(jobUser);
				//System.out.println("RUNONCE "+jobInstance);
				runJob(jobInstance.getJob(), jobInstance);
				this.jobService.endThread(this, dataService, fileService);
			} catch (Exception e) {
				LogUtil.dumpStackTrace(e);
			}
			finally{
				CacheUtil.clear();
			}
		}
		
	}
	
	public void notify(String who, String subject, String body,List<JobInstanceAttachment> atts){
		NotifyService alt = LightUtil.getCfg().getNotifyService();
		if (alt==null) {
			LogUtil.error("no NotifyService installed.");
			return;
		}
		try{
			alt.sendMessage(who, subject, body, (List)atts);
		}
		catch (Exception e){
			e.printStackTrace();
			LogUtil.error("Notify.sendMessage failed: "+e.getMessage());
		}
	}
	
	public boolean interrupted(){
		Thread th=(Thread)GlbHelper.get(this.hashCode()+".thread");
		if (th==null) return false;
		return th.isInterrupted();
	}

	public boolean shutdown(){
		Thread th=(Thread)GlbHelper.get(this.hashCode()+".thread");
		if (th==null) return false;
		th.interrupt();
		return true;
	}
	
	public List<Job> getList() {
		return list;
	}

	public void setList(List<Job> list) {
		this.list = list;
	}

	@Override
	public String toString(){
		return "JobThread{list="+list+"}";
	}

	public IDataService getDataService() {
		return dataService;
	}

	public void setDataService(IDataService dataService) {
		this.dataService = dataService;
	}

	public boolean getIdle() {
		return idle;
	}

	public void setIdle(boolean idle) {
		this.idle = idle;
	}

	public long getLastTick() {
		return lastTick;
	}

	public JobService getJobService() {
		return jobService;
	}

	public void setJobService(JobService jobService) {
		this.jobService = jobService;
	}

	public String getThreadId() {
		return threadId;
	}

	public void setThreadId(String threadId) {
		this.threadId = threadId;
	}

	public boolean hasJob(String jobUuid) {
		for (Job job:waitings){
			if (jobUuid.equals(job.getUuid())) return true;
		}
		for (Job job:list){
			if (jobUuid.equals(job.getUuid())) return true;
		}
		return false;
	}

	public IRepository getFileService() {
		return fileService;
	}

	public void setFileService(IRepository fileService) {
		this.fileService = fileService;
	}

	public void setInstance(JobInstance inst) {
		jobInstance=inst;
	}

}
