package com.masscustsoft.service;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

import com.masscustsoft.Lang.CLASS;
import com.masscustsoft.api.IDataService;
import com.masscustsoft.api.IRepository;
import com.masscustsoft.helper.Upload;
import com.masscustsoft.model.AbstractResult;
import com.masscustsoft.model.Entity;
import com.masscustsoft.util.LightStr;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.LogUtil;
import com.masscustsoft.util.MapUtil;
import com.masscustsoft.util.ReflectUtil;
import com.masscustsoft.util.StreamUtil;
import com.masscustsoft.util.ThreadHelper;
import com.masscustsoft.xml.BeanFactory;

public class DirectConfig extends AbstractConfig {
	String defaultFsId, defaultDsId, defaultSysId, initId, resourceId;

	Integer heartBeat;

	String pingable, secureSessionId;

	List<IRepository> repositories = new ArrayList<IRepository>();

	List<IDataService> dataServices = new ArrayList<IDataService>();

	DirectSessionService sessionService = new DirectSessionService();

	String languageMapping; //en_CA=en; en_GB=en; en_US=en; zh_CN
	String dateFmtMapping; //en_CA=M d, Y; en=M/d/Y; zh_CN=Y年m月d日
	String numberFmtMapping; //en=#,###.##; zh=#,###.##
	String timeFmtMapping; //en_CA=H:i; en=h:iA; zh=H:i
	
	transient String id;

	// package=[]
	transient Map<String, List<DirectDispatch>> dispatchMap = Collections
			.synchronizedMap(new HashMap<String, List<DirectDispatch>>());

	transient BeanFactory beanFactory;

	transient List<UIENTRY> uiEntries = new ArrayList<UIENTRY>();
	transient List<HOOK> hooks = new ArrayList<HOOK>();
	transient Map<String, Feature> featureMap = new TreeMap<String, Feature>();
	transient Map<String, DirectAction> actionMap = new TreeMap<String, DirectAction>();
	
	transient Map<String, Long> itemCaches=new HashMap();
	transient Map<String,String> languages,dateFmts,numberFmts,timeFmts;
	
	
	public void init() throws Exception {
		String modules=supportedModules;
		
		//step 1 plugin to get all classpath set
		loadPlugins(modules);
		
		loadUiPlugins(modules);
		
		Long def=LightUtil.bootupTime%1000;
		for (DirectAction a:actionMap.values()){
			if (a.getCacheable()!=null && a.getCacheable()){
				itemCaches.put(a.getId(), def);
			}
		}
	}

	public static void loadPlugins(String supported) throws Exception{
		ClassLoader cl = CLASS.getLoader();
		BeanFactory bf=BeanFactory.getBeanFactory();
		
		List<String> sup=MapUtil.getSelectList(supported);
		
		LogUtil.info("LOAD PLUGIN");
		Enumeration<URL> all = cl.getResources("META-INF/PLUGIN.xml");
		for (;all.hasMoreElements();){
			URL u=all.nextElement();
			int idx=LightUtil.isSupportedModule(sup,u.getPath());
			if (supported!=null && idx==0) continue;
			InputStream is = u.openStream();
			StringBuffer buf=new StringBuffer();
			StreamUtil.loadStream(is, buf, LightUtil.UTF8);
			PLUGIN plug=(PLUGIN)bf.loadBean(buf.toString());
			for (IDataService d:plug.getDataServices()){
				bf.addDataService(d.getDsId(), d);
			}
			for (IRepository r:plug.getRepositories()){
				bf.addRepository(r.getFsId(), r);
			}
			for (String pkg:plug.getPackages()){
				bf.addPackage(pkg);
			}
		}
	}
	public void loadUiPlugins(String supported) throws Exception {
		ClassLoader cl = CLASS.getLoader();
		BeanFactory bf = BeanFactory.getBeanFactory();
		bf.addClassPathRepository("res", "com/masscustsoft/sys",false);
		
		List<String> sup = MapUtil.getSelectList(supported);

		LogUtil.info("LOAD UI PLUGIN sup="+sup);
		Enumeration<URL> all = cl.getResources("META-INF/UIPLUGIN.xml");
		for (; all.hasMoreElements();) {
			URL u = all.nextElement();
			LogUtil.info("PLUGIN support: "+sup+","+u.getPath());
			int idx = LightUtil.isSupportedModule(sup, u.getPath());
			if (idx == 0)
				continue;
			LogUtil.info("LOAD UI PLUGIN: "+u.getPath());
			InputStream is = u.openStream();
			StringBuffer buf = new StringBuffer();
			StreamUtil.loadStream(is, buf, LightUtil.UTF8);
			UIPLUGIN plug = (UIPLUGIN) bf.loadBean(buf.toString());
			for (DirectAction a : plug.getActions()) {
				a.init(this);
			}
			uiEntries.addAll(plug.getUiEntries());
			hooks.addAll(plug.getHooks());
			for (Feature d : plug.getFeatures()) {
				featureMap.put(d.getFeatureId(), d);
			}
			for (DirectAction a : plug.actions) {
				a.init(this);
			}
		}
	}

