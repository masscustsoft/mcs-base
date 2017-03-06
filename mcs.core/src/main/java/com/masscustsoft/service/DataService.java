package com.masscustsoft.service;

import java.lang.reflect.Field;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.masscustsoft.Lang.CLASS;
import com.masscustsoft.api.CategoryKey;
import com.masscustsoft.api.DateIndex;
import com.masscustsoft.api.FullBody;
import com.masscustsoft.api.FullText;
import com.masscustsoft.api.IDataEnumeration;
import com.masscustsoft.api.IDataService;
import com.masscustsoft.api.IEntity;
import com.masscustsoft.api.IJoin;
import com.masscustsoft.api.IRefiner;
import com.masscustsoft.api.Increasable;
import com.masscustsoft.api.IndexKey;
import com.masscustsoft.api.NumIndex;
import com.masscustsoft.api.TimestampIndex;
import com.masscustsoft.helper.Upload;
import com.masscustsoft.model.ChangeLog;
import com.masscustsoft.model.Entity;
import com.masscustsoft.model.Increasing;
import com.masscustsoft.model.SearchResult;
import com.masscustsoft.model.Sequence;
import com.masscustsoft.model.Variable;
import com.masscustsoft.service.inner.SqlParser;
import com.masscustsoft.util.CacheUtil;
import com.masscustsoft.util.DataUtil;
import com.masscustsoft.util.GlbHelper;
import com.masscustsoft.util.LightStr;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.LogUtil;
import com.masscustsoft.util.MapUtil;
import com.masscustsoft.util.ReflectUtil;
import com.masscustsoft.util.ThreadHelper;
import com.masscustsoft.xml.BeanFactory;

/**
 * Is general used for data manipulation: Add, remove, update and query. Different successor inherited this class for different type of vendors. 
 * 
 * @author JSong
 */
public abstract class DataService extends AbstractDataService {
	
	/**
	 * By default true, while true, any insert/update/delete will be logged as {@link ChangeLog}
	 */
	protected boolean traceable=true;
	
	/**
	 * Add constraint dictionary to current data service. The {@link Constraint} is a way to define forign key integration and cascade deletion.
	 */
	protected List<Constraint> constraints=new ArrayList<Constraint>();
	
	/**
	 * The stored procedure implementation. If not set, {@link DatabaseSpProvider} is used to call database based stored procedure. Or use a mock one.
	 */
	protected StoredProcedureProvider spProvider;
	
	/**
	 * Define a name for this data service. need be unique in current runtime.
	 */
	protected String dsId;
	
	/**
	 * The database name used.
	 */
	protected String databaseName;
	
	private transient Map<String,Constraint> constraintMap=new HashMap<String,Constraint>();
	private transient Map<String,Boolean> fulltextSupports=new HashMap<String,Boolean>();
	
	private static List<DataService> services=new ArrayList<DataService>();
	
	public String getDsId() {
		return dsId;
	}

	public void setDsId(String dsId) {
		this.dsId = dsId;
	}

	public static void reset(){
		for (DataService ds:services){
			ds.destroy();
		}
	}
	
	/**
	 * If dsId is not set, if default is not occupied, used default as dsId, or generate one unique id as dsId.
	 * @throws Exception
	 */
	public void initialize() throws Exception{
		for (Constraint c:constraints){
			constraintMap.put(c.getModel(), c);
		}
		services.add(this);
		//System.out.println("constraints="+constraintMap);
		if (LightStr.isEmpty(dsId)){
			if (BeanFactory.getBeanFactory().getDataService("default")==null) dsId="default"; else dsId=LightUtil.getHashCode();
		}
		BeanFactory.getBeanFactory().addDataService(dsId, this);
	}
	
	/**
	 * Insert a {@link Entity} into data storage.
	 */
	@Override
	final public void insertBean(IEntity bean) throws Exception{
		insertRawBean(bean,false);
	}
	
