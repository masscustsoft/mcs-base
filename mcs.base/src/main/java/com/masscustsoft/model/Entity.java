package com.masscustsoft.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;

import com.masscustsoft.Lang.CLASS;
import com.masscustsoft.api.AutoInc;
import com.masscustsoft.api.Descendant;
import com.masscustsoft.api.IBeanFactory;
import com.masscustsoft.api.IBody;
import com.masscustsoft.api.IDataEnumeration;
import com.masscustsoft.api.IDataService;
import com.masscustsoft.api.IEntity;
import com.masscustsoft.api.IFile;
import com.masscustsoft.api.IReferentItem;
import com.masscustsoft.api.IRepository;
import com.masscustsoft.api.ITraceable;
import com.masscustsoft.api.IndexKey;
import com.masscustsoft.api.PrimaryKey;
import com.masscustsoft.api.Refer;
import com.masscustsoft.api.Referent;
import com.masscustsoft.api.Required;
import com.masscustsoft.api.SQLSize;
import com.masscustsoft.api.SQLTable;
import com.masscustsoft.api.SequenceId;
import com.masscustsoft.helper.Upload;
import com.masscustsoft.model.AbstractResult.ResultType;
import com.masscustsoft.service.Constraint;
import com.masscustsoft.service.PageInfo;
import com.masscustsoft.util.LightStr;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.LogUtil;
import com.masscustsoft.util.MapUtil;
import com.masscustsoft.util.ReflectUtil;
import com.masscustsoft.util.StreamUtil;
import com.masscustsoft.util.ThreadHelper;

/**
 * Ancestor for any persistent-able object.
 *  
 * @author JSong
 *
 */
public class Entity implements IEntity{
	/**
	 * Any Entity has a global unique id, named uuid. If you copied a object, reset the uuid with {@link LightUtil#getHashCode} to set a case-insensitive new uuid.   
	 */
	@IndexKey @SQLSize(64)
	protected String uuid;
	
	/**
	 * The old value. Used to compare any field modified. Only available after retrieve.
	 */
	protected transient Entity old;
	
	/**
	 * The DataService which retrieved this Entity.
	 */
	protected transient IDataService dataService;

	/**
	 * Flag to indicate if this Entity is full-text eligible. Default true, you can override it to return false to disable full-text feature for this Entity.
	 */
	public boolean supportFullText(){
		return true;
	}
	
	public IDataService getDataService() {
		return dataService;
	}

	@Override
	public void setDataService(IDataService dataService) {
		this.dataService = dataService;
	}

	@Override
	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public Entity() {
		uuid = LightUtil.getHashCode();
	}

	protected List<Entity> getDataInMyRec(Class c, int max, Entity me) throws Exception{
		List<Field> fields = ReflectUtil.getFieldMap(c);
		StringBuffer buf=new StringBuffer();
		for (Field f:fields){
			PrimaryKey pk=f.getAnnotation(PrimaryKey.class);
			if (pk==null) continue;
			//System.out.println("keyfield="+f.getName());
			Object v=ReflectUtil.getProperty(me, f.getName());
			if (v==null) continue; //ignore non-exist fields
			if (v instanceof Date) v="@"+LightUtil.encodeShortDate((Date)v);
			else
			if (v instanceof Timestamp) v="@"+LightUtil.encodeLongDate((Timestamp)v);
			if (buf.length()>0) buf.append(",");
			buf.append(f.getName()+":");
			buf.append(v+"");
		}
		//System.out.println("getDataList buf="+buf);
		if (buf.length()==0){
			if (c.isAnnotationPresent(SQLTable.class)) {
				throw new Exception("#[NoPrimaryKeyDefined]: #["+c.getName().toUpperCase()+"]");
			}
			return new ArrayList<Entity>();
		}
		List<Entity> list=dataService.getBeanList(c, buf.toString(), "", 0, max, null);
		return list;
	}
	
	public Map<String,Object> getKeyMap() throws Exception{
		List<Field> myFlds = ReflectUtil.getFieldMap(this.getClass());
		Map<String,Object> terms=new HashMap<String,Object>();
		for (Field f:myFlds){
			PrimaryKey pk=f.getAnnotation(PrimaryKey.class);
			if (pk==null) continue;
			Object v=ReflectUtil.getProperty(this, f.getName());
			if (v==null) continue; //ignore non-exist fields
			if (v instanceof Date) v=LightUtil.encodeShortDate((Date)v);
			else
			if (v instanceof Timestamp) v=LightUtil.encodeLongDate((Timestamp)v);
			terms.put(f.getName(),v+"");
		}
		terms.put("$cacheModel", this.getPrimaryClass().getSimpleName());
		return terms;
	}
	
