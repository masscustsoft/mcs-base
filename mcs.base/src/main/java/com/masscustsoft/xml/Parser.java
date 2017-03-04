package com.masscustsoft.xml;

/**
 * Sep 23, 2008:
 * 	$ cannot be part of a word. because macro need to recognize ${} as a brace.
 */
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.ScriptUtil;
import com.masscustsoft.util.ThreadHelper;

public class Parser {
	protected Reader r;
	protected char ch=0;
	public int ttype; //W=word, S=string, -1=EOF
	public final int EOF=0xffff;
	protected char quoteChar='\"';
	protected boolean skipSpace=true,hasCr=false,extractEscape=true,skipQuotes=false;
	protected StringBuffer str=new StringBuffer();
	protected StringBuffer errors=new StringBuffer();
	protected StringBuffer buf=new StringBuffer();
	
	public Parser(){
		this("",true);
	}
	
	public Parser(String st){
		this(new StringReader(st.trim()),true);
	}
	
	public Parser(String st,boolean skip){
		this(new StringReader(st.trim()),skip);
	}
	
	public Parser(InputStream is, String charset) throws UnsupportedEncodingException{
		this(new InputStreamReader(is, charset),true);
	}
	
	public Parser(Reader r,boolean skip){
		this.skipSpace=skip;
		this.r=r;
		nextToken();
	}

	public void unread(String st){
		//System.out.print("["+st+"]");
		buf.insert(0, st);
		read();
	}
	
	public void read(){
		if (buf.length()>0){
			ch=buf.charAt(0);
			buf.delete(0,1);
		}
		else{
			try {
				ch=(char)r.read();
			} catch (IOException e) {
				ch=(char)EOF;
			}
		}
		//System.out.print(ch);
	}
	
	protected void processW(){
		do {
			str.append((char)ch);
			read();
		}
		while (ch=='_' || Character.isLetterOrDigit(ch));
	}
	
	protected void processS(){
		quoteChar=(char)ch;
		read();
		while (ch!=quoteChar && ch!=EOF){
			if (ch=='\\') {
				if (extractEscape){
					read();
					if (ch=='n') str.append('\n');
					else
					if (ch=='r') str.append('\r');
					else
					if (ch=='t') str.append('\t');
					else
					if (ch=='u'){
						read(); char ch0=ch;
						read(); char ch1=ch;
						read(); char ch2=ch;
						read(); char ch3=ch;
						int unicode=Integer.parseInt(""+ch0+ch1+ch2+ch3, 16);
						str.append((char)unicode);
					}
					else{
						str.append('\\');
						str.append((char)ch);
					}
				}
				read();
			}
			else{
				str.append((char)ch);
				read();
			}
		}
		read(); //ready for next
	}

	protected void processN(){
		do {
			str.append((char)ch);
			read();
		}
		while (ch=='.' || Character.isDigit(ch));
	}
	
	private boolean isSpace(int ch){
		return ch==0 || skipSpace && Character.isWhitespace(ch) || ch==65279;
	}
	
	private void _nextToken(){
		ttype=EOF;
		hasCr=false;
		str.setLength(0);
		while (isSpace(ch)){
			read();
			if (ch=='\n') hasCr=true;
		}
		if (Character.isDigit(ch)){
			ttype='N';
			processN();
			return;
		}
		if (ch=='-'){
			read();
			if (Character.isDigit(ch)){
				ttype='N';
				processN();
				str.insert(0, '-');
				return;
			} 
			else{
				unread("-"+ch);
			}
		}
		if (ch=='_' || Character.isLetterOrDigit(ch)){
			ttype='W';
			processW();
			return;
		}
		if (skipSpace && !skipQuotes && (ch=='\"' || ch=='\'')){ //for String 
			ttype='S';
			processS();
			return;
		}
		//for else
		ttype=ch;
		str.append((char)ch);
		read();
		
	}
	
	public final void nextToken(){
		_nextToken();
	}
	
	public final void nextToken(boolean skipQuotes){
		boolean sq=this.skipQuotes;
		this.skipQuotes=skipQuotes;
		_nextToken();
		this.skipQuotes=sq;
	}
	
	protected void skip(String tk){
		StringBuffer back=new StringBuffer();
		outer:
		while (back.length()==0){
			if (ch==EOF) return;
			for (int i=0;i<tk.length();i++){
				back.append((char)ch);
				if (ch==tk.charAt(i)){
					read();
					if (ch==EOF) break outer;
					continue;
				}
				else{
					str.append(back.substring(0,1));
					unread(back.substring(1));
					back.setLength(0);
					break; //move on
				}
			}
		}
		//to here, fully match, refeed the last read
		
		String kk=tk.substring(tk.length()-1);
		unread(kk+(char)ch);
	}
	
