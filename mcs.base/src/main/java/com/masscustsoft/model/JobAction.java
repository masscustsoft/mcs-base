package com.masscustsoft.model;

import java.io.File;
import java.util.List;

import com.masscustsoft.Lang.CLASS;
import com.masscustsoft.api.FullText;
import com.masscustsoft.api.ITraceable;
import com.masscustsoft.api.IndexKey;
import com.masscustsoft.api.NumIndex;
import com.masscustsoft.api.SQLSize;
import com.masscustsoft.api.SQLTable;
import com.masscustsoft.service.FileTempItem;
import com.masscustsoft.service.JobThread;
import com.masscustsoft.util.LightStr;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.ThreadHelper;

@SQLTable("JOBS")
public class JobAction extends Entity implements ITraceable {
	@IndexKey @SQLSize(64)
	String jobUuid;
	
	@NumIndex
	Integer seqId;
	
	@IndexKey @SQLSize(64)
	String action;
	
	@FullText @SQLSize(64)
	String ifException; //default false, if set, if exception, we just ignore it.
	
	String status;
	
	protected transient Job job;
	protected transient JobThread jobThread;
	protected transient JobInstance jobInstance;
	protected transient JobInstanceAction jobInstanceAction;
	
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
	
	public void warn(String msg){
		jobInstanceAction.warns++;
		jobInstance.log(jobInstanceAction,"warn",msg);
	}
	
	public void info(String msg){
		jobInstanceAction.infos++;
		jobInstance.log(jobInstanceAction,"info",msg);
	}
	
	public void error(String msg){
		jobInstanceAction.errors++;
		jobInstance.log(jobInstanceAction,"error",msg);
	}
	
	public void addAttachment(File f) throws Exception{
		FileTempItem ft=new FileTempItem(f);
		jobInstance.attachmentCount++;
		JobInstanceAttachment att=new JobInstanceAttachment();
		att.setAttachmentDate(LightUtil.longDate());
		att.setJobInstanceUuid(jobInstance.getUuid());
		ExternalFile.newExternalFile(jobThread.getDataService(), jobThread.getFileService(), att, ft, f.getName());
		jobThread.getDataService().insertBean(att);
	}
	
	public void addAttachment(String fn, String body) throws Exception{
		jobInstance.attachmentCount++;
		JobInstanceAttachment att=new JobInstanceAttachment();
		att.setAttachmentDate(LightUtil.longDate());
		att.setJobInstanceUuid(jobInstance.getUuid());
		ExternalFile.newExternalFile(jobThread.getDataService(), jobThread.getFileService(), att, body, fn);
		jobThread.getDataService().insertBean(att);
	}
	
	public void run(JobThread jobThread, Job job, JobInstance inst, JobInstanceAction a) throws Exception{
		this.job=job;
		this.jobThread=jobThread;
		this.jobInstance=inst;
		this.jobInstanceAction=a;
	}

	public Job getJob() {
		return job;
	}

	public void setJob(Job job) {
		this.job = job;
	}
	
	public String getAction() {
		if (action==null) action=CLASS.getSimpleName(getClass());
		return action;
	}

	public String getJobUuid() {
		return jobUuid;
	}

	public void setJobUuid(String jobUuid) {
		this.jobUuid = jobUuid;
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
	
	@Override
	public boolean beforeDelete() throws Exception {
		dataService.deleteBeanList(JobLog.class, "{jobActionUuid:'"+uuid+"'}");
		return super.beforeDelete();
	}

	public JobInstanceAction getJobInstanceAction() {
		return jobInstanceAction;
	}

	public void setJobInstanceAction(JobInstanceAction jobInstanceAction) {
		this.jobInstanceAction = jobInstanceAction;
	}

	public void recalculate(Job job, JobInstance inst, JobInstanceAction a) throws Exception{
		Object recalc=ThreadHelper.get("jobRecalc");
		if (recalc!=null) return;
		List<JobLog> list = inst.getDataService().getBeanList(JobLog.class, "{jobInstanceUuid:'"+inst.getUuid()+"',jobActionUuid:'"+a.getUuid()+"'}","");
		int ii=0,ee=0,ww=0;
		for (JobLog log:list){
			if ("E".equals(log.logLevel)) ee++;
			else
			if ("W".equals(log.logLevel)) ww++;
			else
			if ("I".equals(log.logLevel)) ii++;
		}
		int di=ii-a.infos,de=ee-a.errors,dw=ww-a.warns;
		a.infos+=di;
		a.warns+=dw;
		a.errors+=de;
		inst.infos+=di;
		inst.warns+=dw;
		inst.errors+=de;
		ThreadHelper.set("jobRecalc",true);
	}
	
	@Override
	public void afterInsert() throws Exception {
		if (!LightStr.isEmpty(jobUuid)){
			Job job=dataService.getBean(Job.class, "uuid", jobUuid);
			if (job.getInterval()!=0) Job.restartJobs();
		}
		super.afterInsert();
	}
	
	@Override
	public void afterUpdate() throws Exception {
		if (!LightStr.isEmpty(jobUuid)){
			Job job=dataService.getBean(Job.class, "uuid", jobUuid);
			if (job.getInterval()!=0) Job.restartJobs();
		}
		super.afterUpdate();
	}
	
	@Override
	public void afterDelete() throws Exception {
		if (!LightStr.isEmpty(jobUuid)){
			Job job=dataService.getBean(Job.class, "uuid", jobUuid);
			if (job.getInterval()!=0) Job.restartJobs();
		}
		super.afterDelete();
	}


	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}

