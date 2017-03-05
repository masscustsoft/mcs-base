package com.masscustsoft.model;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.StringBufferInputStream;

import org.apache.commons.fileupload.FileItem;

import com.masscustsoft.api.IDataService;
import com.masscustsoft.api.IFile;
import com.masscustsoft.api.IRepository;
import com.masscustsoft.api.IndexKey;
import com.masscustsoft.api.NumIndex;
import com.masscustsoft.api.SQLSize;
import com.masscustsoft.api.SQLTable;
import com.masscustsoft.service.TempItem;
import com.masscustsoft.util.LightStr;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.StreamUtil;
import com.masscustsoft.util.inner.StreamCrc;

@SQLTable("ExternalFiles")
public class ExternalFile extends Entity{
	@IndexKey @SQLSize(64)
	String crc;

	long size;
	
	@NumIndex
	int refCount;
	
	public ExternalFile(){
		super();
		refCount=1;
	}
	
	public static ExternalFile newExternalFile(IDataService data,IRepository fs, IFile bf, File f) throws Exception{
		InputStream is=new FileInputStream(f);
		StreamCrc crc=StreamUtil.crc32(is);
		is.close();
		return newExternalFile(data,fs,bf,crc,new FileInputStream(f),f.getName());
	}
	
	public static ExternalFile newExternalFile(IDataService data,IRepository fs, IFile bf, FileItem f) throws Exception{
		StreamCrc crc=StreamUtil.crc32(f.getInputStream());
		return newExternalFile(data,fs,bf,crc,f.getInputStream(),f.getName());
	}

	public static ExternalFile newExternalFile(IDataService data,IRepository fs, IFile bf, String body, String name) throws Exception{
		InputStream is=new StringBufferInputStream(body);
		StreamCrc crc=StreamUtil.crc32(is);
		is=new StringBufferInputStream(body);
		ExternalFile ef = ExternalFile.newExternalFile(data, fs, bf, crc, is, name);
		is.close();
		return ef;
	}
	
	public static ExternalFile newExternalFile(IDataService data,IRepository fs, IFile bf, TempItem f, String name) throws Exception{
		StreamCrc crc=StreamUtil.crc32(f.getInputStream());
		return newExternalFile(data,fs,bf,crc,f.getInputStream(),name);
	}
	
	public static ExternalFile newExternalFile(IDataService data, IRepository fs, IFile bf, byte[] da, String name) throws Exception{
		StreamCrc crc=StreamUtil.crc32(da);
		ByteArrayInputStream is = new ByteArrayInputStream(da);
		return ExternalFile.newExternalFile(data, fs, bf, crc,is, name);
	}
	
	public static ExternalFile newExternalFile(IDataService data, IRepository fs, IFile bf, StreamCrc crc,InputStream is, String name) throws Exception{
		String tag="_"+crc.crc+"_"+crc.size;
		ExternalFile ef=data.getBean(ExternalFile.class, "crc",tag);
		String fn;
		if (ef!=null) {
			ef.setRefCount(ef.getRefCount()+1);
			fn=fs.existResource(ef.getUuid());
			if (fn==null){
				fn=fs.saveResource(ef.getUuid(),is);
			}
			is.close();
			data.updateBean(ef);
		}
		else{
			ef=new ExternalFile();
			ef.setCrc(tag);
			ef.setSize(crc.size);
			fn=fs.saveResource(ef.getUuid(),is);
			is.close();
			data.insertBean(ef);
		}
		if (!LightStr.isEmpty(bf.getExternalId())){
			ExternalFile ef0=data.getBean(ExternalFile.class, "uuid", bf.getExternalId());
			if (ef0!=null){
				ef0.setRefCount(ef0.getRefCount()-1);
				if (ef0.getRefCount()<=0) data.deleteBean(ef0);
				else data.updateBean(ef0);
			}
		}
		if (name==null) name="";
		name=name.replace('\\', '/');
		int i=name.lastIndexOf('/');
		if (i>=0) name=name.substring(i+1);
		bf.setName(name);
		bf.setExternalId(ef.getUuid());
		bf.setSize(crc.size);
		return ef;
	}
	
	public int getRefCount() {
		return refCount;
	}

	public void setRefCount(int refCount) {
		this.refCount = refCount;
	}

	public String getCrc() {
		return crc;
	}

	public void setCrc(String crc) {
		this.crc = crc;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	@Override
	public boolean beforeDelete() throws Exception {
		IRepository fs = LightUtil.getRepository();
		if (fs!=null) fs.removeResource(uuid);	
		return true;
	}

	public InputStream getResource(String fsId0) throws Exception{
		IRepository fs = LightUtil.getRepository(fsId0);
		InputStream is=fs.getResource(uuid);
		return is;
	}
	
	public void removeResource() throws Exception{
		refCount--;
		//System.out.println("refCount="+refCount);
		if (refCount<=0){
			dataService.deleteBean(this);
		}
		else{
			dataService.updateBean(this);
		}
	}
	
	@Override
	public String toString(){
		return "ExternalFile(crc="+crc+",size="+size+",ref="+refCount+")";
	}
	
	public static InputStream getResource(IDataService dataService, String fsId0, IFile file) throws Exception{
		if (dataService==null) dataService=LightUtil.getDataService();
		ExternalFile ef = dataService.getBean(ExternalFile.class, "uuid", file.getExternalId());
		if (ef==null){
			IRepository fs = LightUtil.getRepository(fsId0);
			return fs.getResource(file.getExternalId());
		}
		return ef.getResource(fsId0);
	}
}
