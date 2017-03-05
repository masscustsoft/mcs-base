package com.masscustsoft.service;

import com.masscustsoft.util.LightUtil;

public class TcpClusterService extends JGroupClusterService {
	String recvBufferSize;
	String sendBufferSize;
	String initialHosts;
	String multicastPort;
	
	@Override
	public void start() throws Exception{
		setSysVar(LightUtil.macroStr(recvBufferSize),"tcp.recv_buf_size");
		setSysVar(LightUtil.macroStr(sendBufferSize),"tcp.send_buf_size");
		setSysVar(LightUtil.macroStr(initialHosts),"jgroups.tcpping.initial_hosts");
		setSysVar(LightUtil.macroStr(bindIp),"jgroups.bind_addr");
		setSysVar(LightUtil.macroStr(multicastPort),"jgroups.muticast.bind_port");
		super.start();
	}

	public String getRecvBufferSize() {
		return recvBufferSize;
	}

	public void setRecvBufferSize(String recvBufferSize) {
		this.recvBufferSize = recvBufferSize;
	}

	public String getSendBufferSize() {
		return sendBufferSize;
	}

	public void setSendBufferSize(String sendBufferSize) {
		this.sendBufferSize = sendBufferSize;
	}

	public String getInitialHosts() {
		return initialHosts;
	}

	public void setInitialHosts(String initialHosts) {
		this.initialHosts = initialHosts;
	}

	@Override
	public String getProtocol(){
		return "tcp.xml";
	}

	public String getMulticastPort() {
		return multicastPort;
	}

	public void setMulticastPort(String multicastPort) {
		this.multicastPort = multicastPort;
	}
}
