package com.masscustsoft.util;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.UUID;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.masscustsoft.Lang.CLASS;
import com.masscustsoft.api.IBeanFactory;
import com.masscustsoft.api.ICleanup;
import com.masscustsoft.api.IDataService;
import com.masscustsoft.api.IRepository;
import com.masscustsoft.api.JsonField;
import com.masscustsoft.helper.HttpClient;
import com.masscustsoft.service.AbstractConfig;
import com.masscustsoft.service.TempItem;
import com.masscustsoft.xml.Parser;
import com.masscustsoft.xml.XmlNode;

public class LightUtil {
	public final static String UTF8 = "utf-8";
	public static boolean supportTimezone = true;

	public static transient long bootupTime = LightUtil.getCalendar().getTimeInMillis();

	/**
	 * --en Get a UUID which encoded by Base64 --zh_cn
	 * 获得一个全局唯一编码(UUID)，采用Base64编码
	 */
	public static String getUuid() {
		UUID u = UUID.randomUUID();
		long lo = u.getLeastSignificantBits();
		long hi = u.getMostSignificantBits();

		ByteBuffer bb = ByteBuffer.allocate(16);
		bb.putLong(0, lo);
		bb.putLong(8, hi);
		return Base64.encodeToString(bb.array(), -1, false);
	}

	/**
	 * --en Get a UUID which encoded by Hex Code to make it case insensitive
	 * --zh_cn 获得一个全局唯一编码(UUID)，用16进制编码，可用作大小写无关全局编码
	 */
	public static String getHashCode() {
		UUID u = UUID.randomUUID();
		long lo = u.getLeastSignificantBits();
		long hi = u.getMostSignificantBits();

		ByteBuffer bb = ByteBuffer.allocate(16);
		bb.putLong(0, lo);
		bb.putLong(8, hi);
		byte[] b = bb.array();
		return encodeHashCode(b);
	}

	private static String encodeHashCode(byte[] b) {
		StringBuffer buf = new StringBuffer();
		for (byte c : b) {
			int c1 = (((c & 0xf0) >> 4) & 0xf) + 'a';
			int c2 = (c & 0xf) + 'a' + 10;
			// System.out.println("c="+c+",c1="+c1+",c2="+c2);
			buf.append((char) c1);
			buf.append((char) c2);
		}
		return buf.toString();
	}

	public static String encodeHashCode(String ss) {
		try {
			ss = encodeHashCode(ss.getBytes(LightUtil.UTF8));
		} catch (UnsupportedEncodingException e) {
		}
		return ss;
	}

	public static String decodeHashCode(String ss) {
		byte[] buf = new byte[ss.length() / 2];
		for (int i = 0; i < ss.length() / 2; i++) {
			char c1 = ss.charAt(i + i);
			char c2 = ss.charAt(i + i + 1);
			int b1 = (c1 - 'a') & 0xf, b2 = (c2 - 'a' - 10) & 0xf;
			buf[i] = (byte) ((b1 << 4) + b2);
		}
		String ret;
		try {
			ret = new String(buf, LightUtil.UTF8);
		} catch (Exception e) {
			ret = new String(buf);
		}
		return ret;
	}

	@SuppressWarnings("unchecked")
	public static boolean isPrimitive(Class c) {
		String cls = c.getName();
		// if (c.isEnum()) return true;
		if (cls.equals("char") || cls.equalsIgnoreCase("java.lang.Character")) {
			return true;
		}
		if (cls.equalsIgnoreCase("java.lang.String")) {
			return true;
		}
		if (cls.equals("int") || cls.equals("java.lang.Integer")) {
			return true;
		}
		if (cls.equals("long") || cls.equals("java.lang.Long")) {
			return true;
		}
		if (cls.equals("float") || cls.equals("java.lang.Float")) {
			return true;
		}
		if (cls.equals("double") || cls.equals("java.lang.Double")) {
			return true;
		}
		if (cls.equals("boolean") || cls.equals("java.lang.Boolean")) {
			return true;
		}
		if (cls.equalsIgnoreCase("java.sql.Date") || cls.equalsIgnoreCase("java.sql.Timestamp")) {
			return true;
		}
		return false;
	}

	public static int decodeInt(String s) {
		if (s == null)
			return 0;
		try {
			if (s.indexOf('.') > 0)
				s = s.substring(0, s.indexOf('.'));
			return Integer.parseInt(s);
		} catch (Exception e) {
			return 0;
		}
	}

	public static long decodeLong(String s) {
		if (s == null)
			return 0;
		try {
			if (s.indexOf('.') > 0)
				s = s.substring(0, s.indexOf('.'));
			return Long.parseLong(s);
		} catch (Exception e) {
			return 0;
		}
	}

	public static boolean isNumber(String s, boolean ifNull) {
		if (s == null || s.length() == 0)
			return ifNull;
		return decodeDouble(s) != null;
	}

	public static Double decodeDouble(String s) {
		if (s == null || s.length() == 0)
			return null;
		try {
			s = s.replace(",", "");
			return Double.parseDouble(s);
		} catch (Exception e) {
			return null;
		}
	}

	public static float decodeFloat(String s) {
		if (s == null || s.length() == 0)
			return 0;
		try {
			return Float.parseFloat(s);
		} catch (Exception e) {
			return 0;
		}
	}

