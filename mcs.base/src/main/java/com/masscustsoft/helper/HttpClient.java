package com.masscustsoft.helper;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import com.masscustsoft.service.inner.ConnectionStream;
import com.masscustsoft.util.GlbHelper;
import com.masscustsoft.util.LightFile;
import com.masscustsoft.util.LightStr;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.ThreadHelper;

public class HttpClient {

	//static {
	//	HttpURLConnection.setFollowRedirects(false);
	//}
	int timeout=120000;
	
	private Map<String,HttpCookie> cookies=new HashMap<String,HttpCookie>();
	String tokenId;
	String relocation="";
	Integer responseCode;
	Map<String,String> headers=new HashMap<String,String>();
	String charset=LightUtil.UTF8;
	
	transient URL url;
	transient Integer contentLength;
	
	public HttpClient(){
		String tm=(String)GlbHelper.get("HttpClientTimeout");
		if (tm!=null){
			int to=LightUtil.decodeInt(tm);
			if (to<30000) to=30000;
			timeout=to;
		}
	}
	
	public void init(HttpClient c){
		cookies.clear();
		cookies.putAll(c.getCookies());
		timeout=c.getTimeout();
		tokenId=c.getTokenId();
		c.relocation=c.getRelocation();
	}
	
	private void connect(HttpURLConnection conn) throws Exception{
		try{
			conn.connect();
		}
		catch (Exception e){
			throw new Exception("Url not reachable: "+conn.getURL());
		}
	}
	
	public void doGet(String http,StringBuffer buf) throws Exception{
		String httpUrl=http;
		for(;;){
			relocation="";
			url=new URL(httpUrl);
			
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			/*if (httpUrl.startsWith("http://")) {
	            URL url = new URL(httpUrl);
	            conn = (HttpURLConnection) url.openConnection();
	        } else {
	            SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
	            URL url = new URL(httpUrl);
	            HttpsURLConnection ssl = (HttpsURLConnection) url.openConnection();
	            conn=ssl;
	            ssl.setSSLSocketFactory(sslsocketfactory);
	        }*/
			conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.220 Safari/535.1");
			conn.setRequestMethod("GET");
			applyCookies(conn);
			conn.setConnectTimeout(timeout);
			if (!LightStr.isEmpty(tokenId)) conn.addRequestProperty("TokenId", tokenId);
			connect(conn);
			processReponseHeaders(conn);
			if (relocation.length()==0){
				InputStream is = getResponseStream(conn);
				LightFile.loadStream(is, buf, charset);
			}
			httpUrl=relocation;
			conn.disconnect();
			if (httpUrl.length()>0) Thread.sleep(1000); else break;
		}
	}
	
	public void doMimePost(String http,StringBuffer buf, String... paras) throws Exception{
		Map<String,Object> pp=new HashMap<String,Object>();
		for (int i=0;i<paras.length/2;i++){
			String k=paras[i*2];
			String v=paras[i*2+1];
			if (k!=null && v!=null) pp.put(k, v);
		}
		doMimePost(http,buf,"",pp);
	}
	
	private InputStream getResponseStream(HttpURLConnection conn) throws IOException{
		int ret=conn.getResponseCode();
		if (ret!=401){
			try{
				return conn.getInputStream();
			}
			catch (Exception e){
				e.printStackTrace();
				throw new IOException("Url not reachable: "+conn.getURL());
			}
		}
		return new ByteArrayInputStream("".getBytes());
	}

	private void processReponseHeaders(HttpURLConnection conn) throws Exception{
		try{
			responseCode=conn.getResponseCode();
		}
		catch(Exception e){
			throw new Exception("Url not reachable: "+conn.getURL());
		}
		Map<String, List<String>> fields = conn.getHeaderFields();
		//System.out.println("response headers="+fields);
		contentLength=null;
		
		headers.clear();
		
		for (String fld:fields.keySet()){
			List<String> value=fields.get(fld);
			
			if (fld==null) continue;

			if (fld.equals("Set-Cookie")){
				for (String cookie:value){
					setCookie(cookie);
				}
				continue;
			}
			if (fld.equals("Content-Length")){
				contentLength=LightUtil.decodeInt(value.get(0));
				continue;
			}
			
			if (value.size()==0) continue;
			
			String newValue=value.get(0);
			
			if (fld.equals("Location")){
				processRelocate(newValue);
				continue;
			}
			
			headers.put(fld, newValue);
			//System.out.println("Response header "+fld+":"+newValue);
			
		}
	}

