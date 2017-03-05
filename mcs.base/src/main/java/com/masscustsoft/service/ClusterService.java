package com.masscustsoft.service;

import com.masscustsoft.api.IBeanFactory;
import com.masscustsoft.api.ICleanup;
import com.masscustsoft.api.IClusterService;
import com.masscustsoft.util.LightStr;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.LogUtil;
import com.masscustsoft.xml.BeanFactory;

public class ClusterService implements ICleanup,IClusterService {
	String enable;
	protected String clusterId;
	protected String nodeId;
	protected String cloudId;
	protected String bindIp,bindPort;
	protected String service;
	transient IBeanFactory beanFactory;
	
	public boolean init(IBeanFactory bf) throws Exception{
		beanFactory=bf;
		return isEnable();
	}
	
	public void start() throws Exception{
		LogUtil.info("Cluster Service started. ");
	}
	
	public void stop() throws Exception{
		LogUtil.debug("Cluster Service is stopping... ");
	}

	public void cleanup() {
		try {
			stop();
		} catch (Exception e) {
		}	
	}

	public String getClusterId() {
		return clusterId;
	}

	public void setClusterId(String clusterId) {
		this.clusterId = clusterId;
	}

	public Comparable getSender(String group){
		return null;
	}
	
	public void broadcast(String group, Comparable dst, ClusterCmd cmd) throws Exception{
		sendMessage(group, dst, beanFactory.toXml(cmd, 0));
	}
	
	public void sendMessage(String group, Comparable dst, String xml) throws Exception{
		
	}

	public String getNodeId() {
		return nodeId;
	}

	@Override
	public String myClusterId() {
		return LightUtil.macroStr(clusterId);
	}
	
	@Override
	public String myNodeId() {
		return LightUtil.macroStr(nodeId);
	}
	
	@Override
	public String myBindIp() {
		return LightUtil.macroStr(bindIp);
	}
	
	@Override
	public String myBindPort() {
		return LightUtil.macroStr(bindPort);
	}
	
	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	public boolean isEnable(){
		boolean en=LightStr.isEmpty(enable) || "true".equals(LightUtil.macroStr(enable));
		return en;
	}
	
	public String getEnable() {
		return enable;
	}

	public void setEnable(String enable) {
		this.enable = enable;
	}

	public String getCloudId() {
		return cloudId;
	}

	public void setCloudId(String cloundGroup) {
		this.cloudId = cloundGroup;
	}
	
	public String getBindPort() {
		return bindPort;
	}

	public void setBindPort(String bindPort) {
		this.bindPort = bindPort;
	}

	public String getBindIp() {
		return bindIp;
	}

	public void setBindIp(String bindIp) {
		this.bindIp = bindIp;
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	public IBeanFactory getBeanFactory() {
		return beanFactory;
	}

	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

}