	public static boolean decodeBoolean(String s) {
		if (s == null)
			return false;
		try {
			if ("yes".equalsIgnoreCase(s))
				return true;
			if ("no".equalsIgnoreCase(s))
				return false;
			return Boolean.parseBoolean(s);
		} catch (Exception e) {
			return false;
		}
	}

	private static Calendar newCalendar() {
		Calendar c = Calendar.getInstance();
		Integer days = (Integer) ThreadHelper.get("$MockBackDays$");
		if (days != null) {
			c.add(Calendar.DATE, days);
		}
		return c;
	}

	private static Calendar newCalendar(TimeZone tz) {
		Calendar c = Calendar.getInstance(tz);
		Integer days = (Integer) ThreadHelper.get("$MockBackDays$");
		if (days != null) {
			c.add(Calendar.DATE, days);
		}
		return c;

	}

	public static Calendar getCalendar() {
		Calendar c = newCalendar();
		if (!supportTimezone) {
			c.setTimeZone(TimeZone.getTimeZone("GMT"));
			c.setTime(longDate(c));
		}
		return c;
	}

	public static Calendar getCalendar(String tz) {
		Calendar c;
		if (supportTimezone) {
			if (tz == null || tz.length() == 0) {
				tz = "America/New_York";
			}
			c = newCalendar(TimeZone.getTimeZone(tz));
		} else
			c = getCalendar();
		return c;
	}

	public static Calendar getShortCalendar() {
		Calendar c = newCalendar();
		c.setTimeZone(TimeZone.getTimeZone("GMT"));
		return c;
	}

	public static TimeZone getTimezone() {
		if (supportTimezone) {
			String tz = (String) ThreadHelper.get("timezone");
			if (tz == null)
				return TimeZone.getDefault();
			return TimeZone.getTimeZone(tz);
		}
		return TimeZone.getTimeZone("GMT");
	}

	public static String encodeLongDate(java.sql.Timestamp d) {
		return encodeLongDate(d, getTimezone());
	}

	public static String encodeShortDate() {
		return encodeShortDate(shortDate());
	}

	public static String encodeShortDate(java.sql.Date d) {
		return encodeShortDate(d, "yyyy-MM-dd");
	}

	public static String encodeShortDate(java.sql.Date d, String format) {
		return encodeDate((java.util.Date) d, format);
	}

	public static String encodeLongDate(java.sql.Timestamp d, TimeZone tz) {
		if (supportTimezone)
			return encodeLongDate(d, "yyyy-MM-dd'T'HH:mm:ss.SSSZ", tz);
		else
			return encodeLongDate(d, "yyyy-MM-dd'T'HH:mm:ss.SSS", getTimezone());
	}

	public static String encodeLongDate(java.sql.Timestamp d, String format) {
		return encodeLongDate(d, format, getTimezone());
	}

	// js version
	public static String _shortDateFormat() {
		String shortfmt = (String) ThreadHelper.get("shortdtfmt");
		if (shortfmt == null)
			shortfmt = "Y-m-d";
		return shortfmt;
	}

	public static String shortDateFormat() {
		return jsDateFormat(_shortDateFormat());
	}

	public static String _longDateFormat() {
		String longfmt = (String) ThreadHelper.get("longdtfmt");
		if (longfmt == null)
			longfmt = "Y-m-d H:i:s";
		return longfmt;
	}

	public static String longDateFormat() {
		return jsDateFormat(_longDateFormat());
	}

	// d is GMT
	public static String toNativeShortDate(java.sql.Date d) {
		// System.out.println("toNativeShortDate d="+d.toGMTString());
		if (d == null)
			return null;
		SimpleDateFormat fmt = new SimpleDateFormat(shortDateFormat());
		fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
		String s = fmt.format(d);
		return s;
	}

	public static String toNativeLongDate(java.sql.Timestamp d) {
		String s = encodeLongDate(d, longDateFormat(), getTimezone());
		int i = s.indexOf(' ');
		if (i > 0) {
			if (s.substring(i + 1).equals("00:00") || s.substring(i + 1).equals("00:00:00"))
				s = s.substring(0, i);
		}
		return s;
	}

	public static java.sql.Date fromNativeShortDate(String ss) throws Exception {
		if (ss == null || ss.trim().equals("") || ss.equalsIgnoreCase("null"))
			return null;
		String format = shortDateFormat();
		return decodeShortDate(ss, format);
	}

	public static java.sql.Timestamp fromNativeLongDate(String ss) throws Exception {
		if (ss == null || ss.trim().equals("") || ss.equalsIgnoreCase("null"))
			return null;
		java.sql.Timestamp d;
		if (ss.contains("T")) {
			d = decodeLongDate(ss, "yyyy-MM-dd'T'HH:mm:ss", getTimezone());
		} else {
			String fmt = longDateFormat();
			TimeZone tz = getTimezone();
			if (ss.indexOf(':') == -1) {// if no time set
				if (fmt.lastIndexOf(' ') > 0) {
					fmt = fmt.substring(0, fmt.lastIndexOf(' '));
				}
			}
			d = decodeLongDate(ss, fmt, tz);
		}
		return d;
	}