	private void processRelocate(String location){
		//System.out.println("relocate to="+location);
		StringBuffer base=new StringBuffer();
		base.append(url.getProtocol()+"://"+url.getHost());
		if (url.getPort()!=-1) base.append(":"+url.getPort());
		if (location.startsWith("?")) {
			base.append(url.getPath());
			relocation=base+location;
		}
		else
		if (location.startsWith("/")){
			relocation=base+location;
		}
		else relocation=location;
	}

	public void setCookie(String cookie) throws Exception{
		HttpCookie c=new HttpCookie(cookie);
		cookies.put(LightStr.encode(c.getPath()+"#"+c.getName()), c);
	}
	
	public HttpCookie getCookie(String path,String name) throws IOException{
		return cookies.get(LightStr.encode(path+"#"+name));
	}
	
	public String getCookieValue(String path,String name) throws IOException{
		HttpCookie coo = cookies.get(LightStr.encode(path+"#"+name));
		if (coo!=null) return coo.getValue();
		return "";
	}
	
	private void applyCookies(HttpURLConnection conn){
		StringBuffer buf=new StringBuffer();
		String path=url.getPath(); if (path.length()==0) path="/";
		for (HttpCookie c:cookies.values()){
			if (c.getPath()==null || path.startsWith(c.getPath())){
				if (buf.length()>0) buf.append("; ");
				buf.append(c.getName()+"="+c.getValue());
			}
		}
		//System.out.println("apply cookies="+buf.toString());
		conn.setRequestProperty("cookie",buf.toString());
		//System.out.println("Applying cookies ...path="+path+" cookie: "+buf+" cs="+cookies);
	}
	
	public String doPost(String http,StringBuffer buf, String paras, boolean followRedirect) throws Exception{
		return doPost(http,buf,paras,null,followRedirect);
	}
	
	public String doPost(String http,StringBuffer buf, String paras, String headers, boolean followRedirect) throws Exception{
		String httpUrl=http;
	
		url=new URL(httpUrl);
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.setRequestMethod("POST");
		if (paras.startsWith("<")){
			conn.setRequestProperty("Content-Type","text/xml; charset="+charset+"");
		}
		else{
			conn.setRequestProperty("Content-Type","application/x-www-form-urlencoded; charset="+charset+"");
		}
		//conn.setRequestProperty("Expect", "100-continue");
		applyCookies(conn);
		conn.setDoOutput(true);
		conn.setDoInput(true);
		conn.setInstanceFollowRedirects(followRedirect);
		if (!LightStr.isEmpty(headers)){
			String[] ss=headers.split(";");
			for (String s:ss){
				int i=s.indexOf('=');
				if (i<0) continue;
				conn.setRequestProperty(s.substring(0,i), s.substring(i+1));
			}
		}
		
		if (!LightStr.isEmpty(tokenId)) conn.addRequestProperty("TokenId", tokenId);
		connect(conn);
		OutputStreamWriter w=new OutputStreamWriter(conn.getOutputStream(),charset);
		//System.out.println("http REQ: "+paras.toString());
		w.write(paras.toString());
		w.close();
		
		processReponseHeaders(conn);
		if (relocation.length()==0){
			InputStream is = getResponseStream(conn);
			LightFile.loadStream(is, buf, charset);
			is.close();
		}
		//System.out.println("http RESP="+buf);
		httpUrl=relocation;
		conn.disconnect();
		
		return relocation;
	}
	