	protected List<Entity> getDataInMyKey(Class c, int max, Entity me) throws Exception{
		List<Field> fields = ReflectUtil.getFieldMap(c);
		List<Field> myFlds = ReflectUtil.getFieldMap(me.getClass());
		Map<String,Object> terms=new HashMap<String,Object>();
		for (Field f:myFlds){
			PrimaryKey pk=f.getAnnotation(PrimaryKey.class);
			if (pk==null) continue;
			Object v=ReflectUtil.getProperty(me, f.getName());
			if (v==null) continue; //ignore non-exist fields
			if (ReflectUtil.findField(fields, f.getName())==null) continue;
			if (v instanceof Date) v="@"+LightUtil.encodeShortDate((Date)v);
			if (v instanceof Timestamp) v="@"+LightUtil.encodeLongDate((Timestamp)v);
			terms.put(f.getName(),v+"");
		}
		if (terms.size()==0){
			//if (c.isAnnotationPresent(SQLTable.class)) throw new Exception("NoPrimaryKeyDefined: "+c.getName());
			return new ArrayList<Entity>();
		}
		ThreadHelper.set("$$Keys",terms);
		List<Entity> list=dataService.getBeanList(c, LightUtil.toJsonString(terms).toString(), "", 0, max, null);
		return list;
	}
	
	private void verifyPrimaryKey() throws Exception{
		List<Field> fields = ReflectUtil.getFieldMap(getClass());
		for (Field f:fields){
			PrimaryKey pk=f.getAnnotation(PrimaryKey.class);
			Required rq=f.getAnnotation(Required.class);
			Object v=ReflectUtil.getProperty(this, f.getName());
			if (pk!=null&& pk.serverGenerated()) continue;
			if ((pk!=null || rq!=null) && (v==null || v.toString().length()==0)) {
				throw new Exception("MissingKeyField "+f.getName());
			}
		}
	}
	
	private void processAutoInc() throws Exception{
		List<Field> fields = ReflectUtil.getFieldMap(getClass());
		List<Field> keys=new ArrayList<Field>();
		AutoInc ai=null;
		Field ff=null;
		for (Field f:fields){
			PrimaryKey pk=f.getAnnotation(PrimaryKey.class);
			if (pk!=null){
				AutoInc a=f.getAnnotation(AutoInc.class);
				if (a!=null){
					ff=f;
					ai=a;
				}
				else{
					keys.add(f);
				}
			}
		}
		if (ai==null) return; //ai.value=prefix
		Object val=ReflectUtil.getProperty(this, ff.getName());
		String id=getPrimaryClass().getSimpleName()+"_"+ff.getName();
		
		String kk="",vv="";
		for (Field f:keys){
			if (kk.length()>0) kk+=";";
			if (vv.length()>0) vv+=";";
			kk+=f.getName();
			vv+=ReflectUtil.getProperty(this, f.getName());
		}
		
		if (ff.getType().equals(String.class)){
			if (keys.size()==0){
				val=dataService.getSequenceId(id,getPrimaryClass(),ff.getName(),ai.value(),(String)val);
			}
			else{
				val=dataService.getSequenceId(id,getPrimaryClass(),ff.getName(),ai.value(),(String)val,kk,vv);
			}
		}
		else
		if (ff.getType().equals(Long.class)){
			if (keys.size()==0){
				val=dataService.getSequenceId(id,getPrimaryClass(),ff.getName(),(Long)val);
			}
			else{
				val=dataService.getSequenceId(id,getPrimaryClass(),ff.getName(),(Long)val,kk,vv);
			}
		}
		else
		if (ff.getType().equals(Integer.class)){
			if (keys.size()==0){
				val=dataService.getSequenceId(id,getPrimaryClass(),ff.getName(),(Integer)val);
			}
			else{
				val=dataService.getSequenceId(id,getPrimaryClass(),ff.getName(),(Integer)val,kk,vv);
			}
		}
		
		ReflectUtil.setProperty(this, ff.getName(), val);
	}
	
	private void processSequenceId() throws Exception{
		List<Field> fields = ReflectUtil.getFieldMap(getClass());
		List<Field> keys=new ArrayList<Field>();
		SequenceId si=null;
		Field ff=null;
		for (Field f:fields){
			PrimaryKey pk=f.getAnnotation(PrimaryKey.class);
			SequenceId a=f.getAnnotation(SequenceId.class);
			if (a==null && pk!=null){
				keys.add(f);
			}
			else{
				if (a!=null){
					ff=f;
					si=a;
				}
			}
		}
		if (si==null) return;
		Object val=ReflectUtil.getProperty(this, ff.getName());
		String id=getPrimaryClass().getSimpleName()+"_"+ff.getName();
		
		String kk="",vv="";
		
		//if sequenceId has parameters, use it instead of pk
		if (si.value().length>0){
			keys.clear();
			for (String f:si.value()){
				Field fld=ReflectUtil.findField(fields, f);
				if (fld==null) throw new Exception("Field not found: "+f);
				keys.add(fld);
			}
		}
		else{
			//remove last key to make sure sequence works
			if (keys.size()>0) keys.remove(keys.size()-1);
		}
		
		//if (keys.size()==0) throw new Exception("Missing keys for sequence Id: "+ff.getName());
		
		for (Field f:keys){
			if (kk.length()>0) kk+=";";
			if (vv.length()>0) vv+=";";
			kk+=f.getName();
			vv+=ReflectUtil.getProperty(this, f.getName());
		}
		
		if (ff.getType().equals(Long.class)){
			if (keys.size()==0){
				val=dataService.getSequenceId(id,getPrimaryClass(),ff.getName(),(Long)val);
			}
			else{
				val=dataService.getSequenceId(id,getPrimaryClass(),ff.getName(),(Long)val,kk,vv);
			}
		}
		else
		if (ff.getType().equals(Integer.class)){
			if (keys.size()==0){
				val=dataService.getSequenceId(id,getPrimaryClass(),ff.getName(),(Integer)val);
			}
			else{
				val=dataService.getSequenceId(id,getPrimaryClass(),ff.getName(),(Integer)val,kk,vv);
			}
		}
		ReflectUtil.setProperty(this, ff.getName(), val);
	}
	
