package com.masscustsoft.Lang;

import java.io.InputStream;
import java.net.URLClassLoader;

import com.masscustsoft.api.IVariant;

public class CLASSLoader extends ClassLoader{
	
	public CLASSLoader(){
		super(ClassLoader.class.getClassLoader()); 
	} 
	
	public CLASSLoader(URLClassLoader uloader){
		super(uloader); 
	} 
	
	@Override 
	public Class findClass(String name) throws ClassNotFoundException {
		Class cls = findLoadedClass(name);
	    if (cls != null) return cls;

	    boolean isDyn=CLASS.isDynamicClass(name);
	    //System.out.println("isDyn="+isDyn+",name="+name);
	    if (!isDyn) return super.findClass(name);
	    
	    DynamicClassInfo info = CLASS.getDynamicClassExtension(name);
	    
	    try {
	    	Class pcls=CLASS.forName(info.getTemplate());
		    //System.out.println("findClass pcls="+pcls);
			
	    	Object obj=pcls.newInstance();
			//System.out.println("findClass pobj="+obj);
			if (obj instanceof IVariant){
				IVariant var=(IVariant)obj;
				//System.out.println("findClass to call getbyte");
				byte[] b=var.getClassBytes(info);
				//System.out.println("findClass getbyte size="+b.length);
				CLASS.setClassBytes(name, b);
				cls=this.defineClass(name,b,0,b.length);
				this.resolveClass(cls);
				CLASS.setClassBytes(name,b);
				//System.out.println("findClass ready="+cls);
				return cls;
			}
			else throw new ClassNotFoundException();
		} catch (Exception e) {
			e.printStackTrace();
			throw new ClassNotFoundException();
		}
	}

	@Override
	public InputStream getResourceAsStream(String res) {
		URLClassLoader uloader=(URLClassLoader)getParent();
		return uloader.getResourceAsStream(res);
	}

}
