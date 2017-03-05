package com.masscustsoft.service;

import java.io.InputStream;
import java.net.URL;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.annotation.WebServlet;

import com.masscustsoft.api.IBeanFactory;
import com.masscustsoft.api.ICleanup;
import com.masscustsoft.api.IClusterService;
import com.masscustsoft.api.IDataService;
import com.masscustsoft.api.IRepository;
import com.masscustsoft.model.Job;
import com.masscustsoft.model.JobAction;
import com.masscustsoft.model.JobInstance;
import com.masscustsoft.model.JobInstanceAction;
import com.masscustsoft.util.ClusterUtil;
import com.masscustsoft.util.GlbHelper;
import com.masscustsoft.util.LightStr;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.LogUtil;
import com.masscustsoft.util.MapUtil;
import com.masscustsoft.util.StreamUtil;
import com.masscustsoft.util.ThreadHelper;
import com.masscustsoft.xml.BeanFactory;

public class JobService implements ICleanup{
	String enable="auto";
	int maxThreads=25;
	String staleHours="0";
	
	List<JobGroup> groups=new ArrayList<JobGroup>();
	String notifySubject="",notifyBody,notifyBodyFile;
	
	transient List<JobGroup> _groups=new ArrayList<JobGroup>();
	transient int waitings=0,running=0;
	transient List<JobThread> threads=new ArrayList<JobThread>(); //seperate executation
	transient private Map<String,JobThread> map=new HashMap<String,JobThread>();
	transient JobMonitor monitor;
	transient JobGroup defaultGroup=null;
	transient boolean isCoordinator=false,inService=false;
	transient AbstractConfig cfg;
	
	public class JobMonitor implements Runnable{
		AbstractConfig entry;
		
		public void execute(){
			int stat=isEnabled();
			if (stat==0) return;
			
			if (stat==1 && map.size()==0) return;
				
			boolean hasActive=false;
			for (JobThread jt:map.values()){
				if (!jt.needReload || !jt.idle) {hasActive=true; break;}
			}

			if (!hasActive||stat==2){
				try{
					try{
						if (stat!=2) _stopJobs();
					}
					catch(Exception e){
						LogUtil.dumpStackTrace(e);
					}
				}
				catch(RuntimeException ex){
					LogUtil.dumpStackTrace(ex);
				}
				
				try{
					try{
						_startJobs(entry);
					}
					catch(Exception e){
						LogUtil.dumpStackTrace(e);
					}
				}
				catch(RuntimeException ex){
					LogUtil.dumpStackTrace(ex);
				}
			}
		}
		
		@Override
		public void run() {
			while (true){
				try {
					execute();
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					break;
				}
			}
			
		}

		public AbstractConfig getEntry() {
			return entry;
		}

		public void setEntry(AbstractConfig entry) {
			this.entry = entry;
		}
	}
	
	private void mergeJobs(IDataService data, IRepository fs, List<Job> list,boolean inner) throws Exception{
		if (list==null) return;
		Calendar c=LightUtil.getCalendar();
		int hrs=LightUtil.decodeInt(LightUtil.macro(staleHours)+"");
		c.add(Calendar.HOUR, -hrs);
		System.out.println("StaleHours="+hrs);
		
		for (Job a:list){
			if (a.getInterval()<-10){
				a.setInterval(LightUtil.decodeInt((String)GlbHelper.get("jobInterval"+a.getInterval())));
			}
			if (a.getInterval()==0) continue;
			String tid=a.getThreadId();
			if (tid==null) tid="";
			a.setInner(inner);
			JobThread th=map.get(tid+"@"+data.getDsId()+"@"+fs.getFsId());
			if (th==null) {
				th=new JobThread();
				th.setThreadId(tid);
				th.setDataService(data);
				th.setFileService(fs);
				map.put(tid+"@"+data.getDsId()+"@"+fs.getFsId(), th);
			}
			Timestamp nr=a.getNextRunTime();
			if (hrs>0){
				if (!inner && (nr==null || nr.before(LightUtil.longDate(c)))){
					ThreadHelper.set("_ignoreJobReload","1");
					a.setNextRunTime(LightUtil.longDate(a.predictNextTime(LightUtil.getCalendar())));
					data.updateBean(a);
					ThreadHelper.set("_ignoreJobReload",null);
				}
			}
			th.getList().add(a);
		}
	}
	
