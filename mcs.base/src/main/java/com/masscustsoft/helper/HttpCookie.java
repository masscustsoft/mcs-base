package com.masscustsoft.helper;

import java.sql.Timestamp;
import java.util.TimeZone;

import com.masscustsoft.util.LightUtil;

public class HttpCookie {
	static final String dtFmt1="EEE, dd MMM yyyy HH:mm:ss z";
	static final String dtFmt2="EEE, dd-MMM-yyyy HH:mm:ss z";
	String name,value;
	String domain,path;
	Timestamp expires;
	int version;
	
	public HttpCookie(){
		
	}
	
	public HttpCookie(String cookie) throws Exception{
		//System.out.println("setcookie="+cookie);
		String[] ss=cookie.split(";");
		for (String s:ss){
			int i=s.indexOf("=");
			if (i<0) continue;
			String nm=s.substring(0,i).trim();
			String vu=s.substring(i+1).trim();
			if (nm.equalsIgnoreCase("Version")) version=LightUtil.decodeInt(vu);
			else
			if (nm.equalsIgnoreCase("Path")) path=vu;
			else
			if (nm.equalsIgnoreCase("Domain")) domain=vu;
			else
			if (nm.equalsIgnoreCase("Expires")) {
				int idx=vu.indexOf("fmt=");
				String fmt=dtFmt1;
				if (vu.indexOf("-")>=0) fmt=dtFmt2;
				//System.out.println("expires="+vu+",fmt="+fmt+",test="+Util.encodeDate(new Date(), fmt, Locale.US));
				expires=LightUtil.decodeLongDate(vu,fmt, TimeZone.getDefault());
			}
			else{
				//System.out.println("set name "+nm+","+vu);
				name=nm;
				value=vu;
			}
			//System.out.println("setcookie s="+s+", name="+nm+",path="+path+",domain="+domain+",ver="+version+",value="+value);
		}
	}

	@Override
	public String toString(){
		return "{"+name+"="+value+",exp="+LightUtil.encodeLongDate(expires,dtFmt1,TimeZone.getDefault())+",ver="+version+",path="+path+"}";
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Timestamp getExpires() {
		return expires;
	}

	public void setExpires(Timestamp expires) {
		this.expires = expires;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}
}
