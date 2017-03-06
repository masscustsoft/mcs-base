package com.masscustsoft.service;

import java.util.ArrayList;
import java.util.List;

public class SqlSegment{
	StringBuffer sql=new StringBuffer();
	List<Object> vals=new ArrayList<Object>();
	
	public void add(Object p) {
		vals.add(p);
	}

	public void append(String s) {
		sql.append(s);
	}

	public void replaceBy(SqlSegment q2) {
		set(q2.sql.toString());
		vals.clear();
		vals.addAll(q2.vals);
	}
	
	public void merge(SqlSegment q2, String conj) {
		if (q2.length()>0){
			if (length()>0) sql.append(conj);
			sql.append("("+q2.sql+")");
			vals.addAll(q2.vals);
		}
	}

	public int length() {
		return sql.length();
	}

	public void set(String s) {
		sql.delete(0, sql.length());
		sql.append(s);
	}
	
	@Override
	public String toString(){
		return sql.toString();
	}
	
	public String getFrom(){
		int i=sql.indexOf("from");
		int j=sql.indexOf("where");
		if (j<0) return sql.substring(i+4).trim();
		return sql.substring(i+4,j).trim();
	}
	
	public String getWhere(){
		int j=sql.indexOf("where");
		if (j<0) return "";
		return sql.substring(j+5).trim();
	}
	
	public String getFields(){
		int i=sql.indexOf("select");
		int j=sql.indexOf("from");
		return sql.substring(i+6,j).trim();
	}
}