	public void validate() throws Exception{
		
	}
	
	public Class getPrimaryClass(){
		return getClass();
	}
	
	public boolean beforeInsert() throws Exception{
		//process AutoInc and sequenceId
		processAutoInc();
		processSequenceId();
		
		//validate //if (ThreadHelper.get("$$Piping")==null) 
		validate();
		//test if primary key cannot be empty
		verifyPrimaryKey();
		//process primarykeys
		Class c=this.getPrimaryClass();
		
		List<Entity> list=getDataInMyKey(this.getPrimaryClass(),1,this);
		if (list.size()>0) {
			//I18n i18n=(I18n)ThreadHelper.get("i18n");
			throw new Exception("DuplicatedPrimaryKey");
		}
		if (this instanceof IBody){
			LightStr.strokeBody((IBody)this,true,false);
		}
		return true;
	}
	
	public void afterInsert() throws Exception{
		doTrace("insert");
	}
	
	public void validateDelete() throws Exception{
	}

	public boolean beforeDelete() throws Exception{
		if (ThreadHelper.get("$$Piping")==null) validateDelete();
		
		Class c=this.getClass();
		List<IReferentItem> refers=getReferList();
		if (refers.size()>0){
			for (IReferentItem re:refers){
				//List<Entity> list=getDataInMyKey(re,1,this);
				//if (list.size()>0) throw new Exception("ReferenceExist:"+re.getName());
				re.checkReference(this);
			}
		}
		List<IReferentItem> descendants=getDescendantList();
		if (descendants.size()>0){
			for (IReferentItem cc:descendants){
				cc.cascadeDelete(this);
			}
		}
		if (this instanceof IBody) dataService.deleteBeanList(FlyingFile.class, "{ownerId:'"+uuid+"'}");
		
		//delete attributes with IFile
		List<Field> flds = ReflectUtil.getFieldMap(this.getClass());
		for (Field f:flds){
			if (IFile.class.isAssignableFrom(f.getType())){
				IFile file=(IFile)ReflectUtil.getProperty(this, f.getName());
				if ((f.getModifiers()&Modifier.TRANSIENT)==0 && file!=null){
					deleteFile(file);
				}
			}
		}
		//
		return true;
	}
	
	public void afterDelete() throws Exception{
		doTrace("delete");
	}
	
	@Override
	public List<IReferentItem> getReferList() throws Exception{
		List<IReferentItem> list=new ArrayList<IReferentItem>();
		Refer refer=(Refer)this.getClass().getAnnotation(Refer.class);
		if (refer!=null){
			for (Referent re:refer.value()) {
				list.add(ReflectUtil.getReferent(this.getClass(),re));
			}
		}
		Constraint c=dataService.getConstraint(CLASS.getSimpleName(getClass()));
		if (c!=null){
			for (IReferentItem r:c.getRefers()){
				list.add(r);
			}
		}
		//System.out.println("referList="+list);
		return list;
	}
	
	@Override
	public List<IReferentItem> getDescendantList() throws Exception{
		List<IReferentItem> list=new ArrayList<IReferentItem>();
		Descendant refer=(Descendant)this.getClass().getAnnotation(Descendant.class);
		if (refer!=null){
			for (Referent re:refer.value()) list.add(ReflectUtil.getReferent(getClass(),re));
		}
		Constraint c=dataService.getConstraint(CLASS.getSimpleName(getClass()));
		if (c!=null){
			for (IReferentItem r:c.getDescendants()){
				list.add(r);
			}
		}
		return list;
	}
	
	public boolean beforeUpdate() throws Exception{
		processSequenceId();
		validate();
		//test if key changed
		boolean keychanged=false;
		List<Field> fields = ReflectUtil.getFieldMap(getClass());
		for (Field f:fields){
			PrimaryKey pk=f.getAnnotation(PrimaryKey.class);
			if (pk!=null){
				if (changed(f.getName())) { keychanged=true; break; }
			}
		}
		//if keychanged, test if new key already there
		if (keychanged){
			//test if old key has reference
			List<IReferentItem> refers=getReferList();
			if (refers.size()>0){
				for (IReferentItem re:refers){
					//List<Entity> list=getDataInMyKey(re,1,getOld());
					//if (list.size()>0) throw new Exception("ReferenceExist:"+re.getName());
					re.checkReference(this);
				}
			}
			List<Entity> list=getDataInMyKey(this.getClass(),1,this);
			if (list.size()>0 && !list.get(0).getUuid().equals(uuid)) {
				throw new Exception("i18n.DuplicatedPrimaryKey");
			}
			
			verifyPrimaryKey();
		}
		if (this instanceof IBody) LightStr.strokeBody((IBody)this,true,false);
		return true;
	}
	
