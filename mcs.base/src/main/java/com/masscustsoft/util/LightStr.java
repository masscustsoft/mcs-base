package com.masscustsoft.util;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.masscustsoft.api.IBody;
import com.masscustsoft.api.ICleanup;
import com.masscustsoft.service.TempItem;
import com.masscustsoft.xml.Parser;

public class LightStr {
	public static boolean isIdentity(String vv) {
		for (int i = 0; i < vv.length(); i++) {
			char c = vv.charAt(i);
			if (Character.isLetterOrDigit(c) || c == '_')
				continue;
			return false;
		}
		return true;
	}

	public static boolean isKey(String vv) {
		for (int i = 0; i < vv.length(); i++) {
			char c = vv.charAt(i);
			if (Character.isLetterOrDigit(c) || c == '.' || c == '-' || c == '$' || c == '_' || c == '@' || c == '>'
					|| c == '<')
				continue;
			return false;
		}
		return true;
	}

	public static boolean inSet(String vv, String v) {
		if (("|" + vv + "|").indexOf("|" + v + "|") >= 0)
			return true;
		return false;
	}

	public static boolean isEmpty(String s) {
		if (s == null)
			return true;
		if (s.trim().length() == 0)
			return true;
		return false;
	}

	public static String camel(String s) {
		String fld = s.toLowerCase();
		String _ = "_";
		if (fld.contains("."))
			_ = ".";
		int i = fld.indexOf(_);
		while (i >= 0) {
			fld = fld.substring(0, i) + capitalize(fld.substring(i + 1));
			i = fld.indexOf(_);
		}
		return fld;
	}

	public static String decamel(String s) {
		StringBuffer fld = new StringBuffer();
		for (int i = 0; i < s.length(); i++) {
			char ch = s.charAt(i);
			if (i > 0 && Character.isUpperCase(ch)) {
				fld.append("_");
			}
			fld.append(Character.toUpperCase(ch));
		}
		return fld.toString();
	}

	public static String capitalize(String s) {
		if (s.length() == 0) {
			return s;
		}
		String ss = s.substring(0, 1).toUpperCase() + s.substring(1);
		return ss;
	}

	public static String decapitalize(String s) {
		if (s.length() == 0) {
			return s;
		}
		String ss = s.substring(0, 1).toLowerCase() + s.substring(1);
		return ss;
	}

	public static long parseTimestamp(String ss) {
		if (isEmpty(ss))
			return 0;
		char last = ss.charAt(0);
		StringBuffer buf = new StringBuffer();
		buf.append(last);
		for (int i = 1; i < ss.length(); i++) {
			int c = ss.charAt(i) - last;
			if (c < 0)
				c += 10;
			char ch = (char) (c + '0');
			buf.insert(0, ch);
		}
		return Long.parseLong(buf.toString());
	}

	public static String[] parseSalted(String ss) {
		StringBuffer ses = new StringBuffer();
		StringBuffer dd = new StringBuffer();
		int ofs = 0;
		for (int i = 0; i < ss.length(); i++) {
			char ch = ss.charAt(i);
			if (ch >= '0' && ch <= '9') {
				dd.append(ch);
				ofs = ch - '0';
			} else {
				ch -= ofs;
				if (ch < 'a')
					ch += 26;
				ses.append(ch);
			}
		}
		return new String[] { ses.toString(), dd.toString() };
	}

	public static boolean isTrue(String s) {
		return "yes".equals(s) || "true".equals(s);
	}