	/**
	 * Insert a bean supporting rawMode.
	 */
	final public void insertRawBean(IEntity bean, boolean rawMode) throws Exception{
		boolean skip=false;
		//long t0=System.currentTimeMillis();
		if (bean instanceof Entity){
			((Entity)bean).setDataService(this);
			if (!rawMode && ThreadHelper.get("_dumping_")==null) {
				if (((Entity)bean).beforeInsert()==false) skip=true;
			}
		}
		if (!skip){
			clearCache(this,bean.getClass());
			
			Class<?> c = bean.getClass();
			String tbl=LightUtil.getCascadeName(c);
			String xml=BeanFactory.getBeanFactory().toXml(bean,0);
			String uniqueId=bean.getUuid();
			
			//System.out.println("insertRaw,"+tbl+",uid="+uniqueId+",xml="+xml);
			_insertBean("",tbl, uniqueId, xml);
				
			if (bean instanceof Entity){
				if (!rawMode && ThreadHelper.get("_dumping_")==null) ((Entity)bean).afterInsert();
			}	
		}
		//long t1=System.currentTimeMillis();
		//LogUtil.info("PERFORMANCE raw="+rawMode+",skip="+skip+",INSERT="+(t1-t0)+"ms, EN="+bean.getClass().getSimpleName());
	}
	
	/**
	 * Delete a Entity
	 */
	@Override
	final public void deleteBean(IEntity bean) throws Exception{
		boolean skip=false;
		//long t0=System.currentTimeMillis();
		bean.setDataService(this);
		if (ThreadHelper.get("_dumping_")==null){
			if (bean.beforeDelete()==false) skip=true;
		}
		if (!skip){
			clearCache(this,bean.getClass());
			this._deleteBean("",LightUtil.getCascadeName(bean.getClass()), bean.getUuid());
			if (ThreadHelper.get("_dumping_")==null) {
				bean.afterDelete();
			}
		}
		//long t1=System.currentTimeMillis();
		//LogUtil.info("PERFORMANCE DELETE="+(t1-t0)+"ms, EN="+bean.getClass().getSimpleName());
	}
	
	/**
	 * Update a Entity
	 */
	final public void updateBean(IEntity bean) throws Exception{
		//long t0=System.currentTimeMillis();
		bean.setDataService(this);
		if (ThreadHelper.get("_dumping_")==null){ 
			if (bean.beforeUpdate()==false) return;
		}
		_updateBean(bean);
		if (ThreadHelper.get("_dumping_")==null){
			bean.afterUpdate();
		}
		//long t1=System.currentTimeMillis();
		//LogUtil.info("PERFORMANCE UPDATE="+(t1-t0)+"ms, EN="+bean.getClass().getSimpleName());
	}

	/**
	 * Update a bean without trigger beforeUpdate and afterUpdate
	 */
	final public void _updateBean(IEntity bean) throws Exception{
		//if a number field is increaing
		List<Field> flds = ReflectUtil.getFieldMap(bean.getClass());
		for (Field f:flds){
			if (f.isAnnotationPresent(Increasable.class)){
				Object v=ReflectUtil.getProperty(bean, f.getName());
				Object o=bean.getOldValue(f.getName(), null);
				//Decimal, Long, Int
				if (v!=null && o!=null){
					Increasing inc=this.getBean(Increasing.class, "incId", bean.getUuid()+"_"+f);
					boolean isNew=false;
					if (inc==null){
						inc=new Increasing();
						inc.setIncId(bean.getUuid()+"_"+f);
						isNew=true;
					}
					//System.out.println("INC "+f.getName()+",o="+o+",v="+v+",inc="+inc.getIntValue());
					
					if (v instanceof Double){
						double dta=(Double)v-(Double)o;
						if (isNew) inc.setDoubleValue((Double)v);
						else inc.setDoubleValue(inc.getDoubleValue()+dta);
						v=inc.getDoubleValue();
					}
					else
					if (v instanceof Long){
						long dta=(Long)v-(Long)o;
						if (isNew) inc.setLongValue((Long)v);
						else inc.setLongValue(inc.getLongValue()+dta);
						v=inc.getLongValue();
					}
					else
					if (v instanceof Integer){
						int dta=(Integer)v-(Integer)o;
						if (isNew) inc.setIntValue((Integer)v);
						else inc.setIntValue(inc.getIntValue()+dta);
						v=inc.getIntValue();
					}
					if (isNew) insertBean(inc); else updateBean(inc);
					ReflectUtil.setProperty(bean,f.getName(),v);
				}
			}
		}
		//
		BeanFactory bf=BeanFactory.getBeanFactory();
		String xml=bf.toXml(bean,0);
		//System.out.println("upd xml="+xml);
		String old=bf.toXml(bean.getOld(),0);
		clearCache(this,bean.getClass());
		this._updateBean("",LightUtil.getCascadeName(bean.getClass()), bean.getUuid(), old, xml);
	}
	
