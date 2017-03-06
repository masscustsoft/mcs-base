package com.masscustsoft.model;

import java.sql.Date;
import java.util.Calendar;

import com.masscustsoft.api.AutoInc;
import com.masscustsoft.api.FullText;
import com.masscustsoft.api.IndexKey;
import com.masscustsoft.api.NumIndex;
import com.masscustsoft.api.PrimaryKey;
import com.masscustsoft.api.SQLTable;
import com.masscustsoft.util.LightUtil;

@SQLTable("DateRanges")
public class DateRange extends Entity {
	@IndexKey @PrimaryKey
	String ownerId;
	
	@NumIndex @PrimaryKey @AutoInc
	Integer sequenceId;
	
	String rangeType; //AlignToToday, AlignToWeekDay, AlignToMonthDay, Custom
	
	@FullText
	String name;
	
	Date fromDate,toDate;
	
	String fromWeekDay, fromMonthDay, fromMonth;
	
	int fromOffset=-1,toOffset=0;

	/*@Override
	public boolean afterSearch(IDataService data, List list) throws Exception {
		if (list.size()<=1){ //there's a *ADDNEW* for this one
			{
				DateRange d=new DateRange();
				d.setOwnerId(LightUtil.getUserId());
				d.setName("{\"en\":\"Today\",\"zh_CN\":\"今天\"}");
				d.setRangeType("AlignToToday");
				d.setFromOffset(0);
				d.setToOffset(1);
				dataService.insertBean(d);
			}
			{
				DateRange d=new DateRange();
				d.setOwnerId(ServerUtil.getUserId());
				d.setName("{\"en\":\"Yesterday\",\"zh_CN\":\"昨天\"}");
				d.setRangeType("AlignToToday");
				d.setFromOffset(-1);
				d.setToOffset(0);
				dataService.insertBean(d);
			}
			{
				DateRange d=new DateRange();
				d.setOwnerId(ServerUtil.getUserId());
				d.setName("{\"en\":\"This Week\",\"zh_CN\":\"本周\"}");
				d.setRangeType("AlignToWeekDay");
				d.setFromWeekDay("Mon");
				d.setFromOffset(0);
				d.setToOffset(1);
				dataService.insertBean(d);
			}
			{
				DateRange d=new DateRange();
				d.setOwnerId(ServerUtil.getUserId());
				d.setName("{\"en\":\"Last Week\",\"zh_CN\":\"上周\"}");
				d.setRangeType("AlignToWeekDay");
				d.setFromWeekDay("Mon");
				d.setFromOffset(-1);
				d.setToOffset(0);
				dataService.insertBean(d);
			}
			{
				DateRange d=new DateRange();
				d.setOwnerId(ServerUtil.getUserId());
				d.setName("{\"en\":\"This Month\",\"zh_CN\":\"本月\"}");
				d.setRangeType("AlignToMonthDay");
				d.setFromMonthDay("01");
				d.setFromOffset(0);
				d.setToOffset(1);
				dataService.insertBean(d);
			}
			{
				DateRange d=new DateRange();
				d.setOwnerId(ServerUtil.getUserId());
				d.setName("{\"en\":\"Last Month\",\"zh_CN\":\"上月\"}");
				d.setRangeType("AlignToMonthDay");
				d.setFromMonthDay("01");
				d.setFromOffset(-1);
				d.setToOffset(0);
				dataService.insertBean(d);
			}
			{
				DateRange d=new DateRange();
				d.setOwnerId(ServerUtil.getUserId());
				d.setName("{\"en\":\"This Year\",\"zh_CN\":\"今年\"}");
				d.setRangeType("AlignToYearDay");
				d.setFromMonthDay("01");
				d.setFromMonth("Jan");
				d.setFromOffset(0);
				d.setToOffset(1);
				dataService.insertBean(d);
			}
			{
				DateRange d=new DateRange();
				d.setOwnerId(ServerUtil.getUserId());
				d.setName("{\"en\":\"Last Year\",\"zh_CN\":\"去年\"}");
				d.setRangeType("AlignToYearDay");
				d.setFromMonthDay("01");
				d.setFromMonth("Jan");
				d.setFromOffset(-1);
				d.setToOffset(0);
				dataService.insertBean(d);
			}
			return true; //ask to reload
		}
		return false;
	}*/

