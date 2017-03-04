package com.masscustsoft.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassFactory {
	protected static Map<Class,List<Field>> fieldCaches=Collections.synchronizedMap(new HashMap<Class,List<Field>>());
	protected static Map<Class,Map<String,Method>> methodCaches=Collections.synchronizedMap(new HashMap<Class,Map<String,Method>>());
	
	public static List<Field> getFieldMap(Class c){
		List<Field> list=fieldCaches.get(c);
		if (list==null){
			list=new ArrayList<Field>();
			getFields(c,list);
			fieldCaches.put(c,list);
		}
		return list;
	}
	
	private static void getFields(Class c, List<Field> list){
		Class p=c.getSuperclass();
		if (p!=null) getFields(p,list);
		Field[] flds = c.getDeclaredFields();
		for (Field fld:flds){
			int modi=fld.getModifiers();
			if ((modi&24)!=0) continue; //16=final 8=static 128=transient
			list.add(fld); //2=private 4=protected 1=public 
		}
	}
	
	public static Field findField(List<Field> flds,String property){
		for (Field ff:flds){
			if (ff.getName().equals(property)){
				return ff;
			}
		}
		return null;
	}
	
	private static void getMethods(Class c, Map<String,Method> map){
		Class p=c.getSuperclass();
		if (p!=null && !p.equals(Object.class)) getMethods(p,map);
		Method[] methods = c.getDeclaredMethods();
		for (Method method:methods){
			int modi=method.getModifiers();
			if ((modi&26)!=0) continue; //16=final 8=static 128=transient //2=private 4=protected 1=public 
			Class<?>[] types = method.getParameterTypes();
			String name=method.getName();
			for (Class tp:types) name+="$"+tp;
			map.put(name,method); 
		}
	}
	
	public static Map<String,Method> getMethodMap(Class c){
		Map<String,Method> map=methodCaches.get(c);
		if (map==null){
			map=new HashMap<String,Method>();
			getMethods(c,map);
			methodCaches.put(c,map);
		}
		return map;
	}
}
