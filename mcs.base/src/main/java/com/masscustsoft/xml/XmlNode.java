package com.masscustsoft.xml;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.masscustsoft.util.LightStr;
import com.masscustsoft.util.LightUtil;

public class XmlNode {
	private Map<String,String> attributes=new TreeMap<String,String>();
	private List<XmlNode> children=new ArrayList<XmlNode>();
	private Object text="";
	private String tag="";
	
	public XmlNode(){
		
	}
	
	public XmlNode(String tag){
		setTag(tag);
	}
	
	public XmlNode(String tag,String val){
		setTag(tag);
		setText(val);
	}
	
	public XmlNode(String tag,String attr, String val, String... atts){
		setTag(tag);
		setAttribute(attr,val);
		for (int i=0;i<atts.length;i+=2){
			setStr(atts[i],atts[i+1], null);
		}
	}
	
	public Object getText() {
		return text;
	}
	
	public void setText(Object text) {
		this.text = text;
	}
	
	public String getTag() {
		return tag;
	}
	
	public void setTag(String tag) {
		this.tag = tag;
	}
	
	public void setInt(String attr,Integer val){
		if (val!=null) setAttribute(attr,val+"");
	}
	
	public void setStr(String attr,String val){
		setStr(attr,val,"");
	}
	
	public void setStr(String attr,String val, String def){
		if (LightStr.isEmpty(val)) val=null;
		else if (val.equals(def)) return;
		setAttribute(attr,val);
	}
	
	public void setBool(String attr, String val, String def){
		if (LightStr.isEmpty(val)) return;
		if (val.equals(def)) return;
		setStr(attr, "yes".equals(val)||"true".equals(val)||"Y".equals(val)?"true":"false", null);
	}
	
	public void setAttribute(String attr,String val){
		if (val==null){
			attributes.remove(attr);
			return;
		}
		int i=attr.indexOf(".");
		if (i>0){
			String aa=attr.substring(0,i);
			String bb=attr.substring(i+1);
			for (XmlNode s:children){
				if (s.getTag().equalsIgnoreCase(aa)){
					s.setAttribute(bb,val);
					return;
				}
			}
			return; //not found, ignore	
		}
		for (XmlNode s:children){
			if (s.getTag().equalsIgnoreCase(attr)){
				s.setText(val);
				return;
			}
		}
		String att=attr;
		for (String a:attributes.keySet()){
			if (a.equalsIgnoreCase(attr)) {att=a; break;}
		}
		attributes.put(att,val);
	}
	
	public String getAttribute(String attr){
		return attributes.get(attr);
	}
	
	public void removeAttribute(String attr){
		attributes.remove(attr);
	}
	
	public XmlNode addChildNode(XmlNode child){
		children.add(child);
		return child;
	}
	
	public void removeChild(String child) {
		XmlNode tar=null;
		for (XmlNode ch:children){
			if (child.equals(ch.getTag())){
				tar=ch;
				break;
			}
		}
		if (tar!=null) children.remove(tar);
	}
	
	public void addOptionalChild(String tag, String... attrs){
		for (int i=0;i<attrs.length;i+=2){
			String val=attrs[i+1];
			if (LightStr.isEmpty(val)) return;
		}
		XmlNode node=new XmlNode(tag);
		for (int i=0;i<attrs.length;i+=2){
			String val=attrs[i+1];
			node.setAttribute(attrs[i], val);
		}
		addChildNode(node);
	}
	
	public Set<String> getAttributeKeySet(){
		return attributes.keySet();
	}
	
	public List<XmlNode> getChildrenList(){
		return children;
	}