	public static void clearCache(IDataService db, Class c){
		try {
			StringBuffer buf=new StringBuffer();
			while (c!=null) {
				if (c.getSimpleName().equals("Entity")||c.getSimpleName().equals("Object")) break;
				if (buf.length()>0) buf.insert(0,"|");
				buf.insert(0,c.getSimpleName());
				c=c.getSuperclass();
			}
			CacheUtil.expireCache(buf.toString());
		} catch (Exception e) {
			LogUtil.dumpStackTrace(e);
		}
	}
	
	/**
	 * Get a bean in given defer milliseconds.
	 */
	public <T extends IEntity> T getBean(int defer, Class<T> c,Object...paras) throws Exception{
		for (int i=0;i<defer;i+=250){
			T t=getBean(c,paras);
			if (t!=null) return t;
			//System.out.println("Expect waiting..."+c.getName());
			Thread.sleep(250);
		}
		throw new Exception("Expected Bean Not Coming: "+c.getName());
	}
	
	private String _getSpec(Object...paras){
		Map<String,Object> terms=new HashMap<String,Object>();
		for (int i=0;i<paras.length/2;i++){
			Object v=paras[i*2+1];
			if (v instanceof java.sql.Timestamp){
				Timestamp d=(Timestamp)v;
				v="@"+LightUtil.encodeLongDate(d);
			}
			if (v instanceof java.sql.Date){
				java.sql.Date d=(java.sql.Date)v;
				v="@"+LightUtil.encodeShortDate(d);
			}
			if (v instanceof Map || v instanceof List) terms.put(paras[i*2]+"", v);
			else terms.put(paras[i*2]+"",v+"");
		}
		String spec=LightUtil.toJsonString(terms).toString();
		return spec;
	}
	
	/**
	 * Get a Entity without cache.
	 */
	@Override
	public <T extends IEntity> T getBean(Class<T> c,Object...paras) throws Exception{
		String spec=_getSpec(paras);
		List<T> list=getBeanList(c,spec,"",0,1,null);
		T ret=null;
		if (list!=null && list.size()>0) ret=list.get(0);
		return ret;
	}
	
	/**
	 * Get {@link #MAX_RECORD} records.
	 * 
	 * @param c The Entity Class
	 * @param specific The JSON Filter
	 * @param text The full text search.
	 */
	@Override
	public <T extends Object> List<T> getBeanList(Class<T> c, String specific, String text)
		throws Exception {
		return getBeanList(c,specific,text,"");
	}
	
	/**
	 * Delete the Entity match filter.
	 * 
	 * @param c The Entity Class
	 * @param specific The JSON filter
	 */
	@Override
	public <T extends IEntity> void deleteBeanList(Class<T> c, String specific) throws Exception {
		deleteBeanList(c,specific,null,100);
	}
	
	@Override
	public <T extends IEntity> void deleteBeanList(Class<T> c, String specific, String sort) throws Exception {
		deleteBeanList(c,specific,sort,100);
	}

	@Override
	public <T extends IEntity> void deleteBeanList(Class<T> c, String specific, String sort, int batch) throws Exception {
		Upload up=Upload.getUpload();
		int idx=0;
		for (;;){
			PageInfo pg=getBeanList(new Class[]{c},specific,"",0,batch,"","",false);
			List<T> lst=pg.getList();
			if (lst.size()>0){
				//LogUtil.info("Deleting "+c.getSimpleName()+ " on "+specific +" count="+lst.size());
			}
			for (T i:lst){
				//if (up!=null) up.setStatus(idx, pg.getAmount(), "Deleting...");
				deleteBean(i);
				idx++;
			}
			if (lst.size()<batch) break;
		}
	}
	
	/**
	 * Get {@link #MAX_RECORD} records
	 * 
	 * @param c The Entity Class
	 * @param specific JSON filter
	 * @param text The full-text search
	 * @param sort the Sort field, you can put more fields separated by comma, can also put ASC/DESC after the field name separated with space for the sort direction. 
	 */
	@Override
	public <T extends Object> List<T> getBeanList(Class<T> c, String specific, String text, String sort)
		throws Exception {
		List<T> list = getBeanList(c,specific,text,0,MAX_RECORD,sort);
		return list;
	}

	/**
	 * Get a Search result with page support.
	 * 
	 * @param c The Entity Class
	 * @param specific JSON Filter
	 * @param text Full-text filter
	 * @param from Start offset, started from 0 
	 * @param size Max records return
	 * @param sort Sort field or fields.
	 */
	@Override
	public <T extends Object> List<T> getBeanList(Class<T> c, String specific, String text, int from, int size, String sort)
		throws Exception {
		Class[] cs;
		if (c==null)cs=new Class[]{}; else cs=new Class[]{c};
		PageInfo page=getBeanList(cs,specific,text,from,size, sort, "", false);
		return page.getList();
	}
	
