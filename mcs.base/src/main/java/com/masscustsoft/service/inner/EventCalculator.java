package com.masscustsoft.service.inner;

import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.masscustsoft.model.Job;
import com.masscustsoft.util.MapUtil;

public class EventCalculator {
	
	List<String> days,weeks,biweeks,months,times;
	int mK, wK, bK, dK;
	int max;
	
	boolean weekly,biweekly,monthly;
	
	public EventCalculator(Job job, boolean weekly, boolean biweekly, boolean monthly){
		this(job.getMonthFrame(),job.getWeekdayFrame(),job.getBiweekdayFrame(),job.getDayFrame(),job.getTimeFrame(),weekly,biweekly,monthly);
	}
	
	public EventCalculator(String monthFrame, String weekdayFrame, String biweekdayFrame, String dayFrame, String timeFrame, boolean weekly, boolean biweekly, boolean monthly){
		
		this.weekly=weekly;
		this.biweekly=biweekly;
		this.monthly=monthly;
		
		Comparator<String> comp = new Comparator<String>(){
			@Override
			public int compare(String aa, String bb) {
				int a=Integer.parseInt(aa);
				int b=Integer.parseInt(bb);
				if (a<b) return -1;
				if (a>b) return 1;
				return 0;
			}
		};
		
		months=MapUtil.getSelectList(monthFrame); Collections.sort(months, comp);
		weeks=MapUtil.getSelectList(weekdayFrame); Collections.sort(weeks, comp);
		biweeks=MapUtil.getSelectList(biweekdayFrame); Collections.sort(biweeks, comp);
		days=MapUtil.getSelectList(dayFrame); Collections.sort(days, comp);
		times=MapUtil.getSelectList(timeFrame); Collections.sort(times, comp);
		if (weekly) {biweeks.clear(); days.clear();months.clear();}
		if (biweekly) {weeks.clear(); days.clear();months.clear();}
		if (monthly) {weeks.clear(); biweeks.clear();}
		max=1;
		mK=1; wK=1; bK=1; dK=1;
		
		if (times.size()>0) {max*=times.size(); mK*=times.size();wK*=times.size(); bK*=times.size(); dK*=times.size();}  
		if (days.size()>0) {max*=days.size();mK*=days.size(); wK*=days.size(); bK*=days.size();}
		if (biweeks.size()>0) {max*=biweeks.size(); mK*=biweeks.size(); wK*=biweeks.size();}
		if (weeks.size()>0) {max*=weeks.size(); mK*=weeks.size();}
		if (months.size()>0) max*=months.size();
		//System.out.println("max="+max+",m="+months+",weeks="+weeks+",bi="+biweeks+",days="+days+",times="+times);
	}

	public boolean onShot(Calendar c) {
		int mIdx, wIdx, bIdx, dIdx, tIdx;
		
		mIdx=-1;
		wIdx=-1;
		bIdx=-1;
		dIdx=-1;
		tIdx=-1;
		
		if (monthly && months.size()>0){
			String mon=(c.get(Calendar.MONTH)+1)+"";
			mIdx=months.indexOf(mon);
			if (mIdx==-1) return false;
		}
 		if (biweekly && biweeks.size()>0){
 			int ofs=((c.get(Calendar.WEEK_OF_YEAR)-1)%2)*7;
			String biweek=(c.get(Calendar.DAY_OF_WEEK)-1+ofs)+"";
			bIdx=biweeks.indexOf(biweek);
			if (bIdx==-1) return false;
		}
		if (weekly && weeks.size()>0){
			String week=(c.get(Calendar.DAY_OF_WEEK)-1)+"";
			wIdx=weeks.indexOf(week);
			if (wIdx==-1) return false;
		}
		if (monthly && days.size()>0){
			String day=c.get(Calendar.DAY_OF_MONTH)+"";
			dIdx=days.indexOf(day);
			String lstDay=c.getActualMaximum(Calendar.DAY_OF_MONTH)+"";
			//if (c.get(Calendar.DAY_OF_MONTH)>=30 && c.get(Calendar.HOUR_OF_DAY)==0) System.out.println(" dIdx="+dIdx+",lastDay="+lstDay+", day="+day+", date="+LightUtil.encodeLongDate(LightUtil.longDate(c)));
			
			if (dIdx==-1 && lstDay.equals(day)) dIdx=days.indexOf("31"); //last day 
			if (dIdx==-1) return false;
		}
		
		if (times.size()>0){
			String tm=(c.get(Calendar.HOUR_OF_DAY)*100+c.get(Calendar.MINUTE))+"";
			tIdx=times.indexOf(tm);
			if (tIdx==-1) return false;
		}
		return true;
	}
}
