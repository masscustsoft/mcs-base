package com.masscustsoft.util;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import com.masscustsoft.api.IFile;
import com.masscustsoft.model.EmailAttachment;
import com.masscustsoft.service.EmailService;

public class EmailUtil {
	public static EmailAttachment asEmailAttachment(EmailService es,IFile file, String deposition, String fsId0) throws Exception{
		EmailAttachment ea=new EmailAttachment();
		ea.setFileName(file.getName());
		if (deposition==null) deposition="attachment";
		ea.setDeposition(deposition);
		ByteArrayOutputStream os=new ByteArrayOutputStream();
		InputStream is=file.getResource(fsId0);
		StreamUtil.copyStream(is, os, 0);
		ea.setBytes(os.toByteArray());
		is.close();
		os.close();
		return ea;
	}
	
	
}