	public Map<String, String> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}

	public List<XmlNode> getChildren() {
		return children;
	}

	public void setChildren(List<XmlNode> children) {
		this.children = children;
	}

	public XmlNode getChild(String tag){
		XmlNode vars=null;
		for (XmlNode sub:getChildrenList()){
			if (sub.getTag().equals(tag)){
				vars=sub; break;
			}
		}
		return vars;
	}
	
	public XmlNode setChild(String tag, String val){
		XmlNode sub=getChild(tag);
		if (sub==null){
			sub=new XmlNode();
			sub.setTag(tag);
			addChildNode(sub);
		}
		sub.setText(val);
		return sub;
	}
	
	public void setObject(Object val){
		if (val instanceof java.sql.Date){
			setAttribute("_","P");	
		}
		else
		if (val instanceof java.sql.Timestamp){
			setAttribute("_","D");
		}
		else
		if (val instanceof Long){
			setAttribute("_","L");
		}
		else
		if (val instanceof Double || val instanceof BigDecimal){
			setAttribute("_","N");
		}
		else
		if (val instanceof Integer){
			setAttribute("_","I");
		}
		else
		if (val instanceof Float){
			setAttribute("_","F");
		}
		else
		if (val instanceof Boolean){
			setAttribute("_","B");
		}
		setText(LightUtil.encodeObject(val));
	}
	
	@Override
	public String toString(){
		StringBuffer xml=new StringBuffer("{XmlNode");
		if (!LightStr.isEmpty(tag)) xml.append(" .tag="+tag);
		if (text!=null) xml.append(" .text="+text);
		if (attributes.size()>0) xml.append(" .attributes="+attributes);
		if (children.size()>0) xml.append(" .children="+children);
		xml.append("}");
		return xml.toString();
	}
	public String getItemStr(String tags){
		return (String)getItem(tags);
	}
	
	public Object getItem(String tags){
		String tag=tags;
		int i=tags.indexOf(".");
		if (i>0){
			tag=tags.substring(0,i);
			tags=tags.substring(i+1);
		}
		else tags=null;
		
		String val=this.getAttribute(tag);
		if (val==null){
			XmlNode child = this.getChild(tag);
			if (child==null) return null;
			if (tags==null) return child.getText()+"";
			return child.getItem(tags);
		}
		if (tags==null) return val;
		return null;
	}

	public XmlNode getNode(String tags){
		String tag=tags;
		int i=tags.indexOf(".");
		if (i>0){
			tag=tags.substring(0,i);
			tags=tags.substring(i+1);
		}
		else tags=null;
		
		XmlNode child = this.getChild(tag);
		if (child==null) return null;
		if (tags==null) return child;
		return child.getNode(tags);
	}
	
	public List<XmlNode> getNodeList(String tags){
		int idx=tags.lastIndexOf(".");
		String tar=tags;
		if (idx>0){
			tar=tags.substring(idx+1);
			tags=tags.substring(0,idx);
		}
		else{
			tags=null;
		}
		List<XmlNode> nodes=new ArrayList<XmlNode>();
		XmlNode child = tags==null?this:getNode(tags);
		if (child!=null){
			for (XmlNode node:child.getChildren()){
				if (tar==null || tar.equals(node.getTag()) || tar.equals(node.getAttribute("class"))){
					nodes.add(node);
				}
			}
		}
		return nodes;
	}
	
	private void add(TreeMap tm, String key, Object val){
		Object v=tm.get(key);
		if (v==null) tm.put(key, val);
		else{
			if (v instanceof List){
				List ll=(List)v;
				ll.add(val);
			}
			else{
				List ll=new ArrayList();
				ll.add(v);
				ll.add(val);
				tm.put(key,ll);
			}
		}
	}
	
	public Map toJsonObject(){
		TreeMap tm=new TreeMap();
		if (text!=null && !"".equals(text)) add(tm,"text", text);
		for (String k:attributes.keySet()){
			String val=attributes.get(k);
			String key=LightStr.camel(k);
			if (!LightStr.isEmpty(val)) add(tm,key,val);
		}
		for (XmlNode ch:children){
			//System.out.println("child="+ch.children+", attrs="+ch.attributes);
			if (ch.children.size()==0 && ch.attributes.size()==0){
				String name=LightStr.camel(ch.getTag());
				String val=(String)ch.getText();
				if (!LightStr.isEmpty(val)) tm.put(name,val);
			}
			else{
				Map kid=ch.toJsonObject();
				if (kid.size()==1 && kid.get("text")!=null) add(tm,ch.getTag(),kid.get("text"));
				else add(tm,LightStr.camel(ch.getTag()),kid);
			}
		}
		return tm;
	}
	
	public String toXml() throws IOException{
		StringWriter str=new StringWriter();
		XmlWriter w=new XmlWriter(str);
		w.write(this,0);
		return str.getBuffer().toString();
	}

	private void cloneAttributesTo(XmlNode kid){
		for (String key:attributes.keySet()){
			if (kid.getAttribute(key)!=null) continue;
			kid.setAttribute(key, attributes.get(key));
		}
		for (XmlNode node:children){
			Object text=node.getText();
			if (text==null) continue;
			if (LightStr.isEmpty(text.toString())) continue;
			if (kid.getAttribute(node.getTag())!=null) continue;
			kid.setAttribute(node.getTag(), text.toString());
		}
	}
	
	public void filter(String tag2, List<XmlNode> ll) {
		if (tag2.equals(tag)) ll.add(this);
		else{
			for (XmlNode kid:children){
				cloneAttributesTo(kid);
				kid.filter(tag2, ll);
			}
		}
		
	}
}
