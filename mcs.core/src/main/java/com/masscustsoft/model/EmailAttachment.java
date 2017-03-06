package com.masscustsoft.model;


public class EmailAttachment {
	String fileName;
	byte[] bytes;
	String deposition;
	
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getDeposition() {
		return deposition;
	}
	public void setDeposition(String deposition) {
		this.deposition = deposition;
	}
	
	@Override
	public String toString(){
		return "Attachment fileName="+fileName+", deposition="+deposition;
	}
	public byte[] getBytes() {
		return bytes;
	}
	public void setBytes(byte[] bytes) {
		this.bytes = bytes;
	}
}
