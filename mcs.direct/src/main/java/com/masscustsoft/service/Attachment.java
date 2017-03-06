package com.masscustsoft.service;

import java.io.InputStream;

import javax.servlet.http.HttpServletResponse;

import com.masscustsoft.helper.HttpClient;
import com.masscustsoft.helper.Upload;
import com.masscustsoft.model.AbstractResult;
import com.masscustsoft.model.AbstractResult.ResultType;
import com.masscustsoft.model.ExternalFile;
import com.masscustsoft.util.ImageUtil;
import com.masscustsoft.util.LightStr;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.StreamUtil;

public class Attachment extends DirectAction {

	@Override
	protected void run(AbstractResult ret) throws Exception {
		String f = requiredStr("f");
		String id = f;
		int i = id.lastIndexOf('.');
		if (i > 0)
			id = id.substring(0, i);

		HttpServletResponse resp = Upload.getUpload().getResponse();
		LightUtil.doCache(resp);
		//resp.setContentType(Upload.getFileContentType(f));
		//resp.setHeader("Content-Disposition", "inline; filename=" + f + "");
		resp.setHeader("Content-Disposition", "attachment;filename=\"" + f + "\"");
		resp.setContentType("application/octet-stream");
		InputStream is = null;
		Long size=null;
		Long last=LightUtil.bootupTime;
		if (!id.equals("url")) {
			ExternalFile ef = getDs().getBean(ExternalFile.class, "uuid", id);
			if (ef != null) {
				is = ef.getResource(getFs().getFsId());
				size=ef.getSize();
			} else {
				is = getFs().getResource(f);
				last=getFs().getLastModified(f);
			}
		} else {
			HttpClient hc = new HttpClient();
			try {
				String url = requiredStr("url");
				is = hc.doGetDownload(url);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		String ctype = Upload.getFileContentType(f);
		resp.setContentType(ctype);

		String resize = getStr("resize", "");
		String name = getStr("name", f);
		String ext=StreamUtil.fileExt(name);
		if (!LightStr.isEmpty(resize)) {
			is = ImageUtil.doResizeImage(resize,ext, is);
			size=(long)is.available();
		}
		if (is != null) {
			StreamUtil.streamOut(Upload.getUpload(), is, name, null, last);
			is.close();
		}
		ret.setType(ResultType.Stream);
	}

}
