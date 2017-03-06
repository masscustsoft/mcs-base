package com.masscustsoft.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Authenticator;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.masscustsoft.helper.Upload;
import com.masscustsoft.model.EmailAttachment;
import com.masscustsoft.model.EmailMessage;
import com.masscustsoft.service.TempItem;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.LightStr;

public class EmailService {
	String pop3, smtp, email, account, password, auth, smtpPort, pop3Port;
	Boolean ssl;
	
	List<TempItem> tempFiles=new ArrayList<TempItem>();
	
	public EmailService(String pop3, String smtp, String email, String account, String password, String auth, String pop3Port, String smtpPort, Boolean ssl){
		this.pop3=pop3;
		this.smtp=smtp;
		this.email=email;
		this.account=account;
		this.password=password;
		this.auth=auth;
		this.smtpPort=smtpPort;
		this.pop3Port=pop3Port;
		this.ssl=ssl;
		
		System.out.println("ES: "+pop3+","+smtp+","+email+","+account+","+auth+","+pop3Port+","+smtpPort+","+ssl);
	}
	
	private static class MyAuthenticator extends Authenticator {

		String username;
		String password;
		
		public MyAuthenticator(String u,String p){
			this.username=u;
			this.password=p;
		}
		
		public PasswordAuthentication getPasswordAuthentication() {
			return new PasswordAuthentication(username, password);
		}
	}
	
