package com.masscustsoft.Lang;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.masscustsoft.Lang.DynamicClassInfo;
import com.masscustsoft.service.ProxyVariantWrapper;
import com.masscustsoft.service.WebServiceEntity;
import com.masscustsoft.service.WebServiceEntry;
import com.masscustsoft.util.ClassFactory;
import com.masscustsoft.util.LightFile;
import com.masscustsoft.util.LightUtil;

/**
 * A customized Class object to be easy replaced by some rule.
 * 
 * 
 * @author JSong
 *
 */
public class CLASS extends ClassFactory{
	private static Map<String,Class> amap=new HashMap<String,Class>();
	private static Map<Class,String> zmap=new HashMap<Class,String>();
	private static CLASSLoader _loader=null;
	private static Map<String,byte[]> classBytes=new HashMap<String,byte[]>();
	private static List<File> dynamicJars=new ArrayList<File>(); 
	private static List<Integer> jarRefs=new ArrayList<Integer>();
	private static List<File> tempJars=new ArrayList<File>();
	private static Map<String,WebServiceEntry> dynamics=new HashMap<String,WebServiceEntry>();
	/**
	 * If dynamic class, default recognize point is has '__' or '.webservice.' in the class name.
	 * If by divider, the front part is a base class to extend passed in {@link DynamicClassInfo#templte}, the end part will be {@link DynamicClassInfo#categoryId}.
	 */
	public static String DIVIDER="__";
	
	/**
	 * Register the class to avoid duplicate loading.

	 * @param cls The class full name.
	 * @param c The Class itself.
	 */
	public static void reg(String cls, Class c){
		amap.put(cls,c);
		zmap.put(c,cls);
	}
	
	/**
	 * Get a real or mapped name for a given class. Suggest to use to replace Class.getName.
	 */
	public static String getName(Class c){
		String name=zmap.get(c);
		if (name==null) name=c.getName();
		return name;
	}

	/**
	 * Get a real or mapped simple name for a given class. Suggest to use to replace Class.getName.
	 */
	public static String getSimpleName(Class c){
		String name=zmap.get(c);
		if (name==null) name=c.getSimpleName();
		int idx=name.lastIndexOf(".");
		if (idx>0) return name.substring(idx+1);
		return name;
	}
	
	
	/**
	 * Find a class from the given Class loader first, if class not found, use CLASS retry.
	 * @throws MalformedURLException 
	 */
	public static Class forName(String cls, boolean initialize, ClassLoader loader) throws ClassNotFoundException, MalformedURLException{
		if (cls==null) return null;
		Class c=amap.get(cls);
		if (c!=null) return c;
		try{
			c=Class.forName(cls, initialize, loader);
		}
		catch (Exception e){
			c=Class.forName(cls, initialize, getLoader());
		}
		if (!isDynamicClass(cls)) CLASS.reg(cls,c);
		return c;
	}

	/**
	 * Find a class from CLASS
	 * @throws  
	 */
	public static Class forName(String cls) throws ClassNotFoundException{
		if (cls==null) return null;
		Class c=amap.get(cls);
		if (c!=null) return c;
		c=Class.forName(cls, true, getLoader());
		if (!isDynamicClass(cls)) CLASS.reg(cls,c);
		return c;
	}
	
	/**
	 * Get package name for given class.
	 */
	public static String getPackageName(Class c){
		String name=zmap.get(c);
		if (name==null) name=c.getName();
		int idx=name.lastIndexOf(".");
		if (idx>0) return name.substring(0,idx);
		return name;
	}
	
	public static void initDynamic(String service, WebServiceEntry entry){
		System.out.println("init dynamic "+service);
		dynamics.put(service, entry);
	}
	/**
	 * Detect if it's dynamic class. Two cases: the class contains '__' or '.webservice.'.
	 */
	public static boolean isDynamicClass(String name){
		//if (name.endsWith("BeanInfo")) return false;
		//if (classBytes.containsKey(name)) return false;
		int i=name.lastIndexOf(".webservice.");
		if (i>0){
			String simpleName=name.substring(i+12);
			int j=simpleName.indexOf(".");
			String svcName=simpleName;
			if (j>0) svcName=simpleName.substring(0,j);
			//System.out.println("isDyn1:"+name+", svc="+svcName);
			if (dynamics.get(svcName)!=null) return true;
			return false;
		}
		if (name.lastIndexOf(DIVIDER)>0) return true;
		return false;
	}

