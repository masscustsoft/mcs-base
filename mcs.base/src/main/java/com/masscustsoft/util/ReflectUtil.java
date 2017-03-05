package com.masscustsoft.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import com.masscustsoft.Lang.CLASS;
import com.masscustsoft.api.IBeanFactory;
import com.masscustsoft.api.PrimaryKey;
import com.masscustsoft.api.Referent;
import com.masscustsoft.service.ReferentItem;

public class ReflectUtil{
	
	public static List<Field> getFieldMap(Class c){
		return ClassFactory.getFieldMap(c);
	}
	
	public static Field findField(List<Field> flds,String property){
		return ClassFactory.findField(flds, property);
	}
	
	@SuppressWarnings("unchecked")
	public static Object getProperty(Object o, String property) throws Exception{
		if (o==null) return null;
		int idx=property.indexOf(".");
		if (idx>0){
			o=getProperty(o,property.substring(0,idx));
			return getProperty(o,property.substring(idx+1));
		}
		if (o instanceof Map) {
			Map map = (Map) o;
			return map.get(property);
		}
		Class c = o.getClass();
		List<Field> flds=getFieldMap(c);
		Field f = findField(flds,property);
		if (f==null) return null;
		
		String name=f.getName();
		if (name.length()<2 || !Character.isUpperCase(name.charAt(1))) name=LightStr.capitalize(name);
		if (getMethod(f.getDeclaringClass(),o,"get"+name)!=null){
			return f.getDeclaringClass().getMethod("get"+name).invoke(o);
		}
		else
		if (getMethod(f.getDeclaringClass(),o,"is"+name)!=null){
			return f.getDeclaringClass().getMethod("is"+name).invoke(o);
		}
		else{
			int modi = f.getModifiers();
			if ((modi & 129) == 1) {
				return f.get(o);
			}	
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static void setProperty(Object o, String property, Object v) throws Exception{
		//System.out.println("setProp "+property+",v="+v);
		if (o instanceof Map) {
			Map map = (Map) o;
			int idx=property.indexOf(".");
			if (idx>0){
				o=map.get(property.substring(0,idx));
				setProperty(o,property.substring(idx+1),v);
			}
			else map.put(property, v);
			return;
		}
		Class c = o.getClass();
		List<Field> flds=getFieldMap(c);
		Field f = findField(flds,property);
		if (f != null) {
			String name=f.getName();
			if (name.length()<2 || !Character.isUpperCase(name.charAt(1))) name=LightStr.capitalize(name);
			String fn="set" + name;
			try {
				if (f.getType().getName().equals("java.sql.Date") && v!=null && v.getClass().getName().equals("java.sql.Timestamp")){
					Timestamp ts=(java.sql.Timestamp)v;
					v=new java.sql.Date(ts.getTime());
				}
				f.getDeclaringClass().getMethod(fn, f.getType()).invoke(o,v);
			} catch (Exception e) {
				int modi = f.getModifiers();
				if ((modi & 129) == 1) {
					f.set(o, v);
				}
			}
		}
	}
	
	public static Method getMethod(Class c, Object o, String method, Class... paras){
		if (c==null && o!=null) c=o.getClass();
		try{
			Class[] classes = new Class[paras.length];
			for (int i = 0; i < paras.length; i++) {
				classes[i] = paras[i];
			}
			Method m = c.getMethod(method, classes);
			return m;
		}
		catch (Exception e){
			return null;
		}
	}
	
	public static void copyProperties(Object dst, Object src, boolean override){
		//System.out.println("CP src="+LightUtil.toJsonObject(src));
		List<java.lang.reflect.Field> list = getFieldMap(src.getClass());
		List<java.lang.reflect.Field> dlist = getFieldMap(dst.getClass());
				
		for (java.lang.reflect.Field fld:list){
			try{
				Object v=getProperty(src, fld.getName()); 
				Object v2=getProperty(dst, fld.getName());
				//System.out.println("CP0 "+fld.getName()+" src="+fld.getType().getName()+", v="+v+",v2="+v2);
				
				Field dFld=findField(dlist, fld.getName());
				if (v==null) continue;
				if (dFld==null) continue; //ignore if no match
				
				//System.out.println("CP "+fld.getName()+" dFld="+dFld.getType().getName()+", src="+fld.getType().getName()+", v="+v+",v2="+v2);
				
				if (v2==null || (v2 instanceof List) && ((List)v2).size()==0 || override){
					if (v2!=null && v instanceof List) { ((List)v2).addAll((List)v);}
					else
					if (v instanceof java.util.Date && !dFld.getType().equals(fld.getType())){
						//System.out.println("DT dFld="+dFld.getType().getName()+", src="+fld.getType().getName());
						if (dFld.getType().equals(java.sql.Timestamp.class)){
							//copy util.Date to Timestamp
							v=LightUtil.longDate((java.util.Date)v);
						}
						else
						if (dFld.getType().equals(java.sql.Date.class)){
							//copy util.Date to sql.Date
							v=LightUtil.shortDate((java.util.Date)v);
						}
						else
						if (dFld.getType().equals(java.util.Date.class)){
							//copy from Timestamp and Date to util.Date
							if (v instanceof Timestamp) v=LightUtil.localDate((Timestamp)v);
							else v=LightUtil.localDate((java.sql.Date)v);
						}
						setProperty(dst, fld.getName(), v);
					}
					else setProperty(dst, fld.getName(), v);	
				}
			}
			catch(Exception e){e.printStackTrace();}
		}
	}
	
	public static String getGenericType(Field f){
		Type type = f.getGenericType();
		if (type instanceof ParameterizedType){
			ParameterizedType tv=(ParameterizedType)type;
			Type[] av = tv.getActualTypeArguments();
			if (av==null || av.length==0) return "";
			Type tp=av[0];
			return tp.toString();	
		}
		return type.toString();
	}

	public static ReferentItem getReferent(Class mainCls, Referent ref) throws Exception{
		ReferentItem it = new ReferentItem();
		it.setModel(CLASS.getSimpleName(ref.model()));
		for (int i=0;i<ref.keyList().length;i++){
			String key=ref.keyList()[i];
			String map=ref.mappingList()[i];
			it.getKeyMapping().put(key, map);
		}
		getReferent(mainCls,it);
		return it;
	}
	
	private static ReferentItem getReferent(Class mainCls, ReferentItem it) throws Exception{
		if (it.getKeyMapping().size()==0 && !LightStr.isEmpty(it.getModel())){
			IBeanFactory bf=LightUtil.getBeanFactory();
			Class cc=CLASS.forName(bf.findRealClass(it.getModel()));
			//add default primary key
			//case 1: for sub: sub's key in parent
			//case 2: for ref: parent's key in child
			List<Field> fields = ReflectUtil.getFieldMap(cc); // target descendant
			List<Field> pp=ReflectUtil.getFieldMap(mainCls); //parent cls
			//for child
			for (Field f:fields){
				PrimaryKey pk=f.getAnnotation(PrimaryKey.class);
				if (pk!=null){
					String key=f.getName();
					if (ReflectUtil.findField(pp, key)!=null) it.getKeyMapping().put(key, key);
				}
			}
			//for ref
			for (Field f:pp){
				PrimaryKey pk=f.getAnnotation(PrimaryKey.class);
				if (pk!=null){
					String key=f.getName();
					if (ReflectUtil.findField(fields, key)!=null) it.getKeyMapping().put(key, key);
				}
			}
		}
		return it;
	}
	
}
