package com.masscustsoft.helper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.masscustsoft.service.ProgressStatus;
import com.masscustsoft.service.UriContext;
import com.masscustsoft.service.inner.DiskTempItem;
import com.masscustsoft.service.inner.DiskTempItemFactory;
import com.masscustsoft.util.GlbHelper;
import com.masscustsoft.util.LightStr;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.LogUtil;
import com.masscustsoft.util.ReflectUtil;
import com.masscustsoft.util.StreamUtil;
import com.masscustsoft.util.ThreadHelper;
import com.masscustsoft.xml.XmlNode;
import com.masscustsoft.xml.XmlParser;

public class Upload {
	public static final String ATTRP = "p";
	public static final String ATTRVERB = "verb";
	public static final String ATTRUPLOAD = "upload";
	public static final String SESSIONID ="WABSESSIONID";
	
	Map<String, Object> map = new HashMap<String, Object>();
	ProgressStatus listener = null;
	String psid = null;
	HttpServletRequest req;
	HttpServletResponse resp;
	UriContext context;
	String uri;
	String filePath = "";
	
	List<DiskTempItem> files = new ArrayList<DiskTempItem>();

	public HttpServletRequest getRequest() {
		return req;
	}

	public HttpServletResponse getResponse() {
		return resp;
	}

	public static Upload getUpload() {
		return (Upload) ThreadHelper.get(ATTRUPLOAD);
	}

	@SuppressWarnings("unchecked")
	public Upload(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		ThreadHelper.set(ATTRUPLOAD, this);
		this.req = req;
		this.resp = resp;
		this.psid=psid;
		
		Map<String, String> p = new HashMap<String, String>();

		ThreadHelper.set("request", req);
		// LightUtil.info(this,"Upload cookies="+req.getAttribute("cookies"));
		if (req == null)
			return;
		String uri = req.getRequestURI();
		String cpath = LightUtil.getContextPath();
		LogUtil.debug("UPLOAD uri=" + uri + ",cpath=" + cpath);

		for (Enumeration<String> e = req.getHeaderNames(); e.hasMoreElements();) {
			String nm = e.nextElement();
			String val = req.getHeader(nm);
			if (nm.equals("sessionid"))
				nm = "sessionId";
			p.put(nm, val);
		}

		uri = uri.substring(cpath.length());
		if (uri.startsWith("/"))
			uri = uri.substring(1);
		this.uri = uri;
		String verb = uri;
		filePath = "";

		int i = uri.indexOf("/");
		if (i > 0) {
			verb = uri.substring(0, i);
			filePath = uri.substring(i + 1);
		}
		p.put("f", filePath);

		for (Enumeration<String> e = req.getParameterNames(); e.hasMoreElements();) {
			String name = (String) e.nextElement();
			String value=req.getParameter(name);
			if (req.getQueryString()!=null && req.getQueryString().contains("&"+name+"=")) value=URLDecoder.decode(value,LightUtil.UTF8);
			if (name.startsWith("_")) {
				name = name.substring(1);
				value = LightStr.decode(value);
			}
			p.put(name, value);
		}

		ThreadHelper.set(ATTRVERB, verb);
		ThreadHelper.set(ATTRP, p);
		req.setAttribute(ATTRP, p);

		map.putAll(p);

		parseEncryptParams("a");
		parseEncryptParams("b");
		parseEncryptParams("c");

	}

