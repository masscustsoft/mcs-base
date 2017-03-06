package com.masscustsoft.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.masscustsoft.util.MapUtil;
import com.masscustsoft.util.ReflectUtil;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.LightStr;
import com.masscustsoft.util.ThreadHelper;

public class ReportTableHelper {
	List<List<Map>> rows=new ArrayList();
	String widths="";
	List<Map> fields=new ArrayList();
	List<Map> data=new ArrayList();
	
	ReportHelper rpt;
	public ReportTableHelper(ReportHelper rpt){
		this.rpt=rpt;
	}
	
	public int scanHdr(List<Map> items,int y) throws Exception{
		while (y>=rows.size()) rows.add(new ArrayList());
		List<Map> row=rows.get(y);
		int cols=0;
		for (int i=0;i<items.size();i++){
			Map it=items.get(i);
			if (it.get("items")==null){
				int width=MapUtil.getInt(it,"width",80);
				if (widths.length()>0) widths+=","; widths+=width;
				fields.add(it);
				String lbl=(String)it.get("label");
				if (lbl==null){
					lbl=(String)it.get("dataIndex");
					if (lbl!=null) lbl=LightStr.capitalize(lbl); else lbl="";
				}
				lbl=GlbUtil.i18n(lbl);
				row.add((Map)LightUtil.parseJson("{text:'"+lbl+"',rowspan:-"+y+"}"));
				cols++;
			}
			else{
				int cs=scanHdr((List)it.get("items"),y+1);
				row.add((Map)LightUtil.parseJson("{text:'"+GlbUtil.i18n((String)it.get("label"))+"',colspan:"+cs+"}"));
				cols+=cs;
			}
		}
		return cols;
	}
	
	void fillHeader(){
		for (int i=0;i<rows.size();i++){
			List<Map> row = rows.get(i);
			for (int j=0;j<row.size();j++){
				Map c=row.get(j);
				if (c.get("rowspan")!=null){
					long rs=(Long)c.get("rowspan")+rows.size();
					c.put("rowspan",rs);
					if (rs==1) c.remove("rowspan");
				}
				c.put("bold",true);
				c.put("align","center");
				c.put("valign","middle");
				data.add(c);
			}
		}
	}
	
	public void fillData(List<Object> rows) throws Exception{
		for (Object row:rows){
			Map r=(Map)LightUtil.toJsonObject(row);
			for (Map fld:fields){
				Map c=new HashMap();
				Object val=ReflectUtil.getProperty(row, (String)(fld.get("dataIndex")));
				String fmt=(String)fld.get("fmt");
				String ref=(String)fld.get("ref");
				if (ref!=null){
					c.put("id",rpt.convert(ref, r));
				}
				String gt=(String)fld.get("goto");
				if (gt!=null){
					c.put("goto",rpt.convert(ref, r));
				}
				if (!LightStr.isEmpty(fmt)){
					ThreadHelper.set("_value_", val);
					if (!LightStr.isEmpty(fmt)){
						int k=fmt.indexOf("(");
						if (k>=0){
							val=LightUtil.macro("${Unify."+fmt.substring(0, k)+"(_value_,"+fmt.substring(k+1)+"}",'$',r);	
						}
						else{
							String exp= "${Unify."+fmt+"(_value_)}";
							val=LightUtil.macro("${Unify."+fmt+"(_value_)}",'$',r).toString();
						}
					}	
				}
				else
				if (fld.get("value")!=null){
					val=rpt.convert((String)fld.get("value"), r);
				}
				else val=val+"";
				
				c.put("text",val);
				String align=(String)fld.get("align");
				if (align!=null) c.put("align",align);
				c.put("valign","middle");
				data.add(c);
			}
		}
	}

}
