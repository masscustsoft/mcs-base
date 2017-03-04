package com.masscustsoft.service;

import java.io.Serializable;

import org.apache.commons.fileupload.ProgressListener;

import com.masscustsoft.util.GlbHelper;
import com.masscustsoft.util.LightStr;

public class ProgressStatus implements ProgressListener,Serializable{
	String status;
	long size;
	long position;
	int percent; //uploading 50% downloading 50%, so the caller can change maxmum 50% for downloading
	String psid;
	transient long tick0=System.currentTimeMillis();
	
	public ProgressStatus(){
		status="";
		percent=0;
		size=0;
		position=0;
	}
	
	@Override
	public void update(long bytesRead, long contentLength, int items) {
		this.size=contentLength;
		this.position=bytesRead;
		if (size>0) percent=Math.round(position*100/size);
		status="#[Uploading]... "+LightStr.getSizeStr(position)+" / "+LightStr.getSizeStr(size);
		flush();
	}

	private void flush(){
		long tick=System.currentTimeMillis();
		if (tick-tick0>=900){
			if (psid!=null) {
				GlbHelper.set(psid, this);
			}
			tick0=tick;
		}
	}
	//from outside
	public void updateStatus(long pos, long total, String status) {
		if (total>0) percent=Math.round(pos*100/total);
		if (status!=null) setStatus(status);
		flush();
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public long getPosition() {
		return position;
	}

	public void setPosition(long position) {
		this.position = position;
	}

	public int getPercent() {
		return percent;
	}

	public void setPercent(int percent) {
		this.percent = percent;
	}

	public String getPsid() {
		return psid;
	}

	public void setPsid(String psid) {
		this.psid = psid;
	}
	
	public String toString(){
		return "status:"+status+",percent:"+percent+",pos="+position+",size="+size+",psid="+psid;
	}
}