	public void throwException(String err){
		//LogUtil.info("Parsing Exception: "+err);
		errors.append("Exception: "+err+"\n");
		throw new RuntimeException();
	}
	
	public void readScript(StringBuffer buf){
		boolean skip=skipSpace;
		skipSpace=false;
		int nest=0;
		while (ttype!=EOF){
			if (nest==0 && ttype=='}') break;
			//make sure the <> not be recognize as the end of the script, but never effect in a string
			if (ttype=='(' || ttype=='{') nest++; 
			else
			if (ttype==')' || ttype=='}') nest--;
			if (ttype=='S'){
				buf.append(quoteChar);
				buf.append(str);
				buf.append(quoteChar);
			}
			else buf.append(str);
			nextToken();
		}
		skipSpace=skip;
		if (isSpace(ttype)) nextToken();
	}
	
	public List<String> splitVars(char macroKey){ // '$' or '#' '@', '!' '*'
		boolean skip=skipSpace;
		skipSpace=false;
		List<String> res=new ArrayList<String>();
		while (ttype!=EOF){
			if (ttype==macroKey){
				nextToken();
				if (ttype=='{'){
					StringBuffer scr=new StringBuffer();
					nextToken();
					readScript(scr);
					if (ttype=='}') nextToken();
					res.add(macroKey+"{"+scr+"}"); //res.append(obj+"");
				}
				else{
					res.add(macroKey+"");//res.append("$");
				}
			}
			else {
				res.add(str.toString()); //res.append(str);
				nextToken();
			}
		}
		skipSpace=skip;
		return res;
	}

	public Object processVar(String scr){
		if (scr.length()>0){
			char ch=scr.charAt(0);
			if (":$!#@~^%*?".indexOf(ch)>=0){
				if (ThreadHelper.get("javascriptMacroInterpreter")!=null) scr="javascriptMacroInterpreter.jsMacro(\""+scr+"\")";
			}
		}
		return ScriptUtil.runJs("var _="+scr+";if (_==undefined||_==null) _=''; _+='';");
	}
	
	public Object parseVars(char macroKey){ // '$' or '#' '@', '!' '*'
		boolean skip=skipSpace;
		skipSpace=false;
		List<Object> res=new ArrayList<Object>();
		while (ttype!=EOF){
			if (ttype==macroKey){
				nextToken();
				if (ttype=='{'){
					StringBuffer scr=new StringBuffer();
					nextToken();
					readScript(scr);
					if (ttype=='}') nextToken();
					Object obj = processVar(scr.toString());
					if (obj!=null) res.add(obj); //res.append(obj+"");
				}
				else{
					res.add(macroKey);//res.append("$");
				}
			}
			else {
				res.add(str.toString()); //res.append(str);
				nextToken();
			}
		}
		skipSpace=skip;
		if (res.size()==1) return res.get(0);
		StringBuffer ss=new StringBuffer();
		for (Object o:res){
			if (o!=null)
			ss.append(o.toString());
		}
		return ss;
	}

	private void parseJavascriptBody(StringBuffer body) throws Exception{
		nextToken();
		body.append("{");
		boolean skip=skipSpace;
		skipSpace=false;
		while (ttype!='}') {
			if (ttype=='S') body.append(quoteChar+str.toString()+quoteChar); 
			else if (ttype=='{'){
				parseJavascriptBody(body);
			}
			else {
				if (ttype==EOF) throw new Exception("Missing: '}'");
				body.append(str); 
			}
			nextToken();
		}
		body.append("}");
		skipSpace=skip;
		nextToken();
	}

	private void parseFunction(StringBuffer body) throws Exception{
		nextToken();
		body.append("(");
		Stack<Character> stack=new Stack<Character>();
		while (ttype!=')') {
			if (ttype=='S') body.append(quoteChar+str.toString()+quoteChar); 
			else if (ttype=='('){
				parseFunction(body);
				continue;
			}
			else {
				if (ttype==EOF) throw new Exception("Missing: ')'");
				body.append(str); 
				if (str.toString().equals("return")) body.append(" ");
				if (ttype=='{'||ttype=='[') stack.push((char)ttype);
				else
				if (ttype=='}'){
					if (stack.size()==0) throw new Exception("Extra: '}'");
					if (stack.pop()!='{') throw new Exception("Missing: '}'");
				}
				else
				if (ttype==']'){
					if (stack.size()==0) throw new Exception("Extra: ']'");
					if (stack.pop()!='[') throw new Exception("Missing: ']'");
				}
			}
			nextToken();
		}
		if (stack.size()!=0) throw new Exception("Missing: ']' or '}'"+stack);
		body.append(")");
		nextToken();
	}
	
