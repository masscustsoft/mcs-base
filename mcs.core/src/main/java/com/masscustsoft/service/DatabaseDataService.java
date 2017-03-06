package com.masscustsoft.service;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.StringReader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.net.URL;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.lang.StringEscapeUtils;

import com.masscustsoft.Lang.CLASS;
import com.masscustsoft.api.DecimalPrecision;
import com.masscustsoft.api.IDatabaseAccess;
import com.masscustsoft.api.PrimaryKey;
import com.masscustsoft.api.SQLField;
import com.masscustsoft.api.SQLSize;
import com.masscustsoft.api.SQLTable;
import com.masscustsoft.helper.ObjectBuffer;
import com.masscustsoft.model.ClusterNode;
import com.masscustsoft.model.Entity;
import com.masscustsoft.model.ParentalEntity;
import com.masscustsoft.model.SearchResult;
import com.masscustsoft.util.DataUtil;
import com.masscustsoft.util.EncryptUtil;
import com.masscustsoft.util.GlbHelper;
import com.masscustsoft.util.LightStr;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.LogUtil;
import com.masscustsoft.util.MapUtil;
import com.masscustsoft.util.ReflectUtil;
import com.masscustsoft.util.StreamUtil;
import com.masscustsoft.util.ThreadHelper;
import com.masscustsoft.xml.BeanFactory;
import com.masscustsoft.xml.XmlNode;
import com.masscustsoft.xml.XmlParser;

public abstract class DatabaseDataService extends DataService implements IDatabaseAccess{
	protected String serverName = "localhost";
	protected String port;
	protected String user = "";
	protected String password = "";
	protected Integer timeout;
	protected String ddlFileName;
	Map<String, String> params = new HashMap<String, String>();

	protected Boolean productionMode, asyncMode;

	// rawMode is used for JDBC connector
	protected Boolean exclusive, splitBigChar;

	protected List<String> predefinedClassList = new ArrayList<String>();
	protected List<TableMapping> predefinedMappingList = new ArrayList<TableMapping>();
	transient Map<String, Boolean> fulltextFields = new HashMap<String, Boolean>();
	transient public boolean initialized = false;
	protected transient Map<String, String> tableMap = new HashMap<String, String>(); // base
																			// BeanName
																			// ->
																			// Database
																			// Table
																			// name
	transient Map<String, Map<String, String>> columnMap = new HashMap<String, Map<String, String>>(); // tbl.PropertyName
																										// ->
																										// Database
																										// Column
																										// name
	transient Map<String, Map<String, String>> columnMapX = new HashMap<String, Map<String, String>>(); // tbl.PropertyName
																										// ->
																										// Database
																										// Column
																										// name
	protected transient Map<String, List<String>> knownColumns = new HashMap<String, List<String>>(); // tbl
																							// ->
																							// Database
																							// Column
																							// name
																							// list
	transient List<String> activeTables = new ArrayList<String>(); // active
																	// tables,
																	// for
																	// commit
																	// use
	transient List<String> parentals = new ArrayList<String>();
	transient List<String> preparedBeans = new ArrayList<String>();
	transient List<String> gens = new ArrayList<String>();

	transient Map<Connection, ConnectionPool> connections = new HashMap<Connection, ConnectionPool>();
	transient List<Connection> badConnections = new ArrayList<Connection>();

	transient Timer conn_timer = null;

	protected transient Writer ddlFile = null;
	protected transient boolean reindexing = false;

	protected abstract void initialize(Connection conn) throws Exception;

	protected abstract Connection getRawConnection() throws Exception;

	protected Properties getConnectionProperties() {
		Properties p = new Properties();
		p.setProperty("user", LightUtil.macroStr(user));
		p.setProperty("password", EncryptUtil.deobfuscate(LightUtil.macroStr(password)));
		for (String key : params.keySet()) {
			p.setProperty(key, LightUtil.macroStr(params.get(key)));
		}
		return p;
	}

	@Override
	public void initialize() throws Exception {
		super.initialize();
		if (spProvider == null) {
			spProvider = new DatabaseSpProvider();
		}
		if ("true".equals(LightUtil.getVar("_productionMode", "false")) && productionMode == null)
			productionMode = true;
	}

	protected void verifyFullIndex(Connection conn) throws Exception {

	}

