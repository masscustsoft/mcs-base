package com.masscustsoft.service;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.fileupload.FileItem;

import com.masscustsoft.api.IDataService;
import com.masscustsoft.api.IFile;
import com.masscustsoft.api.IRepository;
import com.masscustsoft.helper.Upload;
import com.masscustsoft.model.AbstractResult;
import com.masscustsoft.model.DirectDefault;
import com.masscustsoft.model.DirectRole;
import com.masscustsoft.model.DirectUser;
import com.masscustsoft.model.DirectUserRole;
import com.masscustsoft.model.Entity;
import com.masscustsoft.model.ExternalFile;
import com.masscustsoft.util.LightStr;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.MapUtil;
import com.masscustsoft.util.ReflectUtil;
import com.masscustsoft.util.StreamUtil;
import com.masscustsoft.util.ThreadHelper;

public class DirectComponent {
	String dsId, fsId;

	boolean trap = true;

	protected transient DirectConfig cfg;
	private transient IDataService ds;
	private transient IRepository fs;

	public void init(DirectConfig cfg) throws Exception{
		this.cfg = cfg;
		ThreadHelper.set("$Ds$", getDs());
		ThreadHelper.set("$Fs$", getFs());
	}

	public void call(String act, AbstractResult ret) throws Exception {
		DirectAction a = cfg.getActionMap().get(act);
		if (a != null) {
			a._run(ret);
		}
	}
	
	public DirectSession getSession() throws Exception {
		return (DirectSession) ThreadHelper.get("$$session");
	}

	public String getStr(String field, String def) throws Exception {
		return Upload.getUpload().getStr(field, def);
	}

	public int getInt(String field) throws Exception {
		return getInt(field, 0);
	}

	public int getInt(String field, int def) throws Exception {
		return Upload.getUpload().getInt(field, def);
	}

	public long getLong(String field) throws Exception {
		return getLong(field, 0);
	}

	public long getLong(String field, long def) throws Exception {
		return Upload.getUpload().getLong(field, def);
	}

	public double getNumber(String field) throws Exception {
		return getNumber(field, 0d);
	}

	public Double getNumber(String field, Double def) throws Exception {
		return Upload.getUpload().getNumber(field, def);
	}

	public Date getDate(String field) throws Exception {
		return Upload.getUpload().getDate(field, null);
	}

	public String requiredStr(String field) throws Exception {
		String val = Upload.getUpload().getStr(field, "");
		if (LightStr.isEmpty(val))
			throw new Exception("MissingParameter: " + field);
		return val;
	}

	public String getImageUrl(IFile logo) {
		if (logo != null && logo.getExternalId() != null) {
			return "attach:"+logo.getExternalId() + "." + StreamUtil.fileExt(logo.getName());
		}
		return "";
	}

	public void updateAttachment(Entity en, String imgFld) throws Exception {
		FileItem ff = Upload.getUpload().getFileItem(imgFld);
		if (ff == null)
			return;
		Object f = ReflectUtil.getProperty(en, imgFld);
		if (f == null || !(f instanceof IFile))
			return;
		ExternalFile.newExternalFile(getDs(), getFs(), (IFile) f, ff);
	}

	protected void setFormDefaults() throws Exception {
		DirectSession ses = getSession();
		if (ses == null)
			return;
		String objectId = getStr("objectId", "");
		String fields = getStr("defaultFields", "");
		if (LightStr.isEmpty(objectId) || LightStr.isEmpty(fields))
			return;
		DirectDefault def = getDs().getBean(DirectDefault.class, "userId", ses.getUserId(), "objectId", objectId,
				"fieldId", "defaults");
		Map<String,String> map=new HashMap();
		if (def!=null) map=(Map)LightUtil.parseJson(def.getValue());
		
		List<String> flds = MapUtil.getSelectList(fields);
		for (String fld : flds) {
			String v = getStr(fld, "");
			map.put(fld, v);
		}
		if (def==null){
			def=new DirectDefault();
			def.setUserId(ses.getUserId());
			def.setObjectId(objectId);
			def.setFieldId("defaults");
			def.setValue(LightUtil.toJsonString(map).toString());
			getDs().insertBean(def);
		}
		else{
			def.setValue(LightUtil.toJsonString(map).toString());
			getDs().updateBean(def);
		}
	}

