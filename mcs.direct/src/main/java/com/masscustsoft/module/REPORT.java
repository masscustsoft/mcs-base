package com.masscustsoft.module;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class REPORT extends PANEL{
	String reportTitle;
	
	Integer defaultFontSize;
	
	String pageSize;
	
	List<Map> reportItems=new ArrayList<Map>();
	List<Map> reportOverlays=new ArrayList<Map>();
	
	Boolean defaultTitle,defaultPaging;
	
	public String getReportTitle() {
		return reportTitle;
	}

	public void setReportTitle(String reportTitle) {
		this.reportTitle = reportTitle;
	}

	public Integer getDefaultFontSize() {
		return defaultFontSize;
	}

	public void setDefaultFontSize(Integer defaultFontSize) {
		this.defaultFontSize = defaultFontSize;
	}

	public String getPageSize() {
		return pageSize;
	}

	public void setPageSize(String pageSize) {
		this.pageSize = pageSize;
	}

	public List<Map> getReportItems() {
		return reportItems;
	}

	public void setReportItems(List<Map> reportItems) {
		this.reportItems = reportItems;
	}

	public Boolean getDefaultTitle() {
		return defaultTitle;
	}

	public void setDefaultTitle(Boolean defaultTitle) {
		this.defaultTitle = defaultTitle;
	}

	public Boolean getDefaultPaging() {
		return defaultPaging;
	}

	public void setDefaultPaging(Boolean defaultPaging) {
		this.defaultPaging = defaultPaging;
	}

	public List<Map> getReportOverlays() {
		return reportOverlays;
	}

	public void setReportOverlays(List<Map> reportOverlays) {
		this.reportOverlays = reportOverlays;
	}
	
}
