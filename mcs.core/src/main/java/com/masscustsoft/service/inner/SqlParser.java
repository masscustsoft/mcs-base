package com.masscustsoft.service.inner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.masscustsoft.service.SqlSegment;
import com.masscustsoft.util.MapUtil;
import com.masscustsoft.xml.Parser;

public class SqlParser extends Parser{
	Map<String,Object> terms=null;
	List<String> removes=new ArrayList<String>();
	SqlSegment seg;
	
	public SqlParser(String sql){
		super(sql,false);
	}
	
	//parse :params into values and replace as ?
	public SqlParser(String sql,Map<String,Object> terms, SqlSegment seg){
		super(sql,false);
		this.terms=terms;
		this.seg=seg;
	}
	
	@Override
	public Object processVar(String scr){
		List<String> vars=new ArrayList<String>();
		List<String> sets=new ArrayList<String>();
		while(true){
			int i=scr.indexOf(':');
			if (i<0) break;
			int j=i+1;
			for (;j<scr.length();j++){
				char ch=scr.charAt(j);
				if (!Character.isJavaIdentifierPart(ch)) break;
			}
			String var=scr.substring(i+1,j);
			//continue to search for a = and a constant
			if (j<scr.length() && scr.charAt(j)=='='){
				int k=j+1;
				if (scr.charAt(k)=='\''){
					for (k++;k<scr.length();k++){
						char ch=scr.charAt(k);
						if (!Character.isJavaIdentifierPart(ch)) break;
					}							
				}
				String key=scr.substring(j+2,k);
				if (terms!=null && !key.equals(terms.get(var))){
					return "";
				}
			}
			//deal with in (:aaa)
			String qq="";
			if (i>0 && j<scr.length() && scr.charAt(i-1)=='(' && scr.charAt(j)==')' && terms!=null){
				sets.add(var);
				String vv=terms.get(var)+"";
				List<String> ss=MapUtil.getSelectList(vv);
				for (String s:ss){
					if (qq.length()>0) qq+=",";
					qq+="?";
				}
			}
			else{
				qq="?";
			}
			scr=scr.substring(0,i)+qq+scr.substring(j);

			vars.add(var);
		}
		if (terms!=null){
			for (String v:vars){
				Object vv=terms.get(v);
				if (vv==null || vv.equals("")) return "";
			}
			for (String v:vars){
				Object vv=terms.get(v);
				if (sets.contains(v)){
					List<String> ss=MapUtil.getSelectList(vv.toString());
					for (String s:ss){
						seg.add(s);
					}
				}
				else{
					seg.add(vv+"");
				}
				removes.add(v);
			}
		}
		else removes.addAll(vars);
		return scr;
	}
	
	public List<String> getVarList(){
		return removes;
	}
	
	public void doRemove(){
		for (String v:removes){
			terms.remove(v);
		}
	}
	
}
