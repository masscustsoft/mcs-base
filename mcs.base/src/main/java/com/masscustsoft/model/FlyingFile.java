package com.masscustsoft.model;

import com.masscustsoft.api.IndexKey;
import com.masscustsoft.api.SQLSize;
import com.masscustsoft.api.SQLTable;
import com.masscustsoft.helper.Upload;
import com.masscustsoft.util.LightStr;
import com.masscustsoft.util.LightUtil;

@SQLTable("Attachments")
public class FlyingFile extends BasicFile {
	@IndexKey @SQLSize(64)
	String ownerId; //the owner's uuid

	public String getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}
	
	public AbstractResult doUpload(Upload up){
		JsonResult ret=new JsonResult();
		String ownerId=up.getStr("ownerId", "");
		try {
			if (LightStr.isEmpty(ownerId)) throw new Exception("UnknownOwner");
			FlyingFile ff=new FlyingFile();
			ff.setOwnerId(ownerId);
			ExternalFile ef = ExternalFile.newExternalFile(dataService, LightUtil.getRepository(), ff, up.getFileItem("image"));
			dataService.insertBean(ff);
			ret.setResult("id="+ef.getUuid()+"&fsId="+ LightUtil.getRepository().getFsId());
		} catch (Exception e) {
			ret.setError(e);
		}
		return ret;
	}
}
