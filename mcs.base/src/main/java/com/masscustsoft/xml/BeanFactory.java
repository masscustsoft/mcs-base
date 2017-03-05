package com.masscustsoft.xml;

import java.io.File;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.masscustsoft.Lang.CLASS;
import com.masscustsoft.api.CategoryKey;
import com.masscustsoft.api.DateIndex;
import com.masscustsoft.api.FullBody;
import com.masscustsoft.api.FullText;
import com.masscustsoft.api.IBeanFactory;
import com.masscustsoft.api.ICleanup;
import com.masscustsoft.api.IDataService;
import com.masscustsoft.api.IRepository;
import com.masscustsoft.api.ISessionCleanup;
import com.masscustsoft.api.IndexKey;
import com.masscustsoft.api.Inject;
import com.masscustsoft.api.NumIndex;
import com.masscustsoft.api.TimestampIndex;
import com.masscustsoft.helper.BeanVersion;
import com.masscustsoft.model.Entity;
import com.masscustsoft.service.AbstractConfig;
import com.masscustsoft.service.BeanInterceptor;
import com.masscustsoft.service.ClasspathRepository;
import com.masscustsoft.service.FileRepository;
import com.masscustsoft.service.MemoryRepository;
import com.masscustsoft.util.GlbHelper;
import com.masscustsoft.util.InjectInfo;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.LogUtil;
import com.masscustsoft.util.ReflectUtil;
import com.masscustsoft.util.LightStr;
import com.masscustsoft.util.ThreadHelper;
import com.masscustsoft.xml.inner.CacheFactory;

/**
 * BeanFactory is XML Engine
 * 
 * <p>BeanFactory core engine for XML and Java instance conversion. Based on this engine, Java is XML and XML is Java.
 * <p>1. XML Tag represents a Java Class or a attribute of the class. In general, uppercased tag stands for Class while lowercased tag stands for attribute inside a class. 
 * Ex: we have class name ClassA, the the XML, <b>&lt;ClassA /&gt;</b>, represents a this class.
 * <p>2. Simple fields can be put as XML tag attribute or child tag. 
 * Ex: ClassA has a String 'name' field, you can define the XML <b>&lt;ClassA name='ABC'/&gt;</b> or <b>&ltClassA&gt;&lt;name&gt;ABC&lt;/name&gt;&lt;/ClassA&gt;</b>. 
 * Simple fields include String, double, integer, boolean and Date.
 * <p>3. For complex fields, need be defined as child tag: 
 * Ex: ClassA has a List field named 'list', you can define it as 
 * <b>&lt;ClassA&gt;&lt;list&gt;&lt;item&gt;a&lt;/item&gt;&lt;item&gt;b&lt;/item&gt;&lt;/list&gt;&lt;/Class&gt;</b>
 * <p>4. Above the sample for List introduced a *item* tag which is reserved for simple type. 
 * For complex Object, just use it's Class name. 
 * Ex: ClassA has a *children* field which is ClassB, the XML: 
 * <b>&lt;ClassA&gt;&lt;children&gt;&lt;ClassB .../&gt;&lt;ClassB .../&gt;&lt;/children&gt;&lt;/ClassA&gt;</b>
 * <p>5. for Map field, the key must be String, value can be simple type or complex Object. 
 * Ex: ClassA has a map field, Map<String,Object>, the XML: 
 * <b>&lt;ClassA&gt;&lt;map&gt;&lt;key&gt;1234&lt;/key&gt;&lt;childB&gt;&lt;ChildB .../&gt;&lt;/childB&gt;&lt;/map&gt;&lt;/ClassA&gt;</b>
 *  
 * @author JSong
 *
 */
public class BeanFactory implements IBeanFactory, XStreamListener{
	private transient Date lastModified=new Date();
	
	private CacheFactory global=new CacheFactory();
	
	public CacheFactory getStorage(){
		CacheFactory bs=(CacheFactory)ThreadHelper.get("cacheFactory");
		if (bs==null) bs=global;
		return bs;
	}
	
	private XStream x=new XStream();
		
