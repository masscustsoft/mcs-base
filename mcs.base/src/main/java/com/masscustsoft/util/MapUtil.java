package com.masscustsoft.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.ScriptUtil;

public class MapUtil {
	static public Object _getTerm(Map<String,Object> terms,String term){
		Object v=terms.get(term);
		if (v==null){
			for (Object it:terms.values()){
				if (it instanceof Map){
					v=_getTerm((Map)it,term);
					if (v!=null) return v;
				}
				if (it instanceof List){
					for (Object a:(List)it){
						if (a instanceof Map){
							v=_getTerm((Map)a,term);
							if (v!=null) return v;
						}
					}
				}
			}
			return null;
		}
		else 
		if (v instanceof Map) v=LightUtil.toJsonObject(v);
		return v;
	}
	
	static public String getTerm(Map<String,Object> terms,String term, String def){
		Object v=_getTerm(terms,term);
		if (v==null) return def;
		return v+"";
	}
	
	static public boolean replaceTerm(Map<String,Object> terms,String term, Object val){
		Object v=terms.get(term);
		if (v==null){
			for (Object it:terms.values()){
				if (it instanceof Map){
					if (replaceTerm((Map)it,term,val)) return true;
				}
			}
		}
		else{
			if (val!=null) terms.put(term, val); else terms.remove(term);
			return true;
		}
		return false;
	}
	
	static public Long getTermLong(Map<String,Object> terms,String term, Long def){
		String v=getTerm(terms, term, null);
		if (v==null){
			return def;
		}
		return LightUtil.decodeLong(v);
	}
	
	static public Integer getTermInt(Map<String,Object> terms,String term, Integer def){
		String v=getTerm(terms, term, null);
		if (v==null){
			return def;
		}
		return LightUtil.decodeInt(v);
	}
	
	static public java.sql.Date getTermShortDate(Map<String,Object> terms,String term, java.sql.Date def) throws Exception{
		String v=getTerm(terms, term, null);
		if (v==null){
			return def;
		}
		return LightUtil.decodeShortDate(v);
	}
	
	static public java.sql.Timestamp getTermLongDate(Map<String,Object> terms,String term, java.sql.Timestamp def) throws Exception{
		String v=getTerm(terms, term, null);
		if (v==null){
			return def;
		}
		return LightUtil.decodeLongDate(v);
	}
	
	public static List getList(Map map, String path) {
		List ret = new ArrayList<Map>();
		String ss[] = path.split("\\.");
		Map m = map;
		for (int i = 0; i < ss.length; i++) {
			String name = ss[i];
			Object mm = m.get(name);
			// System.out.println("list.name=" + name + ", mm=" + mm);
			if (mm == null) {
				return ret;
			}
			if (mm instanceof String) {
				ret.add(mm);
				return ret;
			}
			if (mm instanceof Map) {
				m = (Map) mm;
			} else if (ss.length - 1 == i && mm instanceof List) {
				return (List) mm;
			} else {
				return null;
			}
		}
		ret.add(m);
		return ret;
	}
	
	public static Object getAttr(Map map, String path) {
		String ss[] = path.split("\\.");
		Map m = map;
		for (int i = 0; i < ss.length; i++) {
			String name = ss[i];
			Object mm = m.get(name);
			if (mm == null) {
				return null;
			}
			if (mm instanceof Map) {
				m = (Map) mm;
			} else if (ss.length - 1 == i) {
				return mm;
			} else {
				return null;
			}
		}
		return m;
	}

	public static String getStr(Map map, String attr) {
		return getStr(map, attr, "");
	}
	
	public static String getStr(Map map, String attr, String def) {
		Object ret=map.get(attr);
		if (ret==null) return def;
		String s=ret+"";
		s=s.trim();
		if (s.startsWith("\"")) s=s.substring(1);
		if (s.endsWith("\"")) s=s.substring(0,s.length()-1).trim();
		return s;
	}

	public static Boolean getBool(Map map, String attr, Boolean def) {
		String s=getStr(map,attr);
		if (LightStr.isEmpty(s)) return def;
		Boolean ret=LightUtil.decodeBoolean(s);
		if (ret==null) return def;
		return ret;
	}
	
	public static Double getDouble(Map map, String attr, Double def){
		String s=getStr(map,attr);
		if (LightStr.isEmpty(s)) return def;
		s=s.replace(",","");
		Double ret=LightUtil.decodeDouble(s);
		if (ret==null) return def;
		return ret;
	}
	
	public static Integer getInt(Map map, String attr, Integer def){
		Object o=map.get(attr);
		Integer n=null;
		if (o==null) return def;
		if (o instanceof String){
			n=LightUtil.decodeInt((String)o);
		}
		else
		if (o instanceof Long) n=((Long)o).intValue();
		else
		if (o instanceof Integer) n=(Integer)o;
		if (n==null) return def;
		return n;
	}
	
	public static Float getFloat(Map map, String attr, Float def){
		String s=getStr(map,attr);
		if (LightStr.isEmpty(s)) return def;
		s=s.replace(",","");
		Double ret=LightUtil.decodeDouble(s);
		if (ret==null) return def;
		return ret.floatValue();
	}
	
	public static Date getDate(Map map, String attr, String format) throws Exception{
		String s=getStr(map,attr);
		if (LightStr.isEmpty(s)) return null;
		return LightUtil.decodeShortDate(s,format);
	}
	
