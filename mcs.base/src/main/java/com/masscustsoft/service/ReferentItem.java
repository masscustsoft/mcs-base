package com.masscustsoft.service;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.masscustsoft.Lang.CLASS;
import com.masscustsoft.api.IBeanFactory;
import com.masscustsoft.api.IDataService;
import com.masscustsoft.api.IDatabaseAccess;
import com.masscustsoft.api.IEntity;
import com.masscustsoft.api.IReferentItem;
import com.masscustsoft.api.PrimaryKey;
import com.masscustsoft.api.Referent;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.ReflectUtil;
import com.masscustsoft.util.LightStr;

public class ReferentItem implements IReferentItem {
	String table;
	String model;
	
	Map<String,String> keyMapping=new HashMap<String,String>();
	
	@Override
	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	@Override
	public Map<String, String> getKeyMapping() {
		return keyMapping;
	}

	public void setKeyMapping(Map<String, String> keyMapping) {
		this.keyMapping = keyMapping;
	}

	@Override
	public String getTable() {
		return table;
	}

	public void setTable(String table) {
		this.table = table;
	}
	
	public static ReferentItem getReferent(Class mainCls, Referent ref) throws Exception{
		ReferentItem it = new ReferentItem();
		it.setModel(CLASS.getSimpleName(ref.model()));
		for (int i=0;i<ref.keyList().length;i++){
			String key=ref.keyList()[i];
			String map=ref.mappingList()[i];
			it.keyMapping.put(key, map);
		}
		getReferent(mainCls,it);
		return it;
	}
	
	private static ReferentItem getReferent(Class mainCls, ReferentItem it) throws Exception{
		if (it.keyMapping.size()==0 && !LightStr.isEmpty(it.getModel())){
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
					if (ReflectUtil.findField(pp, key)!=null) it.keyMapping.put(key, key);
				}
			}
			//for ref
			for (Field f:pp){
				PrimaryKey pk=f.getAnnotation(PrimaryKey.class);
				if (pk!=null){
					String key=f.getName();
					if (ReflectUtil.findField(fields, key)!=null) it.keyMapping.put(key, key);
				}
			}
		}
		return it;
	}
	
	@Override
	public List<IEntity> getReferentData(IEntity me, int max) throws Exception{
		if (!LightStr.isEmpty(getModel())) getReferent(me.getClass(),this);
		Map<String,Object> terms=new HashMap<String,Object>();
		for (String key:keyMapping.keySet()){
			String map=keyMapping.get(key);
			Object v=ReflectUtil.getProperty(me, map);
			if (v==null) continue; //ignore non-exist fields
			if (v instanceof Date) v="@"+LightUtil.encodeShortDate((Date)v);
			if (v instanceof Timestamp) v="@"+LightUtil.encodeLongDate((Timestamp)v);
			terms.put(key,v+"");
		}
		if (terms.size()==0){
			return new ArrayList<IEntity>();
		}
		IBeanFactory bf=LightUtil.getBeanFactory();
		Class cls=CLASS.forName(bf.findRealClass(model));
		List<IEntity> list=me.getDataService().getBeanList(cls, LightUtil.toJsonString(terms).toString(), "", 0, max, null);
		return list;
	}
	
	@Override
	public void checkReference(IEntity me) throws Exception{
		if (!LightStr.isEmpty(model)){
			List<IEntity> list=getReferentData(me, 1);
			
			if (list.size()>0){
				throw new Exception("#[ReferenceExist]: #["+model.toUpperCase()+"]");
			}
		}
		else
		if (!LightStr.isEmpty(table)){//process table
			if (!(me.getDataService() instanceof IDatabaseAccess)) throw new Exception("Table Reference only works for DatabaseDataService!");
			IDatabaseAccess db=(IDatabaseAccess)me.getDataService();
			Connection conn = db.connect();
			StringBuffer  sql=new StringBuffer("select * from "+table+" where ");
			List<Object> vals=new ArrayList<Object>();
			for (String key:keyMapping.keySet()){
				String map=keyMapping.get(key);
				Object v=ReflectUtil.getProperty(me, map);
				if (v==null) continue; //ignore non-exist fields
				if (v instanceof Date) v="@"+LightUtil.encodeShortDate((Date)v);
				if (v instanceof Timestamp) v="@"+LightUtil.encodeLongDate((Timestamp)v);
				if (vals.size()>0) sql.append(" and ");
				sql.append(key+"=?");
				vals.add(v);
			}
			
			PreparedStatement stmt = null;
			try{
	        	stmt=conn.prepareStatement(sql.toString());
	        	for (int i=0;i<vals.size();i++){
	        		stmt.setObject(i+1, vals.get(i));
	        	}
	        	ResultSet rs=null;
	        	rs=stmt.executeQuery();

	            boolean res=rs.next();
	            rs.close();
	            if (res){
	            	throw new Exception("#[ReferenceExist]: #["+table.toUpperCase()+"]");
	            }
	        }
	        finally{
	        	if (stmt!=null) stmt.close();
	        	db.disconnect(conn);
			}
		}
	}
	
	@Override
	public void cascadeDelete(IEntity me) throws Exception{
		//System.out.println("CASCADE DEL="+me);
		int max=5000;
		IDataService data=me.getDataService();
		while (max==5000){
			List<IEntity> list=getReferentData(me,max);
			for (IEntity en: list){
				//System.out.println("  DEL "+en);
				data.deleteBean(en);
			}
			max=list.size();
		}
	}
	
	@Override
	public String toString(){
		return "{model:"+model+",table:"+table+",keyMapping:"+keyMapping+"}";
	}
}
