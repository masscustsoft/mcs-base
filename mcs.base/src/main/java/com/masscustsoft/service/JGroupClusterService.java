package com.masscustsoft.service;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.ChannelListener;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.Receiver;
import org.jgroups.View;

import com.masscustsoft.util.ClusterUtil;
import com.masscustsoft.util.GlbHelper;
import com.masscustsoft.util.LightStr;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.LogUtil;
import com.masscustsoft.util.ThreadHelper;

class LoadBalanceListener implements Receiver, ChannelListener{
	Channel channel;
	ClusterService service;
	
	public LoadBalanceListener(ClusterService svc, Channel ch){
		channel=ch;
		service=svc;
	}
	
	@Override
	public void viewAccepted(View view) {
		// View members changed. joined or disconnected
		LogUtil.info("CloudView accepted "+view.getMembers());
		try{
			ClusterUtil.updateMembers(view.getMembers());
		}
		catch(Exception e){
			//LogUtil.dumpStackTrace(e);
			e.printStackTrace();
		}
	}

	@Override
	public void receive(Message msg) {
		try{
			ThreadHelper.set("beanFactory", service.getBeanFactory());
			String xml=new String(msg.getRawBuffer(),LightUtil.UTF8);
			//System.out.println("Cloud::"+xml);
			ClusterCmd cmd=(ClusterCmd)service.getBeanFactory().loadBean(xml);
			cmd.run(service,channel.getAddress(),msg.getSrc());
		}
		catch(Exception e){
			//LogUtil.dumpStackTrace(e);
			e.printStackTrace();
		}
	}

	@Override
	public void suspect(Address suspected_mbr) {
		// member not fine, should be remove from list
		
	}

	@Override
	public void block() {
		//
		
	}

	@Override
	public void channelConnected(Channel channel) {
		LogUtil.info("Channel connected "+channel.getClusterName()+"."+channel.getName());
		//notify all balance member
	}

	@Override
	public void channelDisconnected(Channel channel) {
		LogUtil.info("Channel disconnected "+channel.getClusterName()+"."+channel.getName());
	}

	@Override
	public void channelClosed(Channel channel) {
		
	}

	@Override
	public void getState(OutputStream output) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setState(InputStream input) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void unblock() {
		// TODO Auto-generated method stub
		
	}

	
}

class ClusterListener implements Receiver, ChannelListener{
	Channel channel;
	ClusterService service;
	
	public ClusterListener(ClusterService svc, Channel ch){
		channel=ch;
		service=svc;
	}
	
	@Override
	public void viewAccepted(View view) {
		// View members changed. joined or disconnected
		LogUtil.info("ClusterView accepted "+view.getMembers());
		GlbHelper.set("_current_node_", channel.getAddress().toString());
		List<String> list=new ArrayList<String>();
		for (Address a:view.getMembers()){
			list.add(a.toString());
		}
		GlbHelper.set("_node_list_", list);
		//LogUtil.info("CurrentNode="+channel.getAddress()+", Coordinator="+view.getMembers().get(0));
	}

	@Override
	public void receive(Message msg) {
		try{
			ThreadHelper.set("beanFactory", service.getBeanFactory());
			String xml=new String(msg.getRawBuffer(),LightUtil.UTF8);
			//System.out.println("Cluster::"+xml);
			ClusterCmd cmd=(ClusterCmd)service.getBeanFactory().loadBean(xml);
			cmd.run(service,channel.getAddress(),msg.getSrc());
		}
		catch(Exception e){
			//LogUtil.dumpStackTrace(e);
			e.printStackTrace();
		}
	}

	@Override
	public void suspect(Address suspected_mbr) {
		// member not fine, should be remove from list
		
	}

	@Override
	public void block() {
		//
		
	}

	@Override
	public void channelClosed(Channel channel) {
		
	}

	@Override
	public void getState(OutputStream output) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setState(InputStream input) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void unblock() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void channelConnected(Channel channel) {
		LogUtil.info("Channel connected "+channel.getClusterName()+"."+channel.getName());
		
	}

	@Override
	public void channelDisconnected(Channel channel) {
		LogUtil.info("Channel disconnected "+channel.getClusterName()+"."+channel.getName());
		
	}
}

public class JGroupClusterService extends ClusterService{
	
	transient Channel channel=null,cloud=null;
	transient Map<Address,Map<String,Object>> nodeList=new HashMap<Address,Map<String,Object>>();
	transient ClusterListener clusterListener;
	transient LoadBalanceListener loadBalanceListener;
	
	public String getProtocol(){
		return JChannel.DEFAULT_PROTOCOL_STACK;
	}
	
	protected void setSysVar(String var,String name){
		if (var!=null) {
			String s=LightUtil.macroStr(var); 
			if (!LightStr.isEmpty(s)) {
				System.setProperty(name, s);
				//System.out.println("setVAR: "+name+" ="+s);
			}
		}
	}
	
	@Override
	public void start() throws Exception{
		String cId=LightUtil.macroStr(clusterId);
		String nId=LightUtil.macroStr(nodeId);
		String bIp=LightUtil.macroStr(bindIp);
		String bPt=LightUtil.macroStr(bindPort);
		if (LightStr.isEmpty(cId)) throw new Exception("clusterId cannot be empty");
		if (service==null) service="";
		String svc=LightUtil.macroStr(service);
		
		channel=new JChannel(getProtocol());
		clusterListener=new ClusterListener(this,channel);
		channel.setName("{clusterId:'"+cId+"',nodeId:'"+nId+"',bindIp:'"+bIp+"',bindPort:'"+bPt+"',channel:"+channel.hashCode()+",service:'"+svc+"'}");
		channel.setReceiver(clusterListener);
		channel.addChannelListener(clusterListener);
		channel.connect(cId);
		
		channel.getState(null, 10000);
		
		cId=LightUtil.macroStr(cloudId);
		if (!LightStr.isEmpty(cId)){
			cloud=new JChannel(getProtocol());
			loadBalanceListener=new LoadBalanceListener(this,cloud);
			cloud.setName("{clusterId:'"+cId+"',nodeId:'"+nId+"',bindIp:'"+bIp+"',bindPort:'"+bPt+"',path:'"+GlbHelper.get("contextPath")+"',channel:"+cloud.hashCode()+",service:'"+service+"'}");
			cloud.setReceiver(loadBalanceListener);
			cloud.addChannelListener(loadBalanceListener);
			cloud.connect(cId);
		}
		super.start();		
	}
	
	@Override
	public void stop() throws Exception{
		if (channel!=null){
			try{
				channel.disconnect();
			}
			finally{
				channel.close();
			}
			channel=null;
		}
		if (cloud!=null){
			try{
				cloud.disconnect();
			}
			finally{
				cloud.close();
			}
			cloud=null;
		}
		super.stop();
	}
	
	@Override
	public Comparable getSender(String group){
		Channel ch=channel;
		if ("cloud".equals(group)) ch=cloud;
		return ch.getAddress();
	}
	
	@Override
	public void sendMessage(String group, Comparable dst,String xml) throws Exception{
		Channel ch=channel;
		if ("cloud".equals(group)) ch=cloud;
		if (ch!=null) ch.send(new Message((Address)dst,null,xml.getBytes(LightUtil.UTF8)));
	}

}