	/*public String getFieldNameByColumnName(Class<?>[] cs,String fd){
		if (LightStr.isEmpty(fd)) return null;
		for (Class<?> c:cs){
			java.lang.reflect.Field f=LightUtil.findField(LightUtil.getFieldMap(c), fd);
			if (f!=null) return getFieldName(f);
		}
		return null;
	}*/
	
	/**
	 * Find a proper field name.
	 */
	public String getFieldName(Class<?>[] cs,String fd){
		if (cs==null||LightStr.isEmpty(fd)) return null;
		for (Class<?> c:cs){
			java.lang.reflect.Field f=ReflectUtil.findField(ReflectUtil.getFieldMap(c), fd);
			if (f!=null) return getFieldName(f);
		}
		return null;
	}
	
	/**
	 * Find real field name from Field annotation.
	 */
	protected String getFieldName(java.lang.reflect.Field fld){
		return _getFieldName(fld,true);
	}
	
	/**
	 * Find real field name from Field annotation. 
	 * 
	 * @param rich If true, with a index-type prefix
	 */
	protected String _getFieldName(java.lang.reflect.Field fld, boolean rich){
		String name=fld.getName();
		if (fld.isAnnotationPresent(IndexKey.class)) {
			if (rich) name=INDEX_+name;
		} 
		else
		if (fld.isAnnotationPresent(NumIndex.class)) {
			if (rich) name=NUMINDEX_+name;
		} 
		else
		if (fld.isAnnotationPresent(FullBody.class)) {
			if (rich) name=FULLBODY_+name;
		} 
		else
		if (fld.isAnnotationPresent(CategoryKey.class)) {
			if (rich) name=CATEGORY_+name;
		} 
		else 
		if (fld.isAnnotationPresent(FullText.class)) {
			if (rich) name=FULLTEXT_+name;
		} 
		else 
		if (fld.isAnnotationPresent(DateIndex.class)) {
			if (rich) name=DATEINDEX_+name;
		} 
		else 
		if (fld.isAnnotationPresent(TimestampIndex.class)) {
			if (rich) name=TIMESTAMP_+name;
		}
		return name;
	}
	
	/**
	 * Find out all search fields in a string seperated by comma.
	 */
	public String getSearchFields(Class<?>[] cs){
		StringBuffer fields=new StringBuffer();
		Set<String> list=new HashSet<String>();
		for (Class<?> c:cs){
			List<java.lang.reflect.Field> map = ReflectUtil.getFieldMap(c);
			for (java.lang.reflect.Field fld: map) {
				String name=getFieldName(fld);
				if (name==null) continue;
				if ((fld.getModifiers()&128)!=0) continue; //transient
				if (!list.contains(name)) list.add(name);
			}
			//System.out.println("SEARCHFIELD c="+c.getName()+" fields="+list);
		}
		for (String s:list){
			if (fields.length()>0) fields.append(",");
			fields.append(s);
		}
		
		return fields.toString();
	}
	
	/**
	 * Get Entity List using {@link PageInfo}
	 * 
	 * @param cs Array of Entity class to retrieve.
	 * @param specific JSON filter
	 * @param text FullText filter
	 * @param from Start record offset, starts from 0
	 * @param size Page size
	 * @param sort Sort field or fields
	 * @param facet Facet field or fields
	 * @param raw Flag to indicate if the result will be extracted
	 */
	@Override
	public PageInfo getBeanList(Class<?>[] cs, String specific, String text, int from, int size, String sort, String facet, boolean raw) throws Exception{
		PageInfo p=_getBeanList(cs,specific,text,from,size,sort,facet,raw);
		return p;
	}
	
