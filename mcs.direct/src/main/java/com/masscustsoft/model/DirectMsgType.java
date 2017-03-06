package com.masscustsoft.model;

import com.masscustsoft.api.FullText;
import com.masscustsoft.api.IndexKey;
import com.masscustsoft.api.SQLTable;
import com.masscustsoft.model.BasicFile;
import com.masscustsoft.model.Entity;

@SQLTable("direct_messages")
public class DirectMsgType extends Entity {
	@IndexKey
	String messageType;

	@FullText
	String title;

	String message;
	
	BasicFile logo = new BasicFile();

	public String getMessageType() {
		return messageType;
	}

	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}

	public BasicFile getLogo() {
		return logo;
	}

	public void setLogo(BasicFile logo) {
		this.logo = logo;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