	public void parseContent() throws IOException{
		if (req!=null){
			String ctype = req.getContentType();
			if (ctype == null)
				ctype = "";
			int uploadsize = req.getContentLength();
			if (uploadsize > 0) {
				try {
					if (ctype.startsWith("text/json")) {
						parseJsonUpload(req);
						ctype = "text/json";
					} else if (ctype.startsWith("multipart/form-data;")) {
						parseMultipartUpload(req);
					} else if (ctype.startsWith("text/xml")) {
						parseXmlUpload(req);
					} else if (ctype.startsWith("multipart/related") && getSubType(ctype, "type").equals("text/xml")) {
						parseSoapUpload(req);
					} else {
						parsePostUpload(req);
					}
				} catch (Exception e) {
					LogUtil.dumpStackTrace(e);
					LogUtil.info("upload ERR:" + e);
				}
			}
			
			psid = (String) map.get("psid");
			if (psid != null) {
				String sessionId=(String)map.get("sessionId");
				if (LightStr.isEmpty(sessionId)) sessionId=(String)map.get(SESSIONID);
				psid = "p-" + sessionId + "-" + psid;
				listener = new ProgressStatus();
				listener.setPsid(psid);
				GlbHelper.set(psid, listener);
			}
			
			if (!"getProgressInfo".equals(get("cmd")))
				LogUtil.debug("UPLOAD URI=" + uri + ", UPMAP=" + this.getFieldMap());
		}
		
		// process hex and base64
		List<String> flds = new ArrayList<String>();
		for (String fld : map.keySet()) {
			if (map.get(fld) instanceof String)
				flds.add(fld);
		}
		for (String fld : flds) {
			String text = (String) map.get(fld);
			if (text.startsWith("data:")) { // image/png;base64,
				map.remove(fld);
				// BCD value for image
				String ext = text.substring(text.indexOf("/") + 1, text.indexOf(";"));
				map.put(fld, "attachment." + ext);
				map.put(fld + ".filename", "attachment." + ext);
				DiskTempItem it = new DiskTempItem();
				it.setFieldName(fld);
				it.setFileName("picture." + ext);
				text = text.substring(text.indexOf("base64,") + 7);
				it.setCachedContent(org.bouncycastle.util.encoders.Base64.decode(text));
				map.put(fld + ".fileitem", it);
			} else if (fld.indexOf("$HEX$") >= 0) { // fld $HEX$ filename
				map.remove(fld);
				if (text.length() == 0)
					continue;
				String[] parts = fld.split("\\$HEX\\$");

				String fileName = parts[1];
				fld = parts[0];
				map.put(fld, fileName);
				map.put(fld + ".filename", fileName);
				DiskTempItem it = new DiskTempItem();
				it.setFieldName(fld);
				it.setFileName(fileName);
				it.setCachedContent(LightStr.getHexContent(text));

				map.put(fld + ".fileitem", it);
			} else if (fld.indexOf("$BASE64$") >= 0) { // fld $HEX$ filename
				map.remove(fld);
				if (text.length() == 0)
					continue;
				String[] parts = fld.split("\\$BASE64\\$");

				String fileName = parts[1];
				fld = parts[0];
				map.put(fld, fileName);
				map.put(fld + ".filename", fileName);
				DiskTempItem it = new DiskTempItem();
				it.setFieldName(fld);
				it.setFileName(fileName);
				it.setCachedContent(org.bouncycastle.util.encoders.Base64.decode(text));

				map.put(fld + ".fileitem", it);
			}
		}
	}
	
	private void parseEncryptParams(String field) throws IOException {
		String a = getStr(field, null);
		if (a != null) {
			String[] ss = LightStr.parseSalted(a);
			long stamp = LightStr.parseTimestamp(ss[1]), now = System.currentTimeMillis();
			long delta = now - stamp;
			if (delta < 0)
				delta = -delta;
			if (delta > 1000 * 60 * 60 * 24)
				throw new IOException("Bad Request!");
			try {
				String json = LightStr.parseLetters(ss[0]);
				Map<String, Object> m = (Map) LightUtil.parseJson(json);
				for (String k : m.keySet()) {
					Object v = m.get(k);
					if (v == null)
						continue;
					String val;
					if (v instanceof Map)
						val = LightUtil.toJsonString(v).toString();
					else
						val = v.toString();
					if (val.equals("@@@null"))
						continue;
					map.put(k, val);
				}
				map.remove("a");
			} catch (Exception e) {
				throw new IOException("Bad Request");
			}
		}
	}

	private void put(String name, String value) { //cannot merge
//		String val = (String) map.get(name);
//		if (val == null) {
			map.put(name, value);
//		} else {
//			map.put(name, val + ", " + value);
//		}
	}

	private void parsePostUpload(ServletRequest req) throws Exception {
		ServletInputStream is = req.getInputStream();
		StringBuffer buf = new StringBuffer();
		StreamUtil.loadStream(is, buf, LightUtil.UTF8);
		String[] list = buf.toString().split("&");
		for (String ss : list) {
			int idx = ss.indexOf("=");
			if (idx <= 0)
				continue;
			String name = ss.substring(0, idx);
			String value = LightStr.decodeUrl(ss.substring(idx + 1));
			if (name.startsWith("_")) {
				name = name.substring(1);
				value = LightStr.decode(value);
			}
			put(name, value);
		}
	}

	private void parseJsonUpload(ServletRequest req) throws Exception {
		ServletInputStream is = req.getInputStream();
		StringBuffer buf = new StringBuffer();
		StreamUtil.loadStream(is, buf, LightUtil.UTF8);
		Map reqMap = (Map) LightUtil.parseJson(buf.toString());
		map.putAll(reqMap);
	}

