package com.masscustsoft.model;

import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.masscustsoft.api.FullText;
import com.masscustsoft.api.IDataService;
import com.masscustsoft.api.ITraceable;
import com.masscustsoft.api.IndexKey;
import com.masscustsoft.api.JsonField;
import com.masscustsoft.api.SQLSize;
import com.masscustsoft.api.SQLTable;
import com.masscustsoft.api.TimestampIndex;
import com.masscustsoft.helper.Upload;
import com.masscustsoft.service.JobCmdRestartJobs;
import com.masscustsoft.service.JobService;
import com.masscustsoft.service.JobThread;
import com.masscustsoft.service.inner.EventCalculator;
import com.masscustsoft.util.ClusterUtil;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.LogUtil;
import com.masscustsoft.util.MapUtil;
import com.masscustsoft.util.ReflectUtil;
import com.masscustsoft.util.ThreadHelper;

@SQLTable("Jobs")
public class Job extends Entity implements ITraceable{
	transient boolean inner=false;
	
	@IndexKey @SQLSize(64)
	String jobGroup;
	
	@IndexKey @SQLSize(64)
	String userGroup; //group for security
	
	@IndexKey @SQLSize(64)
	String jobSource; 	//job source

	@FullText @SQLSize(255)
	String name; 	//job name
	
	@IndexKey @SQLSize(64)
	int interval=1000;	//ms, 0= no interval -1 daily -2 biweekly -3 monthly -99=from variable
	
	@IndexKey @SQLSize(64)
	String threadId;	//same Id will share the same thread

	String timeFrame, weekdayFrame, biweekdayFrame, dayFrame, monthFrame;
	
	@JsonField(xmlBean=true)
	List<JobAction> actions=new ArrayList<JobAction>();
	
	@IndexKey @SQLSize(64)
	String active;
	
	String selfClean="yes";
	
	Integer keepDays=7;
	
	@IndexKey @SQLSize(64)
	String owner;
	
	@FullText @SQLSize(255)
	String notifyee;
	
	@FullText @SQLSize(255)
	String notifyEvents;
	
	String withAttachments;
	
	@TimestampIndex
	Timestamp startDate,endDate;
	
	String lang,numFmt,longDtFmt,timezoneId;
	
	transient List<String> events;
	transient EventCalculator calc;
	
	@TimestampIndex
	Timestamp lastRunTime,nextRunTime;
	
	public String execute(JobThread jobThread, IDataService data, JobInstance inst) throws Exception{
		boolean skip=false;
		inst.setActionCount(actions.size());
		boolean interrupted=false;
		for (JobAction act:actions){
			if (interrupted) break;
			if (jobThread.interrupted()) throw new InterruptedException();

			Calendar c = LightUtil.getCalendar(this.getTimezoneId());
			long tick=c.getTime().getTime();
			JobInstanceAction a=act.getJobInstanceAction();
			if (a==null) {
				LogUtil.error("Job Structure corrupt! "+inst.getJob().getName());
				return "failed"; 
			}
			a.setRunTime(LightUtil.longDate());
			if (skip) a.setActionStatus("skip");
			else{
				a.setActionStatus("running");
				data.updateBean(a);
				try{
					act.setDataService(data);
					act.run(jobThread,this,inst,a);
					a.setActionStatus("success");
				}
				catch(Exception e){
					if (e instanceof InterruptedException) interrupted=true;
					inst.log(a, "error", interrupted?"Interrupted":e.getMessage());
					a.setActionStatus("failed");
					LogUtil.dumpStackTrace(e);
					if (!"ignore".equals(a.getIfException())) skip=true; //if ignore will run on next step, or all other steps skipped
				}	
			}
			a.setRunSeconds((LightUtil.getCalendar().getTimeInMillis()-tick)/1000D);
			data.updateBean(a);
		}
		if (keepDays!=null && "yes".equals(selfClean)) cleanInstances(keepDays);
		if (interrupted) return "interrupted";
		if (inst.getErrors()>0) return "completedWithErrors";
		if (inst.getWarns()>0) return "completedWithWarnings";
		return "completed"; //complete, partial complete, failed
	}
	
	/*private void cleanInstances(IDataService data,int days){
		Calendar c=LightUtil.getCalendar();;
		c.add(Calendar.DATE, -days);
		try {
			data.deleteBeanList(JobInstance.class, "{jobUuid:'"+uuid+"',runTime:{lt:'"+LightUtil.encodeDate(c.getTime())+"'}}");
			data.commit(true);
		} catch (Exception e) {
			LogUtil.dumpStackTrace(e);
		}
	}*/
	
