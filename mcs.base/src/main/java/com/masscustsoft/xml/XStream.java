package com.masscustsoft.xml;

/**
 * Sep 24,2008:
 * 	prevent addPackage from adding duplicate values.
 */
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.masscustsoft.Lang.CLASS;
import com.masscustsoft.api.Synonym;
import com.masscustsoft.helper.BeanVersion;
import com.masscustsoft.model.Entity;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.LogUtil;
import com.masscustsoft.util.ReflectUtil;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.LightStr;

@SuppressWarnings("unchecked")
public class XStream {
	List<String> packages=new ArrayList<String>();
	Map<String,Class> annotations=new HashMap<String,Class>();
	XStreamListener listener=null;
	String missingTagAction="warning";
	Map<String,String> realClasses=new HashMap<String,String>();
	
	public Object fromXml(String id,String st,BeanVersion ver) throws Exception{
		return fromXml(id,new StringReader(st),ver);
	}
	
	public Object fromXml(String id,InputStream is, String charset, BeanVersion ver) throws Exception{
		return fromXml(id,new InputStreamReader(is, charset),ver);
	}
	
	public Object fromXml(String id,Reader r,BeanVersion ver) throws Exception{
		XmlParser p=new XmlParser(r);
		XmlNode node=p.readNode();
		Object o=fromNode(null,node,id,null,ver);
		return o;
	}
	
	public String toXml(Object obj,int op){
		StringWriter w=new StringWriter();
		try {
			toXml(w,obj,op);
		} catch (IOException e) {
		}
		return w.getBuffer().toString();
	}
	
	public void toXml(Writer w, Object obj, int op) throws IOException{
		XmlWriter writer=new XmlWriter(w);
		XmlNode node=null;
		if (obj instanceof XmlNode){
			node=(XmlNode)obj;
		}
		else{
			node=new XmlNode();
			toNode(null,obj,null,node,false);
		}
		writer.write(node, op);
	}
	
	private String decodeName(String name){
		if (name.indexOf("_")==1){
			for (String prefix:annotations.keySet()){
				if (name.startsWith(prefix)){
					name=name.substring(prefix.length());
					break;
				}
			}
		}
		return name;
	}
	
	public String encodeName(Field f){
		String name=f.getName();
		for (String prefix:annotations.keySet()){
			Class<? extends Annotation> a=annotations.get(prefix);
			if (f.isAnnotationPresent(a)){
				name=prefix+name;
				break;
			}
		}
		return name;
	}
	
