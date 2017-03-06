package com.masscustsoft.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.masscustsoft.model.AbstractResult;
import com.masscustsoft.model.DirectCode;
import com.masscustsoft.model.DirectDescr;
import com.masscustsoft.model.DirectDescrImage;
import com.masscustsoft.util.LightUtil;

public class DirectDispatch extends DirectComponent {
	boolean allowGuest = false;

	String id = "";

	public boolean getAllowGuest() {
		return allowGuest;
	}

	public void setAllowGuest(boolean allowGuest) {
		this.allowGuest = allowGuest;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void doReadDescr(AbstractResult ret) throws Exception {
		String ownerId = requiredStr("ownerId");
		List<DirectDescr> list = getDs().getBeanList(DirectDescr.class, "{ownerId:'" + ownerId + "'}", "", "sequenceId");
		List<Map> result = new ArrayList<Map>();
		for (DirectDescr c : list) {
			Map m = (Map) LightUtil.toJsonObject(c);
			m.put("id", c.getSequenceId());
			m.remove("uuid");
			m.remove("sequenceId");
			m.remove("ownerId");
			m.remove("descrId");
			String type=c.getClass().getSimpleName();
			if (type.startsWith(DirectDescr.class.getSimpleName())){
				type=type.substring(DirectDescr.class.getSimpleName().length());
			}
			m.put("type", type);
			if (c instanceof DirectDescrImage) {
				DirectDescrImage img = (DirectDescrImage) c;
				m.put("image", getImageUrl(img.getImage()));
			}
			result.add(m);
		}
		ret.setResult(result);
	}

	
	
	
	
	
	protected boolean getCodeList(String grpId, List<Map> result) throws Exception{
		return false;
	}
	
	public void doGetCodeList(AbstractResult ret) throws Exception {
		String grpId = requiredStr("grpId");
		String parentId = getStr("parentId", "");
		List<Map> result = new ArrayList<Map>();
		if (!getCodeList(grpId,result)){
			List<DirectCode> list = getDs().getBeanList(DirectCode.class,
					"{grpId:'" + grpId + "',parentId:'" + parentId + "'}", "");
			for (DirectCode c : list) {
				Map m = new HashMap();
				m.put("id", c.getCodeId());
				m.put("name", c.getName());
				result.add(m);
			}

		}
		ret.setResult(result);
	}

	
}
