package com.masscustsoft.service;

import java.util.List;
import java.util.Map;

import com.masscustsoft.api.IDataEnumeration;
import com.masscustsoft.api.IDataService;
import com.masscustsoft.api.IJoin;
import com.masscustsoft.api.IRefiner;
import com.masscustsoft.service.PageInfo;
import com.masscustsoft.util.LightUtil;

public class DataEnumeration<T> implements IDataEnumeration<T>{
	List<T> list=null;
	int index=0,next=0;
	int amount=0;
	Class<T>[] cs;
	String specific,text,sort=null;
	int batch;
	IDataService data;
	boolean raw;
	String sql=null;
	int from=0;
	int to=0;
	int listIdx=0;
	String facet;
	Map attributes=null;
	List<IJoin> joins=null;
	List<IRefiner> refiners=null;
	PageInfo page=null;
	
	public DataEnumeration(IDataService data,Class<T> c,String specific,String text,String sort, int batch, String facet, List<IJoin> joins, List<IRefiner> refiners, boolean raw){
		this(data,c,specific,text,sort,batch,0,0,facet,joins,refiners,raw);
	}
	
	public DataEnumeration(IDataService data,Class<T> c,String specific,String text,String sort, int batch, int from, int to, String facet, List<IJoin> joins, List<IRefiner> refiners, boolean raw){
		this.data=data;
		this.specific=specific;
		this.text=text;
		this.batch=batch;
		this.sort=sort;
		if (c!=null && !Map.class.isAssignableFrom(c)) cs=new Class[]{c}; else cs=new Class[]{};
		this.raw=raw;
		this.from=from;
		this.to=to;
		this.facet=facet;
		this.joins=joins;
		this.refiners=refiners;
	}
	
	public DataEnumeration(IDataService data,String sql,String specific,String sort, int batch, List<IJoin> joins, List<IRefiner> refiners, boolean raw){
		this(data,sql,specific,sort,batch,0,0,joins,refiners,raw);	
	}
	
	public DataEnumeration(IDataService data,String sql,String specific,String sort, int batch, int from, int to, List<IJoin> joins, List<IRefiner> refiners, boolean raw){
		this.data=data;
		this.specific=specific;
		this.sql=sql;
		this.batch=batch;
		this.sort=sort;
		cs=null;
		this.raw=raw;
		this.from=from;
		this.to=to;
		this.joins=joins;
		this.refiners=refiners;
	}
	
	public boolean hasMoreElements() throws Exception{
		if (list==null){
			index=from;
			if (cs==null){
				page=(PageInfo) data.getBeanListBySql(sql,specific,from,batch,sort,facet,raw);
			}
			else{
				page=(PageInfo) data.getBeanList(cs,specific,text,from,batch,sort,facet,raw);
			}
			//support page level refiner
			if (refiners!=null && refiners.size()>0){
				List<Map> lst=(List)LightUtil.toJsonObject(page.getList(),0);
				page.setList(lst);
				for (IRefiner p:refiners){
					String txt=text; if (cs==null|| cs.length==0) txt="@@"+sql;
					if (p!=null) p.refineData(data, cs, (Map)LightUtil.parseJson(specific), txt, sort, facet, raw, page);
				}
			}
			list=page.getList();
			
			amount=page.getAmount();
			attributes=page.getAttributes();
			//
			next=list.size();
			listIdx=0;
			return (list.size()>0);
		}
		if (listIdx<list.size() && (to<=0|| index<to)){
			return true;
		}
		if (index+1>=amount) return false;

		if (cs==null){
			page=(PageInfo<T>) data.getBeanListBySql(sql,specific,next,batch,sort,facet,raw);
			list=page.getList();
			listIdx=0;
		}
		else{
			page=(PageInfo<T>) data.getBeanList(cs,specific,text,next,batch,sort,facet,raw);
			list=page.getList();
			listIdx=0;
		}
		next+=list.size();
		if (list.size()>0) return true;
		return false;
	}

	public T nextElement() throws Exception {
		if (listIdx>=list.size()) return null;
		T t=list.get(listIdx);
		if (joins!=null && joins.size()>0||refiners!=null && refiners.size()>0) t=(T)data.doJoin(t,joins,refiners);
		index++; listIdx++;
		return t;
	}

	public int getAmount() {
		return amount;
	}

	public int getIndex(){
		return index;
	}

	public Map getAttributes() {
		return attributes;
	}

	public void setAttributes(Map attributes) {
		this.attributes = attributes;
	}

	public PageInfo getPage() {
		return page;
	}

}