	public void shutdown(){ //in case some job created threads need to shutdown
	}

	public JobInstance getJobInstance() throws Exception{
		JobInstance inst=new JobInstance();
		inst.setJobUuid(getUuid());
		inst.setUserGroup(getUserGroup());
		inst.setName(getName());
		inst.setOwner(getOwner());
		inst.setJobSource(getJobSource());
		inst.setThreadId(getThreadId());
		inst.setRunTime(LightUtil.longDate());
		inst.setJobInstanceId(dataService.getSequenceId("JobInstanceId", JobInstance.class, "jobInstanceId", inst.getJobInstanceId()));
		return inst;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getInterval() {
		return interval;
	}

	public void setInterval(int interval) {
		this.interval = interval;
	}

	public String getThreadId() {
		return threadId;
	}

	public void setThreadId(String threadId) {
		this.threadId = threadId;
	}

	public boolean getInner() {
		return inner;
	}

	public void setInner(boolean inner) {
		this.inner = inner;
	}

	public static void restartJobs() throws Exception{
		System.out.println("restartJob broadcast!"); new Throwable().printStackTrace();
		ClusterUtil.broadcast(null,null,new JobCmdRestartJobs());
	}
	
	public AbstractResult doRunNow(Upload up){
		JsonResult res=new JsonResult();
		try {
			String jid=up.getStr("uuid", "");
			JobService.runNow(jid);
		} catch (Exception e) {
			res.setError(e);
		}
		return res;
	}

	public String getActive() {
		return active;
	}

	public void setActive(String active) {
		this.active = active;
	}

	@Override
	public void afterInsert() throws Exception {
		if (interval!=0) restartJobs();
		super.afterInsert();
	}
	
	@Override
	public void afterUpdate() throws Exception {
		if ((interval!=0||changed("interval"))&&ThreadHelper.get("_ignoreJobReload")==null) restartJobs();
		super.afterUpdate();
	}
	
	@Override
	public void afterDelete() throws Exception {
		if (interval!=0) restartJobs();
		super.afterDelete();
	}

	public List<JobAction> getActions() {
		return actions;
	}

	public void setActions(List<JobAction> actions) {
		this.actions = actions;
		for (JobAction ja:actions){
			try {
				Method m=ReflectUtil.getMethod(null, ja,"initialize");
				if (m!=null) m.invoke(ja);
			} catch (Exception e){
				LogUtil.dumpStackTrace(e);
				LogUtil.error(e.getMessage());
			}
		}
	}

	public String getWeekdayFrame() {
		return weekdayFrame;
	}

	public void setWeekdayFrame(String weekdayFrame) {
		this.weekdayFrame = weekdayFrame;
	}

	public String getDayFrame() {
		return dayFrame;
	}

	public void setDayFrame(String dayFrame) {
		this.dayFrame = dayFrame;
	}

	public String getMonthFrame() {
		return monthFrame;
	}

	public void setMonthFrame(String monthFrame) {
		this.monthFrame = monthFrame;
	}

	public String getUserGroup() {
		return userGroup;
	}

	public void setUserGroup(String userGroup) {
		this.userGroup = userGroup;
	}

	public String getJobGroup() {
		return jobGroup;
	}

	public void setJobGroup(String jobGroup) {
		this.jobGroup = jobGroup;
	}

	@Override
	public boolean beforeDelete() throws Exception {
		Object ui=ThreadHelper.get("ui-"+uuid);
		//System.out.println("ui="+ui+",ug="+userGroup+",owner="+owner);
		if ("_".equals(userGroup)&&ui!=null || !LightUtil.getUserId().equalsIgnoreCase(owner)) throw new Exception("Job can be deleted only by the owner! ");
		dataService.deleteBeanList(JobAction.class, "{jobUuid:'"+uuid+"'}");
		dataService.deleteBeanList(JobInstance.class, "{jobUuid:'"+uuid+"'}");
		return super.beforeDelete();
	}

	@Override
	public boolean beforeInsert() throws Exception {
		if (owner==null) owner=LightUtil.getUserId();
		if (startDate==null) startDate=LightUtil.longDate();
		nextRunTime=LightUtil.longDate(predictNextTime(LightUtil.getCalendar(timezoneId)));
		return super.beforeInsert();
	}
	
	@Override
	public boolean beforeUpdate() throws Exception {
		if (startDate==null) startDate=LightUtil.longDate();
		if (ThreadHelper.get("_ignoreJobReload")==null){
			nextRunTime=LightUtil.longDate(predictNextTime(LightUtil.getCalendar(timezoneId)));
		}
		return super.beforeUpdate();
	}
		
	public Integer getKeepDays() {
		return keepDays;
	}

	public void setKeepDays(Integer keepDays) {
		this.keepDays = keepDays;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getNotifyee() {
		return notifyee;
	}

	public void setNotifyee(String notifyee) {
		this.notifyee = notifyee;
	}
	
	public Calendar predictNextTime(Calendar c0) throws Exception{
		Calendar c=(Calendar) c0.clone();
		if (interval==0 || "no".equals(active)){
			return null;
		}
		if (startDate!=null){
			if (c.getTime().before(startDate)) c.setTime(startDate);
		}
		if (endDate!=null){
			if (c.getTime().after(endDate)){
				return null;
			}
		}
		if (interval>0){
			c.add(Calendar.MILLISECOND, interval);
			return c;
		}
		EventCalculator calc;
		
		switch(interval){
		case -1: //daily
			calc=new EventCalculator(this,true,false,false);
			break;
		case -2: //biweekly
			calc=new EventCalculator(this,false,true,false);
			break;
		default: //-3 monthly
			calc=new EventCalculator(this,false,false,true);
			break;
		}
		while(true){
			c.add(Calendar.MINUTE, 1);
			if (calc.onShot(c)) break;
		}
		if (endDate!=null){
			if (c.getTime().after(endDate)){
				return null;
			}
		}
		return c;
	}
	
	public String getNotifyEvents() {
		return notifyEvents;
	}

	public void setNotifyEvents(String notifyEvents) {
		this.notifyEvents = notifyEvents;
	}

	public boolean hasEvent(String ev){
		if (events==null) events=MapUtil.getSelectList(notifyEvents);
		return events.contains(ev);
	}

	public String getTimeFrame() {
		return timeFrame;
	}

	public void setTimeFrame(String timeFrame) {
		this.timeFrame = timeFrame;
	}

	public Timestamp getStartDate() {
		return startDate;
	}

	public void setStartDate(Timestamp startDate) {
		this.startDate = startDate;
	}

	public Timestamp getEndDate() {
		return endDate;
	}

	public void setEndDate(Timestamp endDate) {
		this.endDate = endDate;
	}

	public String getWithAttachments() {
		return withAttachments;
	}

	public void setWithAttachments(String withAttachments) {
		this.withAttachments = withAttachments;
	}

	public String getJobSource() {
		return jobSource;
	}

	public void setJobSource(String jobSource) {
		this.jobSource = jobSource;
	}

	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

	public String getNumFmt() {
		if (numFmt==null) numFmt="#,##0.00";
		return numFmt;
	}

	public void setNumFmt(String numFmt) {
		this.numFmt = numFmt;
	}

	public String getLongDtFmt() {
		if (longDtFmt==null) longDtFmt="m/d/Y H:i:s";
		return longDtFmt;
	}

	public void setLongDtFmt(String longDtFmt) {
		this.longDtFmt = longDtFmt;
	}
	
	public String getShortDtFmt() {
		return getLongDtFmt().substring(0,longDtFmt.lastIndexOf(' '));
	}

	public String getBiweekdayFrame() {
		return biweekdayFrame;
	}

	public void setBiweekdayFrame(String biweekdayFrame) {
		this.biweekdayFrame = biweekdayFrame;
	}

	public void cleanInstances(Integer days){
		if (days==null) days=0;
		//if (days<1) days=1;
		Calendar c=LightUtil.getCalendar("GMT");
		c.add(Calendar.DATE, -days);
		try {
			dataService.deleteBeanList(JobInstance.class, "{jobUuid:'"+uuid+"',runTime:{lt:'@"+LightUtil.encodeLongDate(LightUtil.longDate(c))+"'}}", "endTime desc");
		} catch (Exception e) {
			LogUtil.dumpStackTrace(e);
		}
	}

	public String getSelfClean() {
		return selfClean;
	}

	public void setSelfClean(String selfClean) {
		this.selfClean = selfClean;
	}

	public Timestamp getLastRunTime() {
		return lastRunTime;
	}

	public void setLastRunTime(Timestamp lastRunTime) {
		this.lastRunTime = lastRunTime;
	}

	public Timestamp getNextRunTime() {
		return nextRunTime;
	}

	public void setNextRunTime(Timestamp nextRunTime) {
		this.nextRunTime = nextRunTime;
	}

	public String getTimezoneId() {
		return timezoneId;
	}

	public void setTimezoneId(String timezoneId) {
		this.timezoneId = timezoneId;
	}
	
	@Override
	public String toString(){
		return "{jobName:'"+name+"'}";
	}
}