	/**
	 * Fill in proper parameter to dynamic class.
	 * 
	 * If it's web service, it has a simple name after the '.webservice.'. 
	 * If it's a web service related type object, it must has 2 level of names after the '.webservice.', first part is lowercased web service name {@link DynamicClassInfo#categoryId}, last part is the type name {@link DynamicClassInfo#subId}.
	 */
	public static DynamicClassInfo getDynamicClassExtension(String name){
		DynamicClassInfo info=new DynamicClassInfo();
		info.setTargetClass(name);
		
		{
			int i=name.lastIndexOf(".webservice.");
			if (i>0){
				String simpleName=name.substring(i+12);
				//System.out.println("simplename="+simpleName);
				int j=simpleName.indexOf(".");
				String svcName=simpleName;
				if (j>0) svcName=simpleName.substring(0,j);
				WebServiceEntry entry=dynamics.get(svcName);
				if (j>0){
					//.webservice.dbservice.Employee
					String entity=simpleName.substring(j+1);
					WebServiceEntity ent = entry.getWebServiceEntity(entity);
					String template=LightUtil.getBeanFactory().findRealClass(ent.getTemplate());
					info.setCategoryId(svcName);
					info.setSubId(entity);
					info.setConfig(ent);
					info.setTemplate(template);
					return info;
				}
				else{
					//.webservice.DbService
					String template=LightUtil.getBeanFactory().findRealClass(entry.getTemplate());
					info.setConfig(entry);
					info.setCategoryId(svcName);
					info.setTemplate(template);
					return info;		
				}
			}
		}
		{
			int i=name.lastIndexOf(".variantproxy.");
			if (i>0){
				String simpleName=name.substring(i+14);
				//System.out.println("simplename="+simpleName);
				int j=simpleName.indexOf(".");
				String bean=simpleName.substring(0,j);
				String ent=simpleName.substring(j+1);
				
				//the svcName is encoded rep:bean
				String beanId=LightUtil.decodeHashCode(bean);
				
				info.setCategoryId(beanId);
				info.setConfig(null);
				info.setSubId(ent);
				info.setTargetClass(name);
				info.setTemplate(ProxyVariantWrapper.class.getName());
				return info;		
			}
		}
		{
			int i=name.lastIndexOf(DIVIDER);
			info.setCategoryId(name.substring(i+DIVIDER.length()));
			info.setConfig(null);
			info.setTemplate(name.substring(0,i));
			return info;	
		}
	}

	public static CLASSLoader getLoader() throws ClassNotFoundException {
		if (_loader==null){
			URL[] urls=new URL[dynamicJars.size()];
			for (int i=0;i<dynamicJars.size();i++){
				File f=dynamicJars.get(i);
				try {
					File temp=File.createTempFile("WAB", ".jar");
					LightFile.copyFile(f, temp);
					temp.deleteOnExit();
					tempJars.add(temp);
					urls[i]=new URL("file:"+temp.getAbsolutePath());
				} catch (Exception e) {
					e.printStackTrace();
					throw new ClassNotFoundException();
				}
			}
			URLCLASSLoader uloader=new URLCLASSLoader(urls,CLASS.class.getClassLoader());
			_loader=new CLASSLoader(uloader);
		}
		return _loader;
	}

	public static void setClassBytes(String name, byte[] b){
		classBytes.put(name, b);
	}
	
	public static byte[] getClassBytes(String name){
		return classBytes.get(name);
	}
	
	public static void reset() throws Exception{
		amap.clear();
		zmap.clear();
		classBytes.clear();
		//if (_loader!=null) _loader.close();
		for (File temp:tempJars){
			System.out.println("try delete "+temp.getAbsolutePath());
			temp.delete();
		}
		tempJars.clear();
		fieldCaches.clear();
		_loader=null;
	}

	public static void bind(File... jars) throws Exception {
		boolean modi=false;
		for (File f:jars){
			int idx=dynamicJars.indexOf(f);
			if (idx<0){
				modi=true;
				dynamicJars.add(f);
				jarRefs.add(1);
			}
			else{
				jarRefs.set(idx, jarRefs.get(idx)+1);
			}
		}
		if (modi) reset();
	}
	
	public static void unbind(File... jars) throws Exception {
		boolean modi=false;
		for (File f:jars){
			int idx=dynamicJars.indexOf(f);
			if (idx>=0){
				jarRefs.set(idx,jarRefs.get(idx)-1);
				if (jarRefs.get(idx)<=0){
					modi=true;
					dynamicJars.remove(idx);
				}
			}
		}
		
		if (modi) reset();
	}
	
	public static void unbind(String... jars) throws Exception {
		boolean modi=false;
		for (String f:jars){
			File match=null;
			for (File g:dynamicJars){
				if (g.getName().equals(f)) {match=g; break;}
			}
			if (match==null) continue;
			modi=true;
			dynamicJars.remove(match);
		}
		
		if (modi) reset();
	}

	public static InputStream getResourceAsStream(String name) throws ClassNotFoundException {
		CLASSLoader cl = getLoader();
		return cl.getResourceAsStream(name);
	}

	public static URL getResource(String name) throws ClassNotFoundException {
		CLASSLoader cl = getLoader();
		return cl.getResource(name);
	}
}