	public void afterUpdate() throws Exception{
		doTrace("update");
	}

	public Entity getOld() {
		return old;
	}

	public void setOld(Entity old) {
		this.old = old;
	}

	@Override
	public <T> T getOldValue(String fld,T def){
		T val=null;
		if (old!=null)
			try {
				val=(T)ReflectUtil.getProperty(old, fld);
			} catch (Exception e) {
			}
		if (val==null) val=def;
		return val;
	}
	
	public boolean changed(String fld){
		List<Field> flds = ReflectUtil.getFieldMap(this.getClass());
		Field f=ReflectUtil.findField(flds, fld);
		return changed(f,null);
	}
	
	private String getVal(Object n){
		if (n==null) return null;
		if (n instanceof String && ((String)n).equals("")) return null;
		if (n instanceof Date) return LightUtil.encodeShortDate((Date)n);
		if (n instanceof Timestamp) return LightUtil.encodeLongDate((Timestamp)n);
		return n.toString();
	}
	
	protected boolean changed(Field f, StringBuffer scr){
		String fld=f.getName();
		Object o=getOldValue(fld,null);
		Object n=null;
		try {
			n=ReflectUtil.getProperty(this, fld);
		} catch (Exception e) {
			LogUtil.dumpStackTrace(e);
		}
		if (n==null) return false;
		if (f.getAnnotation(PrimaryKey.class)!=null && n instanceof String){
			if (((String)n).equalsIgnoreCase((String)o)) return false;
		}
		if (!n.equals(o)) {
			if (LightStr.isEmpty(n.toString()) && o==null) return false;
			if (scr!=null){
				if (scr.length()>0) scr.append(", ");
				scr.append(fld+"="+getVal(n));
				Object old=getVal(o);
				if (old!=null && !old.equals("")) scr.append(" <== "+getVal(o)+"");
			}
			return true;
		}
		else
		if (f.isAnnotationPresent(PrimaryKey.class)){
			if (scr!=null){
				if (scr.length()>0) scr.append(", ");
				scr.append(fld+"="+getVal(n));
			}
		}
		return false;
	}
	
	public void deleteFile(IFile file) throws Exception {
		if (file==null) return;
		//System.out.println("deleteFile="+LightUtil.toJsonObject(file));
		if (LightStr.isEmpty(file.getExternalId())) return;
		ExternalFile ef=dataService.getBean(ExternalFile.class, "uuid", file.getExternalId());
		//System.out.println("   deleteFile="+ef+", rec="+ef.getRefCount());
		if (ef!=null) ef.removeResource();
		file.setExternalId(null);
		file.setSize(0);
	}
	
	private void _doExport(IEntity en,List<Map> list) throws Exception{
		Map map=new HashMap();
		list.add(map);
		map.put("bean", en);
		//System.out.println("DESC en="+en+", desc="+en.getDescendantList());
		for (IReferentItem ri:en.getDescendantList()){
			List<IEntity> subs = ri.getReferentData(en, 100000);
			List<Map> lst=new ArrayList<Map>();
			map.put(ri.getModel()+"List",lst);
			for (IEntity sub:subs){
				_doExport(sub,lst);
			}
		}
	}
	
	public JsonResult doExport(Upload up){
		String type=up.getStr("exportType","xml");
		if (type.equals("csv")){
			return doExportCsv(up);
		}
		return doExportXml(up);
	}
	
