package com.masscustsoft.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;

import com.masscustsoft.util.GlbHelper;
import com.masscustsoft.util.LightStr;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.StreamUtil;

/**
 * File System based Repository.
 * 
 * @author JSong
 *
 */
public class FileRepository extends Repository{
	/**
	 * The root path for this repository
	 */
	protected String rootFolder;
	
	/**
	 * if non zero, the first n characters will be used as subfolder. 
	 */
	protected int folderLevel=0;
	
	transient File _folder=null;
	
	public int getFolderLevel() {
		return folderLevel;
	}

	public void setFolderLevel(int folderLevel) {
		this.folderLevel = folderLevel;
	}

	public String getRootFolder() {
		return rootFolder;
	}

	public void setRootFolder(String rootFolder) {
		this.rootFolder = rootFolder;
	}

	public File getFolder(){
		if (_folder!=null) return _folder;
		String folder=LightUtil.macroStr(rootFolder);
		File f;
		if (folder.replace('\\','/').indexOf("/")==-1){
			String base=(String)GlbHelper.get("baseDir");
			File root=new File(base);
			f=new File(root,rootFolder);
		}
		else f=new File(folder);
		if (!LightStr.isEmpty(folderName)){
			f=new File(f,folderName);
		}
		f.mkdirs();
		_folder=f;
		//System.out.println("FILEroot="+f.getAbsolutePath());
		return f;
	}
	
	@Override
	public void destroy(){
		_folder=null;
	}
	
	@Override
	public String _existResource(String name) throws IOException{
		File f =getFolder();
		String fn=name;
		if (folderLevel>0){
			fn=name.substring(0,folderLevel)+"/"+fn;
			f=new File(f,name.substring(0,folderLevel));
		}
		f=new File(f,name);
		if (f.exists()) return fn;
		return null;
	}
	
	@Override
	public InputStream _getResource(String name) throws Exception{
		File f =getFolder();
		if (folderLevel>0 && name.length()>folderLevel){
			f=new File(getFolder(),name.substring(0,folderLevel));;
		}
		f=new File(f,name);
		if (name.length()==0 || !f.exists()) return null;
		InputStream is=new FileInputStream(f);
		return is;
	}

	@Override
	public Collection<String> listResources(String folder, String ext) throws IOException{
		File f =getFolder();
		if (!LightStr.isEmpty(folder)) f=new File(f,folder);
		return listResources(f, f, ext);
	}
	
	private Collection<String> listResources(File f, File base, String ext) throws IOException{
		File[] files = f.listFiles();
		Collection<String> ret=new ArrayList<String>();
		String bn=base.getCanonicalPath();
		for (File ff:files){
			if (ff.isDirectory()){
				ret.addAll(listResources(ff,base, ext));
			}
			else{
				String fn=ff.getCanonicalPath().substring(bn.length()+1);
				if (ext!=null){
					if (fn.endsWith(ext)) ret.add(fn.substring(0,fn.length()-ext.length()));
				}
				else ret.add(fn);
			}
		}
		return ret;
	}
	
	@Override
	public String saveResource(String name, InputStream is) throws IOException{
		File f =getFolder();
		String fn=name;
		if (folderLevel>0){
			fn=name.substring(0,folderLevel)+"/"+fn;
			f=new File(f,name.substring(0,folderLevel));
		}
		f.mkdirs();
		f=new File(f,name);
		f.getParentFile().mkdirs();
		OutputStream os=new FileOutputStream(f);
		StreamUtil.copyStream(is, os, 0);
		os.close();
		return fn;
	}

	@Override
	public void removeResource(String name) throws IOException{
		File f =getFolder();
		if (folderLevel>0){
			f=new File(getFolder(),name.substring(0,folderLevel));;
		}
		File g=new File(f,name);
		g.delete();
		if (folderLevel>0){
			f.delete();
		}
	}

	@Override
	public long getLastModified(String name) throws IOException{
		File f =getFolder();
		String fn=name;
		if (folderLevel>0){
			fn=name.substring(0,folderLevel)+"/"+fn;
			f=new File(f,name.substring(0,folderLevel));
		}
		File g=new File(f,name);
		return g.lastModified();
	}

}