	private PageInfo _getBeanList(Class<?>[] cs, String specific, String text, int from, int size, String sort, String facet, boolean raw) throws Exception{
		PageInfo page=new PageInfo(from,size);
		if (text==null) text=""; text=text.trim();
		if ("_@_".equals(text)||text.endsWith("@@_@_")){
			page.setAmount(0);
			page.setList(new ArrayList());
			return page;
		}
		StringBuffer names=new StringBuffer();
		for (Class<?> c:cs){
			if (names.length()>0) names.append(",");
			names.append(LightUtil.getCascadeName(c));
		}
		if (cs.length==0 && !text.startsWith("@@")) text="@@"+text+"@@";
		Map<String,Object> terms;
		if (LightStr.isEmpty(specific)) terms=new TreeMap<String,Object>();
		else 
		if (specific.startsWith("{")) {
			terms=(Map)LightUtil.parseJson(specific);
		}
		else throw new Exception("specific should be a JSON string:"+specific);
		MapUtil.extractTerms(terms);
		
		SearchResult res=new SearchResult();
		if (cs.length>0){
			try{
				Entity en=(Entity)cs[0].newInstance();
				en.setDataService(this);
				en.getBeanList(page,terms,text,sort);
				if (page.getList()!=null) {
					return page;
				}
			}
			catch(InstantiationException e){
				
			}
		}
		
		
		if (res.getAmount()==null) {
			//cache applied here
			if (facet==null) facet="";
			if (sort==null) sort="";
			String cacheId="Search-"+hashCode()+"-"+names+"+"+terms+"-"+LightUtil.macroStr(text)+"-"+sort+"-"+from+"-"+size+"-"+facet;
			res=(SearchResult)CacheUtil.getCache(cacheId);
			if (res==null){
				res=_doSearch(names.toString(),terms,getSearchFields(cs),text,sort,from,size,facet); //sort is not encoded. like reportId
				res.setAttribute("lastUpdate", LightUtil.longDate());
				CacheUtil.setCache(cacheId, res, size==1);
				
			}
		}
		if (!res.getSuccess()) throw new Exception(res.getResult().toString());
		page.setAmount(res.getAmount());
		if (raw){
			page.setList(res.getList());
		}
		else{
			List<Object> list=new ArrayList<Object>();
			for (Map<String,Object> map:res.getList()){
				String xml=(String)map.get(XML);
				Object o=(Object)BeanFactory.getBeanFactory().loadBean(xml);
				if (o instanceof Entity){
					Entity en=(Entity)o;
					en.setDataService(this);
					en.setOld(LightUtil.clone(en,"keep")); //(Entity)BeanFactory.getBeanFactory().loadBean(xml));
				}
				else if (o==null) o=map;
				list.add(o);
			}
			page.setList(list);
		}
		page.setAttributes(res.getAttributes());

		return page;
	}
	
	private Class<?>[] getClassList(String criteria) throws Exception{
		String[] ss=criteria.split(",");
		Set<Class<?>> list=new HashSet<Class<?>>();
		for (String s:ss){
			if (s.trim().length()==0) continue;
			String cname=BeanFactory.getBeanFactory().findRealClass(s);
			Class<?> c=com.masscustsoft.Lang.CLASS.forName(cname);
			list.add(c);
		}
		Class<?>[] cs=new Class[list.size()];
		list.toArray(cs);
		return cs;
	}
	
	/**
	 * An alternative call to getBeanList but using Entity name to retrieve.
	 */
	public PageInfo search(String criteria, String specific, String text, int from, int size, String sort) throws Exception{
		return getBeanList(getClassList(criteria),specific,text,from,size,sort,"", false);
	}

	/**
	 * Retrive Entity as an enumeration. Support Raw.
	 */
	@Override
	public <T> IDataEnumeration<T> enumeration(final Class<T> c, String specific, String text,int batch, boolean raw) throws Exception{
		return new DataEnumeration<T>(this,c,specific,text,null, batch, "", null,null,raw);
	}	
	
	/**
	 * Retrive Entity as an enumeration. Support Raw and Facet.
	 */
	@Override
	public <T> IDataEnumeration<T> enumeration(final Class<T> c, String specific, String text, String sort, int batch, String facet, List<IJoin> joins, List<IRefiner> refiners, boolean raw) throws Exception{
		return new DataEnumeration<T>(this,c,specific,text, sort, batch, facet, joins, refiners, raw);
	}
	
	/**
	 * Retrive Entity as an enumeration. Support Sort.
	 */
	@Override
	public <T> IDataEnumeration<T> enumeration(final Class<T> c, String specific, String text, String sort, int batch, List<IJoin> joins, List<IRefiner> refiners) throws Exception{
		return new DataEnumeration<T>(this,c,specific,text, sort, batch, "", joins,refiners,false);
	}
	
	/**
	 * Retrive Entity By SQL
	 */
	@Override
	public <T> IDataEnumeration<T> enumeration(String sql, String specific, String sort, int batch, List<IJoin> joins, List<IRefiner> refiners, boolean raw) throws Exception{
		return new DataEnumeration<T>(this,sql,specific,sort, batch, joins, refiners, raw);
	}
	
