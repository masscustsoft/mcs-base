package com.masscustsoft.util;

import java.util.Map;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;

import com.masscustsoft.api.IValue;
import com.masscustsoft.service.AbstractConfig;

/**
 * A Script Variable implementer. Allow (1) if a ThreadHelper varaible *scriptEnv* is defined, use it as variable interpreter. This env can be a Map or an {@link IValue} implementer.
 * @author JSong
 *
 */
public class ScriptableHelper extends ImporterTopLevel {
	Map helper;
	
	public ScriptableHelper(Context cx, Map helper) {
		this.helper=helper;
		this.initStandardObjects(cx, false);
	}
	/**
	 * the Get method by Rhyno.
	 */
	@Override
	public Object get(String name, Scriptable start) {
		Object obj = super.get(name, start);
		if (name.equals("Object")||name.equals("JavaPackage")) return obj;
		if (obj != Scriptable.NOT_FOUND) return obj;
		
		obj=null;
		Object env=helper.get("scriptEnv");
		if (env!=null && env instanceof Map){
			Map<String,Object> map=(Map)env;
			obj=map.get(name);
		}
		else
		if (env!=null && env instanceof IValue){
			obj=((IValue)env).get(name);
		}
		if (obj==null){
			obj=helper.get(name);
			if (obj==null){
				obj=GlbHelper.get(name);
			}
		}
		if (obj==null){
			Map vars=(Map)ThreadHelper.get("$Vars$");
			if (vars!=null){
				obj=vars.get(name);
			}
		}
		if (obj==null){
			AbstractConfig cfg = LightUtil.getCfg();
			if (cfg!=null){
				obj=cfg.getVars().get(name);
			}
		}
		if (obj!=null){
			if (!LightUtil.isPrimitive(obj.getClass())){
				if (obj instanceof Map) obj=toNativeObject((Map)obj);
				else {
					obj=new NativeJavaObject(start,obj,ScriptRuntime.ObjectClass);
				}
			}
			//System.out.println("SCR obj="+obj+", type="+obj.getClass().getName());
		}
		return obj;
	}
	
	public static NativeObject toNativeObject(Map map){
		NativeObject nobj = new NativeObject();
		for (Object key : map.keySet()) {
			Object val=map.get(key);
			if (val instanceof Map) val=toNativeObject((Map)val);
		    nobj.defineProperty((String)key, val, NativeObject.READONLY);
		}
		return nobj;
	}
}
