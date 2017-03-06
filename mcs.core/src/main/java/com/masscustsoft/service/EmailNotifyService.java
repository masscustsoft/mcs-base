package com.masscustsoft.service;

import java.util.ArrayList;
import java.util.List;

import com.masscustsoft.api.IFile;
import com.masscustsoft.model.EmailAttachment;
import com.masscustsoft.util.EmailUtil;
import com.masscustsoft.util.LightUtil;

public class EmailNotifyService extends NotifyService {
	String pop3, smtp, email, account, password, auth, pop3Port, smtpPort, ssl;
	
	public List<EmailAttachment> toEmailAttachmentList(EmailService es, String deposition,
			List<? extends IFile> atts, String fsId0) throws Exception{
		if (atts==null) return null;
		List<EmailAttachment> list=new ArrayList<EmailAttachment>();
		for (IFile att:atts){
			list.add(EmailUtil.asEmailAttachment(es, att, deposition,fsId0));
		}
		return list;
	}
	
	@Override
	public void sendMessage(String from, String to, String cc, String bcc, String replyTo, String subject, String body, List<? extends IFile> atts) throws Exception {
		System.out.println("Email notify from="+from+", to="+to);
		EmailService es=new EmailService(LightUtil.macroStr(pop3), LightUtil.macroStr(smtp), LightUtil.macroStr(email), LightUtil.macroStr(account), LightUtil.macroStr(password), LightUtil.macroStr(auth), LightUtil.macroStr(pop3Port), LightUtil.macroStr(smtpPort), "true".equals(ssl)||"yes".equals(ssl));
		es.sendMessage(from, to, cc, bcc, replyTo, subject, body, toEmailAttachmentList(es,null,atts,null));
		es.destroy();
	}

	public String getPop3() {
		return pop3;
	}

	public void setPop3(String pop3) {
		this.pop3 = pop3;
	}

	public String getSmtp() {
		return smtp;
	}

	public void setSmtp(String smtp) {
		this.smtp = smtp;
	}

	@Override
	public String getAccount() {
		return account;
	}

	public void setAccount(String account) {
		this.account = account;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getAuth() {
		return auth;
	}

	public void setAuth(String auth) {
		this.auth = auth;
	}

	public String getPop3Port() {
		return pop3Port;
	}

	public void setPop3Port(String pop3Port) {
		this.pop3Port = pop3Port;
	}

	public String getSmtpPort() {
		return smtpPort;
	}

	public void setSmtpPort(String smtpPort) {
		this.smtpPort = smtpPort;
	}

	public String getSsl() {
		return ssl;
	}

	public void setSsl(String ssl) {
		this.ssl = ssl;
	}

}
