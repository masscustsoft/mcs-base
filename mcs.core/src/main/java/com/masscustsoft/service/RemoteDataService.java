package com.masscustsoft.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.masscustsoft.api.IEntity;
import com.masscustsoft.helper.HttpClient;
import com.masscustsoft.model.ClusterNode;
import com.masscustsoft.model.SearchResult;
import com.masscustsoft.model.XmlResult;
import com.masscustsoft.util.GlbHelper;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.LightStr;
import com.masscustsoft.util.ThreadHelper;
import com.masscustsoft.xml.BeanFactory;

public class RemoteDataService extends DataService {
	String url;
	String remoteId;
	String trustStore;
	String accessId;
	String globalId; //if set globalId the remote will reuse existing cookie. 
	
	transient String _url=null;
	
	@Override
	public void initialize() throws Exception{
		super.initialize();
		_url = LightUtil.macroStr(url);
		if (_url.indexOf("?")==-1 && !_url.endsWith("/service")){
			if (!_url.endsWith("/")) _url+="/";
			_url+="service";
			if (!LightStr.isEmpty(databaseName)) _url+="?databaseName="+databaseName;
		}
	}
	private HttpClient getHttpClient(){
		HttpClient client = HttpClient.getClient(globalId, trustStore, accessId);
		
		//put the connector to transaction
		String transId=(String)ThreadHelper.get("transaction"); //GBL.uuid={hash:{dataservice,conn}}
		if (transId!=null){
			Map<String,Object> trans=(Map)GlbHelper.get("trans-"+transId);
			trans.put(hashCode()+"",this.getDsId());
		}
		
		return client;
	}
	
	private String syncTransaction(){
		String transId=(String)ThreadHelper.get("transaction"); 
		if (transId==null) {
			return "";
		}
		else{
			Map trans=(Map)GlbHelper.get("trans-"+transId);
			if (trans==null) return "";
			return transId;
		}
	}
	
	@Override
	public void _insertBean(String clusterId,String tbl,String uniqueId, String xml) throws Exception{
		HttpClient client=getHttpClient();
		StringBuffer buf=new StringBuffer();
		String transId=syncTransaction();
		client.doMimePost(_url, buf, "cmd","StorageService.insert",TBL,tbl,XML,xml,UniqueID,uniqueId,"localId",remoteId,"transId", transId);
		XmlResult res=(XmlResult)BeanFactory.getBeanFactory().loadBean(buf.toString());
		
		if (!res.getSuccess()) throw new Exception(res.getResult().toString());
	}
	
	@Override
	public void _deleteBean(String clusterId, String tbl, String uniqueId) throws Exception{
		HttpClient client=getHttpClient();
		StringBuffer buf=new StringBuffer();
		String transId=syncTransaction();
		client.doMimePost(_url, buf, "cmd","StorageService.delete",TBL, tbl, UniqueID, uniqueId,"localId",remoteId,"transId",transId);

		XmlResult res=(XmlResult)BeanFactory.getBeanFactory().loadBean(buf.toString());
		if (!res.getSuccess()) throw new Exception(res.getResult().toString());
	}
	
	@Override
	public void _updateBean(String clusterId, String tbl, String uniqueId, String old, String xml) throws Exception{
		HttpClient client=getHttpClient();
		StringBuffer buf=new StringBuffer();
		String transId=syncTransaction();
		client.doMimePost(_url, buf, "cmd","StorageService.update",TBL, tbl, UniqueID, uniqueId, XML, xml, "_old", old,"localId",remoteId,"transId",transId);
		
		XmlResult res=(XmlResult)BeanFactory.getBeanFactory().loadBean(buf.toString());
		if (!res.getSuccess()) throw new Exception(res.getResult().toString());
	}
	
	@Override
	public SearchResult _doSearch(String names,Map<String,Object> terms,String fields,String text,String sortBy,int from, int size, String facet) throws Exception{
		HttpClient client=getHttpClient();
		StringBuffer buf=new StringBuffer();
		
		String transId=syncTransaction();
		client.doMimePost(_url, buf, "cmd", "StorageService.search", "names", names, "terms", LightUtil.toJsonString(terms).toString(),
				"fields", fields, "text", LightStr.encode(text), "from", from+"", "size", size+"", "sort", sortBy==null?"":sortBy,"localId",remoteId, 
				"facet",LightStr.encode(facet),"transId",transId);
		Object o=BeanFactory.getBeanFactory().loadBean(buf.toString());
		if (o instanceof SearchResult){
			return (SearchResult)o;
		}
		if (o instanceof XmlResult){
			XmlResult res=(XmlResult)o;
			if (!res.getSuccess()) throw new Exception(res.getResult()+"");
		}
		throw new Exception("Unknown Error!"+_url);
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String storageUrl) {
		url = storageUrl;
	}

