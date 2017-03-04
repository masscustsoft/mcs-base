package com.masscustsoft.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import com.masscustsoft.util.LightStr;
import com.masscustsoft.util.ThreadHelper;

public class XmlParser extends Parser{
	
	public XmlParser(String st){
		super(st);
	}
	
	public XmlParser(InputStream is, String charset) throws UnsupportedEncodingException{
		super(is, charset);
	}
	
	public XmlParser(Reader r){
		super(r,true);
	}
	
	//powerW
	@Override
	protected void processW(){
		do {
			str.append(ch);
			read();
			if (ch=='-'){
				read();
				if (ch!='-'){
					str.append('-');
				}
				else{
					unread(""+ch);
					ch='-';
					break;
				}
			}
		}
		while (ch=='_' || Character.isLetterOrDigit(ch)||ch=='$'||ch==':'||ch=='.');
	}
	
	public XmlNode readNode() throws IOException{
		XmlNode node=new XmlNode();
		parseNode(node);
		return node;
	}
	
	public void parseNode(XmlNode node) throws IOException{
		_parseNode(node);
		String href=node.getAttribute("href");
		if (href!=null && href.startsWith("cid:")){
			String xml=(String)ThreadHelper.get(href.substring(4));
			if (xml!=null){
				XmlParser xp=new XmlParser(xml);
				XmlNode sub=xp.readNode();
				node.getAttributes().clear();
				node.getAttributes().putAll(sub.getAttributes());
				node.setAttribute("class", sub.getTag());
				node.getChildren().clear();
				node.getChildren().addAll(sub.getChildren());
				node.setText(sub.getText());
			}
		}
	}
	private void _parseNode(XmlNode node) throws IOException{
		if (ttype!='<') throw new IOException("Invalid Xml");
		nextToken();
		if (ttype=='?'){
			skip("<");
			nextToken();
			nextToken();
		}
		if (ttype=='!'){
			nextToken();
			if (ttype=='-'){
				nextToken();
				if (ttype=='-'){
					skip("-->");
					nextToken();
					nextToken();
					return;
				}
				throwException("<!-- is required");
			}
			else
			if (ttype=='['){
				nextToken();
				if (str.toString().equals("CDATA")){
					nextToken();
					if (ttype=='['){
						nextToken();
						skip("]]>");
						node.setText(str.toString());
						nextToken();
						nextToken();
						return;
					}
				}
				throwException("<![CDATA[ is required");
			}
			throwException("<!-- or <![CDATA[ is required");
		}
		if (ttype=='W'){
			node.setTag(str.toString());
			nextToken();
			
			//processing attributes
			while (ttype=='W'){
				String attr=str.toString();
				nextToken();
				if (ttype=='='){
					nextToken();
					if (ttype=='W' || ttype=='S' || ttype=='N'){
						node.setAttribute(attr, LightStr.decodeHtml(str.toString()));
						nextToken();
					}
				}
				else{
					node.setAttribute(attr, "");
				}
			}
			
			//self closing tag
			if (ttype=='/'){
				nextToken();
				if (ttype=='>'){
					nextToken();
					return;
				}
				throwException("Expected /> for "+node.getTag());
			}
			
			//end tag header
			if (ttype=='>'){
				nextToken(true);
				String text="";
				for(;;){
					if (ttype!='<'){
						skip("<");
						if (str.length()>0){
							text+=LightStr.decodeHtml(str.toString());
							node.setText(text);
						}
						nextToken();
						continue;
					}
					nextToken();
					if (ttype=='/'){
						//closing the tag
						nextToken();
						if (ttype=='W'){
							String tag=str.toString(); 
							nextToken();
							if (ttype=='>'){
								if (tag.equals(node.getTag())){
									nextToken();
									return;
								}
							}
							throwException("Tag matching failed: <"+node.getTag()+"> vs </"+tag+">");
						}
						throwException("Close tag expected: </"+node.getTag()+"> but: </"+str);
					}
					else{
						unread("<"+str+ch);
						nextToken();
						//start content of node
						XmlNode child=new XmlNode();
						parseNode(child);
						if (child.getTag().length()>0) 
							node.addChildNode(child); 
						else if (child.getText()!=null){
							text+=child.getText();
							node.setText(text);
						}
					}
				}
			}
		}
		else
		if (ttype=='/'){
			nextToken();
			if (ttype=='>'){
				nextToken();
				return;
			}
			throwException("Close tag expected: /> for "+node.getTag()+"> but: /"+str);
		}
		throwException("Unknown delimeter '"+str+"' in tag: "+node.getTag()+", type="+(char)ttype+", buf="+buf);
	}
}
