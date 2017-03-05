package com.masscustsoft.service;

import org.jgroups.JChannel;

import com.masscustsoft.util.LightUtil;



public class UdpClusterService extends JGroupClusterService{
	String multicastPort;	//default 45588
	String ipTtl;			//default 2
	String multicastIp; 	//default 228.8.8.8
	
	@Override
	public void start() throws Exception{
		setSysVar(LightUtil.macroStr(multicastIp),"jgroups.udp.mcast_addr");
		setSysVar(LightUtil.macroStr(multicastPort),"jgroups.udp.mcast_port");
		setSysVar(LightUtil.macroStr(ipTtl),"jgroups.udp.ip_ttl");
		setSysVar(LightUtil.macroStr(bindIp),"jgroups.bind_addr");
		super.start();
	}
	
	public String getMulticastPort() {
		return multicastPort;
	}

	public void setMulticastPort(String multicastPort) {
		this.multicastPort = multicastPort;
	}

	public String getIpTtl() {
		return ipTtl;
	}

	public void setIpTtl(String ipTtl) {
		this.ipTtl = ipTtl;
	}
	
	@Override
	public String getProtocol(){
		return JChannel.DEFAULT_PROTOCOL_STACK;
	}

	public String getMulticastIp() {
		return multicastIp;
	}

	public void setMulticastIp(String multicastIp) {
		this.multicastIp = multicastIp;
	}


}