	@Override
	public Object _runStoredProcedure(String name, Integer resultType, Object... params) throws Exception {
		HttpClient client=getHttpClient();
		StringBuffer buf=new StringBuffer();
		
		String transId=syncTransaction();
		List<String> p=new ArrayList<String>();
		p.add("cmd"); p.add("StorageService.runStoredProcedure");
		p.add("name"); p.add(name);
		p.add("resultType"); p.add(resultType+"");
		p.add("count"); p.add(params.length+"");
		p.add("localId"); p.add(remoteId);
		p.add("transId"); p.add(transId);
		for (int i=0;i<params.length;i++){
			Object a=params[i];
			//System.out.println("count="+params.length+",a"+i+"="+a);
			p.add(i+""); 
			if (a instanceof OutputParameter){
				p.add("@"+((OutputParameter)a).getType());
			}
			else{
				p.add(a.getClass().getName()+"@"+LightUtil.encodeObject(a));
			}
		}
		String[] list=new String[p.size()];
		p.toArray(list);
		client.doMimePost(_url, buf, list);
		
		XmlResult res=(XmlResult)BeanFactory.getBeanFactory().loadBean(buf.toString());
		if (!res.getSuccess()) throw new Exception(res.getResult().toString());
		
		Map<String,Object> map=(Map)res.getResult();
		Object ret=map.get("ret");
		for (int i=0;i<params.length;i++){
			if (params[i] instanceof OutputParameter){
				OutputParameter o=(OutputParameter)params[i];
				Object v=map.get(i+"");
				if (v!=null) o.setValue(v);
			}
		}
		//System.out.println("sp buf="+buf+ " ret.type="+ret.getClass().getName());
		
		return ret;
	}

	@Override
	public String createTempTable(String tblName,List<Map> fields,List<Map> data) throws Exception{
		HttpClient client=getHttpClient();
		StringBuffer buf=new StringBuffer();
		
		String transId=syncTransaction();
		List<String> p=new ArrayList<String>();
		p.add("cmd"); p.add("StorageService.createTempTable");
		p.add("tableName"); p.add(tblName);
		p.add("fields"); p.add(LightUtil.toJsonString(fields).toString());
		p.add("data"); p.add(LightUtil.toJsonString(data).toString());
		p.add("localId"); p.add(remoteId);
		p.add("transId"); p.add(transId);
		String[] list=new String[p.size()];
		p.toArray(list);
		client.doMimePost(_url, buf, list);
		
		//System.out.println("sp buf="+buf);
		XmlResult res=(XmlResult)BeanFactory.getBeanFactory().loadBean(buf.toString());
		if (!res.getSuccess()) throw new Exception(res.getResult().toString());
		if (res.getResult()!=null) return res.getResult().toString(); else return null;
	}
	
	@Override
	public void dropTempTable(String tblName) throws Exception{
		HttpClient client=getHttpClient();
		StringBuffer buf=new StringBuffer();
		
		String transId=syncTransaction();
		List<String> p=new ArrayList<String>();
		p.add("cmd"); p.add("StorageService.dropTempTable");
		p.add("tableName"); p.add(tblName);
		p.add("localId"); p.add(remoteId);
		p.add("transId"); p.add(transId);
		
		String[] list=new String[p.size()];
		p.toArray(list);
		client.doMimePost(_url, buf, list);
	}
	
	@Override
	public SearchResult _doSearchBySql(String sql, Map<String,Object> terms, int from, int size, String sort, String facet) throws Exception {
		HttpClient client=getHttpClient();
		StringBuffer buf=new StringBuffer();
			
		String transId=syncTransaction();
		client.doMimePost(_url, buf, "cmd", "StorageService.searchBySql", "sql", sql, "terms", LightUtil.toJsonString(terms).toString(),
					"from", from+"", "size", size+"", "sort", sort==null?"":sort,"localId",remoteId,
					"facet",LightStr.encode(facet),"transId", transId);

		Object o=BeanFactory.getBeanFactory().loadBean(buf.toString());
		if (o instanceof SearchResult) return (SearchResult)o;
		if (o instanceof XmlResult){
			XmlResult res=(XmlResult)o;
			if (!res.getSuccess()) throw new Exception(res.getResult()+"");
		}
		throw new Exception("Unknown Error!");
	}

	@Override
	public void _commitTransaction(String transId) throws Exception{
		HttpClient client=getHttpClient();
		StringBuffer buf=new StringBuffer();
			
		client.doMimePost(_url, buf, "cmd", "StorageService.commitTransaction", "transId", transId, "localId",remoteId);

		Object o=BeanFactory.getBeanFactory().loadBean(buf.toString());
		if (o instanceof XmlResult){
			XmlResult res=(XmlResult)o;
			if (!res.getSuccess()) throw new Exception(res.getResult()+"");
		}
		else throw new Exception("Unknown Error!");
	}
	
	@Override
	public void _rollbackTransaction(String transId) throws Exception{
		//if (_url.startsWith("/")) return; //ignore if transaction not setup.
		System.out.println("REMOTEROLLBACK "+transId);
		HttpClient client=getHttpClient();
		StringBuffer buf=new StringBuffer();
			
		client.doMimePost(_url, buf, "cmd", "StorageService.rollbackTransaction", "transId", transId, "localId",remoteId);

		Object o=BeanFactory.getBeanFactory().loadBean(buf.toString());
		if (o instanceof XmlResult){
			XmlResult res=(XmlResult)o;
			if (!res.getSuccess()) throw new Exception(res.getResult()+"");
		}
		else throw new Exception("Unknown Error!");
	}

	
	@Override
	public void validate() throws Exception{
		HttpClient client=getHttpClient();
		StringBuffer buf=new StringBuffer();
			
		client.doMimePost(_url, buf, "cmd", "StorageService.validate", "localId",remoteId);

		Object o=BeanFactory.getBeanFactory().loadBean(buf.toString());
		if (o instanceof XmlResult){
			XmlResult res=(XmlResult)o;
			if (!res.getSuccess()) throw new Exception(res.getResult()+"");
		}
		else throw new Exception("Unknown Error!");
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

	public String getGlobalId() {
		return globalId;
	}

	public void setGlobalId(String globalId) {
		this.globalId = globalId;
	}

	public String getRemoteId() {
		return remoteId;
	}

	public void setRemoteId(String remoteId) {
		this.remoteId = remoteId;
	}

}