	public void _startJobs(AbstractConfig cfg) throws Exception{
		System.out.println("start jobs ");
		inService=true;
		this.cfg=cfg;
		initJobUser("job@start");
		for (JobGroup g:_groups){
			if (defaultGroup==null) defaultGroup=g;
			IDataService db=g.getDataService();
			mergeJobs(db,g.getFileService(),g.getJobs(),true);
			
			//if jobFilter is null, no job loaded; 
			if (LightStr.isEmpty(g.getGroupFilter())) continue;
			
			//if jobGroup empty, load all, or filter by groupId
			String groupFilter=g.getGroupFilter();
			
			List<Job> jobs=db.getBeanList(Job.class, groupFilter, "");
			for (Job job:jobs){
				job.setActions(db.getBeanList(JobAction.class, "{jobUuid:'"+job.getUuid()+"'}", "", 0, 1000, "seqId"));
			}
			mergeJobs(db,g.getFileService(),jobs,false);
			
			List<JobInstance> stopped=db.getBeanList(JobInstance.class, "{status:'running'}", "");
			for (JobInstance inst:stopped){
				inst.setStatus("stopped");
				db.updateBean(inst);
			}
		}
		for (JobThread jt:map.values()){
			jt.setJobService(this);
			GlbHelper.installJob(this,jt);
		}
		addWaitingJob();
		LogUtil.info("JobService started!");
	}
	
	public void initGroups() throws Exception{
		_groups=new ArrayList<JobGroup>();
		Map<String,JobGroup> map=new HashMap<String,JobGroup>();
		
		BeanFactory bf=BeanFactory.getBeanFactory();
		
		for (JobGroup g:groups){
			if (g.getGroupId()!=null){
				String postfix="-"+g.getGroupId();
				List<String> sup=MapUtil.getSelectList(LightUtil.getCfg().getSupportedModules());
				
				ClassLoader cl = WebServlet.class.getClassLoader();
				
				Enumeration<URL> all = cl.getResources("META-INF/JOBGROUP"+postfix+".xml");
				for (;all.hasMoreElements();){
					URL u=all.nextElement();
					int idx=LightUtil.isSupportedModule(sup,u.getPath());
					if (idx==0) continue;
					InputStream is = u.openStream();
					StringBuffer buf=new StringBuffer(); 
					StreamUtil.loadStream(is, buf, LightUtil.UTF8);
					List<JobGroup> lst=(List)bf.loadBean(buf.toString());
					//merge menu into existing one if text same, since it suppose to be top menu
					for (JobGroup l:lst){
						if (!LightStr.isEmpty(l.getGroupId())){
							if (map.get(l.getGroupId())!=null) continue; //ignore same group with same groupId
							map.put(l.getGroupId(),l); //mark it has
						}
						_groups.add(l);
					}
				}
			}
			else{
				//duplicate group detect only for automatic ~ group
				_groups.add(g);	
			}
		}
		System.out.println("JobGroups="+_groups);
	}
	
	public void startJobs(AbstractConfig entry) throws Exception{
		monitor=new JobMonitor();
		monitor.setEntry(entry);
		initGroups();
		if (isEnabled()>0) _startJobs(entry);
		GlbHelper.installJob(this,monitor);
	}

	public void _stopJobs() throws Exception{
		if (!inService) return;
		
		inService=false;
		
		LogUtil.debug("Job Service is stopping...");
		for (JobThread jt:map.values()){
			for (Job a:jt.getList()){
				LogUtil.debug("JobAgent shutdown ..."+a);
				a.shutdown();
			}
			GlbHelper.uninstallJob(this,jt);
			jt.shutdown();
		}
		LogUtil.debug("Job Service is stopped.");
		map.clear();
	}

	public void stopJobs() throws Exception{
		_stopJobs();
		GlbHelper.uninstallJob(this,monitor);
		_groups=null;
	}
	
	public void cleanup() {
		try {
			stopJobs();
		} catch (Exception e) {
		}	
	}

	public void stopJob(String jobUuid){
		if (jobUuid==null) return;
		for (JobThread jt:map.values()){
			if (jt.getIdle()) continue;
			boolean has=jt.hasJob(jobUuid);
			if (has){
				try{
					GlbHelper.uninstallJob(this,jt);
				}
				catch (Exception e){
					LogUtil.dumpStackTrace(e);
				}
				GlbHelper.installJob(this,jt);	
			}
		}
	}
	