	private void parseXmlUpload(ServletRequest req) throws Exception {
		ServletInputStream is = req.getInputStream();
		XmlParser parser = new XmlParser(is, LightUtil.UTF8);
		XmlNode node = parser.readNode();

		resetXmlNode(node);
	}

	long crc = 0;

	public String getField(FileItem it){
		String fld = it.getFieldName();
		int idx = fld.indexOf('@');
		if (idx > 0)
			fld = fld.substring(idx + 1);
		if (fld.endsWith("[]"))
			fld = fld.substring(0, fld.length() - 2); // remove postfix []
		return fld;
	}
	
	@SuppressWarnings("unchecked")
	private void parseMultipartUpload(HttpServletRequest req) throws FileUploadException, IOException {
		DiskTempItemFactory factory = new DiskTempItemFactory();
		Long threshold = (Long) GlbHelper.get("thresholdUploadSize");
		if (threshold == null)
			threshold = 1024l;
		Long max = (Long) GlbHelper.get("maxUploadSize");
		if (max == null || max==0l)
			max = -1l;
		// System.out.println("max="+max+",threshold="+threshold);
		// maximum size that will be stored in memory
		factory.setSizeThreshold((int) threshold.intValue());
		// the location for saving data that is larger than getSizeThreshold()

		// String root=(String)serv.getAttribute("ROOT");
		if (threshold < max || max == -1) {
			factory.setRepository(LightUtil.getCfg().getTempService());
		}

		ServletFileUpload upload = new ServletFileUpload(factory);
		upload.setFileSizeMax(-1);
		// Add listener //no limit to individual file size
		if (listener != null) {
			upload.setProgressListener(listener);
		}
		// maximum size before a FileUploadException will be thrown
		upload.setSizeMax(max);

		List fileItems = upload.parseRequest(req); // System.out.println("fileItems="+fileItems);
		crc = 0;
		for (Object i : fileItems) {
			DiskTempItem it = (DiskTempItem) i;
			String fld = getField(it);

			String fileName = it.getName();
			String text = it.getString();

			if (fileName == null) {
				text = text.replace('\ufffe', '\n');
				put(fld, text);
				// System.out.println("crc-field "+fld+"="+text);
				for (int j = 0; j < text.length(); j++) {
					int ch = (int) text.charAt(j);
					if (ch == 13 || ch == 10 || ch == 9 || ch == 32)
						continue;
					crc += ch;
				}
			} else if (fileName.length() > 0) {
				map.put(fld, fileName);
				map.put(fld + ".filename", fileName);
				map.put(fld + ".fileitem", it);
				files.add(it);
			}
		}
	}

	public Object get(String attr) {
		return map.get(attr);
	}

	public void put(String attr, Object val) {
		map.put(attr, val);
	}

	public int getInt(String attr, int def) {
		String s = (String) get(attr);
		if (s == null || "null".equals(s)) {
			return def;
		}
		return LightUtil.decodeInt(s);
	}

	public long getLong(String attr, long def) {
		String s = (String) get(attr);
		if (s == null || "null".equals(s)) {
			return def;
		}
		return LightUtil.decodeLong(s);
	}

	public String getStr(String attr, String def) {
		String s = (String) get(attr);
		if (s == null || "null".equals(s) || "undefined".equals(s)) {
			return def;
		}
		return s.trim();
	}

	public Boolean getBoolean(String attr, Boolean def) {
		String s = getStr(attr, null);
		if (s == null) {
			return def;
		}
		return LightUtil.decodeBoolean(s);
	}

	public Date getDate(String attr, Date def) throws Exception {
		String s = getStr(attr, null);
		if (s == null)
			return def;
		return LightUtil.fromNativeShortDate(s);
	}

	public Timestamp getTimestamp(String attr, Timestamp def) throws Exception {
		String s = getStr(attr, null);
		if (s == null)
			return def;
		Timestamp ts = LightUtil.fromNativeLongDate(s);
		// LogUtil.info("attr="+attr+", timestamp="+ts.toGMTString());
		return ts;
	}

	public Double getNumber(String attr, Double def) throws Exception {
		String s = getStr(attr, null);
		if (s == null)
			return def;
		return LightUtil.fromNativeNumber(s, def);
	}

	public void setStr(String attr, String def) {
		if (def == null)
			map.remove(attr);
		else
			map.put(attr, def);
	}

