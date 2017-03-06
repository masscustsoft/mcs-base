package com.masscustsoft.service;

public class GmailNotifyService extends EmailNotifyService {
	
	public void initialize(){
		pop3="pop.gmail.com";
		smtp="smtp.gmail.com";
		if (email==null) email=account;
		if (account==null) account=email;
		if (password==null) password="";
		auth="true";
		pop3Port="995";
		smtpPort="465";
		ssl="true";
	}
}
