package com.masscustsoft.model;


import java.sql.Timestamp;

import com.masscustsoft.api.FullText;
import com.masscustsoft.api.IDataService;
import com.masscustsoft.api.IndexKey;
import com.masscustsoft.api.NumIndex;
import com.masscustsoft.api.SQLSize;
import com.masscustsoft.api.SQLTable;
import com.masscustsoft.api.TimestampIndex;
import com.masscustsoft.helper.Upload;
import com.masscustsoft.service.JobCmdStopJob;
import com.masscustsoft.util.ClusterUtil;
import com.masscustsoft.util.LogUtil;
import com.masscustsoft.util.LightUtil;

@SQLTable("JOBS")
public class JobInstance extends Entity {
	@IndexKey @SQLSize(64)
	Long jobInstanceId;
	
	@TimestampIndex
	Timestamp runTime;	//execute time
	
	@TimestampIndex
	Timestamp endTime;	//execute time
	
	Double runSeconds;
	
	@IndexKey @SQLSize(64)
	String status;	//running, success, warning, failed, waiting
	
	@NumIndex
	int warns=0,infos=0,errors=0;
	
	@IndexKey @SQLSize(64)
	String jobUuid;
	
	@FullText @SQLSize(255)
	String name;
	
	@IndexKey @SQLSize(64)
	String userGroup;
	
	@IndexKey @SQLSize(64)
	String jobSource;
	
	@IndexKey @SQLSize(64)
	String owner;

	@IndexKey @SQLSize(64)
	String threadId;
	
	int attachmentCount=0,actionCount=0;
	
	transient Job job=null;
	
	String callback;
	
	@Override
	public boolean supportFullText(){
		return false;
	}
	
	public void log(JobInstanceAction action, String level, String msg){
		if (level.equals("warn")) level="W";
		else
		if (level.equals("error")) level="E";
		else
		level="I";
		if (level.equals("W")) warns++;
		else
		if (level.equals("I")) infos++;
		else
		if (level.equals("E")) errors++;
		JobLog log=new JobLog();
		log.setJobInstanceUuid(getUuid());
		log.setLogDate(LightUtil.longDate());
		log.setLogLevel(level);
		log.setJobActionUuid(action.getUuid());
		
		log.setMessage(msg);
		try {
			dataService.insertBean(log);
		} catch (Exception e) {
			LogUtil.dumpStackTrace(e);
		}
	}
	
	public Timestamp getRunTime() {
		return runTime;
	}

	public void setRunTime(Timestamp runTime) {
		this.runTime = runTime;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
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

	public String getJobUuid() {
		return jobUuid;
	}

	public void setJobUuid(String jobUuid) {
		this.jobUuid = jobUuid;
	}

	public Double getRunSeconds() {
		return runSeconds;
	}

	public void setRunSeconds(Double runSeconds) {
		this.runSeconds = runSeconds;
	}

	public String getUserGroup() {
		return userGroup;
	}

	public void setUserGroup(String userGroup) {
		this.userGroup = userGroup;
	}

	public int getErrors() {
		return errors;
	}

	public void setErrors(int errors) {
		this.errors = errors;
	}

	@Override
	public boolean beforeDelete() throws Exception {
		dataService.deleteBeanList(JobInstanceAction.class, "{jobInstanceUuid:'"+uuid+"'}");
		dataService.deleteBeanList(JobInstanceAttachment.class, "{jobInstanceUuid:'"+uuid+"'}");
		return super.beforeDelete();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public AbstractResult doStopJob(Upload up){
		String jid=up.getStr("jobUuid", null);
		JobCmdStopJob cmd = new JobCmdStopJob();
		cmd.setJobUuid(jid);
		JsonResult res = new JsonResult();
		try{
			ClusterUtil.broadcast(null,null,cmd);
			IDataService db=LightUtil.getDataService();
			String instId = up.getStr("uuid","~");
			JobInstance inst=db.getBean(JobInstance.class, "uuid", instId);
			if (inst!=null){
				inst.setStatus("stopped");
				db.updateBean(inst);
				if ("running".equals(inst.getStatus())){
					JobInstanceAction act = db.getBean(JobInstanceAction.class, "jobInstanceUuid",instId, "actionStatus", "running");
					if (act!=null){
						act.setActionStatus("stopped");
						db.updateBean(act);
					}
				}				
			}
		}
		catch(Exception e){
			res.setError(e);
		}
		return res;
	}

	public int getAttachmentCount() {
		return attachmentCount;
	}

	public void setAttachmentCount(int attachmentCount) {
		this.attachmentCount = attachmentCount;
	}

	public int getActionCount() {
		return actionCount;
	}

	public void setActionCount(int actionCount) {
		this.actionCount = actionCount;
	}

	public String getJobSource() {
		return jobSource;
	}

	public void setJobSource(String jobSource) {
		this.jobSource = jobSource;
	}

	public String getThreadId() {
		return threadId;
	}

	public void setThreadId(String threadId) {
		this.threadId = threadId;
	}

	public Timestamp getEndTime() {
		return endTime;
	}

	public void setEndTime(Timestamp endTime) {
		this.endTime = endTime;
	}

	public void setJob(Job job) {
		this.job=job;
	}

	public Job getJob() {
		return job;
	}

	public Long getJobInstanceId() {
		return jobInstanceId;
	}

	public void setJobInstanceId(Long jobInstanceId) {
		this.jobInstanceId = jobInstanceId;
	}

	public String getCallback() {
		return callback;
	}

	public void setCallback(String callback) {
		this.callback = callback;
	}
}