	public JsonResult doExportXml(Upload up){
		IDataService data=LightUtil.getDataService();
		JsonResult res=new JsonResult();
		String fn=up.getStr("fileName",CLASS.getSimpleName(getClass())+".xml");
		try {
			HttpServletResponse resp = up.getResponse();
			resp.setHeader("Content-Disposition","attachment;filename=\""+fn+"\"");
			resp.setContentType("text/xml");
			resp.setHeader("Cache-Control","maxage=3600");
			resp.setHeader("Pragma","public");
			//resp.setHeader("Cache-Control", "private");
			resp.setHeader("Accept-Ranges", "none");
			
			String specific=up.getStr("specific","{}"); if (specific.length()==0) specific="{}";
			Map<String,Object> spec=(Map)LightUtil.parseJson(specific);
			
			String text=up.getStr("text", "");
			if (!LightStr.isEmpty(text)) spec.put("text", text);
			int ac=up.getInt("action.count", 0);
			if (ac>0){
				String uuids="";
				for (int i=0;i<ac;i++){
					String id=up.getStr("uuid."+i, null);
					if (id==null) continue;
					if (uuids.length()>0){
						uuids+="||";
					}
					uuids+=id;
				}
				spec.put("uuid", uuids);
			}
			ServletOutputStream out = resp.getOutputStream();
			Writer w=new OutputStreamWriter(out, LightUtil.UTF8);
			
			IBeanFactory bf = LightUtil.getBeanFactory();
			IDataEnumeration<? extends Entity> ee = data.enumeration(getClass(), LightUtil.toJsonString(spec).toString(), "", 5000, true);
			List<Map> list=new ArrayList<Map>();
			while (ee.hasMoreElements()){
				Map map=(Map)ee.nextElement();
				String xml=(String)map.get(IDataService.XML); 
				if (xml==null){
					//if from Entity.getBeanList
					list.add(map);
				}
				else{
					IEntity en=(IEntity)bf.loadBean(xml);
					en.setDataService(data);
					_doExport(en,list);	
				}
			}
			w.append(bf.toXml(list, 0));
			w.flush();
			
			res.setType(ResultType.Stream);
		} catch (Exception e) {
			res.setError(e);
			LogUtil.dumpStackTrace(e);
		}
		return res;
	}
	
	
	public JsonResult doExportCsv(final Upload up){
		class CsvExport{

			List<String> fields=new ArrayList<String>();

			Map<String,String> labels=null;
			
			String delimitedBy;
			
			boolean withQuote;
			
			public CsvExport(){
				this.delimitedBy=up.getStr("delimitedBy", "Tab").equals("Tab")?"\t":"," ;
				this.withQuote=up.getStr("withQuote","yes").equals("yes");
			}
			
			String getLabel(String id){
				if (labels!=null){
					String v=labels.get(id);
					if (v!=null) return v;
				}
				return id;
			}
			
			public void writerHeader(Writer w, Class cls,Map<String,Object> attrs) throws IOException {
				String fieldstr=up.getStr("fields","");
				List<String> flds = MapUtil.getSelectList(fieldstr);
				if (flds.size()==0){
					List<Field> fs = ReflectUtil.getFieldMap(cls);
					for (Field f:fs){
						if (f.getName().equals("uuid")) continue;
						if (f.getName().equals("old")) continue;
						if (f.getName().equals("dataService")) continue;
						fields.add(f.getName());
					}
				}
				fields.addAll(flds);
				{
					String hides=up.getStr("hides", null);
					if (hides!=null){
						List<String> hh=MapUtil.getSelectList(hides);
						for (String id:hh){
							fields.remove(id);
						}
						labels=(Map)attrs.get("labels");
					}
				}
				if (attrs!=null){
					Map<String,Boolean>hides=(Map)attrs.get("hides");
					if (hides!=null){
						for (String id:hides.keySet()){
							Boolean hide=hides.get(id);
							if (hide) fields.remove(id); else if (!fields.contains(id)) fields.add(id);
						}
					}
					labels=(Map)attrs.get("labels");
				}
				StringBuffer buf=new StringBuffer();
				for (String id:fields){
					String val=getLabel(id);
					if (buf.length()>0)buf.append(delimitedBy);
					if (withQuote) buf.append("\""+LightUtil.encodeCString(val.toString())+"\""); else buf.append(val.toString());
				}
				w.append(buf);
				w.append("\r\n");
			}

			public void writeRecord(Writer w,Object el) throws Exception {
				StringBuffer buf=new StringBuffer();
				for (String f:fields){
					Object val=ReflectUtil.getProperty(el, f);
					if (val==null) val="";
					if (buf.length()>0)buf.append(delimitedBy);
					if (withQuote) buf.append("\""+LightUtil.encodeCString(val.toString())+"\""); else buf.append(val.toString());
				}
				w.append(buf);
				w.append("\r\n");
			}
			
		}
		
		IDataService data=LightUtil.getDataService();
		JsonResult res=new JsonResult();
		String fn=up.getStr("fileName",CLASS.getSimpleName(getClass())+".csv");
		try {
			HttpServletResponse resp = up.getResponse();
			resp.setHeader("Content-Disposition","attachment;filename=\""+fn+"\"");
			resp.setContentType("text/plain");
			resp.setHeader("Cache-Control","maxage=3600");
			resp.setHeader("Pragma","public");
			//resp.setHeader("Cache-Control", "private");
			resp.setHeader("Accept-Ranges", "none");
			
			String specific=up.getStr("specific","{}"); if (specific.length()==0) specific="{}";
			Map<String,Object> spec=(Map)LightUtil.parseJson(specific);
			
			String text=up.getStr("text", "");
			if (!LightStr.isEmpty(text)) spec.put("text", text);
			
			ServletOutputStream out = resp.getOutputStream();
			Writer w=new OutputStreamWriter(out, LightUtil.UTF8);
			
			IBeanFactory bf = LightUtil.getBeanFactory();
			IDataEnumeration<? extends Entity> ee = data.enumeration(getClass(), LightUtil.toJsonString(spec).toString(), "", 50000, true);
			boolean headered=false;
			CsvExport exp=new CsvExport();
			int ii=0;
			while (ee.hasMoreElements()){
				ii++;
				up.setStatus(ii, ee.getAmount(), null);
				Map map=(Map)ee.nextElement();
				String xml=(String)map.get(IDataService.XML);
				Object el;
				if (xml==null){
					//if from Entity.getBeanList
					el=map;
				}
				else{
					Entity en=(Entity)bf.loadBean(xml);
					en.setDataService(data);
					el=en;	
				}
				if (!headered){
					headered=true;
					exp.writerHeader(w,getClass(),ee.getAttributes());
				}
				exp.writeRecord(w,el);
			}
			w.flush();
			
			res.setType(ResultType.Stream);
		} catch (Exception e) {
			res.setError(e);
			LogUtil.dumpStackTrace(e);
		}
		return res;
	}
	
