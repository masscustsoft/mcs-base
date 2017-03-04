package com.masscustsoft.xml;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import com.masscustsoft.util.GlbHelper;
import com.masscustsoft.util.LightStr;

public class XmlWriter {
	Writer w;
	
	public XmlWriter(Writer w){
		this.w=w;
	}
	
	public void write(XmlNode node, int op) throws IOException{
		if ((op&1)==0) writeCompact(node,false,"");
		else writeVerbose(node,false,"");
	}
	
	private void writeCompact(XmlNode node,boolean newLine,String spc) throws IOException{
		if (newLine) w.write("\r\n");
		w.write(spc+"<"+node.getTag());
		String ref=node.getAttribute("ref");
		if (ref!=null) {
			w.write(" ref='"+ref+"'");
			node.getAttributes().clear();
			node.getChildren().clear();
			node.setText("");
		}
		String cls=node.getAttribute("class");
		if (cls!=null) {
			w.write(" class='"+cls+"'");
			node.getAttributes().remove("class");
		}
		int sz=0,max=160;
		for (String key:node.getAttributeKeySet()){
			String val=node.getAttribute(key);
			if (val!=null){
				if (sz>max){
					w.append("\r\n"+spc);
					sz=0;
				}
				w.write(" "+key); sz+=key.length()+1;
				String v="\""+LightStr.encodeHtml(val)+"\"";
				w.write("="+v); sz+=v.length();
			}
		}
		List<XmlNode> children=node.getChildrenList();
		String text=node.getText().toString();
		if (children.size()==0 && text.length()==0){
			w.write("/>");	
		}
		else{
			w.write(">");
			text=text.trim();
			if (text.length()>0){
				writeBody(text);
			}
			for (XmlNode child:children){
				writeCompact(child,true,spc+" ");
			}
			if (children.size()>0) w.write("\r\n"+spc);
			w.write("</"+node.getTag()+">");
				
		}
	}
	
	private void writeVerbose(XmlNode node,boolean newLine,String spc) throws IOException{
		if (newLine) w.write("\r\n");
		
		w.write(spc+"<"+node.getTag());
		String ref=node.getAttribute("ref");
		if (ref!=null) {
			w.write(" ref='"+ref+"'");
			node.getAttributes().clear();
			node.getChildren().clear();
			node.setText("");
		}
		String _=node.getAttribute("_");
		if (_!=null) {
			w.write(" _='"+_+"'");
			node.getAttributes().clear();
		}
		String text=node.getText().toString();
		String cls=node.getAttribute("class");
		if (cls!=null) {
			w.write(" class='"+cls+"'");
			node.getAttributes().remove("class");
		}
		
		boolean hasAttrs=node.getAttributes().size()+node.getChildren().size()>0;
		if (!hasAttrs && LightStr.isEmpty(text)){
			w.write("/>");
			return;
		}
		w.write(">");
		for (String key:node.getAttributeKeySet()){
			String val=node.getAttribute(key);
			if (val==null) continue;
			w.write("\r\n"+spc+" <"+key+">"+LightStr.encodeHtml(val)+"</"+key+">");
		}
		List<XmlNode> children=node.getChildrenList();
		
		for (XmlNode child:children){
			writeVerbose(child,true,spc+" ");
		}
		if (hasAttrs && text.length()>0) w.write("\r\n"+spc+" ");
		writeBody(text);
		if (hasAttrs) w.write("\r\n"+spc);
		w.write("</"+node.getTag()+">");
	}
	
	private void writeBody(String text) throws IOException{
		if ("true".equals(GlbHelper.get("XmlBodyEncode"))){
			w.write(LightStr.encodeHtml(text));
		}
		else{
			if (text.contains("<")||text.contains(">")||text.contains("%")||text.contains("&")){
				w.write("<![CDATA["+text+"]]>");
			}
			else w.write(text);
		}
	}
}