	protected void parseExpression(StringBuffer exp) throws Exception{	//params.abc, Unify.getLang(), function(){}
		while (ttype!=EOF && ttype!=')' && ttype!='}' && ttype!=']' && ttype!=','){
			if (ttype=='S') exp.append(quoteChar+str.toString()+quoteChar); else exp.append(str);
			nextToken();
			if (ttype=='.'){
				exp.append(str);
				nextToken();
				parseExpression(exp);
			}
			else
			if (ttype=='('){ //it's a function call or function header
				parseFunction(exp);
				if (ttype=='{'){
					parseJavascriptBody(exp);
				}
			}
		}
	}
	
	public Object parseJson(boolean allowVariable) throws Exception{
		if (ttype=='S'){
			String res=LightUtil.decodeCString(str.toString());
			if (!allowVariable) throw new Exception("Unexpected: "+res);
			nextToken();
			if (ttype=='+'){
				StringBuffer exp=new StringBuffer(quoteChar+res+quoteChar);
				parseExpression(exp);
				return "@@@"+exp;
			}
			else {
				return res;
			}
		}
		if (ttype=='W'){
			String res=str.toString();
			if (!allowVariable) throw new Exception("Unexpected: "+res);
			if (res.equals("true")||res.equals("false")) {
				nextToken();
				return Boolean.parseBoolean(res);
			}
			StringBuffer exp=new StringBuffer();
			parseExpression(exp);
			if (exp==null) return null;
			return "@@@"+exp;
		}
		if (ttype=='N'){
			String res=str.toString();
			if (!allowVariable) throw new Exception("Unexpected: "+res);
			
			nextToken();
			
			if (res.indexOf(".")>=0) return Double.parseDouble(res);
			return Long.parseLong(res);
		}
		if (ttype=='{'){ //map here.
			Map<String,Object> map=new TreeMap<String,Object>();
			nextToken();
			while (true){
				if ( ttype==EOF) throw new Exception("Invalid Expression");
				if (ttype=='}'){
					nextToken();
					return map;
				}
				if (ttype=='W'||ttype=='S'){
					String key=str.toString();
					nextToken();
					if (ttype==':'){
						nextToken();
						Object val=parseJson(true);
						map.put(key,val);
					}
					if (ttype==','){
						nextToken();
					}
				}
				else throw new Exception("Invalid Expression");
			}
		}
		
		if (ttype=='['){
			List<Object> list=new ArrayList<Object>();
			nextToken();
			while (true){
				if (ttype==EOF) throw new Exception("Invalid Expression");
				if (ttype==']'){
					nextToken();
					return list;
				}
				Object val=parseJson(true);
				list.add(val);
				if (ttype==','){
					nextToken();
				}
			}
		}
		System.out.println("ttype="+str);
		return null;
	}
	
	public List<String> parseCsv(){
		List<String> list=new ArrayList<String>();
		for (;;){
			if (ttype==EOF) break;
			if (ttype=='S'){
				list.add(str.toString());
			}
			else
			if (ttype==','){
				list.add("");
			}
			else{
				skip(",");
				list.add(str.toString());
			}
			nextToken();
			if (ttype==','){
				nextToken();
			}
		}
		return list;
	}
	
	private void addParam(List<Map<String,String>> list,String type,String value){
		Map<String,String> map=new TreeMap<String,String>();
		map.put("type", type);
		map.put("value",value);
		list.add(map);
	}
	
	public List<Map<String,String>> parseParams(){
		List<Map<String,String>> list=new ArrayList<Map<String,String>>();
		for (;;){
			if (ttype=='S'){
				addParam(list,"S",str.toString());
			}
			else
			if (ttype=='N'){
				addParam(list,"N",str.toString());
			}
			nextToken();
			if (ttype==','){
				nextToken();
			}
			else break;
		}
		return list;
	}
	
	public String stripXml(){ //if it's a json structure...
		boolean skip=skipSpace;
		StringBuffer buf=new StringBuffer();
		while (ttype!=EOF){
			skipSpace=false;
			if (ttype=='<'){
				skipSpace=true;
				nextToken();
				if (ttype=='/'){
					nextToken();
					if (ttype=='W'){
						nextToken();
						if (ttype=='>'){
							nextToken();
							continue;
						}
					}
				}
				else
				if (ttype=='W'){
					nextToken();
					while (ttype!='>'){
						if (ttype=='W'){
							nextToken();
							if (ttype=='='){
								nextToken();
								if (ttype=='W' || ttype=='S' || ttype=='N'){
									nextToken();
									continue;
								}
							}
							else continue;
						}
						else {
							//LogUtil.log("Unknown String '"+str+"'");
							break;
						}
					}
					skip("<");
					buf.append(" "+str);
					nextToken();
					continue;
				}
			}
			buf.append(str);
			nextToken();
		}
		skipSpace=skip;
		return buf.toString();
	}
}