	public static String toNativeNumber(Object num, Integer decimalPrecision) {
		if (num == null)
			return "";
		if (!(num instanceof Double)){
			num = decodeDouble(num.toString());
			if (num == null)
				return "";
		}
		String fmt = "#,##0";
		if (decimalPrecision != null) {
			fmt += ".";
			for (int i = 0; i < decimalPrecision; i++) {
				fmt += "0";
			}
		} else
			fmt += ".############";
		DecimalFormat df = new DecimalFormat(fmt);
		
		String vv = df.format(num);
		String numfmt = numberFormat();
		if (numfmt.equals("#.##0,00")) {
			vv = vv.replace(".", " ").replace(",", ".").replace(" ", ",");
		} else if (numfmt.equals("# ##0,00")) {
			vv = vv.replace(",", " ").replace(".", ",");
		} else if (numfmt.equals("# ##0.00")) {
			vv = vv.replace(",", " ");
		} else if (numfmt.equals("####0.00")) {
			vv = vv.replace(",", "");
			if (vv.endsWith(".0"))
				vv += "0";
		}
		if (vv.endsWith("."))
			vv = vv.substring(0, vv.length() - 1);
		return vv;
	}

	public static Double fromNativeNumber(String vv, Double def) {
		if (vv == null)
			vv = "";
		String numfmt = numberFormat();
		if (numfmt.equals("#,##0.00")) {
			vv = vv.replace(",", "");
		} else if (numfmt.equals("#.##0,00")) {
			vv = vv.replace(".", " ").replace(",", ".").replace(" ", "");
		} else if (numfmt.equals("# ##0,00")) {
			vv = vv.replace(",", ".").replace(" ", "");
		} else if (numfmt.equals("# ##0.00")) {
			vv = vv.replace(" ", "");
		} else if (numfmt.equals("# ##0,00")) {
			vv = vv.replace(" ", "").replace(",", ".");
		}
		Double d = decodeDouble(vv);
		if (d == null || d.isNaN() || d.isInfinite())
			d = def;
		return d;
	}

	public static String encodeLongDate(java.sql.Timestamp d, String format, TimeZone loc) {
		return encodeDate((java.util.Date) d, format, loc);
	}

	public static String encodeDate(java.util.Date d, String format) {
		return encodeDate(d, format, TimeZone.getTimeZone("GMT"));
	}

	public static String encodeDate(java.util.Date d, String format, TimeZone loc) {
		if (d == null) {
			return "";
		}
		SimpleDateFormat fmt = new SimpleDateFormat(format, Locale.US);
		fmt.setTimeZone(loc);
		String s = fmt.format(d);
		return s;
	}

	public static java.sql.Timestamp decodeLongDate(String s) throws Exception {
		return decodeLongDate(s, getTimezone());
	}

	public static String jsDateFormat(String fmt) {
		String fmt2 = fmt.replace("Y", "yyyy").replace("M", "MMM").replace("m", "MM").replace("d", "dd")
				.replace("H", "HH").replace("i", "mm").replace("s", "ss").replace("+", " ");
		// System.out.println("fmt="+fmt+",fmt2="+fmt2);
		return fmt2;
	}

	public static String numberFormat() {
		String numfmt = (String) ThreadHelper.get("numfmt");
		if (numfmt == null)
			numfmt = "#,##0.00";
		return numfmt;
	}

	public static java.sql.Date shortDate(java.util.Date d) {
		Calendar c = getShortCalendar();
		c.setTime(d);
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		java.sql.Date qd = new java.sql.Date(c.getTime().getTime());
		return qd;
	}

	public static java.util.Date localDate(java.sql.Date d) {
		if (d == null)
			return null;
		Calendar c = Calendar.getInstance();
		int ofs = c.getTimeZone().getOffset(d.getTime());
		c.setTime(d);
		c.add(Calendar.MILLISECOND, -ofs);
		return c.getTime();
	}

	public static java.util.Date localDate(java.sql.Timestamp d) {
		if (d == null)
			return null;
		Calendar c = newCalendar();
		int ofs = c.getTimeZone().getOffset(d.getTime());
		c.setTime(d);
		c.add(Calendar.MILLISECOND, -ofs);
		return c.getTime();
	}

	public static java.sql.Date shortDate(Calendar c) throws Exception {
		return shortDate(c.getTime());
	}

	public static java.sql.Date shortDate() {
		return shortDate(getShortCalendar().getTime());
	}

	public static Timestamp longDate(java.util.Date dt) {
		if (!supportTimezone) {
			long t = dt.getTime();
			return new Timestamp(t + dt.getTimezoneOffset());
		}
		return new Timestamp(dt.getTime());
	}

	public static Timestamp longDate(Calendar c) {
		if (c == null)
			return null;
		if (!supportTimezone) {
			long t = c.getTime().getTime();
			return new Timestamp(t + c.getTimeZone().getOffset(t));
		}
		return new Timestamp(c.getTime().getTime());
	}

	public static Timestamp longDate() {
		return longDate(getCalendar());
	}