	//fullpath is to support CLONE which need to have a fullpath
	public boolean toNode(Object parent,Object obj,Class type,XmlNode node,boolean fullPath) {
		if (obj==null){
			node.setTag("null");
			return false;
		}
		if (listener!=null){
			if (listener.onToNode(parent,obj,node,fullPath)) return true;
		}
		if (LightUtil.isPrimitive(obj.getClass())){
			if (LightStr.isEmpty(node.getTag())){
				node.setTag(obj.getClass().getSimpleName());
			}
			node.setText(obj+"");
		}
		if (obj instanceof Map){
			if (node.getTag().length()==0) node.setTag("map");
			else{
				if (type!=null && !CLASS.getSimpleName(type.getClass()).equalsIgnoreCase("Map")){
					node.setAttribute("class", "map");
				}
			}
			Map map=(Map)obj;
			Object[] keys = map.keySet().toArray();
			for (int i=0;i<keys.length;i++){
				String key=(String)keys[i];
				Object val=map.get(key);
				if (val==null) continue;
				////will add later to keep compatible
				//if (key instanceof String && val instanceof String){
				//	node.setAttribute((String)key, (String)val);
				//	continue;
				//}
				////
				XmlNode sub = new XmlNode();
				if (LightUtil.isPrimitive(key.getClass())){
					sub.setTag(LightUtil.encodeObject(key));
					if (LightUtil.isPrimitive(val.getClass())){
						sub.setObject(val);
					}
					else{
						XmlNode child = new XmlNode();
						if (toNode(obj,val,null,child, fullPath)) sub.addChildNode(child);
					}
				}
				else{
					sub.setTag("item");
					XmlNode keyObj = new XmlNode();
					toNode(obj,key,null,keyObj, fullPath);
					XmlNode valObj = new XmlNode();
					toNode(obj,val,null,valObj, fullPath);
					sub.addChildNode(keyObj);
					sub.addChildNode(valObj);
				}
				node.addChildNode(sub);
			}
			if (map.size()==0) return false;
			return true;
		}
		if (obj instanceof List){
			if (node.getTag().length()==0) node.setTag("list");
			else{
				if (type!=null && !CLASS.getSimpleName(type.getClass()).equalsIgnoreCase("List")){
					node.setAttribute("class", "list");
				}
			}
			List list=(List)obj;
			for (Object it:list){
				if (it==null) continue;
				XmlNode child=new XmlNode();
				
				if (LightUtil.isPrimitive(it.getClass())){
					child.setTag("item");
					child.setText(LightUtil.encodeObject(it));
				}
				else{
					toNode(obj,it,null,child,fullPath);
				}
				node.addChildNode(child);
			}
			return true;
		}
		if (LightStr.isEmpty(node.getTag())){
			node.setTag(com.masscustsoft.Lang.CLASS.getSimpleName(obj.getClass()));
		}
		else if (type==null || !type.equals(obj.getClass())){
			node.setAttribute("class", com.masscustsoft.Lang.CLASS.getSimpleName(obj.getClass()));
		}
		for (Field f : ReflectUtil.getFieldMap(obj.getClass())) {
			if ((f.getModifiers()&128)!=0) continue; //skip transient
			Object v=null;
			try {
				v = ReflectUtil.getProperty(obj, f.getName());
			} catch (Exception e) {
				continue;
			}
			if (LightUtil.isPrimitive(f.getType()) || v!=null && LightUtil.isPrimitive(v.getClass())){
				node.setAttribute(encodeName(f), LightUtil.encodeObject(v));
			}
			else
			if (v!=null){
				XmlNode sub=new XmlNode();
				sub.setTag(encodeName(f));
				if (toNode(obj,v,f.getType(),sub, fullPath)) node.addChildNode(sub);
			}
		}
		return true;
	}

	private void enhancePackages(String pp){
		String ss[]=pp.split(",");
		for (String s:ss){
			s=s.trim();
			if (s.length()==0) continue;
			this.addPackage(s);
		}
	}
	
	private String newId(String id, String postfix){
		if (id==null) return null;
		return id+postfix;
	}
	
	private Object _parseAttr(Object obj, String id, Field f, XmlNode sub, Object attr, BeanVersion ver) throws Exception{
		if (LightUtil.isPrimitive(f.getType()) && sub.getAttribute("ref")==null) attr=LightUtil.decodeObject(sub.getText().toString(), f.getType(),false);
		else {
			//attr is the value if not give the class, should use the class name as class;
			if (attr==null && sub.getAttribute("class")==null) {
				try {
					attr=f.getType().newInstance();
				} catch (Exception e) {
				}
			}
			attr=fromNode(obj,sub,newId(id,"_"+f.getName()), attr,ver);
		}
		return attr;
	}
	
	public Object fromNode(Object parent,XmlNode node,String id,Object obj, BeanVersion ver) throws Exception{
		String ref=node.getAttribute("ref");
		if (ref!=null && listener!=null){
			return listener.onExternalBean(id,parent,ref,ver);
		}
		Object o=_fromNode(parent,node,id,obj,ver);
		if (listener!=null){
			listener.cacheBean(id,o,ver);
		}
		return o;
	}
	