	@Override
	public void initThread() {
		ThreadHelper.set("$Cfg$", this);
		ThreadHelper.set("$Ds$", getDs(null));
		ThreadHelper.set("$Fs$", getFs(null));
		BeanFactory.initBeanFactory(this.getBeanFactory().getId());
	}

	private DirectDispatch lookup(String pkg, String act, AbstractResult ret) throws Exception {
		List<DirectDispatch> lst = dispatchMap.get(pkg);
		if (lst == null)
			return null;

		String method = "do" + LightStr.capitalize(act);
		Method m = null;
		for (DirectDispatch dd : lst) {
			m = ReflectUtil.getMethod(dd.getClass(), dd, method, AbstractResult.class);
			if (m != null)
				return dd;
		}
		return null;
	}

	public DirectAction doProcess(String cmd, AbstractResult ret) throws Exception {
		String act = cmd;

		Upload up = Upload.getUpload();

		boolean ok=false;
		DirectAction a = actionMap.get(act);
		if (a != null) {
			a._run(ret);
			ok=true;
		}
		String names = up.getStr("plugins", "");
		if (!LightStr.isEmpty(names)) {
			DirectAction p = actionMap.get("getPlugins");
			if (p!=null){
				p._run(ret);
			}
		}
		if (ok) return a;

		throw new Exception("#[UnknownRequest]:" + act);
	}

	public IDataService getDs(String dsId) {
		if (dsId == null)
			dsId = this.defaultDsId;
		return beanFactory.getDataService(dsId);
	}

	public IRepository getFs(String fsId) {
		if (fsId == null)
			fsId = this.defaultFsId;
		return beanFactory.getRepository(fsId);
	}

	public List<IRepository> getRepositories() {
		return repositories;
	}

	public void setRepositories(List<IRepository> repositories) {
		this.repositories = repositories;
	}

	public List<IDataService> getDataServices() {
		return dataServices;
	}

	public void setDataServices(List<IDataService> dataServices) {
		this.dataServices = dataServices;
	}

	public String getDefaultFsId() {
		return defaultFsId;
	}

	public void setDefaultFsId(String defaultFsId) {
		this.defaultFsId = defaultFsId;
	}

	public String getDefaultDsId() {
		return defaultDsId;
	}

	public void setDefaultDsId(String defaultDsId) {
		this.defaultDsId = defaultDsId;
	}

	public DirectSessionService getSessionService() {
		return sessionService;
	}

	public void setSessionService(DirectSessionService sessionService) {
		this.sessionService = sessionService;
	}

	public BeanFactory getBeanFactory() {
		return beanFactory;
	}

	public String getPingable() {
		return pingable;
	}