	private void _doImport(IDataService data,Class cls,List<Map> list, Map<String,String> idMap) throws Exception{
		for (Map m:list){
			Entity en=(Entity)m.get("bean");
			if (en==null) throw new Exception("i18n.InvalidXml");
			if (!en.getClass().equals(cls)) throw new Exception("i18n.TypeMismatch");
			//change uuid to new
			String old=en.getUuid();
			String id=idMap.get(old); if (id==null){ id=LightUtil.getHashCode(); idMap.put(old, en.getUuid());} 
			en.setUuid(id);
			//change parentUuid to mapping one
			old=(String)ReflectUtil.getProperty(en, "parentUuid");
			if (old!=null){
				id=idMap.get(old); if (id==null){ id=LightUtil.getHashCode(); idMap.put(old, en.getUuid());}
				ReflectUtil.setProperty(en, "parentUuid", id);
			}
			//done
			en.setDataService(data);
			List l=en.getDataInMyKey(cls, 1, en);
			System.out.println("cls="+cls+",en="+LightUtil.toJsonObject(en));
			System.out.println("m="+m+",l="+l);
			if (l.size()>0) throw new Exception("Entity already exists: "+CLASS.getSimpleName(en.getClass())+", "+ThreadHelper.get("$$Keys")); //ignore if exist same key
			//Entity o0=data.getBean(cls, "uuid", en.getUuid());
			//if (o0!=null) continue; //throw new Exception("Entity already exists: "+BeanFactory.getBeanFactory().toXml(en, 0)); //ignore if exist same uuid
			data.insertBean(en);
			for (IReferentItem ri:en.getDescendantList()){
				l=(List)m.get(ri.getModel()+"List");
				if (l==null) continue;
				_doImport(data,CLASS.forName(LightUtil.getBeanFactory().findRealClass(ri.getModel())),l,idMap);
			}
		}
		
	}
	public JsonResult doImport(Upload up){
		JsonResult res=new JsonResult();
		IBeanFactory bf=LightUtil.getBeanFactory();
		
		try {
			String model=up.getStr("model", "");
			if (LightStr.isEmpty(model)) throw new Exception("i18n.InvalidTargetModel!");
			String name=bf.findRealClass(model);
			Class cls = CLASS.forName(name);
			if (LightStr.isEmpty(model)) throw new Exception("i18n.InvalidTargetModel!");

			String method=up.getStr("importMethod", "merge");
			FileItem it = up.getFileItem("importFile");
			InputStream is = it.getInputStream();
			StringBuffer buf=new StringBuffer();
			StreamUtil.loadStream(is, buf, LightUtil.UTF8);
			Object o=bf.loadBean(buf.toString());
			
			IDataService data=LightUtil.getDataService();
			dataService=data;
			
			if (!(o instanceof List)) throw new Exception("i18n.InvalidXml");
			List<Map> list=(List)o;
			System.out.println("RAWLIST="+bf.toXml(list, 1));
			Map<String,String> idMap=new HashMap<String,String>();
			_doImport(data,cls,list, idMap);
				
		} catch (Exception e) {
			res.setError(e);
			LogUtil.dumpStackTrace(e);
		}
		return res;
	}
	
	private void doTrace(String action) throws Exception{
		if (dataService.getTraceable()==false) return;
		if (!(this instanceof ITraceable)) return;
		String userId=LightUtil.getUserId();
		if ("sys".equals(userId)) return;
		if ("jobAgent".equals(userId)) return;
		if ("guest".equals(userId)) throw new Exception("#[InvalidOperation]: "+this.getClass().getName());
		if (userId.startsWith("job@")) return;
		
		IBeanFactory bf=LightUtil.getBeanFactory();
		String xml=bf.toXml(this, 1);
		ChangeLog bt=new ChangeLog();
		bt.setBeanId(uuid);
		bt.setBeanType(LightUtil.getCascadeName(getClass()));
		bt.setAction(action);
		bt.setBeanData(xml);
		bt.setTraceDate(LightUtil.longDate());
		bt.setUserId(userId);
		
		StringBuffer chg=new StringBuffer();
		List<Field> flds = ReflectUtil.getFieldMap(getClass());
		for (Field f:flds){
			if (f.getName().equals("uuid")) continue;
			if ((f.getModifiers()&152)!=0) continue; //16=final 8=static 128=transient
			changed(f,chg);
		}
		if ("update".equals(action) && chg.length()==0) return; //avoid empty save
		try{
			if ("update".equals(action)||"delete".equals(action)){
				Map m=getKeyMap();
				m.remove("$cacheModel");
				if (m.size()==0) m.put("uuid", uuid);
				if ("delete".equals(action))
					bt.setSummary("Where "+m);
				else
					bt.setSummary("SET "+chg.toString()+" Where "+m);
			}
			else{
				bt.setSummary("SET "+chg.toString());
			}
			dataService.insertBean(bt);
		}
		catch(Exception e){
			LogUtil.dumpStackTrace(e);
		}
	}