	public InputStream doGetDownload(String http) throws Exception{
		String httpUrl=http;
		while (httpUrl.length()>0){
			relocation="";
			url=new URL(httpUrl);
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			conn.setUseCaches(true);
			conn.setRequestMethod("GET");
			applyCookies(conn);
			conn.setDoOutput(false);
			if (!LightStr.isEmpty(tokenId)) conn.addRequestProperty("TokenId", tokenId);
			connect(conn);
			
			try{
				processReponseHeaders(conn);
				if (relocation.length()==0){
					InputStream is = new ConnectionStream(conn,getResponseStream(conn));
					return is;
				}
				httpUrl=relocation;
			}
			catch(Exception e){
				conn.disconnect();
				throw new IOException(e);
			}
		}
		return null;
	}
	
	public InputStream doPostDownload(String http, String paras) throws Exception{
		String httpUrl=http;
		while (httpUrl.length()>0){
			relocation="";
			url=new URL(httpUrl);
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			conn.setRequestMethod("POST");
			applyCookies(conn);
			conn.setDoOutput(true);
			if (!LightStr.isEmpty(tokenId)) conn.addRequestProperty("TokenId", tokenId);
			connect(conn);
			OutputStreamWriter w=new OutputStreamWriter(conn.getOutputStream());
			w.write(paras.toString());
			w.close();
			
			try{
				processReponseHeaders(conn);
			}
			catch(Exception e){
				conn.disconnect();
				throw new IOException(e);
			}
			if (relocation.length()==0){
				InputStream is = new ConnectionStream(conn,getResponseStream(conn));
				return is;
			}
			httpUrl=relocation;
			conn.disconnect();
		}
		return null;
	}
	
    static final byte CR = 0x0D;
    static final byte LF = 0x0A;
    static final byte DASH = 0x2D;
    static final byte[] HEADER_SEPARATOR = {CR, LF, CR, LF };
    static final byte[] FIELD_SEPARATOR = {CR, LF};
    static final byte[] STREAM_TERMINATOR = {DASH, DASH};
    static final byte[] BOUNDARY_PREFIX = {CR, LF, DASH, DASH};

    protected void writeMime(OutputStream w, Map<String,Object> fields, String boundary) throws Exception{
    	
    	for (String key:fields.keySet()){
			if (key.endsWith(".filename")) continue;
			Object val=fields.get(key);
			if (val==null) continue;
			if (val instanceof File){
				File f=(File)val;
				InputStream is=new FileInputStream(f); 
				w.write(BOUNDARY_PREFIX);
		        w.write(boundary.getBytes());
		        w.write(FIELD_SEPARATOR);
		        w.write(("Content-Disposition: form-data; name=\""+key+"\"; filename=\""+f.getName()+"\"").getBytes());
		        w.write(FIELD_SEPARATOR);
		        w.write(("Content-Type: application/octet-stream").getBytes());
		        w.write(HEADER_SEPARATOR);
		        LightFile.copyStream(is, w, 0);
			}
			else
			if (val instanceof InputStream){
				InputStream is=(InputStream)val; 
				w.write(BOUNDARY_PREFIX);
		        w.write(boundary.getBytes());
		        w.write(FIELD_SEPARATOR);
		        String fn=(String)fields.get(key+".filename"); if (fn==null) fn="temp"; 
		        w.write(("Content-Disposition: form-data; name=\""+key+"\"; filename=\""+fn+"\"").getBytes());
		        w.write(FIELD_SEPARATOR);
		        w.write(("Content-Type: application/octet-stream").getBytes());
		        w.write(HEADER_SEPARATOR);
		        LightFile.copyStream(is, w, 0);
			}
			else{
				w.write(BOUNDARY_PREFIX);
				w.write(boundary.getBytes());
				w.write(FIELD_SEPARATOR);
				w.write(("Content-Disposition: form-data; name=\""+key+"\"").getBytes());
				w.write(HEADER_SEPARATOR);
				w.write(val.toString().getBytes(charset));
			}
		}
		w.write(BOUNDARY_PREFIX);
		w.write(boundary.getBytes());
		w.write(STREAM_TERMINATOR);
		w.close();
    }
    
