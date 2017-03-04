package com.masscustsoft.xml;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;

public class JsonWriter {
	Writer w;
	
	public JsonWriter(Writer w){
		this.w=w;
	}
	
	public void write(XmlNode node) throws IOException{
		write(node,"");
	}
	
	private void write(Object o) throws IOException{
		if (o instanceof Integer || o instanceof Double || o instanceof Long || o instanceof Boolean || o instanceof BigDecimal) w.write(o.toString());
		else{
			w.write("\""+o.toString()+"\"");
		}
	}
	
	public void write(XmlNode node,String spc) throws IOException{
		//recongnize map or list or element
		if (node.getTag().equals("list")){
			w.write("[");
			boolean first=true;
			for (XmlNode child:node.getChildrenList()){
				if (!first) write(",");
				first=false;
				write(child,spc);
			}
			w.write("]");
		}
		else
		if (node.getAttributeKeySet().size()>0 || node.getChildrenList().size()>0){ //map
			w.write("{");
			boolean first=true;
			for (String attr:node.getAttributeKeySet()){
				if (!first) write(",");
				first=false;
				write(attr);
				write(":");
				write(node.getAttribute(attr));
			}
			for (XmlNode child:node.getChildrenList()){
				if (!first) write(",");
				first=false;
				write(node.getTag());
				write(":");
				write(child,spc);
			}
			w.write("}");
		}
		else{
			//primary item only in text
			Object o=node.getText();
			write(o);
		}
	}
}
