package com.masscustsoft.model;

import java.io.InputStream;

import com.masscustsoft.api.FullText;
import com.masscustsoft.api.IDataService;
import com.masscustsoft.api.IFile;
import com.masscustsoft.api.IndexKey;
import com.masscustsoft.api.NumIndex;
import com.masscustsoft.api.Synonym;


public class BasicFile extends Entity implements IFile {
	@FullText
	protected String name;
	
	@NumIndex @Synonym("i_size")
	protected long size;
	
	@IndexKey 
	protected String externalId; //point to externalFile object
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public long getSize() {
		return size;
	}
	public void setSize(long size) {
		this.size = size;
	}
	public String getExternalId() {
		return externalId;
	}
	public void setExternalId(String externalId){
		this.externalId = externalId;
	}
	
	@Override
	public boolean beforeDelete() throws Exception {
		deleteFile(this);
		return true;
	}
	
	@Override
	public InputStream getResource(String fsId0) throws Exception{
		return ExternalFile.getResource(dataService, fsId0, this);
	}

	public void dumpTo(IDataService dataService, BasicFile target) throws Exception {
		ExternalFile ef=dataService.getBean(ExternalFile.class, "uuid",getExternalId());
		ef.setRefCount(ef.getRefCount()+1);
		dataService.updateBean(ef);
		target.setSize(getSize());
		target.setExternalId(getExternalId());
		target.setName(getName());
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj==null) return false;
		if (!(obj instanceof BasicFile)) return false;
		BasicFile to=(BasicFile)obj;
		if (externalId==null) return false;
		if (externalId.equals(to.getExternalId())) return false;
		if (size!=to.getSize()) return false;
		return true;
	}
	
	@Override
	public String toString() {
		return "{ExternalId:'"+externalId+"',size="+size+"}";
	}
}
