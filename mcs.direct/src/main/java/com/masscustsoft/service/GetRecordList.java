package com.masscustsoft.service;

import java.lang.reflect.Field;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.masscustsoft.api.DateIndex;
import com.masscustsoft.api.FullText;
import com.masscustsoft.api.IndexKey;
import com.masscustsoft.api.NumIndex;
import com.masscustsoft.api.TimestampIndex;
import com.masscustsoft.model.AbstractResult;
import com.masscustsoft.model.DateRange;
import com.masscustsoft.model.Entity;
import com.masscustsoft.model.JsonResult;
import com.masscustsoft.service.PageInfo;
import com.masscustsoft.util.MapUtil;
import com.masscustsoft.util.ReflectUtil;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.LightStr;
import com.masscustsoft.util.ThreadHelper;

public class GetRecordList extends GetRecord {

	Map<String,Object> extraFields=new HashMap<String,Object>();

	List<Map> defaultActions=new ArrayList<Map>();
	
	List<Map> defaultRecords=new ArrayList<Map>();
	
	@Override
	protected void doFilterOut(Entity en, Map rec) throws Exception {
		super.doFilterOut(en, rec);
		processExtraFields(en,rec);
	}
	
	protected void processExtraFields(Entity en, Map rec) throws Exception {
		for (String key:extraFields.keySet()){
			Object val=extraFields.get(key);
			String model=null,tarField=null,keyFields="",action=null;
			Map<String,String> mapping=new HashMap<String,String>();
			if (val instanceof String){
				String ss=(String)val;
				int i=ss.indexOf("@");
				if (i>0){
					keyFields=ss.substring(i+1);
					ss=ss.substring(0,i);
				}
				i=ss.indexOf(".");
				if (i>0){
					model=ss.substring(0,i);
					tarField=ss.substring(i+1);
				}
				else tarField=ss;
			}
			else
			if (val instanceof Map){
				Map m=(Map)val;
				model=(String)m.get("model");
				action=(String)m.get("action");
				tarField=(String)m.get("field");
				keyFields=(String)m.get("keyFields");
				mapping.putAll(m);
			}
			if (model!=null){
				Class c=this.getModelClass(model);
				List<String> pkeys = this.getPrimaryKeys(c,keyFields);
				Map<String,Object> filter=new HashMap<String,Object>();
				boolean empty=false;
				for (String k:pkeys){
					String k2=mapping.get(k);
					if (k2==null) k2=k;
					Object v= rec.get(k2);
					filter.put(k,v);
					if (v==null || LightStr.isEmpty(filter.get(k).toString())) empty=true;
				}
				if (!empty){
					String filt=LightUtil.toJsonString(filter).toString();
					Object cacheVal=ThreadHelper.get(model+"-"+filt);
					if (cacheVal==null){
						List lst = getDs().getBeanList(c, filt, "", 0,1,"");
						if (lst.size()>0){
							cacheVal=lst.get(0);
							ThreadHelper.set(model+"-"+filt,cacheVal);
						}
					}
					if (cacheVal!=null){
						Object v;
						if (tarField.indexOf("${")>=0){
							v=LightUtil.macro(tarField, '$', (Map)LightUtil.toJsonObject(cacheVal));
						}
						else
							v=ReflectUtil.getProperty(cacheVal, tarField);
						rec.put(key, v);
					}	
				}
			}
			else
			if (action!=null){
				RecordAction da = (RecordAction)this.getCfg().getActionMap().get(action);
				JsonResult tmp=new JsonResult();
				Map filter=da.prepare(tmp);
				List<String> pkeys = MapUtil.getSelectList(keyFields);
				boolean empty=false;
				for (String k:pkeys){
					String k2=mapping.get(k);
					if (k2==null) k2=k;
					Object v= rec.get(k2);
					filter.put(k,v);
					if (v==null || LightStr.isEmpty(filter.get(k).toString())) empty=true;
				}
				if (!empty){
					String filt=LightUtil.toJsonString(filter).toString();
					Object cacheVal=ThreadHelper.get(action+"/"+filt);
					if (cacheVal==null){
						da.run(tmp,filter);
						if (tmp.getResult() instanceof List){
							List lst=(List)tmp.getResult();
							if (lst.size()>0){
								cacheVal=lst.get(0);
								ThreadHelper.set(action+"/"+filt,cacheVal);
							}
						}	
					}
					if (cacheVal!=null){
						if (da instanceof GetItems)
							rec.put(key, ReflectUtil.getProperty(cacheVal, "name"));
						else{
							Object v;
							if (tarField.indexOf("${")>=0){
								v=LightUtil.macro(tarField, '$', (Map)LightUtil.toJsonObject(cacheVal));
							}
							else
								v=ReflectUtil.getProperty(cacheVal, tarField);
							rec.put(key, v);
						}
					}
				}
			}
			else{
				if (LightStr.isEmpty(tarField)) tarField=key;
				rec.put(key, ReflectUtil.getProperty(en, tarField));
			}
		}
	}
	
	
	@Override
	public Map prepare(AbstractResult ret) throws Exception {
		Class<? extends Entity> cls = getModelClass();
		Map filter = getFilterMap();
		
		//automatically add extra fields for search
		List<Field> flds = ReflectUtil.getFieldMap(cls);
		for (Field f:flds){
			if (filter.containsKey(f.getName())) continue;
			if (f.getName().equals("text")) continue;
			String v=getStr(f.getName(),"");
			if (LightStr.isEmpty(v)) continue;
			System.out.println("prepare f="+f.getName()+",v="+v);
			if (v.startsWith("dateRange-")){
				v=v.substring(10);
				int seqId=LightUtil.decodeInt(v);
				if (f.getType().isAssignableFrom(Date.class)){
					//it's a DateRange
					DateRange r=getDs().getBean(DateRange.class, "ownerId", getSession().getUserId(), "sequenceId", seqId);
					System.out.println("prepare2 r="+r);
					if (r==null) continue;
					Date from=r.getFrom();
					Date to=r.getTo(from);
					System.out.println("prepare3 from="+from+", to="+to);
					filter.put(f.getName(), LightUtil.parseJson("{le:'"+LightUtil.encodeShortDate(to)+"',ge:'"+LightUtil.encodeShortDate(from)+"'}"));
				}
				else
				if (f.getType().isAssignableFrom(Timestamp.class)){
					//it's a DateRange
					DateRange r=getDs().getBean(DateRange.class, "ownerId", getSession().getUserId(), "sequenceId", seqId);
					if (r==null) continue;
					Date from=r.getFrom();
					Date to=r.getTo(from);
					Calendar c0=LightUtil.getShortCalendar();
					Calendar c=LightUtil.getCalendar();
					c.set(Calendar.HOUR_OF_DAY, 0);
					c.set(Calendar.MINUTE, 0);
					c.set(Calendar.SECOND, 0);
					c.set(Calendar.MILLISECOND, 0);
					c0.setTime(from);
					c.set(Calendar.YEAR,c0.get(Calendar.YEAR));
					c.set(Calendar.MONTH,c0.get(Calendar.MONTH));
					c.set(Calendar.DAY_OF_MONTH,c0.get(Calendar.DAY_OF_MONTH));
					Timestamp lt=LightUtil.longDate(c);
					//vals.put(drf.getDateFromFieldName(),LightUtil.encodeLongDate(LightUtil.longDate(c)));

					c0.setTime(to);
					c.set(Calendar.YEAR,c0.get(Calendar.YEAR));
					c.set(Calendar.MONTH,c0.get(Calendar.MONTH));
					c.set(Calendar.DAY_OF_MONTH,c0.get(Calendar.DAY_OF_MONTH));
					c.add(Calendar.DATE, 1);
					Timestamp ge=LightUtil.longDate(c);
					filter.put(f.getName(), LightUtil.parseJson("{lt:'"+LightUtil.encodeLongDate(lt)+"',ge:'"+LightUtil.encodeLongDate(ge)+"'}"));
				}
				continue;
			}
			if (f.isAnnotationPresent(IndexKey.class) || f.isAnnotationPresent(FullText.class) || f.isAnnotationPresent(DateIndex.class) || f.isAnnotationPresent(TimestampIndex.class) || f.isAnnotationPresent(NumIndex.class)){
				filter.put(f.getName(),v);
			}
		}
		
		doFilterIn(filter);
		
		System.out.println("FILTER="+filter);
		return filter;
	}
	