	/**
	 * convert field name 
	 * @param fld
	 * @return
	 */
	protected String getSimpleField(String fld){
		if (fld.startsWith(INDEX_)) return fld.substring(INDEX_.length());
		if (fld.startsWith(NUMINDEX_)) return fld.substring(NUMINDEX_.length());
		if (fld.startsWith(FULLTEXT_)) return fld.substring(FULLTEXT_.length());
		if (fld.startsWith(FULLBODY_)) return fld.substring(FULLBODY_.length());
		if (fld.startsWith(CATEGORY_)) return fld.substring(CATEGORY_.length());
		if (fld.startsWith(DATEINDEX_)) return fld.substring(DATEINDEX_.length());
		if (fld.startsWith(TIMESTAMP_)) return fld.substring(TIMESTAMP_.length());
		return fld;
	}
	
	@Override
	public int getSequenceId(String seqId, Class<? extends IEntity> cls, String fldName, Integer curr) throws Exception{
		return (int)getSequenceId(seqId,cls,fldName,"",curr==null?(Long)null:new Long(curr.longValue()));
	}
	
	@Override
	public long getSequenceId(String seqId, Class<? extends IEntity> cls, String fldName, Long curr) throws Exception{
		return getSequenceId(seqId,cls,fldName,"",curr);
	}
	
	@Override
	public String getSequenceId(String seqId, Class<? extends IEntity> cls, String fldName, String prefix, String curr) throws Exception{
		if (!LightStr.isEmpty(curr)){
			return curr;
		}
		return prefix+getSequenceId(seqId,cls,fldName,prefix,(Long)null);
	}
	
	private long getSequenceId(String seqId, Class<? extends IEntity> cls, String fldName, String prefix, Long curr) throws Exception{
		if (curr!=null && curr!=0) return curr;
		String transId=(String)ThreadHelper.get("transaction");
		if (!LightStr.isEmpty(transId)) DataUtil.commitTransaction();
		Sequence seq=this.getBean(Sequence.class,"seqId", seqId);
		if (seq==null){
			seq=new Sequence();
			seq.setSeqId(seqId);
			seq.setValue(1);
			insertBean(seq);
		}
		else{
			seq.setValue(seq.getValue()+1);
		}
		while(true){
			IEntity tt=getBean(cls,fldName,prefix+seq.getValue());
			if (tt!=null){
				seq.setValue(seq.getValue()+1);
			}
			else break;
		}
		updateBean(seq);
		
		if (!LightStr.isEmpty(transId)) DataUtil.startTransaction();
		return seq.getValue();
	}
	
	@Override
	public String getSequenceId(String seqId, Class<? extends IEntity> cls, String fldName, String prefix, String curr, String extraFields, String extraValues) throws Exception{
		if (!LightStr.isEmpty(curr)){
			return curr;//prefix+getSequenceId(seqId,cls,fldName,ancestorId, prefix,LightUtil.decodeLong(curr.substring(prefix.length())));
		}
		return prefix+getSequenceId(seqId,cls,fldName,prefix,(Long)null,extraFields,extraValues);
	}
	
	@Override
	public int getSequenceId(String seqId, Class<? extends IEntity> cls, String fldName, Integer curr, String extraFields, String extraValues) throws Exception{
		return (int)getSequenceId(seqId, cls, fldName, "", curr==null?(Long)null:new Long(curr.longValue()), extraFields, extraValues);
	}
	
	@Override
	public long getSequenceId(String seqId, Class<? extends IEntity> cls, String fldName, Long curr, String extraFields, String extraValues) throws Exception{
		return getSequenceId(seqId, cls, fldName, "", curr, extraFields, extraValues);
	}
	
	private long getSequenceId(String seqId, Class<? extends IEntity> cls, String fldName, String prefix, Long curr, String extraFields, String extraValues) throws Exception{
		if (curr!=null && curr!=0) return curr;
		String[] eFields=extraFields.split(";");
		String[] eValues=extraValues.split(";");
		if (eFields.length!=eValues.length) throw new Exception("Wrong extraFieldValue pairs: "+extraFields+" || "+extraValues);
		
		String extraKey=extraValues.replace(';', '_');
		String transId=(String)ThreadHelper.get("transaction");
		if (!LightStr.isEmpty(transId)) DataUtil.commitTransaction();
		Sequence seq=this.getBean(Sequence.class,"seqId", seqId, "ancestorId", extraKey);
		if (seq==null){
			seq=new Sequence();
			seq.setSeqId(seqId);
			seq.setAncestorId(extraKey);
			seq.setValue(1);
			insertBean(seq);
		}
		else{
			seq.setValue(seq.getValue()+1);
		}
		Object[] pp=new Object[eFields.length*2+2];
		for (int i=0;i<eFields.length;i++){
			pp[2+i+i]=eFields[i];
			pp[3+i+i]=eValues[i];
		}
		while(true){
			pp[0]=fldName;
			pp[1]=prefix+seq.getValue();
			IEntity tt=getBean(cls,pp);
			if (tt!=null){
				seq.setValue(seq.getValue()+1);
			}
			else break;
		}
		updateBean(seq);
		
		if (!LightStr.isEmpty(transId)) DataUtil.startTransaction();
		return seq.getValue();
	}
	