	private Object _fromNode(Object parent,XmlNode node,String id,Object obj,BeanVersion ver) throws Exception{
		String extend=node.getAttribute("extend");
		if (extend!=null && listener!=null){
			Object base=listener.onExternalBean(id,parent,extend,ver);
			obj=LightUtil.getBeanFactory().clone(base, null);
		}
		String clazz=node.getAttribute("class");
		if (!LightStr.isEmpty(clazz)) obj=null;
		if (obj==null){
			if (LightStr.isEmpty(clazz)) clazz=decodeName(node.getTag());
			String className=findRealClass(clazz);
			if (className.equals("String") && node.getText()!=null){
				return LightUtil.decodeObject(node.getText()+"", String.class,false);
			}
			if (className.equals("Integer") && node.getText()!=null){
				return LightUtil.decodeObject(node.getText()+"", Integer.class,false);
			}
			if (className.equals("Long") && node.getText()!=null){
				return LightUtil.decodeObject(node.getText()+"", Long.class,false);
			}
			if (className.equals("Float") && node.getText()!=null){
				return LightUtil.decodeObject(node.getText()+"", Float.class,false);
			}
			if (className.equals("Double") && node.getText()!=null){
				return LightUtil.decodeObject(node.getText()+"", Double.class,false);
			}
			if (className.equals("Boolean") && node.getText()!=null){
				return LightUtil.decodeObject(node.getText()+"", Boolean.class,false);
			}
			if (className.equals("Date") && node.getText()!=null){
				return LightUtil.decodeObject(node.getText()+"", Date.class,false);
			}
			if (className.equals("Timestamp") && node.getText()!=null){
				return LightUtil.decodeObject(node.getText()+"", Timestamp.class,false);
			}
			try {
				obj=com.masscustsoft.Lang.CLASS.forName(className).newInstance();
				if (obj instanceof Entity){
					//register to db dataservice
				}
			} catch (Exception e) {
				LogUtil.dumpStackTrace(e);
			}
		}
		if (obj==null) return null;
		if (obj instanceof XmlNode){
			return node;
		}
		if (obj instanceof Map){
			//System.out.println("is a MAP="+node);
			Map map=(Map)obj;
			int i=0;
			for (XmlNode sub:node.getChildrenList()){
				i++;
				String key=sub.getTag();
				//System.out.println("nodechild="+sub);
				if (key.equals("item")){
					XmlNode keyNode=sub.getChildrenList().get(0);
					XmlNode valNode=sub.getChildrenList().get(1);
					Object k=fromNode(parent,keyNode,newId(id,"_key"+i),null,ver);
					Object v=fromNode(parent,valNode,newId(id,"_val"+i),null,ver);
					map.put(k,v);
				}
				else
				if (sub.getChildrenList().size()>0){
					XmlNode child=sub.getChildrenList().get(0);
					Object v=fromNode(parent,child,newId(id,"_"+key),null,ver);
					if (v!=null) map.put(key,v);
				}
				else{
					String tp=sub.getAttribute("_");
					String v=sub.getText().toString();
					Object val=v;
					if (tp!=null){
						if (tp.equals("P")) val=LightUtil.decodeShortDate(v);
						else
						if (tp.equals("D")) val=LightUtil.decodeLongDate(v);
						else
						if (tp.equals("I")) val=LightUtil.decodeInt(v);
						else
						if (tp.equals("L")) val=LightUtil.decodeLong(v);
						else
						if (tp.equals("F")) val=LightUtil.decodeFloat(v);
						else
						if (tp.equals("N")) val=LightUtil.decodeDouble(v);
						else
						if (tp.equals("B")) val=LightUtil.decodeBoolean(v);
					}
					map.put(key, val);
				}
			}
			for (String key:node.getAttributeKeySet()){
				if (key.equals("class")) continue;
				map.put(key, node.getAttribute(key));
			}
		}
		else
		if (obj instanceof List){
			//System.out.println("is a LIST");
			List list=(List)obj;
			int i=0;
			for (XmlNode sub:node.getChildrenList()){
				i++;
				if (sub.getTag().equals("item")){
					if (LightStr.isEmpty(sub.getText()+"") && !LightStr.isEmpty(sub.getAttribute("value")))
						list.add(sub.getAttribute("value"));
					else
						list.add(LightUtil.decodeObject(sub.getText().toString(),String.class,false));
				}
				else
				if (sub.getTag().equals("include")){
					String inc=sub.getAttribute("ref");
					if (listener!=null && inc!=null){
						Object o=listener.onExternalBean(id,parent,inc,ver);
						if (o instanceof List){
							list.addAll((List)o);
							//List lst=(List)o;
							//for (Object oo:lst){
							//	Object ooo=LightUtil.clone(oo,true);
							//	list.add(ooo);		
							//}
						}
					}
				}
				else{
					list.add(fromNode(parent,sub,newId(id,"_item"+i),null,ver));
				}
			}
		}
		else{
			List<Field> fldMap = ReflectUtil.getFieldMap(obj.getClass());
			if (node.getText()!=null && node.getText().toString().length()>0){
				Field f=ReflectUtil.findField(fldMap,"value");
				if (f==null) f=ReflectUtil.findField(fldMap,"text");
				if (f!=null){
					try {
						ReflectUtil.setProperty(obj, f.getName(), node.getText());
					} catch (Exception e) {
						LogUtil.info("SetBeanPropErr:"+f.getName() +" to "+node.getText());
					}
				}
			}
			for (XmlNode sub:node.getChildrenList()){
				Object attr=null;
				if (sub.getTag()==null) continue;
				if (sub.getTag().equals("null")) continue;
				if (sub.getTag().equals("sys:packages")){
					enhancePackages(sub.getText()+"");
					continue;
				}
				//System.out.println("SUB="+sub+", node="+node+",decode="+decodeName(sub.getTag()));
				Field f=ReflectUtil.findField(fldMap,decodeName(sub.getTag()));
				if (f==null) {
					if (missingTagAction.equals("warning")) {
						LogUtil.info("tag not found! sub="+sub+",tag="+sub.getTag()+",id="+id);
					}
					continue;
				}
				try {
					attr = ReflectUtil.getProperty(obj, f.getName());
				} catch (Exception e) {
					continue; //if not a field, ignore
				}
				Object ret=_parseAttr(obj,id,f,sub,attr,ver);
				try {
					Synonym sn=f.getAnnotation(Synonym.class);
					if (ret==null && sn!=null && sn.value().length>0){
						for (String nm:sn.value()){
							XmlNode child=node.getChild(nm); //with prefix i_, in xml
							if (child==null) continue;
							ret=_parseAttr(obj,id,f,child,attr,ver);
							if (ret!=null) break;
						}
					}
					ReflectUtil.setProperty(obj,f.getName(),ret);
				} catch (Exception e) {
					LogUtil.info("SetTagPropErr:"+sub.getTag() +" to "+attr);
					e.printStackTrace();
				}
			}
			for (String name:node.getAttributeKeySet()){
				String v=node.getAttribute(name);
				if (v==null) v="";
				if (name.equals("sys:packages")){
					enhancePackages(v);
					continue;
				}
				Field f=ReflectUtil.findField(fldMap,decodeName(name));
				if (f==null) continue;
				if ((f.getModifiers()&128)!=0) continue; //skip transient
				//
				Synonym sn=f.getAnnotation(Synonym.class);
				if (LightStr.isEmpty(v) && sn!=null && sn.value().length>0){
					for (String nm:sn.value()){
						v=node.getAttribute(nm);//with prefix i_, in xml
						if (!LightStr.isEmpty(v)) break;
					}
				}
				//
				Object val=LightUtil.decodeObject(v,f.getType(),false);
				
				if (val!=null){
					try {
						//System.out.println("name="+f.getName()+",v="+v+",val="+val);
						ReflectUtil.setProperty(obj, f.getName(), val);
					} catch (Exception e) {
						LogUtil.info("SetBeanPropErr:"+f.getName() +" to "+val);
					}
				}
			}
		}
		
		return obj;
	}
	