	public Date getFrom() throws Exception{
		Calendar c=LightUtil.getShortCalendar();
		if (rangeType.equals("AlignToToday")){
			c.add(Calendar.DATE, fromOffset);
			return LightUtil.shortDate(c);
		}
		if (rangeType.equals("AlignToWeekDay")){
			c.set(Calendar.DAY_OF_WEEK, "Sun".equals(fromWeekDay)?Calendar.SUNDAY:"Mon".equals(fromWeekDay)?Calendar.MONDAY:"Tue".equals(fromWeekDay)?Calendar.TUESDAY:"Wed".equals(fromWeekDay)?Calendar.WEDNESDAY:"Thu".equals(fromWeekDay)?Calendar.THURSDAY:"Fri".equals(fromWeekDay)?Calendar.FRIDAY:Calendar.SATURDAY);
			c.add(Calendar.DATE, 7*fromOffset);
			return LightUtil.shortDate(c);
		}
		if (rangeType.equals("AlignToMonthDay")){
			c.add(Calendar.MONTH, fromOffset);
			if (fromMonthDay.equals("Lastdayofmonth")){
				c.set(Calendar.DAY_OF_MONTH, 1);
				c.add(Calendar.MONTH, 1);
				c.add(Calendar.DATE, -1);
			}
			else{
				int day=LightUtil.decodeInt(fromMonthDay);
				c.set(Calendar.DAY_OF_MONTH, day);
			}
			return LightUtil.shortDate(c);
		}
		if (rangeType.equals("AlignToYearDay")){
			c.set(Calendar.MONTH, "Jan".equals(fromMonth)?Calendar.JANUARY:"Feb".equals(fromMonth)?Calendar.FEBRUARY:"Mar".equals(fromMonth)?Calendar.MARCH:"Apr".equals(fromMonth)?Calendar.APRIL
					:"May".equals(fromMonth)?Calendar.MAY:"Jun".equals(fromMonth)?Calendar.JUNE:"Jul".equals(fromMonth)?Calendar.JULY:"Aug".equals(fromMonth)?Calendar.AUGUST
					:"Sep".equals(fromMonth)?Calendar.SEPTEMBER:"Oct".equals(fromMonth)?Calendar.OCTOBER:"Nov".equals(fromMonth)?Calendar.NOVEMBER:Calendar.DECEMBER);
			c.add(Calendar.YEAR, fromOffset);
			if (fromMonthDay.equals("Lastdayofmonth")){
				c.set(Calendar.DAY_OF_MONTH, 1);
				c.add(Calendar.MONTH, 1);
				c.add(Calendar.DATE, -1);
			}
			else{
				int day=LightUtil.decodeInt(fromMonthDay);
				c.set(Calendar.DAY_OF_MONTH, day);
			}
			return LightUtil.shortDate(c);
		}
		return fromDate;
	}
	
	public Date getTo(Date from) throws Exception{
		Calendar c=LightUtil.getShortCalendar();
		if (from!=null) c.setTime(from);
		if (rangeType.equals("AlignToToday")){
			c.add(Calendar.DATE, toOffset-fromOffset-1);
			return LightUtil.shortDate(c);
		}
		if (rangeType.equals("AlignToWeekDay")){
			c.add(Calendar.DATE, 7*(toOffset-fromOffset)-1);
			return LightUtil.shortDate(c);
		}
		if (rangeType.equals("AlignToMonthDay")){
			c.add(Calendar.MONTH, toOffset-fromOffset);
			c.add(Calendar.DATE,-1);
			return LightUtil.shortDate(c);
		}
		if (rangeType.equals("AlignToYearDay")){
			c.add(Calendar.YEAR, toOffset-fromOffset);
			c.add(Calendar.DATE,-1);
			return LightUtil.shortDate(c);
		}
		return toDate;
	}
	
	public String getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}

	public String getRangeType() {
		return rangeType;
	}

	public void setRangeType(String rangeType) {
		this.rangeType = rangeType;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Date getFromDate() {
		return fromDate;
	}

	public void setFromDate(Date fromDate) {
		this.fromDate = fromDate;
	}

	public Date getToDate() {
		return toDate;
	}

	public void setToDate(Date toDate) {
		this.toDate = toDate;
	}

	public Integer getSequenceId() {
		return sequenceId;
	}

	public void setSequenceId(Integer sequenceId) {
		this.sequenceId = sequenceId;
	}

	public String getFromWeekDay() {
		return fromWeekDay;
	}

	public void setFromWeekDay(String fromWeekDay) {
		this.fromWeekDay = fromWeekDay;
	}

	public String getFromMonthDay() {
		return fromMonthDay;
	}

	public void setFromMonthDay(String fromMonthDay) {
		this.fromMonthDay = fromMonthDay;
	}

	public int getFromOffset() {
		return fromOffset;
	}

	public void setFromOffset(int fromOffset) {
		this.fromOffset = fromOffset;
	}

	public int getToOffset() {
		return toOffset;
	}

	public void setToOffset(int toOffset) {
		this.toOffset = toOffset;
	}

	public String getFromMonth() {
		return fromMonth;
	}

	public void setFromMonth(String fromMonth) {
		this.fromMonth = fromMonth;
	}

}