	public void resetXmlNode(XmlNode node) {
		for (String key : node.getAttributeKeySet()) {
			map.put(key, node.getAttribute(key));
		}
		map.put("xmlNode", node);
	}

	public String getFileName(String fld, String def) {
		String f = (String) map.get(fld + ".filename");
		if (f == null)
			f = def;
		return f;
	}

	public FileItem getFileItem(String fld) {
		return (FileItem) map.get(fld + ".fileitem");
	}

	// public InputStream getFileStream(String fld){
	// return (InputStream)map.get(fld+".file");
	// }
	// public File getFile(String fld){
	// return (File)map.get(fld+".TempFile");
	// }

	public Map<String, Object> getMapValue(String prefix) {
		Map<String, Object> m = new HashMap<String, Object>();
		for (String key : map.keySet()) {
			if (key.startsWith(prefix + ".")) {
				m.put(key.substring(prefix.length() + 1), map.get(key));
			}
		}
		return m;
	}

	public Map<String, Object> getFieldMap() {
		return map;
	}

	public void add(Map<String, Object> m) {
		map.putAll(m);
	}

	public void remove(String key) {
		map.remove(key);
	}

	public void destroy() {
		if (psid != null) {
			GlbHelper.remove(psid);
			listener = null;
		}

		for (Object o : map.values()) {
			if (o instanceof InputStream) {
				InputStream is = (InputStream) o;
				try {
					is.close();
				} catch (IOException e) {
				}
			}
		}
		for (Object o : map.values()) {
			if (o instanceof File) {
				File f = (File) o;
				if (!f.delete())
					f.deleteOnExit();
			}
			if (o instanceof DiskTempItem) {
				DiskTempItem df = (DiskTempItem) o;
				if (df.getStoreLocation() != null)
					df.getStoreLocation().delete();
			}
		}

	}

	private static String[][] extToMimeType = new String[][] {
		{ ".gif", "image/gif" }, 
		{ ".png", "image/png" },
		{ ".jpg", "image/jpeg" }, 
		{ ".jpeg", "image/jpeg" },
		{ ".tif", "image/tiff" }, 
		{ ".tiff", "image/tiff" },
		{ ".css", "text/css" }, 
		{ ".htm", "text/html" },
		{ ".html", "text/html" }, 
		{ ".js", "application/x-javascript" },
		{ ".txt", "text/plain" }, 
		{ ".csv", "text/plain" },
		{ ".zip", "application/zip" },
		{ ".exe", "application/octet-stream" },
		{ ".rm", "application/vnd.rn-realmedia" },
		{ ".mp3", "audio/mpeg" }, 
		{ ".mp4", "video/mp4" },
		{ ".swf", "application/x-shockwave-flash" },
		{ ".doc", "application/msword" },
		{ ".docx", "application/msword" },
		{ ".xls", "application/vnd.ms-excel" },
		{ ".xlsx", "application/vnd.ms-excel" },
		{ ".ppt", "application/mspowerpoint" },
		{ ".rtf", "application/msword" },
		{ ".pdf", "application/pdf" },
		{ ".apk", "application/vnd.android.package-archive"},
		{ ".cod", "application/vnd.rim.cod"},
		{ ".jad", "text/vnd.sun.j2me.app-descriptor"},
		{ ".jar", "application/java-archive"},
		{ ".appcache", "text/cache-manifest"},
		{ ".svg", "image/svg+xml"}
	};

	public static String getFileContentType(String fn) {
		String type = "application/octet-stream";
		int lastDot = fn.lastIndexOf('.');
		if (lastDot != -1) {
			String ext = fn.substring(lastDot).toLowerCase();
			for (int i = 0; i < extToMimeType.length; i++) {
				if (extToMimeType[i][0].equals(ext)) {
					type = extToMimeType[i][1];
					break;
				}
			}
		}
		return type;
	}

	public static String getCookie(String key, String def) throws IOException {
		String val = (String) ThreadHelper.get("$Cookie." + key);
		if (val != null)
			return val;
		Upload up = getUpload();
		if (up == null)
			return def;
		String ret=LightUtil.getCookie(up.getRequest(), key, null);
		if (ret==null){
			ret=def;
		}
		return ret;
	}

	public static void setCookie(String key, String value) throws IOException {
		ThreadHelper.set("$Cookie." + key, value);
		Upload up = Upload.getUpload();
		if (up == null)
			return;
		LightUtil.setCookie(up.getRequest(), up.getResponse(), key, value, 2592000, false);
	}

