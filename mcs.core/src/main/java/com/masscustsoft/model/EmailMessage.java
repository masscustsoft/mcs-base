package com.masscustsoft.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import com.masscustsoft.service.EmailService;
import com.masscustsoft.util.StreamUtil;
import com.masscustsoft.util.LightUtil;

public class EmailMessage {
	String from;
	
	String to;
	
	String cc;
	
	String bcc;
	
	String subject;
	
	String body;
	
	String htmlBody;
	
	Date sentDate;
	
	Date deliveryDate;
	
	long size;

	List<EmailAttachment> attachments=new ArrayList<EmailAttachment>();
	
	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public String getCc() {
		return cc;
	}

	public void setCc(String cc) {
		this.cc = cc;
	}

	public String getBcc() {
		return bcc;
	}

	public void setBcc(String bcc) {
		this.bcc = bcc;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getHtmlBody() {
		return htmlBody;
	}

	public void setHtmlBody(String htmlBody) {
		this.htmlBody = htmlBody;
	}

	public Date getSentDate() {
		return sentDate;
	}

	public void setSentDate(Date sentDate) {
		this.sentDate = sentDate;
	}

	public Date getDeliveryDate() {
		return deliveryDate;
	}

	public void setDeliveryDate(Date deliveryDate) {
		this.deliveryDate = deliveryDate;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	private String getNames(Address[] list){
		StringBuffer buf=new StringBuffer();
		if (list!=null)
		for (Address r:list){
			if (buf.length()>0) buf.append(",");
			buf.append(r.toString());
		}
		return buf.toString();
	}
	
	public void parseEmailMessage(EmailService es, Message m) throws Exception{
		subject=m.getSubject();
		sentDate=m.getSentDate();
		
		size=m.getSize();
		to=getNames(m.getRecipients(RecipientType.TO));
		cc=getNames(m.getRecipients(RecipientType.CC));
		bcc=getNames(m.getRecipients(RecipientType.BCC));
		from=getNames(m.getFrom());
		
		parseMime(es, m, m.getContentType(), m.getFileName());
		
		m.setFlag(Flags.Flag.DELETED, true);	
	}
	
	private void parseMime(EmailService es, Object pt, String tp, String fn) throws MessagingException, IOException, Exception{
		String contentId=null,deposition=null;
		
		if (pt instanceof Message){
			Message m=(Message)pt;
			tp=m.getContentType();
			pt=m.getContent();
			fn=m.getFileName();
			contentId="main";
			deposition=m.getDisposition();
			//System.out.println("message tp="+tp+", pt="+pt);
		}
		if (pt instanceof MimeBodyPart){
			MimeBodyPart m=(MimeBodyPart)pt;
			tp=m.getContentType();
			pt=m.getContent();
			fn=m.getFileName();
			contentId=m.getContentID();
			deposition=m.getDisposition();
			//System.out.println("mimebody tp="+tp);
		}
		if (pt instanceof MimeMultipart){
			MimeMultipart mm=(MimeMultipart)pt;
			for (int i=0;i<mm.getCount();i++){
				parseMime(es, mm.getBodyPart(i),"",null);
			}
			return;
		}
		//System.out.println("pt.cls="+pt.getClass().getName()+",fn="+fn);
		if (tp.startsWith("text/plain") && fn==null) {
			String st=pt.toString();
			//System.out.println("plain text");
			setBody(st);
			return;
		}
		if (tp.startsWith("text/html") && fn==null) {
			String st=pt.toString();
			//System.out.println("html text");
			setHtmlBody(st);
			return;
		}
		
		if (fn==null) fn="noname";
		fn=fn.replace('\\', '/');
		int idx=fn.lastIndexOf("/");
		if(idx>=0) fn=fn.substring(idx+1);
		
		InputStream is = null;
		if (pt instanceof InputStream){
			is=(InputStream)pt;
		}
		else 
		if (pt instanceof String){
			String ct=(String)pt;
			is=new ByteArrayInputStream(ct.getBytes(LightUtil.UTF8));
		}
		if (is!=null){
			ByteArrayOutputStream os=new ByteArrayOutputStream();
			StreamUtil.copyStream(is, os, 0);
			is.close();
			os.close();
			EmailAttachment ea=new EmailAttachment();
			ea.setBytes(os.toByteArray());
			ea.setFileName(fn);
			ea.setDeposition(deposition);
			attachments.add(ea);
		}
	}

	public List<EmailAttachment> getAttachments() {
		return attachments;
	}

	public void setAttachments(List<EmailAttachment> attachments) {
		this.attachments = attachments;
	}
	
	@Override
	public String toString(){
		return "Email subject="+subject+", attachments="+attachments;
	}
}