	@Override
	public String getVariable(String varId) throws Exception{
		Variable seq=this.getBean(Variable.class,"varId", varId);
		if (seq==null){
			return "";
		}
		return seq.getValue();
	}
	
	@Override
	public void setVariable(String varId, String val) throws Exception{
		if (!LightStr.isKey(varId)) throw new Exception("varId must be a id compatable!");
		Variable seq=this.getBean(Variable.class,"varId", varId);
		if (seq==null){
			seq=new Variable();
			seq.setVarId(varId);
			seq.setValue(val);
			insertBean(seq);
		}
		else{
			seq.setValue(val);
			updateBean(seq);
		}
	}

	public boolean getTraceable() {
		return traceable;
	}

	public void setTraceable(boolean tracable) {
		this.traceable = tracable;
	}
	
	public void destroy(){
	}
	
	public void _commitTransaction(String transId) throws Exception{
	}
	
	public void _rollbackTransaction(String transId) throws Exception{
	}
	
	public List<Constraint> getConstraints() {
		return constraints;
	}

	public void setConstraints(List<Constraint> constraints) {
		this.constraints = constraints;
	}
	
	public Constraint getConstraint(String cls){
		return constraintMap.get(cls);
	}
	
	public StoredProcedureProvider getSpProvider() {
		return spProvider;
	}

	public void setSpProvider(StoredProcedureProvider spProvider) {
		this.spProvider = spProvider;
	}

	@Override
	final public Integer runStoredProcedure(IEntity owner, String name, Object... params) throws Exception{
		String logsp=(String)GlbHelper.get("LogSP-"+name);
		if (logsp!=null) doDirectTrace(owner,logsp,"",params);
		else{
			logsp=(String)GlbHelper.get("LogSP");
			if ("true".equals(logsp)) doDirectTrace(owner,"SPCall",name,params);
		}
		return (Integer)_runStoredProcedure(name, java.sql.Types.INTEGER, params);
		
	}
	
	protected Object[] filterParams(Object... params){
		List pp=new ArrayList();
		for (Object o:params){
			if (o!=null && o instanceof String){
				String val=(String)o;
				if (val.startsWith("$$$$$")) continue;
				if (val.startsWith("*****")) o=val.substring(5);
			}
			pp.add(o);
		}
		return pp.toArray();
	}
	
	public Object _runStoredProcedure(String name, Integer resultType, Object... params) throws Exception{
		if (spProvider==null) throw new Exception("Not supported.");
	
		Object ret=spProvider.runStoredProcedure(this, name, resultType, filterParams(params));
		
		String effectTables=spProvider.getEffectingTables().get(name);
		if (effectTables==null) effectTables=spProvider.getEffectingTables().get("default");
		System.out.println("SP "+name+", effected="+effectTables);
		if (effectTables!=null) CacheUtil.expireCache(effectTables);
		if (ret==null){
			throw new Exception("Stored procedure has error: a null value returned!");
		}
		return ret;
	}
	
	public Class getBeanClass(String model) throws ClassNotFoundException{
		String clsName=BeanFactory.getBeanFactory().findRealClass(getPureBean(model));
		return CLASS.forName(clsName);
	}
	/**
	 * Detect if an Entity support full text search or not.
	 * 
	 * <p>If model starts with '@' means it's a dynamic model based on context. So no full-text support.
	 * Or call {@link Entity#supportFullText} to identify. A customer model ({@link Entity}) class can override this method to return false to disable full text support. 
	 * By default it's supported.
	 */
	public boolean supportFullText(String model){
		if (model.startsWith("@")) return false;
		String bean=getPureBean(model);
		Boolean b=fulltextSupports.get(bean);
		if (b!=null) return b;
		try {
			Class cls = getBeanClass(bean);
			Entity en=(Entity)cls.newInstance();
			b=en.supportFullText();
		} catch (Exception e) {
			LogUtil.dumpStackTrace(e);
			b=false;
		}
		fulltextSupports.put(model, b);
		return b;
	}