	private Map<String,IRepository> repositories=Collections.synchronizedMap(new HashMap<String,IRepository>());
	private Map<String,IDataService> dataServices=Collections.synchronizedMap(new HashMap<String,IDataService>());
	
	private String id;
	
	/**
	 * Get the singleton instance of BeanFactory. 
	 * 
	 * <p>By default, default packages are added so all classes in these package are ready to use in XML as tag.
	 */
	public static synchronized BeanFactory getBeanFactory(){
		return (BeanFactory)ThreadHelper.get("beanFactory");
	}
	
	public static synchronized BeanFactory initBeanFactory(String bfId){
		if (bfId==null) bfId="";
		BeanFactory bf=(BeanFactory)GlbHelper.get("$BeanFactory$"+bfId);
		if (bf==null){
			System.out.println("newBF "+bfId);
			
			bf=new BeanFactory(bfId);
			GlbHelper.set("$BeanFactory$"+bfId,bf);
			bf.addPackage("com.masscustsoft.app");
			bf.addPackage("com.masscustsoft.service");
			bf.addPackage("com.masscustsoft.client");
			bf.addPackage("com.masscustsoft.util");
			bf.addPackage("com.masscustsoft.xml");
			bf.addPackage("com.masscustsoft.module");
			bf.addPackage("com.masscustsoft.model");
			System.out.println("newBF "+bf.getPackages());
		}
		ThreadHelper.set("beanFactory",bf);
		return bf;
	}
	
	public BeanFactory(String id){
		this.id=id;
		x.setListener(this);
		this.addPackage(CLASS.getPackageName(Entity.class));
		addAnnotation(IDataService.INDEX_, IndexKey.class);
		addAnnotation(IDataService.CATEGORY_, CategoryKey.class);
		addAnnotation(IDataService.FULLTEXT_, FullText.class);
		addAnnotation(IDataService.FULLBODY_, FullBody.class);
		addAnnotation(IDataService.NUMINDEX_, NumIndex.class);
		addAnnotation(IDataService.DATEINDEX_, DateIndex.class);
		addAnnotation(IDataService.TIMESTAMP_, TimestampIndex.class);
	}
	
	public void addFileRepository(String fsId, String folder, int folderLevel, Boolean restricted){
		FileRepository fd=new FileRepository();
		fd.setFolderLevel(folderLevel);
		fd.setRootFolder(folder);
		fd.setRestricted(restricted);
        fd.setFsId(fsId);
        fd.initialize();
	}
	
	public ClasspathRepository addClassPathRepository(String fsId, String prefix,boolean external){
		ClasspathRepository fd=new ClasspathRepository();
		fd.setPrefix(prefix);
        fd.setFsId(fsId);
        fd.setExternal(external);
        fd.initialize();
        return fd;
	}
	
	public void addRepository(String fsId, IRepository fd){
		fd.setFsId(fsId);
		repositories.put(fsId, fd);
		if (fd instanceof ICleanup || fd instanceof ISessionCleanup)GlbHelper.set(fsId, fd);
	}
	
	public void removeRepository(String fsId) throws Exception{
		IRepository r = getRepository(fsId);
		if (r!=null) r.destroy();
		repositories.remove(fsId);
        GlbHelper.remove(fsId);
	}
	
	public void addDataService(String dsId, IDataService fd){
		dataServices.put(dsId, fd);
        if (fd instanceof ICleanup || fd instanceof ISessionCleanup)GlbHelper.set(dsId, fd);
	}
	
	@Override
	public void removeDataService(String dsId) throws Exception{
		IDataService ds = this.getDataService(dsId);
		if (ds!=null){
			ds.destroy();
		}
		dataServices.remove(dsId);
        GlbHelper.remove(dsId);
	}
	