    public void doMimePost(String http,StringBuffer buf, String paras, byte[] bin, String boundary) throws Exception{
    	String httpUrl=http;
		while (httpUrl!=null && httpUrl.length()>0){
			relocation="";
			if (paras.length()>0){
				if (httpUrl.indexOf("?")>=0) httpUrl+="&"; else httpUrl+="?";
				httpUrl+=paras;
			}
			url=new URL(httpUrl);
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type","multipart/form-data; charset="+charset+"; boundary="+boundary);
		    applyCookies(conn);
			conn.setDoOutput(true);
			if (!LightStr.isEmpty(tokenId)) conn.addRequestProperty("TokenId", tokenId);
			connect(conn);
			OutputStream w=conn.getOutputStream();
			w.write(bin);
						
			processReponseHeaders(conn);
			if (relocation.length()==0){
				InputStream is = getResponseStream(conn);
				LightFile.loadStream(is, buf, charset);
				is.close();
			}
			httpUrl=relocation;
			conn.disconnect();
		}
    }
    
	public void doMimePost(String http,StringBuffer buf, String paras, Map<String,Object> fields) throws Exception, IOException{
		String boundary="----------"+LightUtil.encodeLongDate(LightUtil.longDate(), "yyyyMMddHHmmssSSS", TimeZone.getDefault())+"----";
		
		String httpUrl=http;
		//System.out.println("MIMEPOST url="+httpUrl+",token="+tokenId);
		while (httpUrl!=null && httpUrl.length()>0){
			relocation="";
			if (paras.length()>0){
				if (httpUrl.indexOf("?")>=0) httpUrl+="&"; else httpUrl+="?";
				httpUrl+=paras;
			}
			url=new URL(httpUrl);
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type","multipart/form-data; charset="+charset+"; boundary="+boundary);
		    applyCookies(conn);
			conn.setDoOutput(true);
			if (!LightStr.isEmpty(tokenId)) conn.addRequestProperty("TokenId", tokenId);
			connect(conn);
			OutputStream w=conn.getOutputStream();
			writeMime(w, fields, boundary);
						
			processReponseHeaders(conn);
			if (relocation.length()==0){
				InputStream is = getResponseStream(conn);
				LightFile.loadStream(is, buf, charset);
				is.close();
			}
			httpUrl=relocation;
			conn.disconnect();
		}
	}

	public Map<String, HttpCookie> getCookies() {
		return cookies;
	}

	public void setCookies(Map<String, HttpCookie> cookies) {
		this.cookies = cookies;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public Integer getContentLength() {
		return contentLength;
	}

	public void setContentLength(Integer contentLength) {
		this.contentLength = contentLength;
	}

	public String getTokenId() {
		return tokenId;
	}

	public void setTokenId(String tokenId) {
		this.tokenId = tokenId;
	}

	public static HttpClient getClient(String globalId, String trustStore, String accessId){
		HttpClient client;
		if (LightStr.isEmpty(globalId)){
			client=(HttpClient)ThreadHelper.get("httpClient");
		}
		else{
			client=(HttpClient)GlbHelper.get("httpClient-"+globalId);
		}
		if (client==null){
			if (!LightStr.isEmpty(trustStore)){
				System.setProperty("javax.net.ssl.trustStore",LightUtil.macroStr(trustStore));
			}
			client=new HttpClient();
			client.setTokenId(LightUtil.macroStr(accessId));
			if (LightStr.isEmpty(globalId)){
				ThreadHelper.set("httpClient", client);
			}
			else{
				GlbHelper.set("httpClient-"+globalId, client);
			}
		}
		return client;
	}

	public String getRelocation() {
		return relocation;
	}

	public void setRelocation(String relocation) {
		this.relocation = relocation;
	}

	public Integer getResponseCode() {
		return responseCode;
	}

	public void setResponseCode(Integer responseCode) {
		this.responseCode = responseCode;
	}
	
	public String getHeader(String header){
		return headers.get(header);
	}

	public String getCharset() {
		return charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}
	
}
