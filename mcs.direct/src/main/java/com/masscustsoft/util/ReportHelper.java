package com.masscustsoft.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.masscustsoft.service.DirectAction;

public class ReportHelper {
	Map rpt=new HashMap();
	DirectAction ctx;
	
	public ReportHelper(DirectAction a) throws Exception{
		ctx=a;
		rpt.put("_idx_",1);
		rpt.put("items", new ArrayList());
		rpt.put("fills", new ArrayList());
		rpt.put("vars", new HashMap());
		
		if (rpt.get("overlays")==null){
			rpt.put("overlays",(List)LightUtil.parseJson("["
					+ "{text : '${pageNumber} / ${pageCount}',alignment : 'right',y: 20,w : -30,	wPer : 100}, "
					+ "{text : '"+LightUtil.encodeDate(LightUtil.longDate(), "yyyy-MM-dd HH:mm")+"',y : 20,	x : 30},"
					+ "{text : 'By "+ctx.getSession().getUserId()+"', position : 'top',y : -30, x : -30, wPer : 100,	alignment : 'right'}"
					+ "]"));
		}
	}
	
	public String convert(String s){
		return convert(s,null);
	}
	
	public String convert(String s, Map m){
		return LightStr.convert(s, m);
	}
	
	public void close(){
		Integer idx=getIdx();
		Map vars=getVars();
		if (rpt.get("defaultFontSize")==null) rpt.put("defaultFontSize",8);
		if (rpt.get("pageSize")==null) rpt.put("pageSize","A4");
		if (idx!=null){
			vars.put("ChapterCount", idx-1);
		}
		List<Map> items=(List)rpt.get("items");
		closeItems(items,vars);
	}
	
	private void closeItems(List<Map> items,Map vars){
		for (Map m:items){
			String id=(String)m.get("id");
			if (id!=null){
				Object v=vars.get(id);
				if (v==null) v="";
				String tit=(String)m.get("text");
				if (tit!=null){
					String val=v.toString();
					if (val.length()>0) val="#"+val;
					m.put("text",tit.replace("$$0$$", val).replace("$$1$$", vars.get("ChapterCount").toString()));
				}
			}
			if (m.get("items")!=null){
				closeItems((List)m.get("items"),vars);
			}
		}
	}
	
	public void addTitle(String gotoId, String title, int size) throws Exception{
		addTitle(gotoId,title,size,"center");
	}
	
	public void addTitle(String gotoId, String title, int size, String align) throws Exception{
		Integer idx=MapUtil.getInt(rpt,"_idx_",null);
		List items=getItems();
		List fills=(List)rpt.get("fills");
		{
			Map r=(Map)LightUtil.parseJson("{id:'ChapterCount',alignment:'"+align+"',fontSize:"+size+",bold:true,spacingAfter:6,spacingBefore:6}");
			String prefix="";
			if (idx!=null) prefix=idx+" ";
			r.put("text", prefix+convert(title));
			if (gotoId!=null) r.put("label", gotoId);
			items.add(r);
			fills.add(r);
		}
		if (idx!=null){
			idx++;
			rpt.put("_idx_", idx);
		}
	}
	
	public void addTable(List<Map> headers, List rows, int fontSize) throws Exception{
		Map m=new HashMap();
		m.put("pushFontSize", fontSize);
		getItems().add(m);
		addTable(headers,rows);
		m=new HashMap();
		m.put("popFontSize", 0);
		getItems().add(m);
	}
	public void addTable(List<Map> headers, List rows) throws Exception{
		List items=getItems();
		
		ReportTableHelper helper=new ReportTableHelper(this);
		helper.scanHdr(headers, 0);
		helper.fillHeader();
		helper.fillData(rows);
		
		Map tbl=(Map)LightUtil.parseJson("{cls:'table',spacingBefore:6,widthList:'"+helper.widths+"',widthPercentage:100,headerRows:"+helper.rows.size()+"}");
		tbl.put("items", helper.data);
		items.add(tbl);
	}
	
	public void newPage() throws Exception{
		getItems().add((Map)LightUtil.parseJson("{cls:'newpage'}"));
	}

	public Map getVars() {
		return (Map)rpt.get("vars");
	}

	public Integer getIdx() {
		return (Integer)rpt.get("_idx_");
	}

	public Map getRpt() {
		return rpt;
	}

	public List<Map> getItems(){
		return (List)rpt.get("items");
	}
	
	public void add(ReportFormHelper frm) throws Exception {
		Map tbl=(Map)LightUtil.parseJson("{cls:'table',widthList:'50,100',widthPercentage:60,headerRows:0}");
		tbl.put("items", frm.data);
		getItems().add(tbl);
		
	}
	
	public void add(Map r) throws Exception {
		getItems().add(r);
	}
	
	public List<Map> newTable(String jsonTbl) throws Exception{
		Map tbl=(Map)LightUtil.parseJson(jsonTbl);
		List<Map> items=new ArrayList();
		tbl.put("items", items);
		add(tbl);
		return items;
	}
}