	public static String parseLetters(String ss) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < ss.length() / 2; i++) {
			char c1 = ss.charAt(i + i);
			char c2 = ss.charAt(i + i + 1);
			int b1 = (c1 - 'a') & 0xf, b2 = (c2 - 'a' - 10) & 0xf;
			buf.append((char) ((b1 << 4) + b2));
		}
		return buf.toString();
	}

	public static String decodeUrl(String s) throws UnsupportedEncodingException {
		String ret=URLDecoder.decode(s, LightUtil.UTF8);
		return ret;
	}

	public static String encodeUrl(String url) throws UnsupportedEncodingException {
		String msg = URLEncoder.encode(url, LightUtil.UTF8);
		return msg.replaceAll("\\+", "%20");
	}

	private static String[] HTMLENTITIES = { "&", "&amp;", ">", "&gt;", "<", "&lt;", "\"", "&quot;", "'", "&#039;",
			"\\", "&#092;", "\u00a9", "&copy;", "\u00ae", "&reg;" };

	public static String decodeHtml(String s) {
		if (s == null)
			return s;
		for (int i = HTMLENTITIES.length - 2; i >= 0; i -= 2) {
			String key = HTMLENTITIES[i];
			String val = HTMLENTITIES[i + 1];
			s = s.replace(val, key);
		}
		return s;
	}

	public static String encodeHtml(String s) {
		for (int i = 0; i < HTMLENTITIES.length; i += 2) {
			String key = HTMLENTITIES[i];
			String val = HTMLENTITIES[i + 1];
			if (key.equals("'"))
				continue;
			if (s.contains(key)){
				s = s.replace(key, val);
			}
		}
		return s;
	}

	public static Object validateJson(String ss) throws Exception {
		// {action:'abcd'}
		if (isEmpty(ss))
			return null;
		try {
			Parser p = new Parser(ss);
			Object ret = p.parseJson(false);
			if (p.ttype != p.EOF){
				//LogUtil.info("Invalid Expression: " + ss);
				throw new Exception("Invalid Expression");
			}
			return ret;
		} catch (Exception e) {
			System.out.println("JSON="+ss);
			e.printStackTrace();
			throw new Exception("Invalid Expression, Check Server Log for detail");
		}
	}

	public static String decode(String s) throws IOException{
		return Base64.decode(s);
	}
	
	public final static String encode(String s) throws IOException{
		return Base64.encode(s);
	}

	public static String encodeJsonString(String string) {
		if (string == null || string.length() == 0) {
			return "\"\"";
		}

		char b;
		char c = 0;
		int i;
		int len = string.length();
		StringBuffer sb = new StringBuffer(len + 4);
		String t;

		sb.append('"');
		for (i = 0; i < len; i += 1) {
			b = c;
			c = string.charAt(i);
			switch (c) {
			case '\\':
			case '"':
				sb.append('\\');
				sb.append(c);
				break;
			// case '<':
			// case '>':
			// sb.append( '\\' );
			// sb.append( c );
			// break;
			case '\b':
				sb.append("\\b");
				break;
			case '\t':
				sb.append("\\t");
				break;
			case '\n':
				sb.append("\\n");
				break;
			case '\f':
				sb.append("\\f");
				break;
			case '\r':
				sb.append("\\r");
				break;
			default:
				if (c < ' ') {
					t = "000" + Integer.toHexString(c);
					sb.append("\\u").append(t.substring(t.length() - 4));
				} else {
					sb.append(c);
				}
			}
		}
		sb.append('"');
		return sb.toString();
	}

	public static String getSizeStr(long size) {
		if (size<1000) return size+" B";
		if (size<1000000) return Math.round(size/100d)/10d+" KB";
		if (size<1000000000) return Math.round(size/100000d)/10d+" Mb";
		return Math.round(size/100000000d)/10d+" GB";
	}
	
	public static byte[] getHexContent(String text) {
		StringBuffer buf = new StringBuffer(text);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		for (int i = 0; i < buf.length(); i += 2) {
			char a = buf.charAt(i);
			char b = buf.charAt(i + 1);
			if (a >= '0' && a <= '9')
				a -= '0';
			else {
				a -= 'A';
				a += 10;
			}
			if (b >= '0' && b <= '9')
				b -= '0';
			else {
				b -= 'A';
				b += 10;
			}
			byte c = (byte) ((a << 4) + b);
			os.write(c);
		}
		return os.toByteArray();
	}

	public static void setHexContent(byte[] buf, StringBuffer o) {
		for (byte b : buf) {
			byte h = (byte) ((b >> 4) & 0xf);
			int a = (h < 10) ? '0' + h : 'A' + h - 10;
			o.append((char) a);
			byte l = (byte) (b & 0xf);
			a = (l < 10) ? '0' + l : 'A' + l - 10;
			o.append((char) a);
		}
	}

	public static void setHexContent(InputStream is, StringBuffer o)
			throws IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		StreamUtil.copyStream(is, os, 0);
		os.close();
		setHexContent(os.toByteArray(), o);
	}
	
	public static void strokeBody(IBody b, boolean toBody, boolean toHtml)
			throws Exception {
		if (toBody) {
			// convert html into plain
			String htm = b.getHtmlBody();
			if (htm != null) {
				if (htm.startsWith("{")) {
					Map<String, String> map = (Map) LightUtil.parseJson(htm);
					htm = "";
					for (String html : map.values()) {
						if (html != null)
							htm += html.replaceAll("\\<.*?>", "") + " ";
					}
					b.setBody(htm);
				} else
					b.setBody(htm.replaceAll("\\<.*?>", ""));
			}
		}
		if (toHtml) {
			// convert plain into html
			String txt = b.getBody();
			if (txt != null) {
				if (txt.startsWith("{")) {
					Map<String, String> map = (Map) LightUtil.parseJson(txt);
					txt = "";
					for (String text : map.values()) {
						if (text != null)
							txt += text.replace("\n", "<br>")
									.replace(" ", "&nbsp;")
									.replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;");
					}
					b.setHtmlBody(txt);
				} else
					b.setHtmlBody(txt.replace("\n", "<br>")
							.replace(" ", "&nbsp;")
							.replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;"));
			}
		}
	}

	public static String incVersion(String v) {
		if (v == null)
			v = "";
		int idx = 0;
		for (int i = v.length() - 1; i >= 0; i--) {
			char c = v.charAt(i);
			if (!Character.isDigit(c)) {
				idx = i;
				break;
			}
		}
		String prefix = v.substring(0, idx + 1);
		int version = LightUtil.decodeInt(v.substring(idx + 1));
		return prefix + (version + 1);
	}
	
	public static String convert(String s){
		return convert(s,null);
	}
	
	public static String convert(String s, Map m){
		if (s==null) return "";
		if (m==null) m=new HashMap();
		ThreadHelper.set("values",m);
		StringBuffer ret=new StringBuffer();
		int ofs=0;
		while(true){
			int i=s.indexOf("{",ofs);
			if (i<0){
				ret.append(s.substring(ofs));
				return ret.toString();
			}
			ret.append(s.substring(ofs,i));
			int j=s.indexOf("}",i);
			String fld=s.substring(i+1,j);
			Object val="";
			if (fld.indexOf("[")==0){
				int k=fld.indexOf("]");
				String exp=fld.substring(1,k).replaceAll("this\\.", "Unify.");
				System.out.println("exp="+exp);
				val=LightUtil.macro("${"+exp+"}",'$',m);	
			}
			else{
				int k=fld.indexOf(":");
				String fmt="";
				if (k>0){
					fmt=fld.substring(k+1);
					fld=fld.substring(0, k);
				}
				val=LightUtil.macro("${"+fld+"}",'$',m);
				ThreadHelper.set("_value_", val);
				if (!LightStr.isEmpty(fmt)){
					k=fmt.indexOf("(");
					if (k>=0){
						val=LightUtil.macro("${Unify."+fmt.substring(0, k)+"(_value_,"+fmt.substring(k+1),'$',m);	
					}
					else{
						val=LightUtil.macro("${Unify."+fmt+"(_value_)}",'$',m);
					}
				}	
			}
			ret.append(val.toString());
			ofs=j+1;
		}
	}
	
	public static String replace(String text, String searchString,
			String replacement, int max) {
		if (isEmpty(text) || isEmpty(searchString) || replacement == null
				|| max == 0) {
			return text;
		}
		int start = 0;
		int end = text.indexOf(searchString, start);
		if (end == -1) {
			return text;
		}
		int replLength = searchString.length();
		int increase = replacement.length() - replLength;
		increase = (increase < 0 ? 0 : increase);
		increase *= (max < 0 ? 16 : (max > 64 ? 64 : max));
		StringBuffer buf = new StringBuffer(text.length() + increase);
		while (end != -1) {
			buf.append(text.substring(start, end)).append(replacement);
			start = end + replLength;
			if (--max == 0) {
				break;
			}
			end = text.indexOf(searchString, start);
		}
		buf.append(text.substring(start));
		return buf.toString();
	}
	
	public static String replace(String text, String searchString,
			String replacement) {
		return replace(text, searchString, replacement, -1);
	}
	
	public static String join(List<String> list, String joinStr){
		StringBuffer buf=new StringBuffer();
		for (String s:list){
			if (buf.length()>0) buf.append(joinStr);
			buf.append(s);
		}
		return buf.toString();
	}
	
	public static String find(StringBuffer buf, String keyPhase, String endPhase){
		return find(buf,keyPhase,endPhase,1);
	}
	
	public static String find(StringBuffer buf, String keyPhase, String endPhase, int nd){
		int from=0,i=0;
		while(nd>0){
			nd--;
			i=buf.indexOf(keyPhase,from);
			if (i<0) return "";
			from=i+keyPhase.length();
		}
		i+=keyPhase.length();
		if (!isEmpty(endPhase)){
			int j=buf.indexOf(endPhase,i);
			return buf.substring(i,j);
		}
		return buf.substring(i);
	}
	
	public static String find(String buf, String keyPhase, String endPhase){
		return find(buf,keyPhase,endPhase,1);
	}
	
	public static String find(String buf, String keyPhase, String endPhase, int nd){
		int from=0,i=0;
		while(nd>0){
			nd--;
			i=buf.indexOf(keyPhase,from);
			if (i<0) return "";
			from=i+keyPhase.length();
		}
		i+=keyPhase.length();
		if (!isEmpty(endPhase)){
			int j=buf.indexOf(endPhase,i);
			return buf.substring(i,j);
		}
		return buf.substring(i);
	}
	
	public static Color decodeHtmlColor(String colorString) {
		Color color;

		if (colorString.startsWith("#")) {
			colorString = colorString.substring(1);
		}
		if (colorString.endsWith(";")) {
			colorString = colorString.substring(0, colorString.length() - 1);
		}

		int red, green, blue;
		switch (colorString.length()) {
		case 6:
			red = Integer.parseInt(colorString.substring(0, 2), 16);
			green = Integer.parseInt(colorString.substring(2, 4), 16);
			blue = Integer.parseInt(colorString.substring(4, 6), 16);
			color = new Color(red, green, blue);
			break;
		case 3:
			red = Integer.parseInt(colorString.substring(0, 1), 16);
			green = Integer.parseInt(colorString.substring(1, 2), 16);
			blue = Integer.parseInt(colorString.substring(2, 3), 16);
			color = new Color(red, green, blue);
			break;
		case 1:
			red = green = blue = Integer.parseInt(colorString.substring(0, 1), 16);
			color = new Color(red, green, blue);
			break;
		default:
			throw new IllegalArgumentException("Invalid color: " + colorString);
		}
		return color;
	}
	
	public static String formatNumber(String fmt, double num){
		DecimalFormat df = new DecimalFormat(fmt);
		String vv=df.format(num);
		return vv;
	}
	
	public static boolean isNumber(String str) {
		if (isEmpty(str)) return false;
		try{  
			double d = Double.parseDouble(str); 
			return true;
		}  
		catch(NumberFormatException nfe){  
		    return false;  
		}  
	}	
}