	public void beforeSearch(IDataService data, Map<String,Object> params, Map<String, Object> spec) throws Exception{
		
	}

	//return true means a reload for the search result is required,
	public boolean afterSearch(IDataService data, List list) throws Exception{
		return false;
	}
	
	public void beforeCommit(IDataService data, Upload up) throws Exception{
		
	}

	public void afterCommit(IDataService data, List<Entity> list) throws Exception{
		
	}
	
	public Comparable getMax(String spec, String attr) throws Exception{
		Comparable initial=null;
		List<? extends Entity> list=dataService.getBeanList(getClass(), spec, "", 0, 10000, null);
		for (Entity en:list){
			Comparable v=(Comparable)ReflectUtil.getProperty(en, attr);
			if (initial==null) initial=v;
			else{
				if (initial.compareTo(v)<0) initial=v;
			}
		}
		return initial;
	}
	
	public int getMaxInt(String spec, String attr) throws Exception{
		Integer max=(Integer)getMax(spec,attr);
		if (max==null) max=0;
		return max;
	}
	
	public long getMaxLong(String spec, String attr) throws Exception{
		Long max=(Long)getMax(spec,attr);
		if (max==null) max=0L;
		return max;
	}
	


	//chance for Entity to be self-exlained
	@Deprecated
	public void getBeanList(PageInfo page, Map<String,Object> terms, String text, String sort) throws Exception{
	}
	
	//chance to Entity to be self-explained, 
	public void getBeanList(AbstractResult result, Map<String, Object> terms,
			String text, String sortBy, int from, int size)  throws Exception{
	}
	
	public void dump(IDataService r1, IRepository f1, IDataService r2, IRepository f2) throws Exception{
		try{
			r2.insertBean(this);
		}
		catch (Exception e){
			e.printStackTrace();
			return;
		}
		if (this instanceof IFile){
			IFile f=(IFile)this;
			if (LightStr.isEmpty(f.getExternalId())) return;
			ExternalFile ef=r1.getBean(ExternalFile.class, "uuid", f.getExternalId());
			if (ef==null) return;
			ExternalFile exist=r2.getBean(ExternalFile.class, "uuid", f.getExternalId());
			if (exist==null) {
				r2.insertBean(ef);
			}
			InputStream is = f1.getResource(f.getExternalId());
			f2.saveResource(f.getExternalId(), is);
			is.close();
		}
		List<Field> flds = ReflectUtil.getFieldMap(this.getClass());
		for (Field fld:flds){
			if (IFile.class.isAssignableFrom(fld.getType())){
				IFile f=(IFile)ReflectUtil.getProperty(this, fld.getName());
				if (LightStr.isEmpty(f.getExternalId())) return;
				ExternalFile ef=r1.getBean(ExternalFile.class, "uuid", f.getExternalId());
				if (ef==null) return;
				ExternalFile exist=r2.getBean(ExternalFile.class, "uuid", f.getExternalId());
				if (exist==null) {
					r2.insertBean(ef);
				}
				InputStream is = f1.getResource(f.getExternalId());
				f2.saveResource(f.getExternalId(), is);
				is.close();
			}
		}
	}

	public void copyTo(Entity to) throws Exception{
		List<Field> flds = ReflectUtil.getFieldMap(getClass());
		for (Field f:flds){
			ReflectUtil.setProperty(to, f.getName(), ReflectUtil.getProperty(this, f.getName()));
		}
	}
	
	public <T> T copy() throws Exception{
		this.setUuid(LightUtil.getHashCode());
		if (this instanceof IFile){
			IFile f=(IFile)this;
			if (!LightStr.isEmpty(f.getExternalId())){
				ExternalFile ef=dataService.getBean(ExternalFile.class, "uuid", f.getExternalId());
				if (ef!=null){
					ef.setRefCount(ef.getRefCount()+1);
					dataService.updateBean(ef);	
				}
			}
		}
		return (T)this;
	}
	
	public String getInsertFilter() throws Exception{
		return uuid;
	}
	
	public IDataService getDs(){
		return dataService;
	}
	
	public AbstractResult doDownload(Upload up){
		JsonResult ret=new JsonResult();
		try {
			if (!(this instanceof IFile)) throw new Exception("NotDownloadable: "+getClass().getSimpleName());
			ret.setType(ResultType.Stream);
			IDataService ds = LightUtil.getDataService();
			IFile a=(IFile)ds.getBean(getClass(), "uuid", up.getStr("uuid", "~"));
			ExternalFile ef = ds.getBean(ExternalFile.class, "uuid", a.getExternalId());
			String fsId=up.getStr("fsId", LightUtil.getRepository().getFsId());
			IRepository fs = LightUtil.getBeanFactory().getRepository(fsId);
			HttpServletResponse resp = up.getResponse();
			resp.setHeader("Content-Disposition", "attachment;filename=\"" + a.getName() + "\"");
			resp.setContentType("application/octet-stream");
			if (ef!=null){
				InputStream is = ef.getResource(fsId);
				StreamUtil.streamOut(up, is, a.getExternalId(), ef.getSize(), fs.getLastModified(a.getExternalId()));
				is.close();
			}
			else{
				if (fs==null) throw new Exception("NotFound: "+fsId);
				InputStream is=fs.getResource(a.getExternalId());
				StreamUtil.streamOut(up, is, a.getExternalId(), null, fs.getLastModified(a.getExternalId()));
				is.close();
			}
		} catch (Exception e) {
			ret.setError(e);
			LogUtil.dumpStackTrace(e);
		}
		return ret;
	}
	