	public static java.sql.Timestamp decodeLongDate(String s, TimeZone tz) throws Exception {
		if (s == null || s.equalsIgnoreCase("null"))
			return null;
		if (s.startsWith("\"") || s.startsWith("\'"))
			s = s.substring(1, s.length() - 1);
		int len = s.length();
		if (len == 8)
			return decodeLongDate(s, "yyyyMMdd", tz);

		if (len == 10 && s.indexOf("-") > 0) {
			return decodeLongDate(s, "yyyy-MM-dd", TimeZone.getTimeZone("GMT"));
		}
		if (len <= 12) {
			// use short date
			return decodeLongDate(s, shortDateFormat(), tz);
		}
		if (len == 19) {
			// use short date
			if (s.indexOf('T') == 10)
				return decodeLongDate(s, "yyyy-MM-dd'T'HH:mm:ss", TimeZone.getTimeZone("GMT"));
			return decodeLongDate(s, longDateFormat(), tz);
		}
		if (len == 20 && (s.endsWith("Z") || s.endsWith("z"))) {
			// use short date
			return decodeLongDate(s.substring(0, 19), "yyyy-MM-dd'T'HH:mm:ss", TimeZone.getTimeZone("GMT"));
		}
		// 2010-03-07 13:55:04.760000Z
		if (len == 27) {
			return decodeLongDate(s.substring(0, len - 4), "yyyy-MM-dd'T'HH:mm:ss.SSS",
					TimeZone.getTimeZone("Etc/UTC"));
		}

		if (len == 26)
			return decodeLongDate(s, "yyyy-MM-dd'T'HH:mm:ss.SSSzzz", tz);

		if (len == 28)
			return decodeLongDate(s, "yyyy-MM-dd'T'HH:mm:ss.SSSZZZZ", tz);

		if (len == 24) {
			if (s.endsWith("Z") || s.endsWith("z"))
				return decodeLongDate(s.substring(0, len - 1), "yyyy-MM-dd'T'HH:mm:ss.SSS", tz);
			else
				return decodeLongDate(s, "yyyy-MM-dd'T'HH:mm:ssZ", tz);
		}

		if (len == 23) {
			if (s.indexOf(".") > 0)
				return decodeLongDate(s, "yyyy-MM-dd'T'HH:mm:ss.SSS", tz);
			else
				return decodeLongDate(s, "yyyy-MM-dd'T'HH:mm:ssZ", tz);
		}
		if (len == 29) {
			if (s.indexOf("-") == 7)
				return longDate(decodeLongDate(s, "EEE, dd-MMM-yyyy HH:mm:ss zzz", tz));
			else
				return longDate(decodeLongDate(s, "EEE, dd MMM yyyy HH:mm:ss zzz", tz));
		}

		// else use long local
		return decodeLongDate(s, longDateFormat(), tz);
	}

	public static java.sql.Timestamp decodeLongDate(String s, String format, TimeZone tz) throws Exception {
		if (s == null || s.trim().equals("") || s.equalsIgnoreCase("null"))
			return null;
		if (s.startsWith("\"") || s.startsWith("\'"))
			s = s.substring(1, s.length() - 1);
		int i = s.indexOf(" (");
		if (i>0) s=s.substring(0,i);
		
		SimpleDateFormat fmt = new SimpleDateFormat(format, Locale.US);
		if (tz != null)
			fmt.setTimeZone(tz);
		try {
			java.util.Date dt = fmt.parse(s);
			return new java.sql.Timestamp(dt.getTime());
		} catch (Exception e) {
			throw new Exception("InvalidDate");
		}
	}

	// GMT
	public static java.sql.Date decodeShortDate(String s) throws Exception {
		return decodeShortDate(s, "yyyy-MM-dd");
	}

	// GMT
	public static java.sql.Date decodeShortDate(String s, String format) throws Exception {
		// for Sencha Touch, it's not native.
		if (s == null || s.length() == 0)
			return null;
		if (s.startsWith("\""))
			s = s.substring(1, s.length() - 2);
		int i = s.indexOf(" (");
		if (i>0){
			s=s.substring(0,i);
			i=s.lastIndexOf(" ");
			s=s.substring(0,i);
			format="EEE MMM dd yyyy HH:mm:ss";
		}
		
		i = s.indexOf("T");
		if (i > 0) {
			s = s.substring(0, i);
		}
		if (s.indexOf("-") == 4 && s.length() == 10) {
			format = "yyyy-MM-dd";
		}
		if (s == null || s.length() == 0 || "NULL".equalsIgnoreCase(s))
			return null;
		SimpleDateFormat fmt = new SimpleDateFormat(format);
		fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
		try {
			java.util.Date dt = fmt.parse(s);
			java.sql.Date d = shortDate(dt);
			return d;
		} catch (Exception e) {
			throw new Exception("InvalidDate:" + s);
		}
	}

	public static AbstractConfig getCfg() {
		return (AbstractConfig) ThreadHelper.get("$Cfg$");
	}

	// real context path+virtual path
	public static String getContextPath() {
		return (String) ThreadHelper.get("contextPath");
	}

	// only virtual path
	public static String getContextRelativePath() {
		return (String) ThreadHelper.get("contextRelativePath");
	}

	public static String getRelativePath() {
		return (String) ThreadHelper.get("relativePath");
	}

	public static boolean isSQLException(Throwable e){
		System.out.println("isSQLException "+e.getClass().getName());
		if (e instanceof SQLException || e.getClass().getName().startsWith("org.springframework.dao.")) return true;
		if (e.getCause()!=null) return isSQLException(e.getCause());
		return false;
	}
	
	public static List<String> getIpAddresses() throws SocketException{
		List<String> ips=new ArrayList();
		Enumeration e = NetworkInterface.getNetworkInterfaces();
		while(e.hasMoreElements())
		{
		    NetworkInterface n = (NetworkInterface) e.nextElement();
		    Enumeration ee = n.getInetAddresses();
		    while (ee.hasMoreElements())
		    {
		        InetAddress i = (InetAddress) ee.nextElement();
		        String ip=i.getHostAddress();
		        if (ip.indexOf(":")>=0 || ip.equals("127.0.0.1")) continue;
		        ips.add(ip);
		    }
		}
		return ips;
	}

