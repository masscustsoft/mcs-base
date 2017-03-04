package com.masscustsoft.util;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.util.List;

import com.masscustsoft.Lang.CLASS;
import com.masscustsoft.api.IBeanFactory;
import com.masscustsoft.api.PrimaryKey;
import com.masscustsoft.api.Referent;
import com.masscustsoft.service.ReferentItem;

public class ReflectUtil extends LightReflect{
	

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
