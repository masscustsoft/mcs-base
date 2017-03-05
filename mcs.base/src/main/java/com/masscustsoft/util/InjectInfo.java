package com.masscustsoft.util;

import java.util.ArrayList;
import java.util.List;

import com.masscustsoft.api.Inject;
import com.masscustsoft.api.Inject1;
import com.masscustsoft.api.Inject2;
import com.masscustsoft.api.Inject3;
import com.masscustsoft.api.Inject4;
import com.masscustsoft.api.Inject5;
import com.masscustsoft.api.Inject6;
import com.masscustsoft.api.Inject7;
import com.masscustsoft.api.Inject8;
import com.masscustsoft.api.Inject9;

public class InjectInfo {
	public boolean share;
	public String id=""; //shareId
	public String[] p;
	public String attr="";
	public Class clazz=null;
	public String ref="";
	public String fsId;
	
	public List<InjectInfo> list=new ArrayList<InjectInfo>();
	
	public InjectInfo(String fsId,Object ib){
		this.fsId=fsId;
		if (ib instanceof Inject){
			Inject inj=(Inject)ib;
			p=inj.p();
			share=inj.share();
			id=inj.id();
			ref=inj.ref();
			clazz=inj.clazz();
			for (Inject1 j:inj.list()){
				list.add(new InjectInfo(fsId,j));
			}
		}
		else
		if (ib instanceof Inject1){
			Inject1 inj=(Inject1)ib;
			p=inj.p();
			clazz=inj.clazz();
			attr=inj.attr();
			share=inj.share();
			id=inj.id();
			ref=inj.ref();
			for (Inject2 j:inj.list()){
				list.add(new InjectInfo(fsId,j));
			}
		}
		else
		if (ib instanceof Inject2){
			Inject2 inj=(Inject2)ib;
			p=inj.p();
			clazz=inj.clazz();
			attr=inj.attr();
			ref=inj.ref();
			share=inj.share();
			id=inj.id();
			for (Inject3 j:inj.list()){
				list.add(new InjectInfo(fsId,j));
			}
		}	
		else
		if (ib instanceof Inject3){
			Inject3 inj=(Inject3)ib;
			p=inj.p();
			clazz=inj.clazz();
			attr=inj.attr();
			ref=inj.ref();
			share=inj.share();
			id=inj.id();
			for (Inject4 j:inj.list()){
				list.add(new InjectInfo(fsId,j));
			}
		}	
		else
		if (ib instanceof Inject4){
			Inject4 inj=(Inject4)ib;
			clazz=inj.clazz();
			attr=inj.attr();
			ref=inj.ref();
			p=inj.p();
			share=inj.share();
			id=inj.id();
			for (Inject5 j:inj.list()){
				list.add(new InjectInfo(fsId,j));
			}
		}	
		else
		if (ib instanceof Inject5){
			Inject5 inj=(Inject5)ib;
			clazz=inj.clazz();
			attr=inj.attr();
			ref=inj.ref();
			p=inj.p();
			share=inj.share();
			id=inj.id();
			for (Inject6 j:inj.list()){
				list.add(new InjectInfo(fsId,j));
			}
		}
		else
		if (ib instanceof Inject6){
			Inject6 inj=(Inject6)ib;
			clazz=inj.clazz();
			ref=inj.ref();
			attr=inj.attr();
			p=inj.p();
			share=inj.share();
			id=inj.id();
			for (Inject7 j:inj.list()){
				list.add(new InjectInfo(fsId,j));
			}
		}
		else
		if (ib instanceof Inject7){
			Inject7 inj=(Inject7)ib;
			clazz=inj.clazz();
			attr=inj.attr();
			ref=inj.ref();
			p=inj.p();
			share=inj.share();
			id=inj.id();
			for (Inject8 j:inj.list()){
				list.add(new InjectInfo(fsId,j));
			}
		}
		else
		if (ib instanceof Inject8){
			Inject8 inj=(Inject8)ib;
			clazz=inj.clazz();
			ref=inj.ref();
			attr=inj.attr();
			share=inj.share();
			id=inj.id();
			p=inj.p();
			for (Inject9 j:inj.list()){
				list.add(new InjectInfo(fsId,j));
			}
		}
		else
		if (ib instanceof Inject9){
			Inject9 inj=(Inject9)ib;
			clazz=inj.clazz();
			attr=inj.attr();
			ref=inj.ref();
			share=inj.share();
			id=inj.id();
			p=inj.p();
		}
		else
		if (clazz!=null && com.masscustsoft.Lang.CLASS.getSimpleName(clazz).equals("Object")) clazz=null;
	}
	
	public String toString(){
		return "clazz="+clazz+",id="+id+",ref="+ref+",attr="+attr+",list="+list;
	}
}
