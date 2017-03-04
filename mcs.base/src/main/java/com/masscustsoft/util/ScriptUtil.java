package com.masscustsoft.util;

import java.util.Map;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

public class ScriptUtil {
	public static Object runJs(String scr){
		return runJs(scr,null);
	}
	
	public static Object runJs(String scr, Map<String,Object> prop){
		Object old = ThreadHelper.get("scriptEnv");
		if (prop!=null) {
			ThreadHelper.set("scriptEnv", prop);
		}
		Context cx=Context.enter();
		Scriptable scope = new ScriptableHelper(cx, (Map)ThreadHelper.get(null)); //cx.initStandardObjects(scriptHelper);
		
		Object val;
		try {
			cx.evaluateString(scope, "importPackage(Packages.com.masscustsoft.util)",
					"<cmd>", 1, null);
			val = cx.evaluateString(scope, scr, "<cmd>", 1, null);
			if (val instanceof Undefined) {
				val = null;
			} else if (val instanceof NativeJavaObject) {
				NativeJavaObject nav = (NativeJavaObject) val;
				val = nav.unwrap();
			}
		} finally {
			Context.exit();
			if (prop!=null) {
				ThreadHelper.set("scriptEnv", old);
			}
		}
		return val;
	}
}
