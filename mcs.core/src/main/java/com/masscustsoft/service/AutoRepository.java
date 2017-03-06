package com.masscustsoft.service;

import java.io.InputStream;
import java.util.Collection;

import com.masscustsoft.util.LightStr;
import com.masscustsoft.util.LightUtil;

public class AutoRepository extends Repository {
	//format:
	//  C:/ABC  ==> a fileRepos
	//  http://host:port/sub/server_protocol ==> a Remote
	//  classpath:com.masscustsoft.image ==>jar
	String url;
	String defaultUrl;
	
	transient Repository repository=null;
	
	private Repository getRepository(){
		if (repository==null){
			String u=LightUtil.macroStr(url);
			if (LightStr.isEmpty(u)) u=LightUtil.macroStr(defaultUrl);
			//System.out.println("protocol="+this.getFsId()+", url="+url+" ,u="+u);
			if (u.startsWith("http")){
				RemoteRepository rep=new RemoteRepository(); repository=rep;
				String ss[]=u.split(";");
				rep.setUrl(ss[0]);
				for (int i=1;i<ss.length;i++){
					String t=ss[i];
					int idx=t.indexOf("=");
					String key=t.substring(0,idx);
					if (key.equals("accessId")) rep.setAccessId(t.substring(idx+1));
					if (key.equals("remoteId")) rep.setRemoteId(t.substring(idx+1));
					if (key.equals("folderName")) rep.setFolderName(t.substring(idx+1));
				}
				if (LightStr.isEmpty(rep.getFolderName())) rep.setFolderName("default");
			}
			else
			if (u.startsWith("classpath:")){
				ClasspathRepository rep=new ClasspathRepository(); repository=rep;
				rep.setPrefix(u.substring(10));
			}
			else{
				FileRepository rep=new FileRepository(); repository=rep;
				int i=u.lastIndexOf("@");
				if (i>=0){
					rep.setRootFolder(u.substring(0,i));
					rep.setFolderLevel(LightUtil.decodeInt(u.substring(i+1)));
				}
				else{
					rep.setRootFolder(u);
				}
			}
			repository.setCache(getCache());
			repository.setRestricted(getRestricted());
			repository.setParentFsId(getParentFsId());
		}
		return repository;
	}
	
	@Override
	public String _existResource(String name) throws Exception{
		return getRepository()._existResource(name);
	}
	
	@Override
	public InputStream _getResource(String name) throws Exception{
		return getRepository()._getResource(name);
	}
	
	@Override
	public String saveResource(String name,InputStream is) throws Exception{
		return getRepository().saveResource(name, is);
	}
	
	@Override
	public void removeResource(String name) throws Exception{
		getRepository().removeResource(name);
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getDefaultUrl() {
		return defaultUrl;
	}

	public void setDefaultUrl(String defaultUrl) {
		this.defaultUrl = defaultUrl;
	}
	
	@Override
	public long getLastModified(String name) throws Exception{
		return getRepository().getLastModified(name);
	}	
	
	@Override
	public Collection<String> listResources(String folder, String ext) throws Exception{
		return getRepository().listResources(folder, ext);
	}
}
