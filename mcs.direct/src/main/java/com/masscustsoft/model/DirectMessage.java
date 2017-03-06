package com.masscustsoft.model;

import java.sql.Timestamp;

import com.masscustsoft.api.FullBody;
import com.masscustsoft.api.IndexKey;
import com.masscustsoft.api.SQLTable;
import com.masscustsoft.api.TimestampIndex;
import com.masscustsoft.model.Entity;

@SQLTable("direct_messages")
public class DirectMessage extends Entity {
	@IndexKey
	String senderId, ownerId;
	
	@IndexKey
	String messageType; //sys, rcm, vdr
	
	@TimestampIndex
	Timestamp sentTime;
	
	String title;
	
	String message;
	
	@FullBody
	String keywords;

	@IndexKey
	String hasContent;
	
	@IndexKey
	String productId;
	
	public String getSenderId() {
		return senderId;
	}

	public void setSenderId(String senderId) {
		this.senderId = senderId;
	}

	public Timestamp getSentTime() {
		return sentTime;
	}

	public void setSentTime(Timestamp sentTime) {
		this.sentTime = sentTime;
	}

	public String getKeywords() {
		return keywords;
	}

	public void setKeywords(String keywords) {
		this.keywords = keywords;
	}

	public String getMessageType() {
		return messageType;
	}

	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}

	public String getHasContent() {
		return hasContent;
	}

	public void setHasContent(String hasContent) {
		this.hasContent = hasContent;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getProductId() {
		return productId;
	}

	public void setProductId(String productId) {
		this.productId = productId;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}
	
}