	public IRepository getRepository(String fsId) {
		if (LightStr.isEmpty(fsId)) fsId="defaultFs";
		
		IRepository bd=repositories.get(fsId);
		if (bd!=null) return bd;
		
		int i=fsId.indexOf("-");
		if (i>=0){
			String base=fsId.substring(0,i);
			String sub=fsId.substring(i+1);
			IRepository rep=getRepository(base);
			if (rep!=null && rep instanceof FileRepository){
				FileRepository f=(FileRepository)rep;
				FileRepository subRep=new FileRepository();
				subRep.setFolderLevel(f.getFolderLevel());
				subRep.setRootFolder(new File(new File(f.getRootFolder()),sub).getAbsolutePath());
				subRep.setFsId(fsId);
				subRep.initialize();
				return subRep;
			}
		}
		if (fsId.equals("classpath")){
			bd=new ClasspathRepository();
			bd.setFsId(fsId);
			bd.initialize();
			return bd;
		}
		if (fsId.equals("mem")){
			bd=new MemoryRepository();
			bd.setFsId(fsId);
			bd.initialize();
			return bd;
		}
		return null;
	}
	
	@Override
	public IDataService getDataService(String dsId) {
		if (LightStr.isEmpty(dsId)) dsId="default";
		IDataService bd=dataServices.get(dsId);
		if (bd!=null) return bd;
		return null;
	}
	
	@Override
	public Object getBean(String fsId, String beanName) throws Exception{
		BeanProxy proxy=getBeanProxy(fsId,beanName);
		if (proxy==null) return null;
		proxy.setExternal(true);
		
		AbstractConfig cfg = LightUtil.getCfg();
		if (cfg!=null){
			for (BeanInterceptor bi:cfg.getBeanInterceptors()){
				bi.inject(this, proxy);
			}
		}
		
		return proxy.getBean();
	}
	
	private <T extends Object> BeanProxy<T> getBeanProxy(Class<T> cls, String id, InjectInfo ib) throws Exception {
		return getBeanProxy(getRepository("mem"),cls.getSimpleName()+"/"+id.replace(':', '-'),ib);
	}
	
	public <T extends Object> T getBean(Class<T> cls, String id) throws Exception {
		return (T)getBean("mem",cls.getSimpleName()+"/"+id.replace(':', '-'));
	}
	
	public <T extends Object> T getBean(Class<T> cls, Object p, String id) throws Exception {
		BeanProxy bp=getBeanProxy(p);
		String newId=bp.getShortId()+id;
		return (T)getBean("mem",cls.getSimpleName()+"/"+newId);
	}

	public BeanProxy getBeanProxy(String fsId, String beanName) throws Exception{
		return getBeanProxy(fsId,beanName,null);
	}
	
	public IRepository getRepository(String fsId, String beanName){
		if (beanName!=null){
			int idx=beanName.indexOf(":");
			if (idx>0){
				fsId=beanName.substring(0,idx);
				beanName=beanName.substring(idx+1);
			}
		}
		IRepository dep = getRepository(fsId);
		return dep;
	}
	
	public InputStream getResource(String fsId, String beanName) throws Exception{
		if (beanName!=null){
			int idx=beanName.indexOf(":");
			if (idx>0){
				fsId=beanName.substring(0,idx);
				beanName=beanName.substring(idx+1);
			}
		}
		IRepository dep = getRepository(fsId);
		return dep.getResource(beanName);
	}
	
	public String getPureBeanName(String beanName){
		if (beanName==null) return null;
		int idx=beanName.indexOf(":");
		if (idx>0){
			beanName=beanName.substring(idx+1);
		}
		return beanName;
	}
	
	private BeanProxy getBeanProxy(String fsId, String beanName, InjectInfo ib) throws Exception {
		IRepository dep = getRepository(fsId,beanName);
		String bean=getPureBeanName(beanName);
		if (dep!=null){
			return getBeanProxy(dep,bean,ib);
		}
		else {
			throw new Exception("Resource Failure: dep="+dep+", bean="+beanName+", "+bean);
			//return null;
		}
	}
	
	public BeanProxy getBeanProxy(IRepository dep, String beanName) throws Exception{
		return getBeanProxy(dep,beanName,null);
	}
	
	private BeanProxy getProxyById(String id){
		BeanProxy bp=getStorage().get(id);
		if (bp==null) bp=global.get(id);
		return bp;
	}
	