	public void addPackage(String pkg){
		if (!packages.contains(pkg)) packages.add(pkg);
	}
	
	public void removePackage(String pkg){
		if (packages.contains(pkg)) packages.remove(pkg);
	}
	
	public void addAnnotation(String prefix,Class<? extends Annotation> a){
		annotations.put(prefix, a);
	}
	
	public synchronized String findRealClass(String name) {
		if (name.indexOf(".") != -1) return name;
		if (name.equals("list")) return "java.util.ArrayList";
		if (name.equals("map")) return "java.util.TreeMap";
		if (name.equals("Date")) return "java.sql.Date";
		if (name.equals("Timestamp")) return "java.sql.Timestamp";
		String real=realClasses.get(name);
		if (real!=null) return real;
		for (int i=0;i<packages.size();i++){
			String pkg=packages.get(i);
			String newName=pkg+"."+name;
			if (tryClass(newName)){
				realClasses.put(name, newName);
				return newName;
			}
				
		}
		return name;
	}

	private boolean tryClass(String cls) {
		try {
			//System.out.println("tryClass "+cls);
			com.masscustsoft.Lang.CLASS.forName(cls);
		} catch (ClassNotFoundException e) {
			return false;
		} catch (NoClassDefFoundError e) {
			return false;
		}
		return true;
	}

	public XStreamListener getListener() {
		return listener;
	}

	public void setListener(XStreamListener listener) {
		this.listener = listener;
	}

	public String getMissingTagAction() {
		return missingTagAction;
	}

	public void setMissingTagAction(String missingTagAction) {
		this.missingTagAction = missingTagAction;
	}

}