	public static String encodeCString(String s){
		if (s==null) s="";
		return s.replace("\\","\\\\").replace("\n", "\\n").replace("\"", "\\\"").replace("\''", "\\\'").replace("\r", "");
	}
	
	public static String decodeCString(String s){
		return s.replace("\\\'", "\'").replace("\\\"", "\"").replace("\\n", "\n").replace("\\\\","\\");
	}
	
	@SuppressWarnings("unchecked")
	public static String encodeObject(Object v){
		if (v==null) return null;
		Class c=v.getClass();
		//if (c.isEnum()) return c.toString();
		String cls=c.getName();
		if (cls.equals("char") || cls.equals("java.lang.Character")){
			return (java.lang.Character)v+"";
		}
		if (cls.equalsIgnoreCase("java.lang.String")){
			return (String)v;
		}
		if (cls.equalsIgnoreCase("java.util.Date")){
			throw new RuntimeException("java.util.date not supported, please use java.sql.Date or java.sql.Timestamp instead");
		}
		if (cls.equalsIgnoreCase("java.sql.Date")){
			java.sql.Date qd=(java.sql.Date)v;
			return encodeShortDate(qd);
		}
		if (cls.equalsIgnoreCase("java.sql.Timestamp")){
			return encodeLongDate((Timestamp)v,"yyyy-MM-dd'T'HH:mm:ss.SSSZ",TimeZone.getTimeZone("GMT"));	
		}
		if (cls.equals("int") || cls.equals("java.lang.Integer")){
			return v+"";
		}
		if (cls.equals("long") || cls.equals("java.lang.Long")){
			return v+"";
		}
		if (cls.equals("float") || cls.equals("java.lang.Float")){
			return v+"";
		}
		if (cls.equals("double") || cls.equals("java.lang.Double")){
			return v+"";
		}
		if (cls.equals("boolean") || cls.equals("java.lang.Boolean")){
			return v+"";
		}
		return null;
	}
	
	public static String getCascadeName(Class<?> c){
		StringBuffer buf=new StringBuffer();
		while (c!=null) {
			if (c.getSimpleName().equals("Object")) break;
			if (buf.length()>0) buf.insert(0," ");
			buf.insert(0,c.getSimpleName());
			c=c.getSuperclass();
		}
		return buf.toString();
	}
	
	/**
	 * Convert a String expression by expanding any embedded ${} macro, return the original type of any object.   
	 */
	public static Object macro(String buf){
		return macro(buf,'$');
	}
	
	/**
	 * Convert a String expression by expanding any embedded ${} macro, return value will be converted to String.  
	 */
	public static String macroStr(String buf){
		return macro(buf,'$')+"";
	}
	
	/**
	 * Convert a String expression by expanding any give embedded macro. The macroKey is the leading character to the macro. 
	 * 
	 * @param macroKey Valid value can be '$', '%', '#', '@', '!'.
	 */
	public static Object macro(String buf,char macroKey){
		return macro(buf,macroKey,(Map)null);
	}
	
	/**
	 * Convert a String expression by expanding any give embedded macro. The macroKey is the leading character to the macro. An env map that can be used for variable preset. 
	 * 
	 * @param macroKey Valid value can be '$', '%', '#', '@', '!'.
	 * @param env A Map to store candidate variable and value pairs.
	 */
	public static Object macro(String buf,char macroKey,Map<String,Object> env){
		return _macro(buf,macroKey,env);
	}
	
	protected static Object _macro(String buf,char macroKey,Object env){
		if (buf==null||buf.length()==0) return "";
		Object old=ThreadHelper.get("scriptEnv");
		ThreadHelper.set("scriptEnv", env);

		Parser p=new Parser(buf,false);
		Object o=p.parseVars(macroKey);
		if (o==null) o="";
		if (o instanceof StringBuffer) o=o.toString();
		ThreadHelper.set("scriptEnv", old);
		return o;
	}
	
	public static List<String> splitMacro(String buf,char macroKey){
		if (buf==null) return null;
		Parser p=new Parser(buf,false);
		List<String> o=p.splitVars(macroKey);
		return o;
	}
	
	public static void noCache(HttpServletResponse resp) {
		if (resp.containsHeader("Cache-Control")) return;
		resp.setHeader("Pragma", "no-cache");
		resp.addHeader("Cache-Control", "no-store, no-cache, must-revalidate");
		resp.setDateHeader("Expires", 0);
	}

