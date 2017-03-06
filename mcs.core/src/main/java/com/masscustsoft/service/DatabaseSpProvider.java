package com.masscustsoft.service;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.TimeZone;

import com.masscustsoft.api.IDataService;
import com.masscustsoft.helper.ObjectBuffer;
import com.masscustsoft.util.LogUtil;
import com.masscustsoft.util.LightUtil;

public class DatabaseSpProvider extends StoredProcedureProvider {
	@Override
	public Object runStoredProcedure(IDataService data, String name, Integer resultType, Object... params) throws Exception{
		DatabaseDataService db=(DatabaseDataService)data;
		Connection conn=null;
		CallableStatement proc=null;
		try {
			conn=db.connect(); 
			ObjectBuffer scr=new ObjectBuffer("{? = call dbo."+name+"(",") }");
			for (Object o:params){
				scr.append("", "?");
			}
			String debug=scr.toString()+"::";
			for (Object o:params){
				if (o instanceof java.sql.Date) debug+=LightUtil.encodeShortDate((java.sql.Date)o); 
				else
				if (o instanceof java.sql.Timestamp) debug+=LightUtil.encodeLongDate((Timestamp)o,TimeZone.getTimeZone("GMT")); 
				else debug+=o;
				debug+="|";
			}
			
			LogUtil.debug("Call SP: "+debug);
		    proc =conn.prepareCall(scr.toString());
		    int pid=1;
		    proc.registerOutParameter(pid, resultType);
		    pid++;
		    for (int i=0;i<params.length;i++,pid++){
		    	Object o=params[i];
		    	if (o==null) proc.setObject(pid, null);
		    	else
		    	if (o instanceof OutputParameter){
		    		OutputParameter p=(OutputParameter)o;
		    		proc.registerOutParameter(pid, p.type);
		    		p.id=pid;
		    	}
		    	else
		    	if (o instanceof Integer) proc.setInt(pid, (Integer)o);
		    	else
		    	if (o instanceof Long) proc.setLong(pid, (Long)o);
		    	else
//		    	if (o instanceof java.util.Date){
//		    		proc.setTimestamp(pid, new java.sql.Timestamp(((java.util.Date)o).getTime()));//non any more
//		    	}
//		    	else
				if (o instanceof Double || o instanceof BigDecimal) proc.setDouble(pid, (Double)o);
				else
				if (o instanceof java.sql.Timestamp){
					//Calendar c=Calendar.getInstance(TimeZone.getTimeZone("GMT"));
					//proc.setTimestamp(pid, (java.sql.Timestamp)o,c);
					proc.setString(pid, LightUtil.encodeLongDate((java.sql.Timestamp)o,"yyyy-MM-dd HH:mm:ss.SSS",TimeZone.getTimeZone("GMT")));
				}
				else
				if (o instanceof java.sql.Date){
					//Calendar c=Calendar.getInstance(TimeZone.getTimeZone("GMT"));
					//proc.setDate(pid, (java.sql.Date)o,c);
					proc.setString(pid, LightUtil.encodeShortDate((java.sql.Date)o,"yyyy-MM-dd"));
				}
				else proc.setObject(pid, o);
		    }
		    
		    proc.execute();
		    LogUtil.debug("Call SP succ: "+debug);
		    for (int i=0;i<params.length;i++,pid++){
		    	Object o=params[i];
		    	if (o instanceof OutputParameter){
		    		OutputParameter p=(OutputParameter)o;
		    		p.value=proc.getObject(p.id);
		    	}
		    }
		    return proc.getObject(1);
		}
		finally{
			if (proc!=null) proc.close();
			db.disconnect(conn);
		}
	}
	
}
