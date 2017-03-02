package com.masscustsoft.service;

import java.util.List;

import com.masscustsoft.api.IFile;

public class NotifyService {
	public String getAccount(){
		return "";
	}
	
	public void sendMessage(String receiver, String subject, String message, List<? extends IFile> atts) throws Exception{
		sendMessage(null, receiver, null, null, null, subject, message, atts);
	}
	
	public void sendMessage(String from, String to, String cc, String bcc, String replyTo, String subject, String message, List<? extends IFile> atts) throws Exception{
		System.out.println("Notice: "+subject+"\n{{"+message+"}}\n");
	}

	public void sendMessage(String from, String to, String subject, String message, List<? extends IFile> atts) throws Exception {
		sendMessage(from, to, null, null, null, subject, message, atts);
	}
}