	private Session getSession(){
		Properties props = new Properties();
		if (pop3!=null) props.put("mail.pop3.host", pop3);
		if (smtp!=null) props.put("mail.smtp.host", smtp);
		props.put("mail.smtp.auth", LightStr.isEmpty(auth)||"yes".equals(auth)||"true".equals(auth)?"true":"false"); 
		if (smtpPort!=null) props.put("mail.smtp.port", smtpPort);
		if (pop3Port!=null) props.put("mail.pop3.port", pop3Port);
		if (ssl!=null && ssl){
			Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
			props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
			props.put("mail.smtp.socketFactory.fallback", "false");
			props.put("mail.pop3.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
			props.put("mail.pop3.socketFactory.fallback", "false");
		}
		Authenticator a= new MyAuthenticator(account, password);
		Session session= Session.getInstance(props, a);
		session.setDebug(false);
		return session;
	}
	
	public void sendMessage(String from, String to, String cc, String bcc, String replyTo, String subject, String body, List<EmailAttachment> atts) throws Exception {
		Thread.currentThread().setContextClassLoader( getClass().getClassLoader() );
		
		Session session=getSession();	
		
		MimeMessage message = new MimeMessage(session);
		
		String fr=from; if (LightStr.isEmpty(fr)) fr=LightUtil.macroStr(email);
		if (!LightStr.isEmpty(fr)){
			message.setFrom(new InternetAddress(fr));
			//message.setReplyTo(new InternetAddress[]{new InternetAddress(fr)});
		}
		//System.out.println("fr="+fr+",reciever="+receiver);
		if (!LightStr.isEmpty(to)){
			String comma=";";
			if (to.indexOf(";")==-1) comma=",";
			String[] list=to.split(comma);
			for (String s:list){
				message.addRecipient(Message.RecipientType.TO, new InternetAddress(s.trim()));
			}	
		}
		
		if (!LightStr.isEmpty(cc)){
			String comma=";";
			if (cc.indexOf(";")==-1) comma=",";
			String[] list=cc.split(comma);
			for (String s:list){
				message.addRecipient(Message.RecipientType.CC, new InternetAddress(s.trim()));
			}	
		}
		
		if (!LightStr.isEmpty(bcc)){
			String comma=";";
			if (bcc.indexOf(";")==-1) comma=",";
			String[] list=bcc.split(comma);
			for (String s:list){
				message.addRecipient(Message.RecipientType.BCC, new InternetAddress(s.trim()));
			}	
		}
		
		if (!LightStr.isEmpty(replyTo)){
			String comma=";";
			if (replyTo.indexOf(";")==-1) comma=",";
			String[] list=replyTo.split(comma);
			InternetAddress rep[]=new InternetAddress[list.length];
			for (int i=0;i<list.length;i++){
				String s=list[i];
				rep[i]=new InternetAddress(s.trim());
			}	
			message.setReplyTo(rep);
		}
		
		message.setSubject(subject,"UTF-8");
		Multipart multipart = new MimeMultipart();
		 
		MimeBodyPart messageBodyPart = new MimeBodyPart();
		message.setText(body, "UTF-8", "html");
		
		if (body!=null && isHtml(body)) messageBodyPart.setText(body,"UTF-8","html"); else messageBodyPart.setText(body,"UTF-8");
		
		multipart.addBodyPart(messageBodyPart);

		if (atts!=null){
			for (EmailAttachment a:atts){
				final String fn=a.getFileName();
				messageBodyPart = new MimeBodyPart();
				MyDataSource ds=new MyDataSource(a.getFileName(),Upload.getFileContentType(fn),a.getBytes());
				messageBodyPart.setDataHandler(new DataHandler(ds));
				messageBodyPart.setFileName(fn);
				messageBodyPart.setDisposition(Part.ATTACHMENT);
				multipart.addBodyPart(messageBodyPart);
			}
		}
	    message.setContent(multipart);
	    
	    System.out.println("Send Email: "+message.toString());
		Transport.send(message);
	}
	
	public List<EmailMessage> loadInbox() throws Exception{
		Session session=getSession();
		Store store = session.getStore("pop3");
		List<EmailMessage> list =new ArrayList<EmailMessage>(); 
	    try{
	    	store.connect();
	    	Folder inbox = store.getFolder("INBOX");
		    if (inbox != null) {
			    inbox.open(Folder.READ_ONLY);

			    Message[] messages = inbox.getMessages();
			    for (Message m:messages){
			    	EmailMessage msg=new EmailMessage();
			    	msg.parseEmailMessage(this,m);
			    	list.add(msg);
			    }
			    inbox.close(true);
		    }
	    }
	    finally{
		    store.close();
	    }
	    return list;
	}
	
	private boolean isHtml(String body){
		int i=body.indexOf("<");
		if (i<0) return false;
		if (i==0) return true;
		if (body.endsWith(">")) return true;
		int j=body.indexOf(">");
		int k=body.indexOf("<",i+1);
		if (j-i>1 && (k==-1 || k>j)) return true;
		return false;
	}
	
	class MyDataSource implements DataSource {
	    private String name;
	    private String contentType;
	    private byte[] baos=null;
	    private InputStream is=null;
	    
	    MyDataSource(String name, String contentType, InputStream is) throws IOException {
	        this.name = name;
	        this.contentType = contentType;
	        this.is = is;
	    }
	    
	    MyDataSource(String name, String contentType, byte[] buf) throws IOException {
	        this.name = name;
	        this.contentType = contentType;
	        this.baos = buf;
	    }
	    
	    public String getContentType() {
	        return contentType;
	    }

	    public InputStream getInputStream() throws IOException {
	    	if (is!=null) return is;
	        if (baos!=null) return new ByteArrayInputStream(baos);
	        return null;
	    }

	    public String getName() {
	        return name;
	    }

	    public OutputStream getOutputStream() throws IOException {
	        throw new IOException("Cannot write to this read-only resource");
	    }

		@Override
		protected void finalize() throws Throwable {
			if (is!=null) is.close();
			super.finalize();
		}
	}
	
	public void hookFile(TempItem f) {
		tempFiles.add(f);	
	}

	public void destroy(){
		for (TempItem f:tempFiles){
			f.delete();
		}
		tempFiles.clear();
	}


	@Override
	protected void finalize() throws Throwable {
		destroy();
		super.finalize();
	}
	
	public static void main(String args[]) throws Exception{
		String pop3="pop.gmail.com", smtp="smtp.gmail.com", email="masscustsoft.admin@gmail.com", account="masscustsoft.admin@gmail.com", password="", auth="true", pop3Port="995", smtpPort="465";
		Boolean ssl=true;
		
		EmailService ntf=new EmailService(pop3, smtp, email, account, password, auth, pop3Port, smtpPort, ssl);
		//ntf.sendMessage(email, "jasongcs@gmail.com", "Test61", "Body", null);
		List<EmailMessage> msgs = ntf.loadInbox();
		for (EmailMessage msg:msgs){
			System.out.println("SUB="+msg.getSubject());
			System.out.println(" body="+msg.getHtmlBody());
		}
		
		ntf.destroy();
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

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

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

	public String getAuth() {
		return auth;
	}

	public void setAuth(String auth) {
		this.auth = auth;
	}

	public String getSmtpPort() {
		return smtpPort;
	}

	public void setSmtpPort(String smtpPort) {
		this.smtpPort = smtpPort;
	}

	public String getPop3Port() {
		return pop3Port;
	}

	public void setPop3Port(String pop3Port) {
		this.pop3Port = pop3Port;
	}

	public Boolean getSsl() {
		return ssl;
	}

	public void setSsl(Boolean ssl) {
		this.ssl = ssl;
	}

	public List<TempItem> getTempFiles() {
		return tempFiles;
	}

	public void setTempFiles(List<TempItem> tempFiles) {
		this.tempFiles = tempFiles;
	}

	
}

