package com.masscustsoft.util;

import java.util.Random;

import com.masscustsoft.service.TempItem;
import com.masscustsoft.service.TempService;

public class TempUtil {
	public static TempItem getTempFile() throws Exception {
		TempService temp=LightUtil.getCfg().getTempService();
		return temp.newTempItem(null);
	}

	public static String getTempFileName(){
		Random r=new Random(1000);
		String prefix="tmp-"+LightUtil.getUserId();
		return prefix+"_" + LightUtil.getCalendar().getTime().getTime() + r.nextLong()+".tmp";
	}
	
    
}
