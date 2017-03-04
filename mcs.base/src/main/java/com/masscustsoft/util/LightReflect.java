package com.masscustsoft.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

public class LightReflect {
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
}
