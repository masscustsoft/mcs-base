package com.masscustsoft.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

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
}
