package com.masscustsoft.service;

import com.masscustsoft.util.LightUtil;

public class SysCmdExpireCache extends ClusterCmd{
	String pattern;
	
	@Override
	public void run(ClusterService clu,Comparable me,Comparable src) throws Exception{
		LightUtil.getCfg().getCacheService().expireCache(pattern);
	}

	public String getPattern() {
		return pattern;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}
}