	public static void doCache(HttpServletResponse resp) {
		if (resp.containsHeader("Cache-Control")) return;
		resp.setHeader("Cache-Control", "public");
		resp.setHeader("Pragma", "cache");
		Calendar c=LightUtil.getCalendar(); c.add(Calendar.DATE, 7);
    	resp.setDateHeader("Expires", c.getTime().getTime());
    	try {
			resp.setDateHeader("Last-Modified", LightUtil.decodeLongDate("2000-01-01T00:00:00.000+0000").getTime());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static StringBuffer toJsonString(Object o){
		return toJsonString(o,false);
	}
	
	public static StringBuffer toJsonString(Object o, boolean shortName){
		StringWriter writer=new StringWriter();
		try {
			writeJson(writer,o,null,shortName);
			writer.close();
		} catch (IOException e) {
			LogUtil.dumpStackTrace(e);
		}
		return writer.getBuffer();
	}
	
	public static StringBuffer toShortJsonString(Object o){
		StringWriter writer=new StringWriter();
		try {
			writeJson(writer,o,null, true);
			writer.close();
		} catch (IOException e) {
			LogUtil.dumpStackTrace(e);
		}
		return writer.getBuffer();
	}
	
	private static void writeJson(Writer w,Object node,Boolean isLimiter, boolean shortName) throws IOException{
		if (node==null) {
			w.write("null");
			return;
		}
		if (node instanceof String){
			String s=(String)node;
			if (isLimiter!=null && isLimiter){
				w.write(s);
			}
			else{
				if (s.startsWith("@@@")){
					w.write(s.substring(3));
				}
				else
				w.write(LightStr.encodeJsonString(s));
			}
			return;
		}
		if (node instanceof java.sql.Timestamp){
			Timestamp dt=(Timestamp)node;
			w.write("\""+encodeLongDate(dt)+"\"");	
			return;
		}
		if (node instanceof java.sql.Date){
			java.sql.Date dt=(java.sql.Date)node;
			w.write("\""+encodeShortDate(dt)+"\"");				
			return;
		}
		if (isPrimitive(node.getClass())){
			w.write(""+node);
			return;
		}
		if (node instanceof List){
			w.append("[");
			List<Object> list=(List)node;
			boolean first=true;
			for (Object o:list){
				if (!first) writeJson(w,",",true,shortName);
				writeJson(w,o,false,shortName);
				first=false;
			}
			w.append("]");
			return;
		}
		if (node instanceof Map){
			w.append("{");
			Map<String,Object> map=(Map)node;
			boolean first=true;
			for (String key:map.keySet()){
				if (!first) writeJson(w,",",true, shortName);
				if (shortName) {
					if (LightStr.isIdentity(key)) writeJson(w,key,true,shortName); else writeJson(w,key,false,shortName); 
				}
				else writeJson(w,key,false,shortName);
				writeJson(w,":",true,shortName);
				writeJson(w,map.get(key),false,shortName);
				first=false;
			}
			w.append("}");
			return;
		}
		writeJson(w,toJsonObject(node),false,shortName);
	}
	public static Object parseJson(String s) throws Exception{
		return LightStr.validateJson(s);
	}
	
	public static Object toJsonObject(Object o){
		return toJsonObject(o,0);
	}
	
	//1:_class 2:_xml 4:disable _xmlBean
	public static Object toJsonObject(Object o,int options){
		if (o == null) return null;
		if (o instanceof XmlNode) return ((XmlNode)o).toJsonObject();
		if (o instanceof Character) return o+"";
		if (o instanceof Boolean) return o;
		if (o instanceof String) return o;
		if (o instanceof Integer) return o;
		if (o instanceof Double) return o;
		if (o instanceof BigDecimal) return ((BigDecimal)o).doubleValue();
		if (o instanceof Long) return o;
		if (o instanceof java.sql.Timestamp) return o;
		if (o instanceof java.sql.Date) return o;
		if (o instanceof java.util.Date) return shortDate((java.util.Date)o);
		if (o instanceof List){
			List list=new ArrayList();
			List ll=(List)o;
			for (int i=0;i<ll.size();i++){
				Object item=ll.get(i);
				list.add(toJsonObject(item,options));
			}
			return list;
		}
		if (o instanceof Map){
			Map map=new TreeMap();
			List keys=new ArrayList();
			for (Object key:((Map)o).keySet()){
				keys.add(key);
			}
			for (int i=0;i<keys.size();i++){
				Object key=keys.get(i);
				Object val=((Map)o).get(key);
				map.put(key.toString(), toJsonObject(val,options));
			}
			return map;
		}
		Class c=o.getClass();
		Map map=new TreeMap();
		List<Field> list = ReflectUtil.getFieldMap(c);
		//System.out.println("C="+c.getName()+", o="+o);
		for (int i=0;i<list.size();i++){
			Field f=list.get(i);
			//16=final 8=static 128=transient
			String alia=null;
			boolean toXml=false;
			JsonField jf=f.getAnnotation(JsonField.class);
			if (jf!=null){
				alia=jf.value();
				if (jf.output()==false) continue;
				toXml=jf.xmlBean();
				if ((options&4)!=0) toXml=false; //force ignore xmlBean if 4 is set
			}
			if ((f.getModifiers()&128)!=0 && alia==null) continue;
			if (alia==null || alia.length()==0) alia=f.getName();
			if (alia==null) continue;
			try {
				Object obj=ReflectUtil.getProperty(o, f.getName());
				Object v=null;
				if (!toXml){
					if ((options&128)!=0 && obj!=null && (obj instanceof List ||obj instanceof Map));
					else v=toJsonObject(obj,options);
				}
				else v=getBeanFactory().toXml(obj,1);
				if (v!=null) map.put(alia, v);
			} catch (Exception e) {
				LogUtil.dumpStackTrace(e);
			}
		}
		if ((options&1)!=0){ //_class as partial of the object
			map.put("_cls_", CLASS.getName(c));
		}
		if ((options&2)!=0){ //_xml as partial of the object
			map.put("_xml_", getBeanFactory().toXml(o,1));
		}
		return map;
	}
	
	public static Object fromJsonObject(Object o) throws Exception{
		if (o==null) return null;
		if (LightUtil.isPrimitive(o.getClass())) return o;
		if (o instanceof List){
			List to=new ArrayList();
			List from=(List)o;
			for (Object it:from){
				to.add(fromJsonObject(it));
			}
			return to;
		}
		if (!(o instanceof Map)) return o;
		Map map=(Map)o;
		String clz=(String)map.get("_cls_");
		if (clz==null) return o;
		if (!clz.contains(".")){
			clz=getBeanFactory().findRealClass(clz);
		}
		Class cls=CLASS.forName(clz);
		Object ret=cls.newInstance();
		List<Field> list = ReflectUtil.getFieldMap(cls);
		for (Field f:list){
			//16=final 8=static 128=transient
			if ((f.getModifiers()&128)!=0) continue;
			try {
				Object val=ReflectUtil.getProperty(o, f.getName());
				Object tar=ReflectUtil.getProperty(ret, f.getName());
				
				if (val==null) continue;
				if (tar instanceof List && val instanceof List){
					List to=(List)tar;
					List from=(List)val;
					for (Object it:from){
						to.add(fromJsonObject(it));
					}
				}
				else
				if (tar instanceof Map && val instanceof Map){
					Map to=(Map)tar;
					Map from=(Map)val;
					for (Object it:from.keySet()){
						to.put(it, fromJsonObject(from.get(it)));
					}
				}
				else{
					ReflectUtil.setProperty(ret, f.getName(), fromJsonObject(val));
				}
			} catch (Exception e) {
				LogUtil.dumpStackTrace(e);
			}
		}
		return ret;
	}

	public static <T> T getVar(String var, T def){
		int i=var.indexOf("||");
		String or=null;
		if (i>0){
			or=var.substring(i+2);
			var=var.substring(0,i);
		}
		if (var.startsWith("@")){
			return (T)var;
		}
		T v=null;
		
		if (v==null){
			AbstractConfig en=getCfg();
			if (en!=null) v=(T)en.getVars().get(var);
		}
		if (v==null) v=(T)GlbHelper.get(var);
		if (v==null){
			if (or!=null) v=getVar(or,def);
			else v=def;
		}
		return v;
	}

	public static synchronized IBeanFactory getBeanFactory(){
		return (IBeanFactory)ThreadHelper.get("beanFactory");
	}
	
	public static String getCookie(HttpServletRequest req,String key, String def) throws IOException {
		String path=getContextPath(); if (LightStr.isEmpty(path)) path="/";
		Cookie[] cookies = req.getCookies();
		if (cookies != null) {
			for (int i = 0; i < cookies.length; i++) {
				Cookie cook = cookies[i];
				if (cook.getName().equals(key)) {
					String st = cook.getValue();
					if (st.startsWith("*"))
						st="*"+LightStr.decode(st.substring(1));
					else{
						try {
							st = LightStr.decodeUrl(st);
						} catch (UnsupportedEncodingException e) {
						}
					}
					if (st.startsWith("\"")) st=st.substring(1);
					if (st.endsWith("\"")) st=st.substring(st.length()-1);
					if (st.length()==0) st=def;
					if (st!=null) def=st.replace('+', ' '); 
					break;
				}
			}
		}
		//System.out.println("ServerGetCookie "+key+"="+key+", path="+path+", val="+def+", domain="+req.getServerName());
		return def;
	}
	
	public static boolean isSecure(HttpServletRequest req){
		String url=LightUtil.getVar("mainUrl", "");
		if (url.length()==0){
			req.getProtocol();
		}
		return url.toLowerCase().startsWith("https");
	}
	
	public static void setCookie(HttpServletRequest req, HttpServletResponse resp,String key, String value, int age, boolean httpOnly) throws IOException {
		if (resp==null) return;
		Cookie cook;
		try {
			String path=getContextPath(); if (LightStr.isEmpty(path)) path="/";
			if (!value.startsWith("*")){
				cook = new Cookie(key, LightStr.encodeUrl(value));
			}
			else
				cook = new Cookie(key, "*"+LightStr.encode(value.substring(1)));
			cook.setPath(path);
			cook.setMaxAge(age);
			cook.setHttpOnly(httpOnly);
			cook.setSecure(isSecure(req));
			//cook.setDomain(req.getServerName());
			resp.addCookie(cook);
			
			List cookies=(List)ThreadHelper.get("_cookies_");
			if (cookies==null){
				cookies=new ArrayList();
				ThreadHelper.set("_cookies_", cookies);
			}
			cookies.add(cook);
			//System.out.println("ServerSetCookie key="+key+", path="+path+", val="+value+", domain="+cook.getDomain());
		} catch (UnsupportedEncodingException e) {
		}
	}
	
	public static IDataService getDataService(){
		IDataService data=(IDataService)ThreadHelper.get("$Ds$");
		if (data==null){
			data=LightUtil.getCfg().getDs();
			if(data==null){
				System.out.println("cfg="+LightUtil.getCfg());
			}
		}
		return data;
	}
	
	public static IRepository getRepository(){
		IRepository data=(IRepository)ThreadHelper.get("$Fs$");
		if (data==null){
			AbstractConfig cfg=getCfg();
			if (cfg!=null) data = cfg.getFs();
		}
		return data;
	}

	public static IDataService getDataService(String dsId){
		if (dsId==null) return getDataService();
		IDataService ds = LightUtil.getBeanFactory().getDataService(dsId);
		if (ds==null) ds=getDataService();
		return ds;
	}

	public static IRepository getRepository(String fsId){
		if (fsId==null) return getRepository();
		IRepository fs = LightUtil.getBeanFactory().getRepository(fsId);
		if (fs==null) fs=getRepository();
		return fs;
	}

	
	public static String getUserId() {
		String userId=(String)ThreadHelper.get("userId");
		if (userId==null) userId="guest";
		return userId;
	}
	
	public static String getAppVersion(){
		String version=(String)GlbHelper.get("appVersion");
		if (LightStr.isEmpty(version)) version=LightUtil.bootupTime+"";
		return version;
	}
	
	@SuppressWarnings("unchecked")
	public static Object decodeObject(String v, Class c, boolean local) throws Exception{
		if (v==null) return v;
		//if (c.isEnum()) return Enum.valueOf(c, v);
		String cls=c.getName();
		if (cls.equals("char") || cls.equals("java.lang.Character")){
			return (java.lang.Character)v.charAt(0);
		}
		if (cls.equals("java.lang.String")){
			return v;
		}
		if (cls.equals("java.lang.Object")){
			return v;
		}
		if (cls.equals("int") || cls.equals("java.lang.Integer")){
			if (local){
				return (int)Math.round(fromNativeNumber(v,0d));
			}
			else return decodeInt(v);
		}
		if (cls.equals("long") || cls.equals("java.lang.Long")){
			if (local){
				return Math.round(fromNativeNumber(v,0d));
			}
			else return decodeLong(v);
		}
		if (cls.equals("float") || cls.equals("java.lang.Float")){
			if (local){
				return fromNativeNumber(v,0d).floatValue();
			}
			else return decodeFloat(v);
		}
		if (cls.equals("double") || cls.equals("java.lang.Double")){
			if (local){
				return fromNativeNumber(v,0d);
			}
			else return decodeDouble(v);
		}
		if (cls.equals("boolean") || cls.equals("java.lang.Boolean")){
			return decodeBoolean(v);
		}
		if (cls.equalsIgnoreCase("java.util.Date")){
			throw new RuntimeException("java.util.date not supported, please use java.sql.Date or java.sql.Timestamp instead");
		}
		if (cls.equalsIgnoreCase("java.sql.Date")){
			java.sql.Date dt;
			if (local) {
				dt=fromNativeShortDate(v);
			}
			else {
				dt=decodeShortDate(v);
			}
			if (dt==null) return null;
			return shortDate(dt);

		}
		if (cls.equalsIgnoreCase("java.sql.Timestamp")){
			if (local) {
				return fromNativeLongDate(v);
			}
			else {
				TimeZone timezone=getTimezone();
				return decodeLongDate(v,timezone);
			}
		}

		return null;
	}
	
	public static void clearup(Map<String,Object> map){
		Object[] oo=map.values().toArray();
		for (int i=0;i<oo.length;i++){
			Object o=oo[i];
			if (o instanceof ICleanup){
				ICleanup cln=(ICleanup)o;
				cln.cleanup();
			}
			else
			if (o instanceof File){
				File f=(File)o;
				if (f.exists()) {
					//System.out.println("deleting "+f.getAbsolutePath());
					StreamUtil.xdelete(f);
				}
			}
			else
			if (o instanceof TempItem){
				TempItem f=(TempItem)o;
				//f.delete();
			}
		}
		map.clear();
	}
	
	public static int isSupportedModule(List<String> sup, String url){
		if (sup==null||sup.size()==0) return 0;
		url=url.toLowerCase().replace('\\','/');
		int idx=0;
		for (String s:sup){
			idx++;
			if (url.contains("wab.direct")) return idx;
			if (url.contains("/"+s.toLowerCase())) return idx;
		}
		return 0;
	}
	
	public static Map loadFromCfgSite(String cfgSiteUrl,String ctxId) throws Exception{
		List<String> ips = LightUtil.getIpAddresses();
		if (ips.size()==0) ips.add("nofound");
		String ip=LightStr.join(ips, ", ");
		String ctx=LightStr.replace(ctxId, "/", "");
		if (LightStr.isEmpty(cfgSiteUrl)) return new HashMap();
		String url = cfgSiteUrl + "/service?$=" + LightStr.encode("{cmd:'GET_CFG',serverIP:'"+ip+"',contextId:'"+ctx+"'}");
		HttpClient hc = new HttpClient();
		StringBuffer buf = new StringBuffer();
		hc.doGet(url, buf);
		String props=LightStr.decode(buf.toString());
		Map m=(Map)LightUtil.parseJson(props); m.remove("successful");
		System.out.println("CfgSiteUrl="+cfgSiteUrl+",IP="+ip+",ctx="+ctx+",BUF=" + m);
		return m;
	}
	
	public static String getLang(){
		String lang=(String)ThreadHelper.get("$$lang");
		if (lang==null) lang="en";
		return lang;
	}
	
	public static String getToken(String salt) throws Exception{
		if (salt==null) salt="SuperEasy";
		String ss="{time:'"+new Date().getTime()+"',uid:'"+LightUtil.getUserId()+"'}";
		String token=EncryptUtil.encrypt(ss, salt);
		return token;
	}
	
}
