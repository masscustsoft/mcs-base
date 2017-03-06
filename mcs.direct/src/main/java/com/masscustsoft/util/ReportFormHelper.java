package com.masscustsoft.util;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportFormHelper {
	List<Map> data=new ArrayList();
	Object rcd;
	
	public ReportFormHelper(Object rcd){
		this.rcd=rcd;
	}
	
	public void add(String field, String value){
		if (LightStr.isEmpty(value)) return;
		
		Map m=new HashMap();
		m.put("text",GlbUtil.i18n(LightStr.capitalize(field))+":");
		data.add(m);
		
		m=new HashMap();
		m.put("text",value);
		m.put("bold", true);
		data.add(m);
	}
	
	public void add(String field) throws Exception{
		Object val=ReflectUtil.getProperty(rcd,field);
		if (val==null) val="";
		if (val instanceof Date){
			val=GlbUtil.shortDate(val);
		}
		else
		if (val instanceof Timestamp){
			val=GlbUtil.longDate(val);
		}
		else
		if (val instanceof Double){
			val=GlbUtil.decimal(val,2);
		}
		add(field,val.toString());
	}
	
	public void add(String field, int dec) throws Exception{
		Object val=ReflectUtil.getProperty(rcd,field);
		if (val==null) val="0";
		Double num=LightUtil.decodeDouble(val.toString());
		if (num==null || num==0d) return;
		add(field, GlbUtil.decimal(num, dec));
	}
	
	public void add(String field, boolean i18n) throws Exception{
		Object val=ReflectUtil.getProperty(rcd,field);
		if (val==null) val="";
		String value=GlbUtil.i18n((String)val);
		add(field,value);
	}
}
