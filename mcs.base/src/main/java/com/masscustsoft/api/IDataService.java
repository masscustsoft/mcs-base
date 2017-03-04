package com.masscustsoft.api;

import java.util.List;
import java.util.Map;

import com.masscustsoft.service.Constraint;
import com.masscustsoft.service.PageInfo;

/**
 * Interface for data manipulation service. Suggest to extend DataService for new DataService connector.
 * 
 * @author JSong
 *
 */
public interface IDataService{

	public static final String UniqueID="i_uuid"; //uniqueId_, this _uuid is a one id each time you insert
	public static final String TBL="class_";
	public static final String XML="xml_";
	
	public static final String INDEX_="i_";
	public static final String CATEGORY_="c_";
	public static final String NUMINDEX_="o_";
	public static final String FULLTEXT_="t_";
	public static final String FULLBODY_="b_";
	public static final String DATEINDEX_="z_";
	public static final String TIMESTAMP_="d_";
	
	
	public boolean getTraceable();
	
	public void insertBean(IEntity bean) throws Exception;
	public void updateBean(IEntity bean) throws Exception;
	public void _updateBean(IEntity bean) throws Exception;
	 public void deleteBean(IEntity bean) throws Exception;
	public <T extends IEntity> T getBean(Class<T> c,Object...paras) throws Exception;
	public <T extends IEntity> T getBean(int defer, Class<T> c,Object...paras) throws Exception;
	public <T extends Object> List<T> getBeanList(Class<T> c, String specific, String text)	throws Exception;
	public <T extends IEntity> void deleteBeanList(Class<T> c, String specific) throws Exception;
	public <T extends IEntity> void deleteBeanList(Class<T> c, String specific, String sort) throws Exception;
	public <T extends IEntity> void deleteBeanList(Class<T> c, String specific, String sort, int batch) throws Exception;
	public <T extends Object> List<T> getBeanList(Class<T> c, String specific, String text, String sort) throws Exception;
	public <T extends Object> List<T> getBeanList(Class<T> c, String specific, String text, int from, int size, String sort) throws Exception;
	
	public PageInfo search(String criteria, String specific, String text, int from, int size, String sort) throws Exception;
	
	public List<Map> getBeanListBySql(String sql, String specific, int from, int size, String sort)	throws Exception;
	public PageInfo getBeanListBySql(String sql, String specific, int from, int size, String sort, String facet, boolean raw) throws Exception;
	public PageInfo getBeanList(Class<?>[] cs, String specific, String text, int from, int size, String sort, String facet, boolean raw) throws Exception;
		
	public int getSequenceId(String seqId, Class<? extends IEntity> cls, String fldName, Integer curr) throws Exception;
	public long getSequenceId(String seqId, Class<? extends IEntity> cls, String fldName, Long curr) throws Exception;
	public String getSequenceId(String seqId, Class<? extends IEntity> cls, String fldName, String prefix, String curr) throws Exception;
	public String getSequenceId(String seqId, Class<? extends IEntity> cls, String fldName, String prefix, String curr, String extraFields, String extraValues) throws Exception;
	public int getSequenceId(String seqId, Class<? extends IEntity> cls, String fldName, Integer curr, String extraFields, String extraValues) throws Exception;
	public long getSequenceId(String seqId, Class<? extends IEntity> cls, String fldName, Long curr, String extraFields, String extraValues) throws Exception;
	
	public List<Constraint> getConstraints();
	public Constraint getConstraint(String cls);
	
	public <T> IDataEnumeration<T> enumeration(final Class<T> c, String specific, String text,int batch, boolean raw) throws Exception;
	public <T> IDataEnumeration<T> enumeration(final Class<T> c, String specific, String text, String sort, int batch, String facet, List<IJoin> joins, List<IRefiner> refiners, boolean raw) throws Exception;
	public <T> IDataEnumeration<T> enumeration(final Class<T> c, String specific, String text, String sort, int batch, List<IJoin> joins, List<IRefiner> refiners) throws Exception;
	public <T> IDataEnumeration<T> enumeration(String sql, String specific, String sort, int batch, List<IJoin> joins, List<IRefiner> refiners, boolean raw) throws Exception;
	
	public Map doJoin(Object rcd, List<IJoin> joins, List<IRefiner> refiners) throws Exception;
	public Integer runStoredProcedure(IEntity owner, String name, Object... params) throws Exception;
	public Object _runStoredProcedure(String name, Integer resultType, Object... params) throws Exception;
	
	public boolean supportFullText(String model);
	public String createTempTable(String tblName,List<Map> fields,List<Map> data) throws Exception;
	public void dropTempTable(String tblName) throws Exception;
}