	private synchronized BeanProxy  getBeanProxy(IRepository dep, String beanName, InjectInfo ib) throws Exception{
		int idx=beanName.indexOf("@");
		if (idx>=0){
			BeanProxy proxy=getBeanProxy(dep,beanName.substring(0,idx));
			if (proxy==null) return null;
			Object bean=proxy.getBean();
			Object prop=ReflectUtil.getProperty(bean,beanName.substring(idx+1));
			proxy=this.getBeanProxy(prop);
			return proxy;
		}
		Map<String, String> runnings = getStorage().runnings;
		String fullName=dep.getFsId()+":"+beanName; //endswith / means external
		BeanProxy proxy = getProxyById(fullName);
		if (proxy == null){
			while (runnings.get(fullName) != null) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					break;
				}
				proxy = getProxyById(fullName);
				if (proxy != null) break;
			}
			if (proxy==null){
				runnings.put(fullName, "1");
				LogUtil.Inc(1);
				//LogUtil.info("loadBean name="+fullName);
				long ver = dep.getLastModified(beanName+".xml");
				loadBean(dep,beanName,ver);
				runnings.remove(fullName);
				proxy = getProxyById(fullName);
				//process Inject and Inject Initialize
				//since on inner already injected once
				//if (proxy!=null) doFullInject(dep,fullName,proxy.getBean(),ib);
				LogUtil.Inc(-1);
				
				//dump all beans with version
				//System.out.println("---- BeanId: "+fullName+" ver="+proxy.getLastModified()+", "+(proxy.getLastModified()==LightUtil.bootupTime));
			}
		}
		return proxy;
	}
	
	private void doInjectP(Object inj, String fullname, String[] p) throws Exception{
		if (p==null || inj==null) return;
		Class c=inj.getClass();
		List<Field> imap = ReflectUtil.getFieldMap(c);
		//for p
		for (String ss:p){
			int i=ss.indexOf("=");
			if (i<0) continue;
			String sa=ss.substring(0,i);
			String sb=ss.substring(i+1);
			Field ifld=ReflectUtil.findField(imap,sa);
			if (ifld==null) continue;
			if (ifld.getType().isAssignableFrom(List.class)){ //if attribute is list, value can be comma string list, support #{} macro
				String[] s3=sb.split(",");
				try {
					List list=(List)ReflectUtil.getProperty(inj, ifld.getName());
					for (String s4:s3){
						Object s1=LightUtil.macro(s4,'#');
						if (LightUtil.isPrimitive(ifld.getType())){
							s1=LightUtil.decodeObject(s1.toString(),ifld.getType(),false);
						}
						if (s1 instanceof List) {
							list.addAll((List)s1);
						}
						else {
							list.add(s1);
						}
					}
				} catch (Exception e) {
					LogUtil.info("ERR: Set List Prop '"+sa+"' to '"+sb+"'! "+e.getMessage());
					LogUtil.dumpStackTrace(e);
				}
			}
			else{
				Object s1=LightUtil.macro(sb,'#');
				if (LightUtil.isPrimitive(ifld.getType())){
					s1=LightUtil.decodeObject(s1.toString(),ifld.getType(),false);
				}
				//LogUtil.info("Inject fld="+ifld.getName()+" val="+s1);
				try {
					ReflectUtil.setProperty(inj, sa, s1);
				} catch (Exception e) {
					LogUtil.info("ERR: Set Prop "+sa+" to "+s1+"! "+e.getMessage());
					LogUtil.dumpStackTrace(e);
				}
			}
		}
	}
	
	private void doInjectList(Object inj, String fullname, List<InjectInfo> list) throws Exception{
		if (list==null || inj==null) return;
		//for list
		for (InjectInfo ii:list){
			if (ii.attr.length()>0){
				//a attr defination
				//String name=fullname+"_"+ii.attr;
				//if (ii.share) name=ii.id;
				Object o=getInjectBean(ii,fullname,ii.attr,ii.clazz); //find prop named attr under cls with p set
				//if (ii.ref.length()>0) o=this.getBean(ii.protocol, ii.ref);
				//else o=this.getBean(ii.clazz, fixFullName(name), ii);
				try {
					ReflectUtil.setProperty(inj, ii.attr, o);
				} catch (Exception e) {
					LogUtil.info("ERR: Set Annotation Prop "+ii.attr+" to "+o+"! "+e.getMessage());
					LogUtil.dumpStackTrace(e);
				}
			}
			else{
				try {
					Object s1= ReflectUtil.getProperty(inj, "list"); //if no attr set, try to append to list
					if (s1!=null && s1 instanceof List){
						List ls=(List)s1;
						//String name=fullname+"_"+ls.size();
						//if (ii.share) name=ii.id;
						Object o=getInjectBean(ii,fullname,ls.size()+"",ii.clazz);
						//if (ii.ref.length()>0) o=this.getBean(ii.protocol, ii.ref);
						//else o=this.getBean(ii.clazz, fixFullName(name), ii);
						ls.add(o);
					}
					else LogUtil.info("ERR: not a list! inj="+inj+",list="+s1+",iblist="+list);
				} catch (Exception e) {
					LogUtil.info("ERR: Set Annotation List failed: inj='"+inj+"'! "+e.getMessage());
					LogUtil.dumpStackTrace(e);
				}
			}
		}
	}
	
	private Object getInjectBean(InjectInfo ii, String fullName, String fldName, Class fldType) throws Exception{
		BeanProxy inj;
		if (ii.ref.length()>0){ // if ref is set, get external bean 
			inj=getBeanProxy(ii.fsId, ii.ref, ii);
		}
		else {
			String newId;
			if (ii.share){
				newId=ii.id;
			}
			else{
				if (ii.id.length()>0) {
					if (ii.id.startsWith("/")){
						int idx=fullName.indexOf("/");
						if (idx>=0) newId=fullName.substring(0,idx)+"/"+ii.id;
						else newId=fullName+"/"+ii.id;
					}
					else
					newId=fullName+"/"+ii.id;
				}
				else newId=fullName+"/"+fldName; //fullname is the parent beanId
			}
			//System.out.println("INJ newid="+newId+", type="+fldType.getSimpleName());
			inj=getBeanProxy(fldType, newId, ii);
			//System.out.println("INJ ="+inj);
		}
		return inj.getBean();
	}
	
	private void doPropertyInject(String fsId,String fullName, Object obj) throws Exception{
		//LogUtil.log(this, "doInject for "+obj+", fullname="+fullName);
		if (obj==null) return;
		Class<?> c = obj.getClass();
		List<Field> map = ReflectUtil.getFieldMap(c);
		for (Field fld : map) {
			if (fld.isAnnotationPresent(Inject.class)) {
				//LogUtil.info("inject "+fullName+" fld="+fld.getName());
				Inject ib=fld.getAnnotation(Inject.class);
				InjectInfo ii=new InjectInfo(fsId,ib);
				Object lastThis=ThreadHelper.get("me");
				ThreadHelper.set("me", obj);
				Object tar=ReflectUtil.getProperty(obj, fld.getName());
				//System.out.println("fld="+fld.getName()+",tar="+tar);
				if (tar==null){
					Object inj=getInjectBean(ii,fullName,fld.getName(),fld.getType());
					try {
						ReflectUtil.setProperty(obj, fld.getName(), inj);
					} catch (Exception e1) {
						LogUtil.error("ERR: Set Prop "+fld.getName()+" to "+inj+"! "+e1.getMessage());
					}
					//process parameter support
					//process injectBeanInit method 
				}
				ThreadHelper.set("me", lastThis);
			}
		}
	}

	private void doFullInject(IRepository dep, String fullName, Object obj, InjectInfo ib) throws Exception{
		if (dep==null) return;
		//process Inject and Inject Initialize
		InjectInfo jb=null;
		if (obj!=null){
			Class c=obj.getClass();
			if (c.isAnnotationPresent(Inject.class)){
				Inject cb=(Inject) c.getAnnotation(Inject.class);
				jb=new InjectInfo(dep.getFsId(),cb);
			}
		}
		//p first
		if (ib!=null) doInjectP(obj,fullName,ib.p); //inner inject on class first
		if (jb!=null) doInjectP(obj,fullName,jb.p); //outer inject
		//list second
		if (ib!=null) doInjectList(obj,fullName,ib.list);
		if (jb!=null) doInjectList(obj,fullName,jb.list);
		
		//property inject
		doPropertyInject(dep.getFsId(),fullName,obj);
		
		//after injection, do a init()
		try {
			Method m=ReflectUtil.getMethod(null, obj,"initialize");
			if (m!=null) m.invoke(obj);
		} catch (Exception e){
			LogUtil.dumpStackTrace(e);
			LogUtil.error(e.getMessage());
		}

	}

	public BeanProxy getBeanProxy(Object obj){
		BeanProxy p=getStorage().getByObj(obj);
		if (p==null) p=global.getByObj(obj);
		return p;
	}
	
	public void putBean(String name,Object obj, long tm){
		if (name==null||obj==null||name.indexOf("$$$")>=0) return; //temp id, no saving
		getStorage().put(name, obj, tm);
	}
	
	protected void removeBean(String name){
		getStorage().remove(name);
		global.remove(name);
	}
	
	protected void loadBean(IRepository bd,String name,long tm) throws Exception{
		if (bd==null) {
			LogUtil.info("no bean found for "+name);
			return;
		}
		//LogUtil.debug("loadBean "+bd.getFsId()+","+name);
		Object obj=null;
		String xml;
		String id=bd.getFsId()+":"+name;
		if (bd instanceof MemoryRepository){
			int i=name.indexOf("/");
			String cls=name;
			if (i>=0){
				cls=name.substring(0,i);
				if (cls.indexOf('.')==-1){
					cls=this.findRealClass(cls);
				}
			}
			try {
				obj=com.masscustsoft.Lang.CLASS.forName(cls).newInstance();
			} catch (Exception e) {
				obj=null;
				LogUtil.info("Class not found:"+e.getMessage()+", name="+name);
			}
			putBean(id,obj,tm);
			//missing injection
			doFullInject(bd, id, obj, null);
			return;
		}
		else{
			xml=bd.getXml(name);
		}
		if (xml==null || xml.length()==0) return; // if no bean found, return directly.
		try{
			x.fromXml(id,xml,new BeanVersion(tm));
		}
		catch (Exception e){
			System.out.println("Load XML failed. name="+name);
			throw e;
		}
	}

	public Object loadBean(String id,String xml,long tm) throws Exception {
		Object obj = x.fromXml(id,xml,new BeanVersion(tm));
		if (obj==null) throw new Exception("Xml not a bean: "+xml);
		return obj;
	}

	public Object loadBean(String xml) throws Exception {
		if (LightStr.isEmpty(xml)) return null;
		return loadBean((String)null,xml,LightUtil.bootupTime);
	}
	
	//op compact=1 verbose=0 
	public String toXml(Object obj,int op) {
		String xml=x.toXml(obj,op);
		return xml;
	}

	public void saveBean(String fsId, String beanName, Object obj) throws Exception{
		int idx=beanName.indexOf(":");
		if (idx>0){
			fsId=beanName.substring(0,idx);
			beanName=beanName.substring(idx+1);
		}
		IRepository dep = getRepository(fsId);
		if (dep!=null){
			saveBean(dep,beanName,obj);
		}
		else LogUtil.info("invalid fdId to save bean!");
	}

	public void saveBean(IRepository bd, String beanName, Object obj) throws Exception{
		String xml=toXml(obj,0);
		//System.out.println("xml="+xml);
		bd.saveXml(beanName, xml);
	}

	public Map<String, IRepository> getRepositories() {
		return repositories;
	}

	public Date getLastModified() {
		return lastModified;
	}

	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}
	
	//xml->node
	@Override
	public Object onExternalBean(String id, Object parent,String ref, BeanVersion ver) throws Exception{
		IRepository bd=this.getRepository("syscore",id);
		IRepository rep=this.getRepository(bd.getFsId(), ref);
		String raw=this.getPureBeanName(ref);
		String name=LightUtil.macroStr(raw);
		
		String tar=rep.getFsId()+":"+name;
		//System.out.println("external tar="+tar);
		BeanProxy p=getStorage().get(tar);
		if (p==null){
			int dot=name.indexOf(".");
			if (dot>0) name=name.substring(0,dot);
			Object o=getBean(rep.getFsId(), name);
			p=getStorage().get(tar);
		}
		if (p.getLastModified()>ver.getLastModified()) ver.setLastModified(p.getLastModified());
		if (p==null) throw new Exception("Missing "+tar+", ref="+ref);
		
		if (!name.equals(raw)){
			return varantAgent(rep,raw,p.getBean());
		}
		else
		return p.getBean();
	}

	@Override
	public void cacheBean(String id,Object obj,BeanVersion ver) throws Exception {
		if (id==null||obj==null) return;
		if (obj instanceof Map) return;
		putBean(id, obj, ver.getLastModified());
		//System.out.println("CacheBean "+id);
		this.doFullInject(this.getRepository("syscore", id), id, obj, null);
	}

	public void addPackage(String pkg){
		System.out.println("addPkg "+pkg+" to "+id);
		x.addPackage(pkg);
	}
	
	public void removePackage(String pkg){
		x.removePackage(pkg);
	}
	
	public void addAnnotation(String prefix,Class<? extends Annotation> a){
		x.addAnnotation(prefix, a);
	}
	
	public String findRealClass(String name){
		if (name==null) return null;
		return x.findRealClass(name);
	}

	@Override
	public boolean onToNode(Object parent,Object bean, XmlNode node, boolean fullPath){
		if (parent==null) return false;
		BeanProxy proxy = getBeanProxy(bean);
		if (proxy==null) return false;
		if (proxy.getExternal()){
			if (LightStr.isEmpty(node.getTag())){
				node.setTag(CLASS.getSimpleName(bean.getClass()));
			}
			if (parent==null){
				node.setAttribute("ref", proxy.getBeanId());
			}
			else{
				BeanProxy p=getBeanProxy(parent);
				if (p!=null && p.getFsId().equals(proxy.getFsId())){
					node.setAttribute("ref", fullPath?proxy.getBeanId():proxy.getName());
				}
				else{
					node.setAttribute("ref", proxy.getBeanId());
				}
			}
		}
		return false;
	}

	public String getEncodeName(Field f){
		return x.encodeName(f);
	}
	
	public <T> T clone(T from, String newId) throws Exception{
		//if id starts with _
		String id=newId;
		if (newId!=null && newId.startsWith("_")) {
			BeanProxy p=getBeanProxy(from);
			if (p==null) throw new Exception("Bean is not clonable!");
			id=p.getBeanId()+newId;
			BeanProxy proxy=(BeanProxy)getStorage().get(id);
			if (proxy!=null) return (T)proxy.getBean();
		}
		
		XmlNode node=new XmlNode();
		x.toNode(null, from, null, node, true);
		
		T val=(T)x.fromNode(null,node,id,null,new BeanVersion(LightUtil.bootupTime));
		if (val instanceof Entity){
			Entity en=(Entity)val;
			if (!"keep".equals(id) && LightStr.isEmpty(id)) en.setUuid(LightUtil.getHashCode());
		}
		try {
			Method m=ReflectUtil.getMethod(null, val,"initialize");
			if (m!=null) m.invoke(val);
		} catch (Exception e){
			throw e;
		}
		return val;
	}

	public <T> T getSubBean(Object host, Class<T> clazz, String postfix) throws Exception {
		BeanProxy proxy = getBeanProxy(host);
		return getBean(clazz,proxy.getBeanId()+postfix);
	}
	
	//create a proxy extend from bean and extend all methods but call from current macro-ed bean
	private Object varantAgent(IRepository rep, String raw, Object bean) {
		
		return null;
	}

	public Map<String, IDataService> getDataServices() {
		return dataServices;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<String> getPackages(){
		return x.packages;
	}

}