	public void setPingable(String pingable) {
		this.pingable = pingable;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	public String getSecureSessionId() {
		return secureSessionId;
	}

	public void setSecureSessionId(String secureSessionId) {
		this.secureSessionId = secureSessionId;
	}

	public Integer getHeartBeat() {
		return heartBeat;
	}

	public void setHeartBeat(Integer heartBeat) {
		this.heartBeat = heartBeat;
	}

	public List<UIENTRY> getUiEntries() {
		return uiEntries;
	}

	public UIENTRY getUIEntryByName(String name) {
		for (UIENTRY en : uiEntries) {
			if (en.getName().equals(name)) {
				return en;
			}
		}
		return null;
	}

	public void getHooks(DirectComponent dd, String hook, Map m) throws Exception {
		for (HOOK h : hooks) {
			if (h.getName().equals(hook))
				if (h.accept(dd))
					h.doGet(dd, m);
		}
	}

	public void setHooks(DirectComponent dd, String hook, Entity en) throws Exception {
		for (HOOK h : hooks) {
			if (h.getName().equals(hook))
				if (h.accept(dd))
					h.doSet(dd, en);
		}
	}

	public void clearHooks(DirectComponent dd, String hook, Entity en) throws Exception {
		for (HOOK h : hooks) {
			if (h.getName().equals(hook))
				if (h.accept(dd))
					h.doClear(dd, en);
		}
	}

	public Map<String, Feature> getFeatureMap() {
		return featureMap;
	}

	public Map<String, DirectAction> getActionMap() {
		return actionMap;
	}

	public String getInitId() {
		return initId;
	}

	public void setInitId(String initId) {
		this.initId = initId;
	}

	public String getDefaultSysId() {
		return defaultSysId;
	}

	public void setDefaultSysId(String defaultSysId) {
		this.defaultSysId = defaultSysId;
	}

	public String getResourceId() {
		return resourceId;
	}

	public void setResourceId(String resourceId) {
		this.resourceId = resourceId;
	}
	
	public String getAppVersion(){
		String ver=(String)getVars().get("AppVersion");
		if (LightStr.isEmpty(ver)) ver=LightUtil.bootupTime+"";
		return ver;
	}

	public Map<String, Long> getItemCaches() {
		return itemCaches;
	}

	public synchronized void incVersion(Class c) throws Exception {
		for (DirectAction a:actionMap.values()){
			if (a.getCacheable()!=null && a.getCacheable()){
				if (a.isRelated(c)){
					itemCaches.put(a.getId(),itemCaches.get(a.getId())+1);
				}
			}
		}
	}
	

	public String getLanguageMapping() {
		return languageMapping;
	}

	public void setLanguageMapping(String languageMapping) {
		this.languageMapping = languageMapping;
	}

	public String getDateFmtMapping() {
		return dateFmtMapping;
	}

	public void setDateFmtMapping(String dateFmtMapping) {
		this.dateFmtMapping = dateFmtMapping;
	}

	public String getNumberFmtMapping() {
		return numberFmtMapping;
	}

	public void setNumberFmtMapping(String numberFmtMapping) {
		this.numberFmtMapping = numberFmtMapping;
	}

	public String getTimeFmtMapping() {
		return timeFmtMapping;
	}

	public void setTimeFmtMapping(String timeFmtMapping) {
		this.timeFmtMapping = timeFmtMapping;
	}
	
	private Map<String,String> parseMapping(String mapping, String defs) {
		if (LightStr.isEmpty(mapping)) mapping=defs;
	
		List<String> lst = MapUtil.getSelectList(mapping,";");
		Map links=new LinkedHashMap();
		for (String st:lst){
			int i=st.indexOf("=");
			String lang=st,langId=st;
			if (i>0){
				lang=st.substring(0,i);
				langId=st.substring(i+1);
			}
			links.put(lang, langId);
		}
		return links;
	}
	
	public Map<String,String> getLanguages() {
		if (languages==null){
			languages=parseMapping(getLanguageMapping(),"en_US=en;en_CA=en;en_GB=en;zh_CN");
		}
		return languages;
	}
	
	private Map<String,String> getNumberFmts() {
		if (numberFmts==null){
			numberFmts=parseMapping(getNumberFmtMapping(),"en=#,###.##;zh=#,###.##");
		}
		return numberFmts;
	}
	
	private Map<String,String> getDateFmts() {
		if (dateFmts==null){
			dateFmts=parseMapping(getDateFmtMapping(),"en_CA=M d, Y; en=M/d/Y; zh_CN=Y年m月d日");
		}
		return dateFmts;
	}
	
	private Map<String,String> getTimeFmts() {
		if (timeFmts==null){
			timeFmts=parseMapping(getTimeFmtMapping(),"en_CA=H:i; en=h:iA; zh=H:i");
		}
		return timeFmts;
	}
	
	public void detectPreference(){
		HttpServletRequest request=Upload.getUpload().getRequest();
		Enumeration locales = request.getLocales();
        String lang=null;
        Map<String, String> langs = getLanguages();
        while (locales.hasMoreElements()) {
            Locale locale = (Locale) locales.nextElement();
            String id=locale.toString();
            for (String l:langs.keySet()){
            	if (id.startsWith(l)){
            		lang=id;
            		break;
            	}
            }
            if (lang!=null) break;
        }
        if (lang==null) lang="en_US";
        ThreadHelper.set("$$locale", lang);
        ThreadHelper.set("$$lang", langs.get(lang));
        
        String numFmt="#,##0.00", dateFmt="Y-m-d", timeFmt="H:i";
        
        Map<String, String> nums = getNumberFmts();
        Map<String, String> dates = getDateFmts();
        Map<String, String> times = getTimeFmts();
        
        for (String l:nums.keySet()){
        	if (l.startsWith(lang)){
        		numFmt=nums.get(l);
        		break;
        	}
        }
        
        for (String l:dates.keySet()){
        	if (l.startsWith(lang)){
        		dateFmt=dates.get(l);
        		break;
        	}
        }
        
        for (String l:times.keySet()){
        	if (l.startsWith(lang)){
        		timeFmt=times.get(l);
        		break;
        	}
        }
        ThreadHelper.set("$$numFmt", numFmt);
        ThreadHelper.set("$$dateFmt", dateFmt);
        ThreadHelper.set("$$timeFmt", timeFmt);
   }

	@Override
	public IDataService getDs() {
		return getDs(null);
	}

	@Override
	public IRepository getFs() {
		return getFs(null);
	}
}
