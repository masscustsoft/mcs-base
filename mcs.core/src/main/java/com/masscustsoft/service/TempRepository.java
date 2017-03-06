package com.masscustsoft.service;

import java.io.InputStream;
import java.io.OutputStream;

import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.StreamUtil;

public class TempRepository extends Repository {

	@Override
	public InputStream _getResource(String name) throws Exception {
		TempItem it = LightUtil.getCfg().getTempService().getTempItem(name);
		return it.getInputStream();
	}

	@Override
	public String saveResource(String name, InputStream is) throws Exception {
		TempItem it = LightUtil.getCfg().getTempService().newTempItem(name);
		OutputStream os = it.getOutputStream();
		StreamUtil.copyStream(is, os, 0);
		os.close();
		return name;
	}

	@Override
	public void removeResource(String name) throws Exception {
		TempItem it = LightUtil.getCfg().getTempService().newTempItem(name);
		it.delete();
	}

	@Override
	public long getLastModified(String name) throws Exception {
		return LightUtil.bootupTime;
	}
	
}
