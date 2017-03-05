package com.masscustsoft.model;

import com.masscustsoft.api.FullBody;
import com.masscustsoft.api.IDataEnumeration;
import com.masscustsoft.api.IDataService;
import com.masscustsoft.api.IParental;
import com.masscustsoft.api.IndexKey;
import com.masscustsoft.api.SQLSize;
import com.masscustsoft.helper.Upload;
import com.masscustsoft.util.LightStr;
import com.masscustsoft.util.LightUtil;

public class ParentalEntity extends Entity implements IParental{
	@IndexKey @SQLSize(64)
	protected
	String parentId;
	
	@FullBody @SQLSize(255)
	String parents;
	
	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public String getParents() {
		return parents;
	}

	public void setParents(String parents) {
		this.parents = parents;
	}

	public AbstractResult doChangeParent(Upload up){
		IDataService data=LightUtil.getDataService();
		XmlResult res=new XmlResult();
		String uuid=up.getStr("uuid", "");
		String newParentId=up.getStr("newParentId","");
		try {
			if (LightStr.isEmpty(uuid) || LightStr.isEmpty(newParentId))
				throw new Exception("NodeNotFound");
			ParentalEntity node=data.getBean(ParentalEntity.class, "uuid", uuid);
			//if (node==null) throw new Exception("NodeNotFound");
			//ParentalEntity p=null;
			//if (!newParentId.equals("root")){
			//	p=data.getBean(ParentalEntity.class, "uuid", newParentId);
			//	if (p==null) throw new Exception("NodeNotFound");
			//}
			//System.out.println("node="+LightUtil.toJsonObject(node)+",parent="+newParentId);
			//if (p!=null) System.out.println("parent="+LightUtil.toJsonObject(p));
			node.setParentId(newParentId);
			data.updateBean(node);
		} catch (Exception e) { 
			res.setError(e);
		}
		return res;
	}
	
	//
	protected void doParental() throws Exception{
		IDataService data=dataService;
		setParents(parentId);
		if (!LightStr.isEmpty(parentId) && !"NULL".equals(parentId)){
			ParentalEntity en=data.getBean(ParentalEntity.class, "uuid", parentId);
			if (en!=null){
				setParents(parentId+" "+en.getParents());
			}
		}
		//if p has children, reset all children
		IDataEnumeration<ParentalEntity> e = data.enumeration(ParentalEntity.class, "{parentId:'"+getUuid()+"'}", "", 50, false);
		for (;e.hasMoreElements();){
			ParentalEntity en=e.nextElement();
			en.setParents(getUuid()+" "+getParents());
			data.updateBean(en);
		}
	}
	
	@Override
	public boolean beforeInsert() throws Exception {
		if (!super.beforeInsert()) return false;
		doParental();
		return true;
	}
	
	@Override
	public boolean beforeUpdate() throws Exception {
		if (!super.beforeUpdate()) return false;
		if (changed("parentId") || changed("parents")) doParental();
		return true; 
	}
	
	@Override
	public boolean beforeDelete() throws Exception{
		dataService.deleteBeanList(ParentalEntity.class, "{parentId:'"+this.getUuid()+"'}");
		return super.beforeDelete();
	}
	
}