	public AbstractResult doDelete(Upload up){
		String id=up.getStr("uuid", "");
		JsonResult ret=new JsonResult();
		try{
			if (LightStr.isEmpty(id)) throw new Exception("Failed to delete.");
			IDataService ds = LightUtil.getDataService();
			Entity a=(Entity)ds.getBean(getClass(), "uuid", id);
			if (a!=null) ds.deleteBean(a);
		}
		catch (Exception e){
			ret.setError(e);
		}
		return ret;
	}

	private void _swap(IEntity m, IEntity b, String seqFld) throws Exception{
		Object old=ReflectUtil.getProperty(m, seqFld);
		Object cur=ReflectUtil.getProperty(b, seqFld);
		ReflectUtil.setProperty(m, seqFld, cur);
		ReflectUtil.setProperty(b, seqFld, old);
	}
	
	public void doChgOrder(Upload up, AbstractResult ret) throws Exception{
		String uuid=up.getStr("uuid", "~");
		Class cls=getPrimaryClass();
		IEntity m=dataService.getBean(cls, "uuid",uuid);
		if (m==null) return;
		
		List<Field> fields = ReflectUtil.getFieldMap(getClass());
		List<Field> keys=new ArrayList<Field>();
		SequenceId si=null;
		Field ff=null;
		for (Field f:fields){
			PrimaryKey pk=f.getAnnotation(PrimaryKey.class);
			if (pk!=null){
				keys.add(f);
			}
			else{
				SequenceId a=f.getAnnotation(SequenceId.class);
				if (a!=null){
					ff=f;
					si=a;
				}
			}
		}
		if (si==null) return;
		Object val=ReflectUtil.getProperty(this, ff.getName());
		String id=cls.getSimpleName()+"_"+ff.getName();
		
		String kk="",vv="";
		
		//if sequenceId has parameters, use it instead of pk
		if (si.value().length>0){
			keys.clear();
			for (String f:si.value()){
				Field fld=ReflectUtil.findField(fields, f);
				if (fld==null) throw new Exception("Field not found: "+f);
				keys.add(fld);
			}
		}
		else{
			//remove last key to make sure sequence works
			if (keys.size()>0) keys.remove(keys.size()-1);
		}
		
		//if (keys.size()==0) return;
		
		int dir=up.getInt("dir",0);
		
		String seqFld=ff.getName();
				
		StringBuffer filter=new StringBuffer();
		for (Field f:keys){
			Object v=ReflectUtil.getProperty(m, f.getName());
			if (v==null) continue;
			if (filter.length()>0) filter.append(",");
			filter.append(f.getName()+":'"+v+"'");
		}
		if (filter.length()>0) filter.append(",");
		
		List<IEntity> less=null; 
		if (dir==-2){
			filter.append(seqFld+":{lt:"+ReflectUtil.getProperty(m,seqFld)+"}");
			less=dataService.getBeanList(cls, "{"+filter+"}", "",seqFld+" asc");
			for (int i=less.size()-1;i>=0;i--){
				IEntity x=less.get(i);
				_swap(m,x,seqFld);
			}
			for (IEntity x:less){
				dataService.updateBean(x);
			}
			dataService.updateBean(m);
			less=null;
		}
		else
		if (dir==-1){
			filter.append(seqFld+":{lt:"+ReflectUtil.getProperty(m,seqFld)+"}");
			less=dataService.getBeanList(cls, "{"+filter+"}", "", 0,1,seqFld+" desc");
		}
		else
		if (dir==1){
			filter.append(seqFld+":{gt:"+ReflectUtil.getProperty(m,seqFld)+"}");
			less = dataService.getBeanList(cls, "{"+filter+"}", "", 0,1,seqFld+" asc");
		}
		else
		if (dir==2){
			filter.append(seqFld+":{gt:"+ReflectUtil.getProperty(m,seqFld)+"}");
			less=dataService.getBeanList(cls, "{"+filter+"}", "",seqFld+" asc");
			IEntity cur=m;
			for (int i=0;i<less.size();i++){
				IEntity x=less.get(i);
				_swap(m,x,seqFld);
			}
			for (IEntity x:less){
				dataService.updateBean(x);
			}
			dataService.updateBean(m);
			less=null;
		}
		if (less!=null && less.size()>0){
			IEntity b=less.get(0);
			_swap(m,b,seqFld);
			dataService.updateBean(b);
			dataService.updateBean(m);
		}
	}
}


