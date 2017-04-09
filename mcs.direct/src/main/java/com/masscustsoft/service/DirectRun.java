package com.masscustsoft.service;

import java.lang.reflect.Field;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;

import com.masscustsoft.Lang.CLASS;
import com.masscustsoft.api.IDataService;
import com.masscustsoft.api.IFile;
import com.masscustsoft.api.IRepository;
import com.masscustsoft.helper.Upload;
import com.masscustsoft.model.Entity;
import com.masscustsoft.model.ExternalFile;
import com.masscustsoft.model.JsonResult;
import com.masscustsoft.util.DateUtil;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.ReflectUtil;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.ThreadHelper;

public class DirectRun{
	String userId;
	Integer mockDays=0;
	Date mockDate;
	Boolean active=true;
	
	List<String> detects=new ArrayList<String>();
	List<Entity> inserts=new ArrayList<Entity>();
	List<String> deletes=new ArrayList<String>();
	List<Map> actions=new ArrayList<Map>();

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public List<Map> getActions() {
		return actions;
	}

	public void setActions(List<Map> actions) {
		this.actions = actions;
	}

	public Integer getMockDays() {
		return mockDays;
	}

	public void setMockDays(Integer mockDays) {
		this.mockDays = mockDays;
	}

	public Date getMockDate() {
		return mockDate;
	}

	public void setMockDate(Date mockDate) {
		this.mockDate = mockDate;
	}

	public List<Entity> getInserts() {
		return inserts;
	}

	public void setInserts(List<Entity> inserts) {
		this.inserts = inserts;
	}

	public List<String> getDeletes() {
		return deletes;
	}

	public void setDeletes(List<String> deletes) {
		this.deletes = deletes;
	}

	public void run(DirectConfig cfg, IDataService ds, IRepository fs) throws Exception {
		if (!active) return;
		DirectRun a=this;
		
		if (detects.size()>0){
			for (String st:detects){
				Class<Entity> cls = CLASS.forName(cfg.getBeanFactory().findRealClass(st));
				List<Entity> lst = ds.getBeanList(cls, "{}", "");
				if (lst.size()>0) return;
			}
		}
		int days=a.getMockDays();
		if (a.mockDate!=null){
			Calendar c=LightUtil.getShortCalendar();
			c.setTime(a.mockDate);
			days=-DateUtil.daysFrom(a.mockDate);
		}
		ThreadHelper.set("$MockBackDays$",days);
		System.out.println("==================mockDate="+LightUtil.encodeShortDate(LightUtil.shortDate()));
		
		for (String del:deletes){
			Class enCls=CLASS.forName(cfg.getBeanFactory().findRealClass(del));
			ds.deleteBeanList(enCls, "{}");
		}
		
		for (Entity en:inserts){
			extractExternalFiles(ds,fs,en);
			ds.insertBean(en);
		}
		
		for (Map m:a.getActions()){
			
			Upload up=new Upload(null,null);
			DirectSession ses=cfg.getSessionService().newSession(cfg, "auto", "auto", "auto");
			ses.setUserId(a.getUserId());
			up.setStr("sessionId",ses.getSessionId());
			
			up.getFieldMap().putAll(m);
			up.parseContent();
			
			System.out.println("Action Request: "+LightUtil.toJsonObject(m));
			JsonResult ret = new JsonResult();
			try {
				String cmd = up.getStr("action", "");
				ses.getDirectConfig().doProcess(cmd, ret);
			} catch (Exception e) {
				ret.setError(e);
				e.printStackTrace();
			}
			System.out.println("Action Response: "+LightUtil.toJsonObject(ret));
		}
		ThreadHelper.set("$MockBackDays$",null);
	}
	
	private void extractExternalFiles(IDataService ds, IRepository fs,Entity en) throws Exception {
		en.setDataService(ds);
		for (Field fld:ReflectUtil.getFieldMap(en.getClass())){
			Object val=ReflectUtil.getProperty(en, fld.getName());
			if (val==null) continue;
			if (val instanceof IFile){
				IFile f=(IFile)val;
				String id=f.getExternalId();
				if (id!=null && id.startsWith("data:")){
					byte[] data = Base64.decodeBase64(id.substring(id.indexOf(',')+1));
					ExternalFile ef=ExternalFile.newExternalFile(ds, fs, f, data, f.getName());
					f.setExternalId(ef.getUuid());
				}
			}
		}
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	public List<String> getDetects() {
		return detects;
	}

	public void setDetects(List<String> detects) {
		this.detects = detects;
	}
}