	//create a job and a instance
	public JobInstance launchJob(IDataService dataService,Job agent, String invoker, String status, String callback) throws Exception{
		//LogUtil.info("launchJob job="+agent);
		agent.setDataService(dataService);
		JobInstance inst=agent.getJobInstance();
		if (status.equals("running")) inst.setRunTime(LightUtil.longDate());
		inst.setStatus(status); //"running" or "waiting"
		inst.setCallback(callback);
		if (cfg==null) cfg=LightUtil.getCfg();
		IClusterService svc = cfg.getClusterService();
		String cluster=svc==null?"":svc.myNodeId();
		inst.setThreadId(cluster+":"+agent.getThreadId()+":"+Thread.currentThread().getId());
		inst.setDataService(dataService);
		if (invoker!=null) inst.setOwner(invoker);
		dataService.insertBean(inst);
		List<JobAction> actions = agent.getActions();
		if (actions.size()==0) actions=dataService.getBeanList(JobAction.class, "{jobUuid:'"+agent.getUuid()+"'}", "", 0, 1000, "seqId");
		for (JobAction a:actions){
			JobInstanceAction act=new JobInstanceAction();
			act.setActionStatus("waiting");
			act.setSeqId(a.getSeqId());
			act.setErrors(0);
			act.setInfos(0);
			act.setJobInstanceUuid(inst.getUuid());
			act.setIfException(a.getIfException());
			act.setJobActionUuid(a.getUuid());
			act.setAction(a.getAction());
			a.setJobInstanceAction(act);
			//System.out.println("new job inst act inst="+inst.getUuid()+" a="+a.getUuid()+", act="+act.getUuid());
			dataService.insertBean(act);
		}
		return inst;
	}
	
	/*public void RunJob(String jobUuid, String who) throws Exception{
		if (jobUuid==null) return;
		for (JobThread jt:map.values()){
			boolean has=false;
			for (Job job:jt.getList()){
				if (jobUuid.equals(job.getUuid())){
					has=true; break;
				}
			}
			if (!has) continue;
			jt.getThread().interrupt();
			try{
				jt.getThread().join(30000);
			}
			catch (Exception e){
				jt.getThread().destroy();
				LogUtil.dumpStackTrace(e);
			}
			Thread th = new Thread(jt);
			jt.setForcedJobUuid(jobUuid);
			jt.setInvoker(who);
			jt.setThread(th);
			th.start();
			break;
		}
	}*/
	
	public List<JobGroup> getGroups() {
		return groups;
	}

	public void setGroups(List<JobGroup> groups) {
		this.groups = groups;
	}

	public String getEnable() {
		return enable;
	}

	public void setEnable(String enable) {
		this.enable = enable;
	}
	
	public Collection<JobThread> getThreadList(){
		return map.values();
	}
	
	protected int isEnabled(){
		if (isCoordinator) return 1;
		
		String _enable=LightUtil.macroStr(enable);
		
		if ("true".equals(_enable)) return 1;
		if ("false".equals(_enable)) return 0;
		List<String> nodeList=(List)GlbHelper.get("_node_list_");
		String currentNode=(String)GlbHelper.get("_current_node_");
		if (currentNode==null || nodeList==null) return 1; //if no cluster
		if (nodeList.indexOf(currentNode)==0){
			//I am coordinator
			if (isCoordinator) return 1;
			isCoordinator=true;
			return 2;
		}
		return 0;
	}

	public static JobInstance runNow(String jobId) throws Exception{
		return runNow(jobId,LightUtil.getUserId(),"");
	}
	
	public static JobInstance runNow(String jobId, String invoker, String callback) throws Exception{
		if (LightStr.isEmpty(jobId)) return null;
		IDataService db = LightUtil.getDataService();
		List<JobInstance> list=db.getBeanList(JobInstance.class, "{jobUuid:'"+jobId+"',status:'running||waiting'}","");
		if (list.size()>0) throw new Exception("The job has already launched!");
		JobService svc = LightUtil.getCfg().getJobService();
		if (svc==null) throw new Exception("not JobService configured");
		Job job=db.getBean(Job.class, "uuid", jobId);
		if (job==null) throw new Exception("unknown jobId:"+jobId);
		JobInstance inst=svc.launchJob(db, job, invoker, "waiting", callback);
		JobCmdRunNow cmd = new JobCmdRunNow();
		ClusterUtil.broadcast(null,null,cmd);
		return inst;
	}

