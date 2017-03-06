package com.masscustsoft.service;

import java.util.ArrayList;
import java.util.List;

public class DirectData extends DirectComponent{
	List<DirectRun> actions=new ArrayList<DirectRun>();
	
	public void updateTestData(DirectConfig cfg) throws Exception{
		//execute actions
		String ss=cfg.getSecureSessionId();
		cfg.setSecureSessionId("no");
		for (DirectRun a:actions){
			a.run(cfg,getDs(),getFs());
		}
		cfg.setSecureSessionId(ss);
	}
	
	public List<DirectRun> getActions() {
		return actions;
	}

	public void setActions(List<DirectRun> actions) {
		this.actions = actions;
	}
}