	@Override
	public void run(AbstractResult ret, Map filter) throws Exception {
		Class<? extends Entity> cls = getModelClass();
		
		String _sort=getStr("sort",sort);
		PageInfo<Entity> pg = getDs().getBeanList(new Class[] { cls }, LightUtil.toJsonString(filter).toString(),
				getStr("text", ""), getInt("start"), getInt("limit", 10), _sort, "", false);
		if (pg.getList().size()==0 && defaultActions.size()>0){
			runActions(defaultActions);
			pg = getDs().getBeanList(new Class[] { cls }, LightUtil.toJsonString(filter).toString(),
					getStr("text", ""), getInt("start"), getInt("limit", 10), _sort, "", false);
		}
		List<Map> result = new ArrayList<Map>();
		if (pg.getStart()==0){
			result.addAll(defaultRecords);
		}
		for (Entity c : pg.getList()) {
			Map m = (Map) LightUtil.toJsonObject(c);
			doFilterOut(c, m);
			if (hook!=null) cfg.getHooks(this, hook, m);
			result.add(m);
		}
		 
		ret.setAmount(pg.getAmount());
		ret.setResult(result);
		ret.setAttribute("itemCaches", cfg.getItemCaches());
	}

	public Map<String, Object> getExtraFields() {
		return extraFields;
	}

	public void setExtraFields(Map<String, Object> extraFields) {
		this.extraFields = extraFields;
	}

	public List<Map> getDefaultActions() {
		return defaultActions;
	}

	public void setDefaultActions(List<Map> defaultActions) {
		this.defaultActions = defaultActions;
	}

	public List<Map> getDefaultRecords() {
		return defaultRecords;
	}

	public void setDefaultRecords(List<Map> defaultRecords) {
		this.defaultRecords = defaultRecords;
	}

}