	public static void extractTerms(Map<String,Object> terms){
		List<String> removes=new ArrayList<String>();
		String uuid=null;
		for (String key:terms.keySet()){
			Object val=terms.get(key);
			if (val instanceof String){
				String v=(String)val;
				if (v.startsWith("@@@")){
					Object vv=ScriptUtil.runJs(v.substring(3));
					if (vv!=null) terms.put(key, vv+"");
					else removes.add(key);
					v=vv+"";
				}
				if (key.equals("uuid") && !LightStr.isEmpty(v)){
					uuid=v;
					break;
				}
			}
			else
			if (val instanceof Map){
				extractTerms((Map)val);
			}
		}
		for (String key:removes)terms.remove(key);
		if (uuid!=null){
			terms.clear();
			terms.put("uuid", uuid);
		}
	}
	
	public static void mergeTerms(Map<String,Object> terms, Map<String,Object>spec, Map<String,Object>values){
		for (String key:terms.keySet()){
			Object val=terms.get(key);
			if (val instanceof String){
				String v=(String)val;
				if (v.startsWith("@@@params.")){
					Object vv=values.get(v.substring(10));
					if (vv!=null) spec.put(key, vv+"");
				}
			}
			else
			if (val instanceof Map){
				Map m=(Map)spec.get(key);
				if (m==null){
					m=new HashMap();
					spec.put(key, m);
				}
				mergeTerms((Map)val,m,values);
			}
		}
	}
	
	public static List<String> getSelectList(String mems, String deli){
		List<String> l=new ArrayList<String>();
		if (!LightStr.isEmpty(mems)){
			int i=mems.indexOf("*==*");
			if (i>0) mems=mems.substring(i+4).trim();
			String semi=" ";
			if (mems.indexOf(deli)>=0) semi=deli;
			String[] list=mems.split(semi);
			for (String s:list){
				s=s.trim();
				if (s.length()==0) continue;
				l.add(s);
			}
		}
		return l;
	}
	
	public static List<String> getSelectList(String mems){
		return getSelectList(mems,",");
	}
	
	public static String mergeSelectList(List<String> mems){
		return mergeSelectList(mems,", ");
	}
	
	public static String mergeSelectList(List<String> mems, String deli){
		StringBuffer buf=new StringBuffer();
		for (String s:mems){
			if (buf.length()!=0) buf.append(deli);
			buf.append(s);
		}
		return buf.toString();
	}
	
	public static Map getMacroMap(Map filter){
		Map m=new HashMap();
		for (Object key : filter.keySet()){
			Object val=filter.get(key);
			if (val instanceof Map){
				val=getMacroMap((Map)val);
			}
			else
			if (val instanceof String){
				val=LightUtil.macro((String)val);
			}
			m.put(key, val);
		}
		return m;
	}

	public static void setIfBool(Map it, String fld, Object obj, String method) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Boolean val=null;
		Object b=it.get(fld);
		if (b==null) return;
		if (b instanceof String) val=LightUtil.decodeBoolean((String)b);
		else
		if (b instanceof Boolean) val=(Boolean) b;
		if (val==null) return;
		Method m=ReflectUtil.getMethod(null, obj, method, boolean.class);
		if (m!=null){
			m.invoke(obj, val.booleanValue());
		}
	}
	
	public static void setIfBool(Map it, String fld, Object obj) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		String method="set"+LightStr.capitalize(fld);
		setIfBool(it,fld,obj,method);
	}
	
	public static void setIfInt(Map it, String fld, Object obj, String method) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Integer val=null;
		Object b=it.get(fld);
		if (b==null) return;
		if (b instanceof String) val=LightUtil.decodeInt((String)b);
		else
		if (b instanceof Double) val=((Double)b).intValue();
		else
		if (b instanceof Long) val=((Long)b).intValue();	
		else
		if (b instanceof Integer) val=((Integer)b).intValue();
		if (val==null) return;
		Method m=ReflectUtil.getMethod(null, obj, method, int.class);
		if (m!=null){
			m.invoke(obj, val.intValue());
		}
	}
	
	public static void setIfInt(Map it, String fld, Object obj) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		String method="set"+LightStr.capitalize(fld);
		setIfInt(it,fld,obj,method);
	}
	
	
	public static void setIfFloat(Map it, String fld, Object obj, String method) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Double val=null;
		Object b=it.get(fld);
		if (b==null) return;
		if (b instanceof String) val=LightUtil.decodeDouble((String)b);
		else
		if (b instanceof Double) val=((Double)b);
		else
		if (b instanceof Long) val=((Long)b).doubleValue();	
		if (val==null) return;
		Method m=ReflectUtil.getMethod(null, obj, method, float.class);
		if (m!=null){
			m.invoke(obj, val.floatValue());
		}
	}
	
	public static void setIfFloat(Map it, String fld, Object obj) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		String method="set"+LightStr.capitalize(fld);
		setIfFloat(it,fld,obj,method);
	}
	
	public static void setIfDouble(Map it, String fld, Object obj, String method) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Double val=null;
		Object b=it.get(fld);
		if (b==null) return;
		if (b instanceof String) val=LightUtil.decodeDouble((String)b);
		else
		if (b instanceof Double) val=((Double)b);
		else 
		if (b instanceof Long) val=((Long)b).doubleValue();	
		if (val==null) return;
		Method m=ReflectUtil.getMethod(null, obj, method, double.class);
		if (m!=null){
			m.invoke(obj, val.doubleValue());
		}
	}
	
	public static void setIfStr(Map it, String fld, Object obj, String method) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		String val=null;
		Object b=it.get(fld);
		if (b==null) return;
		if (b instanceof String) val=(String)b;
		else
		val=b+"";
		if (val==null) return;
		Method m=ReflectUtil.getMethod(null, obj, method, String.class);
		if (m!=null){
			m.invoke(obj, val);
		}
	}
	
	public static void setIfStr(Map it, String fld, Object obj) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		String method="set"+LightStr.capitalize(fld);
		setIfStr(it,fld,obj,method);
	}
}
