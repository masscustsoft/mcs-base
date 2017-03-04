package com.masscustsoft.xml;

import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

public class JsonParser extends Parser{
	
	public JsonParser(String st){
		super(st);
	}
	
	public JsonParser(InputStream is, String charset) throws UnsupportedEncodingException{
		super(is, charset);
	}
	
	public JsonParser(Reader r){
		super(r,true);
	}
	/**
	 * main entry for XML Parser
	 * @return
	 */
	public XmlNode readNode(){
		XmlNode node=new XmlNode();
		parseNode(node);
		return node;
	}
	
	private void parseNode(XmlNode node){
		if (ttype=='S'){
			String res=str.toString();
			node.setText(res);
			nextToken();
			return;
		}
		if (ttype=='W'){
			String res=str.toString();
			nextToken();
			if (res.equals("true")||res.equals("false")) node.setText(Boolean.parseBoolean(res));
			else node.setText(res);
		}
		if (ttype=='N'){
			String res=str.toString();
			nextToken();
			if (res.indexOf(".")>=0) node.setText(Double.parseDouble(res));
			else node.setText(Long.parseLong(res));
		}
		if (ttype=='{'){ //map here.
			nextToken();
			while (true){
				if (ttype=='}' || ttype==0){
					if (node.getTag()==null){
						if (node.getAttribute("class")!=null){
							node.setTag(node.getAttribute("class"));
							node.removeAttribute("class");
						}
						else node.setTag("map");
					}
					nextToken();
					return;
				}
				if (ttype=='S' || ttype=='W'){
					String key=str.toString();
					nextToken();
					if (ttype==':' || ttype=='='){
						nextToken();
						XmlNode child=new XmlNode();
						parseNode(child);
						child.setTag(key);
						node.addChildNode(child);
					}
					if (ttype==','){
						nextToken();
					}
				}
			}
		}
		if (ttype=='['){
			nextToken();
			node.setAttribute("class","list");
			while (true){
				if (ttype==']' || ttype==0){
					nextToken();
					return;
				}
				XmlNode child=new XmlNode();
				parseNode(child);
				child.addChildNode(child);
				if (ttype==','){
					nextToken();
				}
			}
		}

		throwException("unknown delimeter '"+(char)ttype+"',type="+ttype+", buf="+buf);
	}
}