	public String createTempTable(String tblName,List<Map> fields,List<Map> data) throws Exception{
		throw new Exception("Not supported!");
	}
	
	public void dropTempTable(String tblName) throws Exception{
		throw new Exception("Not supported!");
	}

	public List<Map> getBeanListBySql(String sql, String specific, int from, int size, String sort)	throws Exception {
		PageInfo pg=getBeanListBySql(sql,specific,from, size, sort, "", false);
		return (List)pg.getList();
	}
	
	public PageInfo getBeanListBySql(String sql, String specific, int from, int size, String sort, String facet, boolean raw) throws Exception{
		Map<String,Object> terms;
		if (LightStr.isEmpty(specific)) terms=new HashMap<String,Object>();
		else 
		if (specific.startsWith("{")) terms=(Map)LightUtil.parseJson(specific);
		else throw new Exception("specific should be a JSON string:"+specific);
		
		MapUtil.extractTerms(terms);
		
		SearchResult res = _doSearchBySql(sql,terms,from,size,sort,facet); //sort is not encoded. like reportId
		if (!res.getSuccess()) throw new Exception(res.getResult().toString());
		PageInfo page=new PageInfo(from,size);
		page.setAmount(res.getAmount());
		if (raw){
			page.setList(res.getList());
		}
		else{
			List<Object> list=new ArrayList<Object>();
			for (Map<String,Object> map:res.getList()){
				String xml=(String)map.get(XML);
				if (xml!=null){
					Object o=(Object)BeanFactory.getBeanFactory().loadBean(xml);
					if (o instanceof Entity){
						Entity en=(Entity)o;
						en.setDataService(this);
						en.setOld(LightUtil.clone(en,null)); //(Entity)BeanFactory.getBeanFactory().loadBean(xml));
					}
					list.add(o);
				}
				else{
					list.add(map);
				}
			}
			page.setList(list);
		}
		return page;
	}
	
	public String getDatabaseName() {
		return databaseName;
	}
	
	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}

	/**
	 * Process the joins after the data is retrieved. 
	 * 
	 * @param dataService The DataService
	 * @param rcd The individule record
	 */
	@Override
	public Map doJoin(Object rcd, List<IJoin> joins, List<IRefiner> refiners) throws Exception{
		Map m=(Map)LightUtil.toJsonObject(rcd);
		if (joins!=null){
			for (IJoin j:joins){
				j.doJoin(this,m);
			}
		}
		if (refiners!=null){
			for (IRefiner p:refiners){
				if (p!=null) p.refineRow(this,null,m,null);
			}
		}
		
		return m;
	}
	
	protected void doDirectTrace(IEntity owner, String action, String summary, Object... params) throws Exception{
		String userId=LightUtil.getUserId();
		if (LightStr.isEmpty(userId) || userId.equals("guest") || userId.equals("jobUser")) return;
		
		ChangeLog bt=new ChangeLog();
		bt.setBeanId(LightUtil.getHashCode());
		bt.setBeanType(owner==null?"":LightUtil.getCascadeName(owner.getClass()));
		bt.setAction(action);
		bt.setBeanData("");
		bt.setTraceDate(LightUtil.longDate());
		bt.setUserId(userId);
		
		StringBuffer sum=new StringBuffer(summary);
		for (Object o:params){
			if (o==null) continue;
			if (o instanceof OutputParameter) continue; 
			if (sum.length()>0) sum.append(", ");
			if (o instanceof Date) sum.append(LightUtil.encodeShortDate((Date)o));
			else
			if (o instanceof Timestamp) sum.append(LightUtil.encodeLongDate((Timestamp)o));
			else
			if (o instanceof String){
				String val=(String)o;
				if (val.startsWith("$$$$$")) sum.append(val.substring(5)); 
				else 
				if (val.startsWith("*****")) sum.append("*****"); 
				else sum.append(val);
			}
			else
				sum.append(o.toString());
		}
		bt.setSummary(sum.toString());
		insertBean(bt);
	}
	
	protected SqlSegment resolveSql(String sql,Map<String,Object> terms){
		SqlSegment seg=new SqlSegment();
		SqlParser p=new SqlParser(LightUtil.macroStr(sql),terms,seg);
		Object o=p.parseVars('@');
		p.doRemove();
		seg.set(o+"");
		return seg;
	}

	public void validate() throws Exception{
	}
}