	public static void setCookie(String key, String value, int age) throws IOException {
		ThreadHelper.set("$Cookie." + key, value);
		Upload up = Upload.getUpload();
		if (up == null)
			return;
		LightUtil.setCookie(up.getRequest(), up.getResponse(), key, value, age, false);
	}

	public static void clrCookie(String key) throws IOException {
		ThreadHelper.set("$Cookie." + key, null);
		Upload up = Upload.getUpload();
		if (up == null)
			return;
		LightUtil.setCookie(up.getRequest(), up.getResponse(), key, "", 0, false);
	}

	public void setStatus(long position, long size, String status) {
		if (listener != null) {
			listener.updateStatus(position, size, status);
		}
	}

	public void setStatus(int per, String status) {
		if (listener != null) {
			listener.updateStatus(per, 100, status);
		}
	}

	public void setStatus(String status) {
		if (listener != null) {
			listener.updateStatus(0, 0, status);
		}
	}

	private String hexStr(byte b) {
		b &= 0xff;
		return hex(b >> 4) + "" + hex(b);
	}

	private char hex(int b) {
		b &= 0xf;
		if (b < 10)
			return (char) ('0' + b);
		return (char) ('A' + b - 10);
	}

	public static String getClientIp() {
		String s = (String) ThreadHelper.get("_ClientIP");
		return s;
	}

	public static String getTokenId() {
		String s = (String) ThreadHelper.get("_TokenID");
		return s;
	}

	public static String getGroup() {
		String s = (String) ThreadHelper.get("_Group");
		return s;
	}

	public String getHost() {
		String url = req.getScheme() + "://" + req.getServerName();
		if (req.getServerPort() != 80)
			url += ":" + req.getServerPort();
		return url;
	}

	public UriContext getContext() {
		return context;
	}

	public void setContext(UriContext context) {
		this.context = context;
	}

	private String getSubType(String ctype, String subType) {
		int i = ctype.indexOf(" " + subType + "=\"");
		if (i < 0)
			return "";
		int j = ctype.indexOf("\"", i + subType.length() + 3);
		return ctype.substring(i + 3 + subType.length(), j);
	}

	@SuppressWarnings("unchecked")
	private void parseSoapUpload(HttpServletRequest req) throws FileUploadException, IOException {
		String ctype = req.getContentType();
		String boundary = "--" + getSubType(ctype, "boundary");
		String start = getSubType(ctype, "start");

		StringBuffer buf = new StringBuffer();
		ServletInputStream is = req.getInputStream();
		StreamUtil.loadStream(is, buf, LightUtil.UTF8);
		is.close();

		LogUtil.info("SOAP BODY::\n" + buf);
		// System.out.println("boundary="+boundary);
		int ii = 0, bs = boundary.length();
		for (;;) {
			int i = buf.indexOf(boundary, ii);
			// System.out.println("ii="+ii+", i="+i);
			String part = buf.substring(ii, i).trim();
			if (part.length() > 0) {
				int m = part.indexOf("Content-ID: <"), n = part.indexOf(">", m);
				String cid = part.substring(m + 13, n);
				int j = part.indexOf("\r\n\r\n");
				String body = part.substring(j + 4);
				ThreadHelper.set(cid, body);
				// System.out.println("cid="+cid+", body="+body);
			}
			if (buf.charAt(i + bs) != '\r')
				break;
			ii = i + boundary.length() + 2;
		}
		// System.out.println("MAP="+map);
		String main = (String) ThreadHelper.get(start);
		// System.out.println("MAIN="+main);

		XmlParser xp = new XmlParser(main);
		XmlNode node = xp.readNode();
		resetXmlNode(node);
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public long getCrc() {
		return crc;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		map.put("f",filePath);
		this.filePath = filePath;
	}
	
	public List<DiskTempItem> getFiles(){
		return files;
	}
	
	public int getAttatchmentCount(Class cls){
		int nn=0;
		for (DiskTempItem it:files){
			String fld=getField(it);
			if (ReflectUtil.findField(ReflectUtil.getFieldMap(cls), fld)==null) continue;
			nn++;
		}
		System.out.println("cls="+cls+", atts="+nn);
		return nn;
	}
	
	public static String getWebRoot(){
		Upload up=Upload.getUpload();
		HttpServletRequest req = up.getRequest();
		String url=req.getScheme()+"://"+req.getServerName();
		if (req.getServerPort()!=80) url+=":"+req.getServerPort();
		url+=LightUtil.getContextPath();
		return  url;
	}
}
