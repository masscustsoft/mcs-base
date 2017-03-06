package com.masscustsoft.service;

import java.awt.Color;
import java.awt.image.BufferedImage;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import com.masscustsoft.helper.Upload;
import com.masscustsoft.model.AbstractResult;
import com.masscustsoft.model.AbstractResult.ResultType;
import com.masscustsoft.util.ImageUtil;
import com.masscustsoft.util.LightUtil;

public class Captcha extends DirectAction {

	@Override
	protected void run(AbstractResult ret) throws Exception {
		DirectSession ses = getSession();
		String text = "????";
		if (ses != null) {
			text = ses.getCaptcha();
		}
		BufferedImage img = ImageUtil.createCaptchaImage(120, 32, 24, 0.25, Color.blue, text);
		HttpServletResponse resp = Upload.getUpload().getResponse();
		resp.setContentType("image/png");
		LightUtil.noCache(resp);
		ret.setType(ResultType.Stream);
		ServletOutputStream os = resp.getOutputStream();
		ImageUtil.saveImage(img, os);
	}

}