	public synchronized Connection connect() throws Exception {
		if (!initialized) {
			initialized = true;
			Boolean async = this.asyncMode;
			boolean trace = this.traceable;
			{
				this.traceable = false;
				this.asyncMode = false;
				// System.out.println("async mode="+this.asyncMode);

				processPredefinedClasses();
				// LogUtil.debug("predefined: tableMap="+tableMap);
				// LogUtil.debug("predefined: columnMap="+columnMap);
				// LogUtil.debug("predefined: columnMapX="+columnMapX);
				// LogUtil.debug("predefined: knownColumns="+knownColumns);
			}

			Connection conn = getPoolConnection();
			{
				try {
					verifyFullIndex(conn);
				} catch (Exception e) {
					e.printStackTrace();
					reindexing = true;
					// runDdl("BACKUP LOG "+db()+" WITH TRUNCATE_ONLY");
					ThreadHelper.set("_reindexing_", true);
					stripIndexes(conn);
				}
				if (reindexing) {
					String fn = LightUtil.macroStr(ddlFileName);
					if (!LightStr.isEmpty(fn)) {
						File ff = new File(fn);
						ff.getParentFile().mkdirs();
						ddlFile = new FileWriter(ff);
					}
				}

				// System.out.println("installbasicsfunctions!!!!!!!!!!!!!");
				installBasicFunction(conn);
				initialize(conn);

				// preload schema
				List<String> tables = new ArrayList<String>();
				for (String tbl : tableMap.values()) {
					if (!tables.contains(tbl))
						tables.add(tbl);
				}
				for (String tbl : tables) {
					updateSchema(conn, tbl);
				}

				disconnect(conn);
			}
			this.traceable = trace;
			this.asyncMode = async;

			// System.out.println("async mode2="+this.asyncMode);
			if (ddlFile != null) {
				ddlFile.close();
				ddlFile = null;
			}

			if (reindexing) {
				ClusterNode cluster = this.getBean(ClusterNode.class);
				if (cluster == null) {
					cluster = new ClusterNode();
					this.insertBean(cluster);
				}
				ThreadHelper.set("_reindexing_", null);
			}
			// System.out.println("Create timer!");
			conn_timer = new Timer();
			if (timeout == null)
				timeout = 60; // 1 min
			conn_timer.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					int idles = 0;
					long t0 = System.currentTimeMillis();
					List<Connection> removes = new ArrayList<Connection>();
					synchronized (connections) {
						for (ConnectionPool p : connections.values()) {
							if (p.idle == false)
								continue;
							// System.out.println("t="+(t0-p.lastTick)+",to="+timeout*1000);
							if (t0 - p.lastTick >= timeout * 1000) {
								p.idle = false;
								try {
									p.conn.close();
								} catch (SQLException e) {
									LogUtil.dumpStackTrace(e);
								}
								removes.add(p.conn);
							} else
								idles++;
						}
						for (Connection c : removes) {
							connections.remove(c);
						}
					}
					// LogUtil.info("DatabaseConnection."+dsId+": Pool
					// size="+connections.size()+",idles="+idles+",
					// "+connections.values()+", timeout="+timeout*1000);
				}

			}, 30000, 60000);
		}
		return getPoolConnection();
	}

	protected abstract void stripIndexes(Connection conn) throws Exception;

	public synchronized Connection getPoolConnection() throws Exception {
		// ThreadHelper.transaction records a id where it stored in
		// globalhelper.
		String transId = (String) ThreadHelper.get("transaction"); // GBL.uuid={hash:{dataservice,conn}}
		if (!LightStr.isEmpty(transId)) {
			Map<String, Object> trans = (Map) GlbHelper.get("trans-" + transId);
			// DataService
			// ds=(DataService)trans.get(hashCode()+"");//{hash={dataService,conn}}
			Connection conn = (Connection) GlbHelper.get("conn-" + transId + "-" + hashCode());
			if (conn == null) {
				trans.put(hashCode() + "", this.getDsId());
				conn = _getPoolConnection();
				conn.setAutoCommit(false);
				GlbHelper.set("conn-" + transId + "-" + hashCode(), conn);
			}
			return conn;
		}
		return _getPoolConnection();
	}

	private synchronized Connection _getPoolConnection() throws Exception {
		synchronized (connections) {
			for (Connection conn : badConnections) {
				try {
					conn.close();
				} catch (SQLException e) {
					LogUtil.dumpStackTrace(e);
				}
				connections.remove(conn);
			}
		}
		badConnections.clear();

		for (ConnectionPool pool : connections.values()) {
			if (pool.idle) {
				pool.idle = false;
				if (!sanityTest(pool)) {
					// System.out.println("Sanity failed");
					badConnections.add(pool.conn);
					continue;
				}
				pool.lastTick = System.currentTimeMillis();
				// LogUtil.debug("Lease connection: "+pool.conn);
				return pool.conn;
			}
		}
		Connection conn = getRawConnection();
		ConnectionPool pool = new ConnectionPool(conn);
		pool.idle = false;
		pool.lastTick = System.currentTimeMillis();
		synchronized (connections) {
			connections.put(conn, pool);
		}
		LogUtil.debug("New connection: " + conn);
		return conn;
	}

	public void disconnect(Connection conn) throws Exception {
		String transId = (String) ThreadHelper.get("transaction");
		if (!LightStr.isEmpty(transId))
			return;
		_disconnect(conn);
	}

	private void _disconnect(Connection conn) throws Exception {
		if (conn != null) {
			ConnectionPool pool = connections.get(conn);
			if (pool != null) {
				pool.idle = true;
				// LogUtil.debug("Release connection: "+conn);
			}
		}
	}

	@Override
	protected String getFieldName(java.lang.reflect.Field fld) {
		return _getFieldName(fld, true);
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	protected String _getSql(Connection conn, String sql) throws Exception {
		// System.out.println("_getSql="+sql);
		String res = null;
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			ResultSet rs = null;
			rs = stmt.executeQuery(sql);
			if (rs.next()) {
				res = rs.getString(1);
			}
			rs.close();
		} finally {
			if (stmt != null)
				stmt.close();
		}
		return res;
	}

	protected Integer _getSqlInt(Connection conn, String sql) throws Exception {
		Integer res = null;
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			ResultSet rs = null;
			rs = stmt.executeQuery(sql);
			if (rs.next()) {
				res = rs.getInt(1);
			}
			rs.close();
		} finally {
			if (stmt != null)
				stmt.close();
		}
		return res;
	}

	public String getSql(String sql) throws Exception {
		String transId = (String) ThreadHelper.get("transaction");
		if (!LightStr.isEmpty(transId)) {
			DataUtil.commitTransaction();
		}
		Connection conn = connect();
		try {
			return _getSql(conn, sql);
		} finally {
			disconnect(conn);
			if (!LightStr.isEmpty(transId)) {
				DataUtil.startTransaction();
			}
		}
	}

	public Integer getSqlInt(String sql) throws Exception {
		return LightUtil.decodeInt(getSql(sql));
	}

	public void runDdl(String sql, Object... params) throws Exception {
		try {
			if (ddlFile != null) {
				ddlFile.append(sql + "\n\r\n\r");
			}
			LogUtil.info("RunDdl " + sql);
			runSql(null, sql, params);
		} catch (Exception e) {
			if (ThreadHelper.get("_reindexing_")!=null){
				LogUtil.dumpStackTrace(e);
			}
			else throw e;
		}
	}
	
	protected Object _runSql(Connection conn, String sql, Object... params) throws Exception {
		// System.out.println("_runSql="+sql);
		Statement st = null;
		try {
			if (params.length == 0 && !sql.substring(0, 6).equalsIgnoreCase("select")) {
				// Oracle is stupid, must use Statement to run Trigger create code.
				st = conn.createStatement();
				return st.execute(sql);
			} else {
				PreparedStatement stmt = conn.prepareStatement(sql);
				st = stmt;
				for (int i = 0; i < params.length; i++) {
					Object v = params[i];
					if (v instanceof Date) {
						Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
						stmt.setDate(i + 1, (Date) v, c);
					} else if (v instanceof Timestamp) {
						Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
						stmt.setTimestamp(i + 1, (Timestamp) v, c);
					} else
						stmt.setObject(i + 1, v);
				}
				if (stmt.execute()) {
					List<Map> list = new ArrayList<Map>();
					for (;;) {
						ResultSet rs = stmt.getResultSet();
						if (rs != null) {
							ResultSetMetaData m = rs.getMetaData();
							while (rs.next()) {
								Map<String, Object> vals = new HashMap<String, Object>();
								for (int c = 1, len = m.getColumnCount(); c <= len; c++) {
									Object val = rs.getObject(c);
									if (val == null)
										continue;
									String nm = m.getColumnName(c);
									vals.put(nm, val);
								}
								list.add(vals);
							}
							rs.close();
						}
						if (stmt.getMoreResults() == false && (stmt.getUpdateCount() == -1))
							break;
					}
					return list;
				}
				return stmt.getUpdateCount();
			}
		} finally {
			if (st != null)
				st.close();
		}
	}

	public Object runSql(Entity owner, String sql, Object... params) throws Exception {
		String transId = (String) ThreadHelper.get("transaction");
		if (!LightStr.isEmpty(transId)) {
			DataUtil.commitTransaction();
		}
		Connection conn = connect();
		// LogUtil.debug("runSql="+sql);
		try {
			//doDirectTrace(owner, "runSQL", sql, params);
			return _runSql(conn, sql, filterParams(params));
		} finally {
			disconnect(conn);
			if (!LightStr.isEmpty(transId)) {
				DataUtil.startTransaction();
			}
		}
	}

	public ResultSet openQuery(Connection conn, String sql) throws Exception {
		return openQuery(conn, sql, false);
	}

	public ResultSet openQuery(Connection conn, String sql, boolean editable) throws Exception {
		Statement stmt = null;
		if (editable)
			stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
		else
			stmt = conn.createStatement();
		ResultSet rs = null;
		rs = stmt.executeQuery(sql);
		return rs;
	}

	public void closeQuery(ResultSet rs) throws Exception {
		if (rs == null)
			return;
		Statement st = rs.getStatement();
		try {
			rs.close();
		} finally {
			if (st != null)
				st.close();
		}
	}

	protected void setColumnMapping(String bean, String name, String fname, boolean gen) throws Exception {
		// System.out.println("col map
		// bean="+bean+",name="+name+",fname="+fname);
		String tbl = getTableName(bean);
		if (tbl == null) {
			tbl = bean;
			tableMap.put(bean, bean);
		}
		Map<String, String> map = columnMap.get(tbl);
		Map<String, String> mapx = columnMapX.get(tbl);
		if (map == null) {
			map = new HashMap<String, String>();
			columnMap.put(tbl, map);
		}
		if (mapx == null) {
			mapx = new HashMap<String, String>();
			columnMapX.put(tbl, mapx);
		}
		map.put(name, fname);
		mapx.put(fname, name);
		if (gen) {
			String genFld = bean + "$" + name;
			if (!gens.contains(genFld))
				gens.add(genFld);
		}
	}

	protected void registerBean(String bean, String TBL, List<FieldMapping> fields) throws Exception {
		if (bean.equals("map")) return;
		if (tableMap.get(bean) != null)
			return;

		BeanFactory bf = BeanFactory.getBeanFactory();
		Class CLS = CLASS.forName(bf.findRealClass(bean));
		if (TBL == null)
			TBL = getTableName(bean);

		tableMap.put(bean, TBL);
		if (ParentalEntity.class.isAssignableFrom(CLS) && !parentals.contains(TBL))
			parentals.add(TBL);
		// process columns
		List<Field> flds = ReflectUtil.getFieldMap(CLS);
		for (Field f : flds) {
			if (f.getName().equals("dataService"))
				continue;
			if (f.getName().equals("old"))
				continue;
			SQLField sf = (SQLField) f.getAnnotation(SQLField.class);
			String col = bf.getEncodeName(f).toUpperCase();
			if (sf != null && sf.value().length() > 0)
				col = sf.value();
			if (col.equals("I_UUID") || col.equals("OLD"))
				continue;

			PrimaryKey pk = (PrimaryKey) f.getAnnotation(PrimaryKey.class);
			boolean gen = false;
			if (pk != null && pk.serverGenerated())
				gen = true;
			setColumnMapping(bean, bf.getEncodeName(f), col, gen);
		}
		if (fields != null) {
			for (FieldMapping fm : fields) {
				String col = fm.getFieldName();
				Field f = ReflectUtil.findField(flds, fm.getAttribute());
				setColumnMapping(bean, bf.getEncodeName(f), col,
						fm.getServerGenerated() != null && fm.getServerGenerated());
			}
		}

		prepareTableFor(bean, TBL);
	}

	public void processPredefinedClasses() throws Exception {
		BeanFactory bf = BeanFactory.getBeanFactory();
		tableMap.clear();
		tableMap.put("Entity", "Entity"); // default Enitity mapping
		parentals.clear();
		parentals.add("Entity");
		List<String> tables = getTableList(LightUtil.getCfg().getSupportedModules());
		for (String tbl : tables) {
			int i = tbl.lastIndexOf(".");
			String name = tbl;
			if (i > 0) {
				name = tbl.substring(i + 1);
			}
			if (!predefinedClassList.contains(name))
				predefinedClassList.add(name);
		}

		for (TableMapping tm : predefinedMappingList) {
			registerBean(tm.getEntity(), tm.getTableName(), tm.getFields());
		}
		for (String bean : predefinedClassList) {
			registerBean(bean, null, null);
		}
		
	}

	public List<String> getTableList(String supported) throws Exception{
		List<String> ret=new ArrayList<String>();
		
		List<String> sup=MapUtil.getSelectList(supported);
		
		ClassLoader cl = CLASS.getLoader();
		BeanFactory bf=BeanFactory.getBeanFactory();
		
		LogUtil.info("LOAD TABLES");
		ret.add("DeviceLogin");
		ret.add("FlyingFile");
		ret.add("ChangeLog");
		ret.add("ExternalFile");
//		ret.add("BasicSession");
//		ret.add("FileBlock");
//		ret.add("Favorite");
//		ret.add("PortletInstance");
//		ret.add("DateRange");
//		ret.add("Access");
		Enumeration<URL> all = cl.getResources("META-INF/TABLES.xml");
		for (;all.hasMoreElements();){
			URL u=all.nextElement();
			int idx=LightUtil.isSupportedModule(sup,u.getPath());
			if (idx==0) continue;
			String grp=sup.get(idx-1);
			InputStream is = u.openStream();
			StringBuffer buf=new StringBuffer();
			StreamUtil.loadStream(is, buf, LightUtil.UTF8);
			//LogUtil.info("TABLES.xml="+buf);
			List<String> tbls=(List)bf.loadBean(buf.toString());
			for (String it:tbls){
				if (!ret.contains(it)) ret.add(grp+"."+it);
			}
		}
		return ret;
	}
	protected String getTableName(String bean) throws Exception {
		return getTableName(bean,false);
	}
	
	protected String getTableName(String bean, boolean prepare) throws Exception {
		if (bean == null)
			return null;
		String pb = getPureBean(bean);
		String tbl = tableMap.get(pb);
		if (tbl == null) {
			BeanFactory bf = BeanFactory.getBeanFactory();
			try {
				Class c = CLASS.forName(bf.findRealClass(pb));
				for (;;) {
					tbl = tableMap.get(CLASS.getSimpleName(c));
					if (tbl == null) {
						SQLTable t = (SQLTable) c.getAnnotation(SQLTable.class);
						if (t != null) {
							tbl = t.value();
							break;
						}
					} else
						break;
					Class su = c.getSuperclass();
					if (su == null || su.equals(Object.class) || su.equals(Entity.class))
						break;
					c = su;
				}
			} catch (Exception e) {
				LogUtil.dumpStackTrace(e);
			}
			if (tbl == null) {
				tbl = tableMap.get("Entity");
			}
			else{
				if (prepare) prepareTableFor(bean, tbl);
			}
		}
		return tbl;
	}

	protected String getColumnName(String tbl, String encodeCol) {
		if (LightStr.isEmpty(tbl)) {
			return LightStr.decamel(encodeCol);
		}
		Map<String, String> map = columnMap.get(tbl);
		if (map != null) {
			String col = map.get(encodeCol);
			if (col == null)
				col = encodeCol;
			encodeCol = col;
		}
		return encodeCol.toUpperCase();
	}

	protected String getEncodedName(String tbl, String col) {
		if (LightStr.isEmpty(tbl)) {
			return LightStr.camel(col);
		}
		if (col.equals("XML_"))
			return "xml_";
		if (col.equals("CLASS_"))
			return "class_";
		Map<String, String> mapx = columnMapX.get(tbl);
		if (mapx == null)
			return col;
		String name = mapx.get(col.toUpperCase());
		if (name == null)
			name = col;
		return name;
	}

	protected abstract boolean tableExist(Connection conn, String tbl) throws Exception;

	protected abstract void createBasicTable(Connection conn, String tbl) throws Exception;

	protected abstract boolean upgradeBasicTable(Connection conn, String bean, String tbl) throws Exception;

	protected void upgradeData(Connection conn, String bean) throws Exception {
		String tbl = getTableName(bean);
		updateUuid(tbl);
		runDdl("update " + tbl + " set " + TBL + "='" + LightUtil.getCascadeName(getBeanClass(bean)) + "' where "
				+ TBL + " is null");
	}

	protected abstract void updateUuid(String tbl) throws Exception;

	protected abstract boolean indexExist(Connection conn, String idx) throws Exception;

	protected abstract boolean triggerExist(Connection conn, String trigger) throws Exception;

	protected abstract boolean functionExist(Connection conn, String fun) throws Exception;

	protected abstract boolean columnExist(Connection conn, String tbl, String col) throws Exception;

	protected abstract boolean _fulltextExist(Connection conn, String tbl, String col) throws Exception;

	protected synchronized boolean fulltextExist(Connection conn, String tbl, String col) throws Exception {
		Boolean exist = fulltextFields.get((tbl + "." + col).toUpperCase());
		if (exist == null) {
			exist = _fulltextExist(conn, tbl, col);
			fulltextFields.put((tbl + "." + col).toUpperCase(), exist);
		}
		return exist;
	}

	protected abstract void upgradeBasicIndex(Connection conn, String bean) throws Exception;

	protected void installBasicFunction(Connection conn) throws Exception {

	}

	protected void updateSchema(Connection conn, String tbl) throws Exception {
		ResultSet mm = conn.getMetaData().getColumns(null, null, tbl, null);
		List<String> list = knownColumns.get(tbl);
		if (list == null) {
			list = new ArrayList<String>();
			knownColumns.put(tbl, list);
		}
		while (mm.next()) {
			String col = mm.getString("column_name").toUpperCase();
			if (list.indexOf(col) < 0)
				list.add(col);
		}
		// System.out.println("known fields of "+tbl+"="+list);
	}

	protected void prepareTableFor(String bean, String tbl) throws Exception {
		if (preparedBeans.contains(bean))
			return;
		Connection conn = connect();
		try {
			prepareTableFor(conn, bean);
		} finally {
			disconnect(conn);
		}
	}

	// raw means from bootup, or is recursive
	protected void prepareTableFor(Connection conn, String bean) throws Exception {
		BeanFactory bf = BeanFactory.getBeanFactory();
		ThreadHelper.set("$$$" + bean, true);
		String tbl = getTableName(bean);
		preparedBeans.add(bean);
		// System.out.println("Prepare Table "+tbl);
		if (!tableExist(conn, tbl)) {
			System.out.println("Create Table " + tbl);
			createBasicTable(conn, tbl);
		} else {
			if (upgradeBasicTable(conn, bean, tbl)) {
				// System.out.println("~~~~~~~~~~~~~~~~~~~updatedata "+tbl);
				upgradeData(conn, bean);
			}
		}
		upgradeBasicIndex(conn, bean);
		if (reindexing) {
			// process bean special fields
			
			Class c = CLASS.forName(bf.findRealClass(bean));
			List<Field> fields = ReflectUtil.getFieldMap(c);
			for (Field f : fields) {
				if (f.getName().equals("dataService"))
					continue;
				if (f.getName().equals("old"))
					continue;
				SQLSize sz = f.getAnnotation(SQLSize.class);
				DecimalPrecision dp = f.getAnnotation(DecimalPrecision.class);
				Integer size = null;
				if (sz != null) {
					size = sz.value();
					if (size <= 0)
						size = null;
				}
				Integer pre = null;
				if (dp != null)
					pre = dp.value();
				verifyColumn(conn, bean, this.getFieldName(f), size, pre);
			}
		}
		ThreadHelper.set("$$$" + bean, null);
	}

	protected abstract void syncTrigger(Connection conn, String bean, boolean force) throws Exception;

	public void markActive(Class cls) throws Exception {
		markActive(CLASS.getSimpleName(cls));
		// clearCache(cls);
	}

	public void markActive(String bean) throws Exception {
		String tbl = getTableName(bean);
		if (activeTables.indexOf(tbl) >= 0)
			return;
		activeTables.add(tbl);
	}

	// here fld should be t_name format
	protected abstract int addColumn(Connection conn, String bean, String fld, Integer size, Integer precision)
			throws Exception; // -1=not support, 0=already 1=new col, 2:new
								// index 3:new col+index

	protected synchronized int verifyColumn(Connection conn, String bean, String fld, Integer size, Integer precision)
			throws Exception {
		if (fld.equals(UniqueID))
			return 0;
		if (fld.length() > 2 && fld.charAt(1) != '_')
			return -1;
		String tbl = getTableName(bean,true);
		int ret = addColumn(conn, bean, fld, size, precision);
		// System.out.println("VERIFYCOLUMN "+bean+", "+fld+", exists="+ret+",
		// t="+(System.currentTimeMillis()-t0));
		this.setColumnMapping(bean, fld, getColumnName(tbl, fld), false);
		return ret;
	}

	/*
	 * public Timestamp getDate(Date dt){ if (dt==null) return null; Calendar
	 * c=Calendar.getInstance(); c.setTime(dt); Timestamp ts=new
	 * java.sql.Timestamp(c.getTimeInMillis()-c.get(Calendar.ZONE_OFFSET)-c.get(
	 * Calendar.DST_OFFSET)); return ts; }
	 */

	private boolean isGenField(String bean, String fld) {
		return gens.contains(bean + "$" + fld);
	}

	// vals==null stands for update or insert
	protected void recordFilter(Connection conn, String bean, String tbl, XmlNode node, StringBuffer flds,
			StringBuffer vals, List<Object> values) throws Exception {
		List<String> toRemove = new ArrayList<String>();
		for (String fld : node.getAttributeKeySet()) {
			String val = node.getAttribute(fld);
			if (verifyColumn(conn, bean, fld, null, null) < 0) continue; // -1 not a required column 0 not exists
			String col = getColumnName(tbl, fld);
			if (isGenField(bean, fld))
				continue;
			toRemove.add(fld);
			if (fld.equals(UniqueID))
				continue;

			if (vals != null) { // insert
				if (values.size() > 0) {
					flds.append(",");
					vals.append(",");
				}
				flds.append(col);
				vals.append("?");
			} else {
				if (values.size() > 0)
					flds.append(",");
				flds.append(col + "=?"); // upd
			}

			if (fld.startsWith(DATEINDEX_)) {
				if (!LightStr.isEmpty(val)) {
					java.sql.Date ts = LightUtil.decodeShortDate(val);
					values.add(ts);
					// System.out.println("DATE "+fld+", val="+val+",
					// ts="+ts.toGMTString());
				} else
					values.add(null);
			} else if (fld.startsWith(TIMESTAMP_)) {
				if (!LightStr.isEmpty(val)) {
					Timestamp ts = LightUtil.decodeLongDate(val);
					values.add(ts);
				} else
					values.add(null);
			} else if (fld.startsWith(NUMINDEX_)) {
				Double d = LightUtil.decodeDouble(val);
				if (d == null)
					d = 0d;
				values.add(d);
			} else if (fld.startsWith(FULLBODY_) || fld.startsWith(FULLTEXT_)) {
				values.add(splitWord(val,false));
			} else
				values.add(val);
		}
		if (!isExclusive()){
			for (String f : toRemove) {
				node.removeAttribute(f);
			}
		}
	}

	protected Timestamp timestampToDb(Timestamp tm) { // MySQL need to override
														// to convert to local
														// to store.
		return tm;
	}

	protected Timestamp timestampFromDb(Timestamp tm) { // MySQL need to
														// override to convert
														// to local to store.
		return tm;
	}

	@Override
	public void _insertBean(String clusterId, String beans, String uniqueId, String xml) throws Exception {
		String bean = getPureBean(beans); // ENTITY USER
		registerBean(bean,null,null);
		
		Connection conn = connect();
		StringBuffer flds = new StringBuffer(), vals = new StringBuffer();
		List<Object> values = new ArrayList<Object>();
		XmlParser p = new XmlParser(new StringReader(xml));
		XmlNode node = p.readNode();
		String tbl = this.getTableName(bean);
		
		recordFilter(conn, bean, tbl, node, flds, vals, values);

		if (values.size() > 0) {
			flds.append(",");
			vals.append(",");
		}
		flds.append(UniqueID + "," + XML + "," + TBL);
		vals.append("?,?,?");
		values.add(uniqueId);
		values.add(node.toXml());
		values.add(beans);

		String sql = "insert into " + dbo() + tbl + "(" + flds + ") values(" + vals + ")";

		// System.out.println("insert sql="+sql);
		PreparedStatement st = conn.prepareStatement(sql);// ,
															// Statement.RETURN_GENERATED_KEYS);
		try {
			for (int i = 0; i < values.size(); i++) {
				Object v = values.get(i);
				if ("NULL".equals(v))
					v = null;
				// System.out.println("insert v"+i+"="+v);
				if (v != null && v instanceof Date) {
					Date dt = (Date) v;
					Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
					st.setDate(i + 1, dt, c);
					// System.out.println(" asDate="+((Date)v).toGMTString());
				} else if (v != null && v instanceof Timestamp) {
					Timestamp dt = (Timestamp) v;
					Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
					st.setTimestamp(i + 1, timestampToDb(dt), c);
					// System.out.println(" ts asDate="+dt.toGMTString()+",
					// dt="+dt);
				} else
					st.setObject(i + 1, v);
			}
			st.executeUpdate();
		} catch (Exception e) {
			LogUtil.dumpStackTrace(e);
			LogUtil.log("SQL=" + sql + ",values=" + values);
			LightUtil.getCfg().processSQLException(e, null);
		} finally {
			st.close();
			disconnect(conn);
		}
		markActive(bean);
	}

	protected abstract SqlSegment contains(String fld, String val);

	protected abstract SqlSegment freetext(String[] fieldList, String val);

	protected abstract String dbo();

	private SqlSegment myTbl(SqlSegment seg, String fullName, String pureName) {
		if (seg == null)
			seg = new SqlSegment();
		//if (!supportFullText(pureName))
		if (asyncMode == null || asyncMode) {
			seg.add(fullName);
			seg.add(fullName + "!");
			seg.set(TBL + ">=? and " + TBL + "<?");
		} else {
			seg.replaceBy(contains(TBL, pureName));
		}
		return seg;
	}

	@Override
	public void _deleteBean(String clusterId, String beans, String uniqueId) throws Exception {
		// System.out.println("beans="+beans);
		String bean = getPureBean(beans);
		String tbl = this.getTableName(bean);
		Connection conn = connect();
		StringBuffer where = new StringBuffer();
		List<Object> values = new ArrayList<Object>();
		where.append(UniqueID + "=?");
		values.add(uniqueId);
		if (!LightStr.isEmpty(bean)) {
			SqlSegment seg = myTbl(null, beans, bean);
			values.addAll(seg.vals);
			where.append(" and " + seg.sql);
		}
		String sql = "delete from " + tbl + " where " + where;
		// System.out.println("delete sql="+sql+",values="+values);
		PreparedStatement st = conn.prepareStatement(sql);
		try {
			for (int i = 0; i < values.size(); i++) {
				st.setObject(i + 1, values.get(i));
			}
			st.execute();
			int n = st.getUpdateCount();
		} catch (Exception e) {
			LogUtil.dumpStackTrace(e);
			LogUtil.log("SQL=" + sql + ",values=" + values);
			LightUtil.getCfg().processSQLException(e,null);
		} finally {
			st.close();
			disconnect(conn);
		}
		markActive(bean);
	}

	@Override
	public void _updateBean(String clusterId, String beans, String uniqueId, String old, String xml) throws Exception {
		XmlNode now = new XmlParser(new StringReader(xml)).readNode();
		XmlNode base = new XmlParser(new StringReader(old)).readNode();

		boolean amended = false;

		for (String ff : base.getAttributeKeySet()) {
			if (now.getAttribute(ff) == null) {
				now.setAttribute(ff, "NULL");
				amended = true;
			}
		}

		if (amended) {
			xml = now.toXml();
		}

		// //System.out.println("UUUPDATE TBL="+beans);
		String bean = getPureBean(beans);
		registerBean(bean,null,null);
		
		String tbl = this.getTableName(bean);
		Connection conn = connect();
		StringBuffer sets = new StringBuffer(), where = new StringBuffer();
		List<Object> values = new ArrayList<Object>();

		XmlParser p = new XmlParser(new StringReader(xml));
		XmlNode node = p.readNode();

		recordFilter(conn, bean, tbl, node, sets, null, values);

		if (values.size() > 0)
			sets.append(",");
		sets.append(XML + "=?," + TBL + "=?");
		values.add(node.toXml());
		values.add(beans);

		where.append(UniqueID + "=?");
		values.add(uniqueId);
		String sql = "update " + dbo() + tbl + " set " + sets + " where " + where;
		// System.out.println("update sql="+sql+",values="+values);
		PreparedStatement st = conn.prepareStatement(sql);
		try {
			for (int i = 0; i < values.size(); i++) {
				Object v = values.get(i);
				if ("NULL".equals(v))
					v = null;
				if (v != null && v instanceof Date) {
					Date dt = (Date) v;
					Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
					st.setDate(i + 1, dt, c);
					// System.out.println(" asDate="+((Date)v).toGMTString());
				} else if (v != null && v instanceof Timestamp) {
					Timestamp dt = (Timestamp) v;
					Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
					st.setTimestamp(i + 1, timestampToDb(dt), c);
					// System.out.println(" ts asDate="+dt.toGMTString()+",
					// dt="+dt);
				} else
					st.setObject(i + 1, v);
			}
			st.execute();
		} catch (Exception e) {
			throw new Exception(e.getMessage());
		} finally {
			st.close();
			disconnect(conn);
		}
		markActive(bean);
	}

	protected abstract boolean doCommit(Connection conn, String tbl) throws Exception;

	@Override
	public void _rollbackTransaction(String transId) throws Exception {
		Connection conn = (Connection) GlbHelper.get("conn-" + transId + "-" + hashCode());
		// System.out.println("ROLLBACK conn="+conn);
		if (conn == null)
			return;
		conn.rollback();
		conn.setAutoCommit(true);
		_disconnect(conn);
		GlbHelper.remove("conn-" + transId + "-" + hashCode());
	}

	@Override
	public void _commitTransaction(String transId) throws Exception {
		Connection conn = (Connection) GlbHelper.get("conn-" + transId + "-" + hashCode());
		if (conn == null)
			return;

		conn.commit();
		conn.setAutoCommit(true);
		List<String> list = new ArrayList<String>(activeTables);
		activeTables.clear();
		for (String tbl : list) {
			if (asyncMode != null && asyncMode == false) {
				while (!doCommit(conn, tbl))
					Thread.sleep(25);
			}
		}
		_disconnect(conn);
		GlbHelper.remove("conn-" + transId + "-" + hashCode());
	}

	protected abstract String toShortDate(Date dt);

	protected abstract String toLongDate(Timestamp dt);

	private String getVal(String v, boolean str) {
		if (v.startsWith("$$$"))
			return v.substring(3);
		return v;
	}

	public String toDateColumn(String col) {
		return col;
	}

	public String getLikeExpression(String col, String patStr) {
		return col + " like " + patStr;
	}

	protected SqlSegment getValueSql(String tbl, String op, String fld, String val) throws Exception {
		if (val.indexOf("||") >= 0) {
			String[] vals = val.split("\\|\\|");
			SqlSegment s1 = new SqlSegment();
			for (String v : vals) {
				if (v.length() == 0)
					continue;
				SqlSegment sub = getValueSql(tbl, op, fld, v);
				s1.merge(sub, " or ");
			}
			return s1;
		}
		if (val.indexOf("&&") >= 0) {
			String[] vals = val.split("\\&\\&");
			SqlSegment s1 = new SqlSegment();
			for (String v : vals) {
				if (v.length() == 0)
					continue;
				SqlSegment sub = getValueSql(tbl, op, fld, v);
				s1.merge(sub, " and ");
			}
			return s1;
		}
		String col = this.getColumnName(tbl, fld);
		if (val.startsWith("!")) {
			SqlSegment sub = getValueSql(tbl, op, fld, val.substring(1));
			if (sub.length() > 0) {
				sub.set("NOT (" + sub.sql + ")");
			}
			return sub;
		}
		if (val.equalsIgnoreCase("null")) {
			SqlSegment s1 = new SqlSegment();
			s1.set("(" + col + " is null or " + col + "='NULL' or " + col + "='')");
			return s1;
		}
		if (val.startsWith("#")) {
			SqlSegment s1 = contains(col, val.substring(1));
			return s1;
		}
		if (fld.startsWith(DATEINDEX_)) {
			if (val.startsWith("@")) {
				val = val.substring(1);
			}
			if (val.startsWith("$$$")) {
				val = val.substring(3);
				SqlSegment s1 = new SqlSegment();
				s1.set(toDateColumn(col) + op + val);
				return s1;
			}
			Date d = LightUtil.decodeShortDate(val.toUpperCase());
			SqlSegment s1 = new SqlSegment();
			s1.set(toDateColumn(col) + op + "?");
			s1.add(d);
			return s1;
		}
		if (fld.startsWith(TIMESTAMP_)) {
			if (val.startsWith("@")) {
				val = val.substring(1);
			}
			if (val.startsWith("$$$")) {
				val = val.substring(3);
				SqlSegment s1 = new SqlSegment();
				s1.set(toDateColumn(col) + op + val);
				return s1;
			}
			Timestamp d = LightUtil.decodeLongDate(val.toUpperCase());
			// val=toLongDate(d);
			// System.out.println("TIMESTAMP
			// "+col+",val="+LightUtil.encodeLongDate(d)+",raw="+val);
			SqlSegment s1 = new SqlSegment();
			s1.set(toDateColumn(col) + op + "?");
			s1.add(d);
			return s1;
		}
		if (fld.startsWith(FULLBODY_) || val.contains("*")) {
			SqlSegment s1 = new SqlSegment();
			s1.set(getLikeExpression(col, "?"));
			s1.add(val.replace('*', '%'));
			return s1;
		}

		if (LightStr.isEmpty(val))
			return new SqlSegment(); // "0=0";

		if (op.equals("contains")) {
			SqlSegment s1 = contains(col, val);
			return s1;
		}
		if (op.equals("startsWith")) {
			SqlSegment s1 = new SqlSegment();
			s1.set(getLikeExpression(col, "?"));
			s1.add(getVal(val, true) + "+'%'");
			return s1;
		}
		if (fld.startsWith(NUMINDEX_)) {
			SqlSegment s1 = new SqlSegment();
			s1.set(col + op + "?");
			s1.add(getVal(val, false));
			return s1;
		}
		SqlSegment s1 = new SqlSegment();
		s1.set(col + op + "?");
		s1.add(getVal(val, true));
		return s1;
	}

	private void setTerms(SqlSegment s3, String defaultName, Map<String, Object> terms, Map<String, String> nameMap,
			String tbl) throws Exception {
		for (String nm : terms.keySet()) {
			Object vv = terms.get(nm);
			if (vv == null)
				continue;
			if (vv instanceof Map) {
				Map<String, Object> termx = (Map) vv;
				Object is = termx.get("_is");
				Object iff = termx.get("_if");
				termx.remove("_is");
				termx.remove("_if");
				if (is != null) {
					if (is.equals(iff))
						setTerms(s3, nm, termx, nameMap, tbl);
				} else {
					setTerms(s3, nm, termx, nameMap, tbl);
				}
				continue;
			}
			if (vv instanceof List) {
				List<Object> listx = (List) vv;
				SqlSegment q1 = new SqlSegment();
				for (Object mm : listx) {
					if (mm instanceof Map) {
						Map<String, Object> mmm = (Map<String, Object>) mm;
						SqlSegment q2 = new SqlSegment();
						setTerms(q2, nm, mmm, nameMap, tbl);
						q1.merge(q2, " OR ");
					} else {
						SqlSegment q2 = getValueSql(tbl, "=", getMappedName(nameMap, nm), (String) mm);
						q1.merge(q2, " OR ");
					}
				}
				s3.merge(q1, " and ");
				continue;
			}
			String ss = vv.toString();
			if (LightStr.isEmpty(ss))
				continue;

			String op = "=";
			if (nm.equals("eq")) {
				nm = defaultName;
				op = "=";
			} else if (nm.equals("lt")) {
				nm = defaultName;
				op = "<";
			} else if (nm.equals("le")) {
				nm = defaultName;
				op = "<=";
			} else if (nm.equals("gt")) {
				nm = defaultName;
				op = ">";
			} else if (nm.equals("ge")) {
				nm = defaultName;
				op = ">=";
			} else if (nm.equals("contains")) {
				op = nm;
				nm = defaultName;
			} else if (nm.equals("startsWith")) {
				op = nm;
				nm = defaultName;
			}
			String name = getMappedName(nameMap, nm);
			if (name == null)
				continue;
			SqlSegment sub = getValueSql(tbl, op, name, ss);

			s3.merge(sub, " and ");
		}
	}

	private String getMappedName(Map<String, String> nameMap, String nm) {
		String name = nameMap.get(nm);
		if (nameMap.size() > 0) {
			if (name == null) {
				if (nm.contains("."))
					name = nm;
				else
					return null;
			}
		} else
			name = nm;
		return name;
	}

	protected SqlSegment getSearchSql(Connection conn, String bean, String tbl, String[] fieldList,
			Map<String, Object> terms, String text, String alia) throws Exception {
		List<Map> joins = (List) terms.get("joins");
		if (joins != null) {
			terms.remove("joins");
		}

		Map<String, String> nameMap = new HashMap<String, String>();

		if (!LightStr.isEmpty(bean)) {
			for (String fld : fieldList) {
				if (!LightStr.isEmpty(fld)) {
					verifyColumn(conn, bean, fld, null, null);
					nameMap.put(getSimpleField(fld), fld);
				}
			}
		}

		SqlSegment s1 = new SqlSegment();
		if (bean != null) {
			myTbl(s1, bean, getPureBean(bean));
		}

		SqlSegment s2 = new SqlSegment();
		text = text.trim();
		if (text.length() > 0) {
			s2.replaceBy(freetext(fieldList, text));
		}

		SqlSegment s3 = new SqlSegment();
		setTerms(s3, null, terms, nameMap, tbl);

		SqlSegment q = new SqlSegment();
		q.merge(s1, " and ");
		q.merge(s2, " and ");
		q.merge(s3, " and ");

		if (joins != null && joins.size() > 0) {
			String alia0 = alia;
			int ii = 0;
			Class[] cls = new Class[] { CLASS.forName(BeanFactory.getBeanFactory().findRealClass(getPureBean(bean))) };

			for (Map m : joins) {
				String alia2 = alia0 + "_" + ii + "";
				ii++;
				String bean2 = (String) m.get("names");
				Class[] cls2 = new Class[] {
						CLASS.forName(BeanFactory.getBeanFactory().findRealClass(getPureBean(bean2))) };
				String tbl2 = getTableName(bean2);
				String fields2 = (String) m.get("fields");
				String[] fieldList2 = fields2.split(",");
				Map<String, Object> terms2 = (Map) m.get("terms");

				String by = (String) m.get("by");
				String as = (String) m.get("as");
				if (by.indexOf(',') >= 0) {
					String[] bys = by.split(",");
					String[] ass = as.split(",");
					if (bys.length != ass.length)
						throw new Exception("by/as not match!");
					for (int i = 0; i < bys.length; i++) {
						String byfld = bys[i]; // getFieldName(cls,bys[i])
						String asfld = ass[i]; // getFieldName(cls2,ass[i])
						String byf = this.getColumnName(tbl, byfld);
						String asf = this.getColumnName(tbl2, asfld);
						terms2.put(alia2 + "." + asf, "$$$" + alia0 + "." + byf);
					}
				} else {
					String byf = this.getColumnName(tbl, by); // getFieldName(cls,by)
					// System.out.println("tbl2="+tbl2+",cls2="+cls2[0].getSimpleName()+",as="+as+",
					// fld="+getFieldName(cls2,as));
					String asf = this.getColumnName(tbl2, as); // getFieldName(cls2,as)
					terms2.put(alia2 + "." + asf, "$$$" + alia0 + "." + byf);
				}
				SqlSegment where2 = getSearchSql(conn, bean2, tbl2, fieldList2, terms2, "", alia2);
				if (where2.length() > 0) {
					if (q.length() > 0)
						q.append(" and ");
					q.append("exists (" + "select 1 from " + dbo() + tbl2 + " " + alia2 + " where " + where2.sql + ")");
					q.vals.addAll(where2.vals);
				}
			}
		}
		return q;
	}

	protected boolean isExclusive() {
		if (exclusive==null || !exclusive)
			return false;
		return true;
	}

	protected boolean isSplitBigChar() {
		if (splitBigChar==null || !splitBigChar)
			return false;
		return true;
	}
	
	protected String defaultSort() {
		return "";
	}

	private String getOrderBy(String sortBy) {
		if (!LightStr.isEmpty(sortBy) && !sortBy.equalsIgnoreCase("NULL")) {
			StringBuffer orderby = new StringBuffer();
			String[] sorts = sortBy.split(",");
			for (String sort : sorts) {
				if (orderby.length() == 0)
					orderby.append(" order by ");
				else
					orderby.append(", ");
				sort = sort.trim();
				int i = sort.indexOf(' ');
				String desc = "";
				if (i >= 0) {
					desc = sort.substring(i + 1);
					sort = sort.substring(0, i);
				}
				orderby.append(LightStr.decamel(sort));
				if (desc.equalsIgnoreCase("desc"))
					orderby.append(" desc");
			}
			return orderby.toString();
		}
		return "";
	}

	private String getOrderBy(Class[] cs, String tbl, String sortBy) {
		if (!LightStr.isEmpty(sortBy) && !sortBy.equalsIgnoreCase("NULL")) {
			StringBuffer orderby = new StringBuffer();
			String[] sorts = sortBy.split(",");
			for (String sort : sorts) {
				if (sort.equals("undefined"))
					continue;
				sort = sort.trim();
				int i = sort.indexOf(' ');
				String desc = "";
				if (i >= 0) {
					desc = sort.substring(i + 1);
					sort = sort.substring(0, i);
				}
				sort = getFieldName(cs, sort);
				if (sort != null) {
					if (orderby.length() == 0)
						orderby.append(" order by ");
					else
						orderby.append(", ");
					orderby.append(getColumnName(tbl, sort));
					if (desc.equalsIgnoreCase("desc"))
						orderby.append(" desc");
				}
			}
			return orderby.toString();
		} else {
			return defaultSort();
		}
	}

	protected String union() {
		return "union";
	}

	@Override // sortBy is raw format
	public SearchResult _doSearch(String names, Map<String, Object> terms, String fields, String text, String sortBy,
			int from, int size, String facet) throws Exception {

		SearchResult result = _preSearch(names, terms, fields, text, sortBy, from, size, facet);
		if (result.getAmount() != null)
			return result;

		String[] fieldList = fields.split(",");

		Connection conn = connect();

		// String[] nameList=names.split(",");
		String bean = getSearchBean(names);
		String tbl = getTableName(bean);

		SqlSegment q1;

		Class[] cs = null;

		try {
			if (names.length() == 0) { // a SQL search here
				String sql = "";
				if (text.startsWith("@@")) { // to indicate a mixture sql and
												// free search text
					text = text.substring(2);
					int i = text.indexOf("@@");
					sql = text.substring(0, i);
					text = text.substring(i + 2);
				} else {
					sql = text;
					text = "";
				}
				q1 = resolveSql(sql, terms);
				SqlSegment where = getSearchSql(conn, null, null, fieldList, terms, text, "X_");
				q1.set("select X_.* from (" + q1.sql + ") X_");
				q1.merge(where, " where ");
				q1.append(getOrderBy(sortBy));
			} else {
				Class cls = CLASS.forName(BeanFactory.getBeanFactory().findRealClass(getPureBean(bean)));

				SqlSegment where = getSearchSql(conn, bean, tbl, fieldList, terms, text, "t");

				cs = new Class[] { cls };
				if (this.getPureBean(bean).equals("ParentalEntity")) { // need
																		// to
																		// cross
																		// many
																		// table
																		// to
																		// retrieve
					q1 = new SqlSegment();
					for (String t : parentals) {
						if (q1.length() > 0)
							q1.append("\n" + union() + "\n");
						q1.append("select " + UniqueID + "," + XML + "," + TBL + " from " + dbo() + t + " t");
						q1.merge(where, " where ");
					}
				} else {
					q1 = new SqlSegment();
					q1.set("select t.* from " + dbo() + tbl + " t");
					q1.merge(where, " where ");
					q1.append(getOrderBy(cs, tbl, sortBy));
				}
			}
		} finally {
			disconnect(conn);
		}

		_afterSearch(result, cs, q1, from, size, tbl);

		return result;
	}

	protected void _afterSearch(SearchResult result, Class[] cs, SqlSegment q, int from, int size, String tbl)
			throws Exception {
		// System.out.println("Search Conn="+conn+"
		// SQL="+q.sql+",values="+q.vals+","+conn.isClosed());
		Connection conn = connect();
		PreparedStatement st = null;
		ResultSet rs = null;
		try {
			st = conn.prepareStatement(q.sql.toString(), ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
			for (int i = 0; i < q.vals.size(); i++) {
				Object v = q.vals.get(i);
				if (v instanceof Date) {
					st.setDate(i + 1, (Date) v, LightUtil.getCalendar("GMT"));
				} else if (v instanceof Timestamp) {
					st.setTimestamp(i + 1, (Timestamp) v, LightUtil.getCalendar("GMT"));
				} else
					st.setObject(i + 1, v);
			}
			if (size <= 0)
				size = MAX_RECORD;

			// long t0=System.currentTimeMillis();
			//LogUtil.debug("sql="+q.sql+",values="+q.vals);
			rs = st.executeQuery();
			// System.out.println("st="+st+",size="+size+",from="+from+",row="+rs.getRow());
			// for(;from>0;from--) { if (!rs.next()) break; amount++;}
			boolean ok = rs.absolute(from + 1);
			int i = 0;
			ResultSetMetaData m = rs.getMetaData();
			if (ok)
				while (i < size) {
					Map<String, Object> map = new HashMap<String, Object>();
					if (!isExclusive()) {
						Map<String, String> vals = new HashMap<String, String>();
						Map<String, String> types = new HashMap<String, String>();
						for (int c = 1, len = m.getColumnCount(); c <= len; c++) {
							String val = rs.getString(c);
							// if (val==null) continue;
							String nm = m.getColumnName(c);
							// if (cs!=null && cs.length>0){
							// String n=this.getEncodedName(tbl, nm);
							// if (n!=null) nm=n;
							// }
							int type = m.getColumnType(c);
							if (nm.startsWith(TIMESTAMP_) || type == java.sql.Types.TIMESTAMP) {
								Calendar cal = Calendar.getInstance();
								cal.setTimeZone(TimeZone.getTimeZone("GMT"));

								Timestamp ts = rs.getTimestamp(c, cal);
								if (ts != null) {
									ts = timestampFromDb(ts);
									val = LightUtil.encodeLongDate(ts);
									types.put(nm, "D");
								} else
									val = null;

							} else if (nm.startsWith(DATEINDEX_) || type == java.sql.Types.DATE) {
								Calendar cal = LightUtil.getCalendar();
								cal.setTimeZone(TimeZone.getTimeZone("GMT"));
								Date ts = rs.getDate(c, cal);
								if (ts != null) {
									val = LightUtil.encodeShortDate(ts);
									types.put(nm, "P");
								} else
									val = null;
							} else if (nm.startsWith(FULLTEXT_) || nm.startsWith(FULLBODY_)) {
								val = desplitWord(val);
							}
							else if (nm.equalsIgnoreCase("I_UUID")) nm="i_uuid";
							vals.put(nm, val);
						}
						String xml = cs == null || cs.length == 0 ? null : vals.get(XML);
						vals.remove(XML);
						String beans = cs == null || cs.length == 0 ? null : vals.get(TBL);
						XmlNode node = new XmlNode();
						if (beans != null) {
							map.put(TBL, beans);
							vals.remove(TBL);
						}

						if (beans == null)
							node.setTag("map");
						else if (LightStr.isEmpty(xml)) {
							node.setTag(getPureBean(beans));
						} else {
							XmlParser p = new XmlParser(xml);
							p.parseNode(node);
						}
						//
						registerBean(node.getTag(), null, null);
						//
						for (String name : vals.keySet()) {
							String fld = getEncodedName(tbl, name);
							String val = vals.get(name);
							String type = types.get(name);
							if (type != null) {
								XmlNode sub = node.addChildNode(new XmlNode());
								sub.setTag(fld);
								sub.setAttribute("_", type);
								sub.setText(val);
								node.removeAttribute(fld);
							} else
								node.setAttribute(fld, val);
						}
						// System.out.println("node="+node);
						if (cs == null || cs.length == 0)
							node.removeAttribute("class");
						xml = node.toXml();
						// if (cs==null||cs.length==0)
						// System.out.println("xml="+xml);
						map.put(XML, xml);
					} else {
						map.put(UniqueID, rs.getString(UniqueID));
						map.put(XML, rs.getString(XML));
						map.put(TBL, rs.getString(TBL));
					}
					result.getList().add(map);
					// System.out.println("res.row="+rs.getRow()+",map="+map);
					i++;
					if (!rs.next())
						break;
				}
			// while (rs.next()) amount++;
			int amount = rs.last() ? rs.getRow() : 0;
			// System.out.println("last="+rs.last()+",row="+rs.getRow());
			// long t1=System.currentTimeMillis();
			// LogUtil.info("select
			// cost="+(t1-t0)+",amount="+amount+",list="+result.getList());
			result.setAmount(amount);
		} catch (Exception e) {
			LogUtil.log("Error Conn=" + conn + ",SQL=" + q.sql + ",values=" + q.vals);
			LogUtil.dumpStackTrace(e);
			LightUtil.getCfg().processSQLException(e,null);
		} finally {
			if (rs != null)
				rs.close();
			if (st != null)
				st.close();
			disconnect(conn);
		}
	}

	@Override
	public SearchResult _doSearchBySql(String sql, Map<String, Object> terms, int from, int size, String sortBy,
			String facet) throws Exception {
		SearchResult result = new SearchResult();
		SqlSegment q = resolveSql(sql, terms);
		Connection conn = connect();
		try {
			SqlSegment where = getSearchSql(conn, null, null, null, terms, "", "X_");
			q.set("select X_.* from (" + q.sql + ") X_");
			q.merge(where, " where ");
			q.append(getOrderBy(sortBy));
		} finally {
			disconnect(conn);
		}
		_afterSearch(result, new Class[] {}, q, from, size, null);
		return result;
	}

	public List<String> getPredefinedClassList() {
		return predefinedClassList;
	}

	public void setPredefinedClassList(List<String> predefinedClassList) {
		this.predefinedClassList = predefinedClassList;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public List<TableMapping> getPredefinedMappingList() {
		return predefinedMappingList;
	}

	public void setPredefinedMappingList(List<TableMapping> predefinedMappingList) {
		this.predefinedMappingList = predefinedMappingList;
	}

	public String getServerName() {
		return serverName;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public Boolean getProductionMode() {
		return productionMode;
	}

	public void setProductionMode(Boolean productionMode) {
		this.productionMode = productionMode;
	}

	public boolean isProductionMode(String tbl) {
		if (tbl.equalsIgnoreCase("Entity"))
			return false;
		return productionMode != null && productionMode;
	}

	public Integer getTimeout() {
		return timeout;
	}

	public void setTimeout(Integer timeout) {
		this.timeout = timeout;
	}

	@Override
	public void destroy() {
		if (conn_timer != null)
			conn_timer.cancel();
		List<Connection> list = new ArrayList<Connection>();
		for (Connection c : connections.keySet()) {
			list.add(c);
		}
		for (Connection c : list) {
			try {
				c.close();
			} catch (SQLException e) {
				LogUtil.dumpStackTrace(e);
			}
			connections.remove(c);
		}
	}

	public abstract void dumpTables(File root, String pkg, String filter) throws Exception;

	public Boolean getAsyncMode() {
		return asyncMode;
	}

	public void setAsyncMode(Boolean asyncMode) {
		this.asyncMode = asyncMode;
	}

	public String getDdlFileName() {
		return ddlFileName;
	}

	public void setDdlFileName(String ddlFileName) {
		this.ddlFileName = ddlFileName;
	}

	protected String getVarcharType() {
		return "varchar";
	}

	protected String getDatetimeType() {
		return "datetime";
	}

	protected String getDateType() {
		return "date";
	}

	protected ObjectBuffer _createTempTable(String tblName) {
		return new ObjectBuffer("create " + tblName + "(", ")");
	}

	protected String _dropTempTable(String tblName) {
		return "drop table " + tblName;
	}

	protected String _getTempTable(String tblName) {
		return tblName;
	}

	@Override
	public void dropTempTable(String tblName) throws Exception {
		String sql;
		if (tblName.startsWith("#")) {
			sql = _dropTempTable(tblName.substring(1));
		} else {
			sql = "drop table " + tblName;
		}
		runDdl(sql);
	}

	@Override
	public String createTempTable(String tblName, List<Map> fields, List<Map> data) throws Exception {
		try {
			dropTempTable(tblName);
		} catch (Exception e) {
		}

		ObjectBuffer sql;
		if (tblName.startsWith("#")) {
			sql = _createTempTable(tblName.substring(1));
		} else {
			sql = new ObjectBuffer("create table " + tblName + "(", ")");
		}

		String tbl = _getTempTable(tblName);

		int count = 0;
		for (Map f : fields) {
			String type = (String) f.get("fieldType");
			String def = (String) f.get("default");
			if (def == null || def.equalsIgnoreCase("null"))
				def = "";
			if ("C".equals(type)) {
				sql.append("", f.get("fieldName") + " " + getVarcharType() + "(" + f.get("fieldSize") + ") default '"
						+ def + "'");
			} else if ("I".equals(type)) {
				sql.append("", f.get("fieldName") + " integer default " + LightUtil.decodeInt(def));
			} else if ("N".equals(type)) {
				sql.append("", f.get("fieldName") + " numeric(" + f.get("fieldSize") + "," + f.get("decimalPrecision")
						+ ") default " + LightUtil.decodeDouble(def));
			} else if ("D".equals(type)) {
				sql.append("", f.get("fieldName") + " " + getDateType());
			} else if ("T".equals(type)) {
				sql.append("", f.get("fieldName") + " " + getDatetimeType());
			} else
				continue;
			count++;
		}
		this.runDdl(sql.toString());
		for (Map row : data) {
			int i = 0;
			ObjectBuffer insert = new ObjectBuffer("insert into " + tbl + "(", ")");
			ObjectBuffer values = new ObjectBuffer("values(", ")");
			List<Object> params = new ArrayList<Object>();
			for (Map f : fields) {
				String fld = LightStr.camel((String) f.get("fieldName"));
				Object val = row.get(fld);
				if (val == null)
					continue;
				String type = (String) f.get("fieldType");
				if ("C".equals(type)) {
				} else if ("I".equals(type)) {
					if (val instanceof Integer)
						;
					else
						val = LightUtil.decodeInt(val.toString());
				} else if ("N".equals(type)) {
					if (val instanceof Double)
						;
					else
						val = LightUtil.decodeDouble(val.toString());
				} else if ("D".equals(type)) {
					if (val instanceof Date)
						;
					else
						val = LightUtil.decodeShortDate(val.toString());
				} else if ("T".equals(type)) {
					if (val instanceof Timestamp)
						;
					else
						val = LightUtil.decodeLongDate(val.toString());
				} else
					continue;
				insert.append("", f.get("fieldName") + "");
				values.append("", "?");
				// System.out.println("fld="+fld+" val="+row.get(fld)+"
				// row="+row);
				params.add(val);
				i++;
			}
			if (i == 0)
				continue;
			Object[] pp = new Object[params.size()];
			params.toArray(pp);
			this.runDdl(insert.toString() + " " + values.toString(), pp);
		}

		return tbl;
	}

	public String getSanityTestSql() {
		return "select 1";
	}

	public boolean sanityTest(ConnectionPool p) {
		long tick = System.currentTimeMillis();
		if (tick - p.lastTick < 5000)
			return true; // if access before last successful one, less then 5
							// seconds, no sanity test needed
		Statement st = null;
		// System.out.println("Sanity Test @"+p.hashCode());
		try {
			st = p.conn.createStatement();
			st.execute("select 1");
		} catch (Exception e) {
			return false;
		} finally {
			if (st != null)
				try {
					st.close();
				} catch (Exception e) {
					return false;
				}
		}
		return true;
	}

	// convert Chinese Char to \\uxxxx
	protected String splitWord(String val, boolean query) {
		if (val==null || !isSplitBigChar()) return val;
		String st=StringEscapeUtils.escapeJava(val);
		if (query) st=st.replace('\\', ' ');
		return st;
	}

	protected String desplitWord(String val) {
		return StringEscapeUtils.unescapeJava(val);
	}

	public Map<String, String> getParams() {
		return params;
	}

	public void setParams(Map<String, String> params) {
		this.params = params;
	}

	public Map<Connection, ConnectionPool> getConnections() {
		return connections;
	}

	@Override
	public void validate() throws Exception {
		getSql("select 1");
	}

	public Map<String, String> getTableMap() {
		return tableMap;
	}

	public Boolean getExclusive() {
		return exclusive;
	}

	public void setExclusive(Boolean exclusive) {
		this.exclusive = exclusive;
	}

	public Boolean getSplitBigChar() {
		return splitBigChar;
	}

	public void setSplitBigChar(Boolean splitBigChar) {
		this.splitBigChar = splitBigChar;
	}
}

class ConnectionPool {
	Connection conn;
	long lastTick;
	boolean idle;

	public ConnectionPool(Connection conn) {
		this.conn = conn;
		lastTick = System.currentTimeMillis();
		idle = false;
	}

	@Override
	public String toString() {
		return "{Pool " + conn + ", idle=" + idle + ", life=" + (System.currentTimeMillis() - lastTick) + "}";
	}
}
