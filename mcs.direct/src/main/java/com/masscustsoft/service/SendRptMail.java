package com.masscustsoft.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.masscustsoft.api.IFile;
import com.masscustsoft.model.AbstractResult;
import com.masscustsoft.model.ExternalFile;
import com.masscustsoft.service.NotifyService;
import com.masscustsoft.util.StreamUtil;
import com.masscustsoft.util.LightUtil;

public class SendRptMail extends DirectAction {
	@Override
	protected void run(AbstractResult ret) throws Exception {
		DirectSession ses = getSession();
		NotifyService nfs = LightUtil.getCfg().getNotifyService();
		if (nfs==null) return;

		String id=requiredStr("externalId");
		if (id.contains(".")) id=id.substring(0,id.indexOf("."));
		
		final String externalId=id;
		List<IFile> attrs=new ArrayList();
		attrs.add(new IFile(){

			@Override
			public String getName() {
				return "report.pdf";
			}

			@Override
			public void setName(String name) {
			}

			@Override
			public long getSize() {
				return 0;
			}

			@Override
			public void setSize(long size) {
				
			}

			@Override
			public String getExternalId() {
				return externalId;
			}

			@Override
			public void setExternalId(String externalPath) {
				
			}
		
			@Override
			public InputStream getResource(String fsId0) throws Exception {
				return ExternalFile.getResource(null, fsId0, this);
			}
		});
		
		
		nfs.sendMessage(requiredStr("mailTo"), requiredStr("mailSubject"), getStr("mailBody",""), attrs);
	}
}
