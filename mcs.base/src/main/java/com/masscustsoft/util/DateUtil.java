package com.masscustsoft.util;

import java.sql.Date;

public class DateUtil {
	public static int daysFrom(Date dt){
		return daysBetween(dt,LightUtil.shortDate());
	}
	
	public static int daysTo(Date dt){
		return daysBetween(LightUtil.shortDate(),dt);
	}
	
	public static int daysBetween(Date dt, Date now){
		int days=(int)((now.getTime()-dt.getTime())/86400000l);
		return days;
	}
}
