package com.masscustsoft.service;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.masscustsoft.helper.HttpClient;
import com.masscustsoft.model.XmlResult;
import com.masscustsoft.util.LightStr;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.xml.BeanFactory;

public class RemoteRepository extends Repository {
	String url;
	String remoteId; //remote side beanRepository id
	String trustStore;
	String accessId;
	String globalId;
	
	private HttpClient getHttpClient(){
		return HttpClient.getClient(globalId, trustStore, accessId);
	}

	private String get_url(){
		String	_url = LightUtil.macroStr(url);
		int i=_url.indexOf("?");
		if (i==-1 && !_url.endsWith("/service")){
			if (!_url.endsWith("/")) _url+="/";
			_url+="service";
			if (!LightStr.isEmpty(folderName)){
				_url+="?folderName="+folderName;
			}
		}
		return _url;
	}
	
	public String getUrl() {
		return url;
	}

	public void setUrl(String storageUrl) {
		this.url=storageUrl;
	}

	public String getRemoteId() {
		return remoteId;
	}

	public void setRemoteId(String remoteId) {
		this.remoteId = remoteId;
	}
	
	@Override
	public String _existResource(String name) throws Exception{
		HttpClient client=getHttpClient();
		StringBuffer buf=new StringBuffer();
		Map<String,Object> pp=new HashMap<String,Object>();
		pp.put("name",name);
		pp.put("localId", remoteId);
		client.doMimePost(get_url(), buf, "cmd=RepositoryService.existResource", pp);
		XmlResult res=(XmlResult)BeanFactory.getBeanFactory().loadBean(buf.toString());
		if (!res.getSuccess()) throw new Exception(res.getResult().toString());
		Object o=res.getResult();
		if (o!=null) return o.toString(); else return null;
	}
	
	@Override
	public InputStream _getResource(String name) throws Exception{
		HttpClient client=getHttpClient();
		return client.doPostDownload(get_url(), "cmd=RepositoryService.getResource&localId="+remoteId+"&name="+name);
	}

	@Override
	public String saveResource(String name, InputStream is) throws Exception {
		HttpClient client=getHttpClient();
		StringBuffer buf=new StringBuffer();
		Map<String,Object> pp=new HashMap<String,Object>();
		pp.put("name",name);
		pp.put("file", is);
		pp.put("localId", remoteId);
		System.out.println("save resource name="+name+", is.avail="+is.available());
		client.doMimePost(get_url(), buf, "cmd=RepositoryService.saveResource", pp);
		XmlResult res=(XmlResult)BeanFactory.getBeanFactory().loadBean(buf.toString());
		if (!res.getSuccess()) throw new Exception(res.getResult().toString());
		return res.getResult().toString();
	}

	@Override
	public void removeResource(String name) throws Exception {
		HttpClient client=getHttpClient();
		StringBuffer buf=new StringBuffer();
		Map<String,Object> pp=new HashMap<String,Object>();
		pp.put("name",name);
		pp.put("localId", remoteId);
		client.doMimePost(get_url(), buf, "cmd=RepositoryService.removeResource", pp);
	}

	public String getGlobalId() {
		return globalId;
	}

	public void setGlobalId(String globalId) {
		this.globalId = globalId;
	}

	public String getTrustStore() {
		return trustStore;
	}

	public void setTrustStore(String trustStore) {
		this.trustStore = trustStore;
	}

	public String getAccessId() {
		return accessId;
	}

	public void setAccessId(String accessId) {
		this.accessId = accessId;
	}
	
	@Override
	public long getLastModified(String name) throws Exception{
		HttpClient client=getHttpClient();
		StringBuffer buf=new StringBuffer();
		Map<String,Object> pp=new HashMap<String,Object>();
		pp.put("name",name);
		pp.put("localId", remoteId);
		client.doMimePost(get_url(), buf, "cmd=RepositoryService.getLastModified", pp);
		XmlResult res=(XmlResult)BeanFactory.getBeanFactory().loadBean(buf.toString());
		if (!res.getSuccess()) throw new Exception(res.getResult().toString());
		Object o=res.getResult();
		if (o!=null) return LightUtil.decodeLong(o+""); else return LightUtil.bootupTime;
	}
	
	@Override
	public Collection<String> listResources(String folder, String ext) throws Exception{
		HttpClient client=getHttpClient();
		StringBuffer buf=new StringBuffer();
		Map<String,Object> pp=new HashMap<String,Object>();
		pp.put("folder",folder);
		pp.put("ext",ext);
		pp.put("localId", remoteId);
		client.doMimePost(get_url(), buf, "cmd=RepositoryService.listResources", pp);
		XmlResult res=(XmlResult)BeanFactory.getBeanFactory().loadBean(buf.toString());
		if (!res.getSuccess()) throw new Exception(res.getResult().toString());
		List<String> o=(List)res.getResult();
		return o;
	}
}