	private void runWaitingJobs(IDataService dataService,IRepository fs) throws Exception{
		List<JobInstance> insts=dataService.getBeanList(JobInstance.class, "{status:'waiting'}", "");
		System.out.println("runWaitingJobs="+insts.size());
		waitings=insts.size();
		running=1;
		System.out.println("runWaitingJobs="+insts+" db="+dataService.getDsId());
		for (JobInstance inst:insts){
			if (threads.size()>=maxThreads) break;
			System.out.println("MAX="+maxThreads);
			Job job=dataService.getBean(Job.class, "uuid",inst.getJobUuid());
			System.out.println("job="+LightUtil.toJsonString(job));
			job.setActions(dataService.getBeanList(JobAction.class, "{jobUuid:'"+job.getUuid()+"'}", "", 0, 1000, "seqId"));
			//System.out.println("actions="+LightUtil.toJsonString(job.getActions()));
			for (JobAction a:job.getActions()){
				JobInstanceAction act = dataService.getBean(JobInstanceAction.class, "jobInstanceUuid", inst.getUuid(),"jobActionUuid",a.getUuid());
				//System.out.println("act="+act+" inst="+inst.getUuid()+", jobact="+a.getUuid());
				a.setJobInstanceAction(act);
			}
			inst.setJob(job);
			
			JobThread jt = new JobThread();
			jt.setThreadId(job.getUuid());
			jt.setDataService(dataService);
			jt.setFileService(fs);
			threads.add(jt);
			
			inst.setStatus("running");
			dataService.updateBean(inst);	
			
			jt.setJobService(this);
			jt.setInstance(inst);
			GlbHelper.installJob(this,jt);
			//System.out.println("JOB LAUNCHED");
			waitings--;
		}
		running=0;
	}
	
	public void addWaitingJob() {
		if (defaultGroup!=null){
			LogUtil.info("EXECUTE WAITING JOB THREADS="+threads.size());
			if (running>0){
				waitings=100;
				return;
			}
			try {
				runWaitingJobs(defaultGroup.getDataService(),defaultGroup.getFileService());
			} catch (Exception e) {
				LogUtil.dumpStackTrace(e);
			}
		}
	}

	public void restartJobs() {
		for (JobThread jt:map.values()){
			jt.needReload=true;
		}
	}

	public String getNotifySubject() {
		return notifySubject;
	}

	public void setNotifySubject(String notifySubject) {
		this.notifySubject = notifySubject;
	}

	public String getNotifyBody() throws Exception{
		if (notifyBody==null){
			if (notifyBodyFile!=null){
				IBeanFactory bf=LightUtil.getBeanFactory();
				IRepository rep = bf.getRepository("server", notifyBodyFile);
				StringBuffer buf=new StringBuffer();
				StreamUtil.loadStream(rep.getResource(bf.getPureBeanName(notifyBodyFile)),buf,LightUtil.UTF8);
				notifyBody=buf.toString();
			}
		}
		return notifyBody;
	}

	public void setNotifyBody(String notifyBody) {
		this.notifyBody = notifyBody;
	}

	public String getNotifyBodyFile() {
		return notifyBodyFile;
	}

	public void setNotifyBodyFile(String notifyBodyFile) {
		this.notifyBodyFile = notifyBodyFile;
	}

	public int getMaxThreads() {
		return maxThreads;
	}

	public void setMaxThreads(int maxThreads) {
		this.maxThreads = maxThreads;
	}

	public void endThread(JobThread jobThread,IDataService ds, IRepository fs) throws Exception {
		//System.out.println("RUNONCE END"+jobThread);
		threads.remove(jobThread);
		//System.out.println("RUNONCE WAITINGS="+waitings);
		if (waitings>0) this.runWaitingJobs(ds, fs);
	}

	//this is used by google appengine if thread not allowed
	public void runJobs(){
		monitor.execute();
		
		for (JobThread jt:map.values()){
			jt.execute();
		}
	}

	public String getStaleHours() {
		return staleHours;
	}

	public void setStaleHours(String staleHours) {
		this.staleHours = staleHours;
	}

	public void initJobUser(final String jobUser) {
		if (getCfg()!=null) {
			getCfg().initThread();
		}
		ThreadHelper.set("userId","jobUser");
	}

	public AbstractConfig getCfg() {
		return cfg;
	}

}