	public Map getDefaults(String objectId, String fieldId) throws Exception{
		Map map =null;
		if (!LightStr.isEmpty(objectId)){
			DirectDefault def = getDs().getBean(DirectDefault.class, "userId", getUserId(), "objectId", objectId, "fieldId", fieldId);
			if (def != null)
				map = (Map) LightUtil.parseJson(def.getValue());
		}
		if (map==null) map= new HashMap();
		return map;
	}
	
	public IDataService getDs() {
		if (ds == null)
			ds = cfg.getDs(dsId);
		return ds;
	}

	public IRepository getFs() {
		if (fs == null)
			fs = cfg.getFs(fsId);
		return fs;
	}

	public String getDsId() {
		return dsId;
	}

	public void setDsId(String dsId) {
		this.dsId = dsId;
	}

	public String getFsId() {
		return fsId;
	}

	public void setFsId(String fsId) {
		this.fsId = fsId;
	}

	public DirectConfig getCfg() {
		return cfg;
	}

	public boolean isTrap() {
		return trap;
	}

	public void setTrap(boolean trap) {
		this.trap = trap;
	}

	public String getUserId() throws Exception {
		return getSession().getUserId();
	}
	
	public String getVar(String var) throws Exception {
		return getSession().getVar(var);
	}
	
	//willmovetoaction
	protected void initSession() throws Exception{
		cfg.detectPreference();
		String sessionId = getStr("sessionId", "");
		long ts=0;
		if (LightStr.isTrue(cfg.getSecureSessionId())){
			String[] ss=LightStr.parseSalted(sessionId);
			sessionId=ss[0];
			ts=LightStr.parseTimestamp(ss[1]);
		}
		
		DirectSession ses = cfg.getSessionService().getSession(sessionId);
		if (ses == null) {
			 if (trap) throw new Exception("#[SessionExpired]");
			 return;
		}
		if (trap){
			Upload.getUpload().getFieldMap().putAll(ses.getVars());
			Upload.getUpload().getFieldMap().put("userId",ses.getUserId());
		}
		
		long now=System.currentTimeMillis()-ses.getDelta();
		long dt=now-ts,ofs=1000*cfg.getSessionService().getTimeoutSeconds();
		if (LightStr.isTrue(cfg.getSecureSessionId()) && dt>ofs){
			cfg.getSessionService().expireSession(ses.getSessionId());
			throw new Exception("#[SessionExpired]");
		}
		ThreadHelper.set("userId", ses.getUserId());
		ThreadHelper.set("$$session", ses);	
	}
	
	public String getLocale(){
		return (String)ThreadHelper.get("$$locale");
	}
	
	public String getLang(){
		return (String)ThreadHelper.get("$$lang");
	}
	
	public String getNumFmt(){
		return (String)ThreadHelper.get("$$numFmt");
	}
	
	public String getDateFmt(){
		return (String)ThreadHelper.get("$$dateFmt");
	}
	
	public String getTimeFmt(){
		return (String)ThreadHelper.get("$$timeFmt");
	}
	
	protected List<Map> getRoleList(String userId) throws Exception{
		List<Map> ret=new ArrayList<Map>();
		
		List<DirectUserRole> lst = getDs().getBeanList(DirectUserRole.class, "{userId:'"+userId+"',active:'yes'}", "");
		List<DirectUserRole> plst = getDs().getBeanList(DirectUserRole.class, "{proxyId:'"+userId+"',active:'yes'}", "");
		lst.addAll(plst);
		for (DirectUserRole ur:lst){
			DirectUser u=getDs().getBean(DirectUser.class,"userId", ur.getUserId(),"active","yes");
			if (u==null) continue;
			DirectRole r=getDs().getBean(DirectRole.class,"roleId",ur.getRoleId(),"active","yes");
			if (r==null) continue;
			Map m=(Map)LightUtil.toJsonObject(u);
			m.put("roleId", ur.getRoleId());
			List acc=MapUtil.getSelectList(r.getAccessId());
			for (String s:MapUtil.getSelectList(ur.getExtraAccesses())){
				if (!acc.contains(s)) acc.add(s);
			}
			m.put("accesses", acc);
			m.remove("password");
			m.remove("uuid");
			m.remove("active");
			ret.add(m);
		}
		return ret;
		
	}
}
