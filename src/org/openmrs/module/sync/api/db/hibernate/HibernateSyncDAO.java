/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.sync.api.db.hibernate;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.openmrs.GlobalProperty;
import org.openmrs.OpenmrsObject;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.DAOException;
import org.openmrs.module.sync.SyncClass;
import org.openmrs.module.sync.SyncConstants;
import org.openmrs.module.sync.SyncRecord;
import org.openmrs.module.sync.SyncRecordState;
import org.openmrs.module.sync.SyncStatistic;
import org.openmrs.module.sync.SyncUtil;
import org.openmrs.module.sync.api.db.SyncDAO;
import org.openmrs.module.sync.ingest.SyncImportRecord;
import org.openmrs.module.sync.server.RemoteServer;
import org.openmrs.module.sync.server.RemoteServerType;
import org.openmrs.util.OpenmrsConstants;

public class HibernateSyncDAO implements SyncDAO {

    protected final Log log = LogFactory.getLog(getClass());

    /**
     * Hibernate session factory
     */
    private SessionFactory sessionFactory;
    
    public HibernateSyncDAO() { }
    
    /**
     * Set session Factory interceptor
     * 
     * @param sessionFactory
     */
    public void setSessionFactory(SessionFactory sessionFactory) { 
        this.sessionFactory = sessionFactory;
    }
    
    /**
     * @see org.openmrs.module.sync.api.db.SyncDAO#createSyncRecord(org.openmrs.module.sync.SyncRecord)
     */
    public void createSyncRecord(SyncRecord record) throws DAOException {
        if (record.getUuid() == null) {
            //TODO: Create Uuid if missing?
            throw new DAOException("SyncRecord must have a UUID");
        }
        
        Session session = sessionFactory.getCurrentSession();
        session.save(record);
    }

    /**
     * @see org.openmrs.module.sync.api.db.SyncDAO#updateSyncRecord(org.openmrs.module.sync.SyncRecord)
     */
    public void updateSyncRecord(SyncRecord record) throws DAOException {
        Session session = sessionFactory.getCurrentSession();
        session.saveOrUpdate(record);
    }

    /**
     * @see org.openmrs.module.sync.api.db.SyncDAO#deleteSyncRecord(org.openmrs.module.sync.SyncRecord)
     */
    public void deleteSyncRecord(SyncRecord record) throws DAOException {
        Session session = sessionFactory.getCurrentSession();
        session.delete(record);
    }

    /**
     * @see org.openmrs.module.sync.api.db.SyncDAO#createSyncImportRecord(org.openmrs.module.sync.engine.SyncImportRecord)
     */
    public void createSyncImportRecord(SyncImportRecord record) throws DAOException {
        if (record.getUuid() == null) {
            //TODO: Create Uuid if missing?
            throw new DAOException("SyncImportRecord must have a UUID");
        }
        Session session = sessionFactory.getCurrentSession();
        session.save(record);
    }

    /**
     * @see org.openmrs.module.sync.api.db.SyncDAO#updateSyncImportRecord(org.openmrs.module.sync.engine.SyncImportRecord)
     */
    public void updateSyncImportRecord(SyncImportRecord record) throws DAOException {
        Session session = sessionFactory.getCurrentSession();
        session.merge(record);
    }

    /**
     * @see org.openmrs.module.sync.api.db.SyncDAO#deleteSyncImportRecord(org.openmrs.module.sync.engine.SyncImportRecord)
     */
    public void deleteSyncImportRecord(SyncImportRecord record) throws DAOException {
        Session session = sessionFactory.getCurrentSession();
        session.delete(record);
    }
    
    /**
     * @see org.openmrs.module.sync.api.db.SyncDAO#getNextSyncRecord()
     */
    @SuppressWarnings("unchecked")
    public SyncRecord getFirstSyncRecordInQueue() throws DAOException {
        List<SyncRecord> result = sessionFactory.getCurrentSession()
            .createCriteria(SyncRecord.class)
            .add(Restrictions.in("state", new SyncRecordState[]{SyncRecordState.NEW, SyncRecordState.PENDING_SEND}))
            .addOrder(Order.asc("timestamp"))
            .addOrder(Order.asc("recordId"))
            .setFetchSize(1)
            .list();
        
        if (result.size() < 1) {
            return null;
        } else {
            return result.get(0);
        }
    }

    /**
     * @see org.openmrs.module.sync.api.db.SyncDAO#getNextSyncRecord()
     */
    @SuppressWarnings("unchecked")
    public SyncRecord getLatestRecord() throws DAOException {
        List<SyncRecord> result = sessionFactory.getCurrentSession()
            .createCriteria(SyncRecord.class)
            .addOrder(Order.desc("timestamp"))
            .addOrder(Order.desc("recordId"))
            .setFetchSize(1)
            .list();
        
        if (result.size() < 1) {
            return null;
        } else {
            return result.get(0);
        }
    }

    /**
     * @see org.openmrs.module.sync.api.db.SyncDAO#getSyncRecord(java.lang.String)
     */
    public SyncRecord getSyncRecord(String uuid) throws DAOException {
        return (SyncRecord) sessionFactory.getCurrentSession()
        		.createCriteria(SyncRecord.class)
        		.add(Restrictions.eq("uuid", uuid)) 
        		.uniqueResult();
    }

    public SyncRecord getSyncRecordByOriginalUuid(String originalUuid) throws DAOException {
        return (SyncRecord) sessionFactory.getCurrentSession()
                .createCriteria(SyncRecord.class)
                .add(Restrictions.eq("originalUuid", originalUuid)) 
                .uniqueResult();
    }

    /**
     * @see org.openmrs.module.sync.api.db.SyncDAO#getSyncImportRecord(java.lang.String)
     */
    public SyncImportRecord getSyncImportRecord(String uuid) throws DAOException {
        return (SyncImportRecord) sessionFactory.getCurrentSession()
        		.createCriteria(SyncImportRecord.class)
        		.add(Restrictions.eq("uuid", uuid))
        		.uniqueResult();
    }
    
    /**
     * @see org.openmrs.module.sync.api.db.SyncDAO#getSyncImportRecords(org.openmrs.module.sync.engine.SyncRecordState)
     */
    @SuppressWarnings("unchecked")
    public List<SyncImportRecord> getSyncImportRecords(SyncRecordState state) throws DAOException {
        return sessionFactory.getCurrentSession()
            .createCriteria(SyncImportRecord.class)
            .add(Restrictions.eq("state", state))
            .addOrder(Order.asc("timestamp"))
            .addOrder(Order.asc("recordId"))
            .list();
    }

    /**
     * @see org.openmrs.module.sync.api.db.SyncDAO#getSyncRecords()
     */
    @SuppressWarnings("unchecked")
    public List<SyncRecord> getSyncRecords() throws DAOException {
        return sessionFactory.getCurrentSession()
            .createCriteria(SyncRecord.class)
            .addOrder(Order.asc("timestamp"))
            .addOrder(Order.asc("recordId"))
            .list();
    }

    /**
     * @see org.openmrs.module.sync.api.db.SyncDAO#getSyncRecords(org.openmrs.module.sync.engine.SyncRecordState)
     */
    @SuppressWarnings("unchecked")
    public List<SyncRecord> getSyncRecords(SyncRecordState state) throws DAOException {
        return sessionFactory.getCurrentSession()
            .createCriteria(SyncRecord.class)
            .add(Restrictions.eq("state", state))
            .addOrder(Order.asc("timestamp"))
            .addOrder(Order.asc("recordId"))
            .list();
    }

    /**
     * @see org.openmrs.module.sync.api.db.SyncDAO#getSyncRecords(org.openmrs.module.sync.engine.SyncRecordState)
     */
    @SuppressWarnings("unchecked")
    public List<SyncRecord> getSyncRecords(SyncRecordState[] states, boolean inverse) throws DAOException {
    	String maxResultsString = Context.getAdministrationService().getGlobalProperty(SyncConstants.PROPERTY_NAME_MAX_RECORDS);
    	int maxResults = 0;
    	
    	if (maxResultsString == null) {
    		maxResults = Integer.parseInt(SyncConstants.PROPERTY_NAME_MAX_RECORDS_DEFAULT);
    	} else {
    		maxResults = Integer.parseInt(maxResultsString);
    	}

    	if (maxResults < 1) {
    		maxResults = Integer.parseInt(SyncConstants.PROPERTY_NAME_MAX_RECORDS_DEFAULT);
    	}
    	
    	if ( inverse ) {
            return sessionFactory.getCurrentSession()
            .createCriteria(SyncRecord.class)
            .add(Restrictions.not(Restrictions.in("state", states)))
            .addOrder(Order.asc("timestamp"))
            .addOrder(Order.asc("recordId"))
            .setMaxResults(maxResults)
            .list();
    	} else {
            return sessionFactory.getCurrentSession()
            .createCriteria(SyncRecord.class)
            .add(Restrictions.in("state", states))
            .addOrder(Order.asc("timestamp"))
            .addOrder(Order.asc("recordId"))
            .setMaxResults(maxResults)
            .list();
    	}
    }

    @SuppressWarnings("unchecked")
    public List<SyncRecord> getSyncRecords(SyncRecordState[] states, boolean inverse, RemoteServer server) throws DAOException {
    	String maxResultsString = Context.getAdministrationService().getGlobalProperty(SyncConstants.PROPERTY_NAME_MAX_RECORDS);
    	int maxResults = 0;
    	
    	if (maxResultsString == null) {
    		maxResults = Integer.parseInt(SyncConstants.PROPERTY_NAME_MAX_RECORDS_DEFAULT);
    	} else {
    		maxResults = Integer.parseInt(maxResultsString);
    	}

    	if (maxResults < 1) {
    		maxResults = Integer.parseInt(SyncConstants.PROPERTY_NAME_MAX_RECORDS_DEFAULT);
    	}

    	if ( inverse ) {
            return sessionFactory.getCurrentSession()
            .createCriteria(SyncRecord.class, "s")
            .createCriteria("serverRecords", "sr")
            .add(Restrictions.not(Restrictions.in("sr.state", states)))
            .add(Restrictions.eq("sr.syncServer", server))
            .addOrder(Order.asc("s.timestamp"))
            .addOrder(Order.asc("s.recordId"))
            .setMaxResults(maxResults)
            .list();
        } else {
            return sessionFactory.getCurrentSession()
            .createCriteria(SyncRecord.class, "s")
            .createCriteria("serverRecords", "sr")
            .add(Restrictions.in("sr.state", states))
            .add(Restrictions.eq("sr.syncServer", server))
            .addOrder(Order.asc("s.timestamp"))
            .addOrder(Order.asc("s.recordId"))
            .setMaxResults(maxResults)
            .list();
        }
    }
    
    /**
	 * @see org.openmrs.module.sync.api.db.SyncDAO#deleteSyncRecords(org.openmrs.module.sync.SyncRecordState[],
	 *      java.util.Date)
	 */
	public Integer deleteSyncRecords(SyncRecordState[] states, Date to) throws DAOException {
//		Query serverRecordsQuery = sessionFactory.getCurrentSession().createQuery(
//		    "delete from SyncServerRecord where state in (:states) and syncRecord.timestamp < :to");
//		serverRecordsQuery.setParameterList("states", states);
//		serverRecordsQuery.setDate("to", to);
//		serverRecordsQuery.executeUpdate();
		
		List<String> stateStrings = new ArrayList<String>();
		for (SyncRecordState s : states) {
			stateStrings.add(s.name());
		}
		
		
//		Query tmp = sessionFactory.getCurrentSession().createSQLQuery("select sr.record_id from sync_record sr where (select count(*) from sync_server_record ssr where ssr.state in (:states) and ssr.record_id = sr.record_id) = 1 and sr.timestamp <= :to");
//		tmp.setParameterList("states", stateStrings);
//		tmp.setDate("to", to);
//		List<Object> rows = tmp.list();
		
		// delete all rows in sync_server_id that are of the right state and are old
		Query deleteSSRQuery = sessionFactory
	        .getCurrentSession()
	        .createSQLQuery("delete from sync_server_record ssr where ssr.state in (:states) and (select timestamp from sync_record sr where sr.record_id = ssr.record_id) < :to");
		deleteSSRQuery.setParameterList("states", stateStrings);
		deleteSSRQuery.setDate("to", to);
		Integer quantityDeleted = deleteSSRQuery.executeUpdate(); // this quantity isn't really used
		
		// if a sync_record now has zero sync_record_server rows, then that means all
		// the rows were deleted in the previous query and so the sync_record can also be deleted
		Query deleteQuery = sessionFactory
		        .getCurrentSession()
		        .createSQLQuery(
		            "delete from sync_record sr where (select count(*) from sync_server_record ssr where ssr.record_id = sr.record_id) = 0 and sr.timestamp <= :to");
		deleteQuery.setDate("to", to);
		quantityDeleted = deleteQuery.executeUpdate();
		return quantityDeleted;
	}
	
    /**
	 * @see org.openmrs.module.sync.api.db.SyncDAO#getSyncRecords(java.util.Date,
	 *      java.util.Date, Integer, Integer)
	 */
	@SuppressWarnings("unchecked")
    public List<SyncRecord> getSyncRecords(Date from, Date to, Integer firstRecordId, Integer numberToReturn, boolean oldestToNewest)
            throws DAOException {
        Criteria criteria = sessionFactory.getCurrentSession()
            .createCriteria(SyncRecord.class);
        
        if (from != null)
        	criteria.add(Restrictions.gt("timestamp", from)); // greater than
           
        if (to != null)
            criteria.add(Restrictions.le("timestamp", to)); // less-than or equal
        
        if (firstRecordId != null)
        	criteria.add(Restrictions.gt("recordId", firstRecordId));
        
        if (numberToReturn != null)
        	criteria.setMaxResults(numberToReturn);
        
        if (oldestToNewest) {
	        criteria.addOrder(Order.asc("timestamp"));
	        criteria.addOrder(Order.asc("recordId"));
        }
        else {
        	criteria.addOrder(Order.desc("timestamp"));
	        criteria.addOrder(Order.desc("recordId"));
        }
        
        return criteria.list();
    }

    
    /**
     * @see org.openmrs.module.sync.api.db.SyncDAO#getGlobalProperty(String propertyName)
     */
    public String getGlobalProperty(String propertyName) 
        throws DAOException {
        
        if (propertyName == null)
            throw new DAOException("Cannot retrieve property with null property name.");

        GlobalProperty gp = (GlobalProperty) sessionFactory.getCurrentSession().get(GlobalProperty.class, propertyName);
        
        if (gp == null)
            return null;

        return gp.getPropertyValue();    
        
    }
    
    /**
     * @see org.openmrs.module.sync.api.db.SyncDAO#setGlobalProperty(String propertyName, String propertyValue)
     */
    public void setGlobalProperty(String propertyName, String propertyValue) 
        throws DAOException {
        
        if (propertyName == null)
            throw new DAOException("Cannot set property with null property name.");

        Session session = sessionFactory.getCurrentSession();
        
        // try to look up the global property first so we use the same uuid for the gp
        GlobalProperty gp = (GlobalProperty)session.get(GlobalProperty.class, propertyName);
        if (gp == null) {
        	// the gp doesn't exist, create a new one with a new uuid now
        	gp = new GlobalProperty(propertyName, propertyValue);
        	gp.setUuid(SyncUtil.generateUuid());
        }
        else {
        	gp.setPropertyValue(propertyValue);
        }
        
        session.merge(gp);
    }

    /**
     * @see org.openmrs.module.sync.api.db.SyncDAO#saveRemoteServer(org.openmrs.module.sync.engine.RemoteServer)
     */
    public void saveRemoteServer(RemoteServer server) throws DAOException {
        Session session = sessionFactory.getCurrentSession();
        session.saveOrUpdate(server);
    }

    /**
     * @see org.openmrs.module.sync.api.db.SyncDAO#deleteRemoteServer(org.openmrs.module.sync.engine.RemoteServer)
     */
    public void deleteRemoteServer(RemoteServer record) throws DAOException {
        Session session = sessionFactory.getCurrentSession();
        session.delete(record);
    }

    /**
     * @see org.openmrs.module.sync.api.db.SyncDAO#getGlobalProperty(String propertyName)
     */
    public RemoteServer getRemoteServer(Integer serverId) throws DAOException {        
        return (RemoteServer)sessionFactory.getCurrentSession().get(RemoteServer.class, serverId);
    }

    /**
     * @see org.openmrs.module.sync.api.db.SyncDAO#getGlobalProperty(String propertyName)
     */
    public RemoteServer getRemoteServer(String uuid) throws DAOException {        
        return (RemoteServer)sessionFactory.getCurrentSession()
        .createCriteria(RemoteServer.class)
        .add(Restrictions.eq("uuid", uuid))
        .uniqueResult();
    }

    /**
     * @see org.openmrs.module.sync.api.db.SyncDAO#getGlobalProperty(String propertyName)
     */
    public RemoteServer getRemoteServerByUsername(String username) throws DAOException {        
        return (RemoteServer)sessionFactory.getCurrentSession()
        .createCriteria(RemoteServer.class)
        .add(Restrictions.eq("childUsername", username))
        .uniqueResult();
    }

    /**
     * @see org.openmrs.module.sync.api.db.SyncDAO#getGlobalProperty(String propertyName)
     */
    @SuppressWarnings("unchecked")
    public List<RemoteServer> getRemoteServers() throws DAOException {        
        return (List<RemoteServer>)sessionFactory.getCurrentSession().createCriteria(RemoteServer.class).list();
    }

    /**
     * @see org.openmrs.module.sync.api.db.SyncDAO#getGlobalProperty(String propertyName)
     */
    public RemoteServer getParentServer() throws DAOException {        
        return (RemoteServer)sessionFactory.getCurrentSession()
        		.createCriteria(RemoteServer.class)
        		.add(Restrictions.eq("serverType", RemoteServerType.PARENT))
        		.uniqueResult();
    }

    /**
     * @see org.openmrs.module.sync.api.db.SyncDAO#saveSyncClass(org.openmrs.module.sync.SyncClass)
     */
    public void saveSyncClass(SyncClass syncClass) throws DAOException {
        Session session = sessionFactory.getCurrentSession();
        session.saveOrUpdate(syncClass);
    }

    /**
     * @see org.openmrs.module.sync.api.db.SyncDAO#deleteSyncClass(org.openmrs.module.sync.SyncClass)
     */
    public void deleteSyncClass(SyncClass syncClass) throws DAOException {
        Session session = sessionFactory.getCurrentSession();
        session.delete(syncClass);
    }

    /**
     * @see org.openmrs.module.sync.api.db.SyncDAO#getSyncClass(Integer)
     */
    public SyncClass getSyncClass(Integer syncClassId) throws DAOException {        
        return (SyncClass)sessionFactory.getCurrentSession().get(SyncClass.class, syncClassId);
    }

    /**
     * @see org.openmrs.module.sync.api.db.SyncDAO#getSyncClasses()
     */
    @SuppressWarnings("unchecked")
    public List<SyncClass> getSyncClasses() throws DAOException {        
        
        List<SyncClass> classes = (List<SyncClass>)sessionFactory.getCurrentSession()
                .createCriteria(SyncClass.class)
                .addOrder(Order.asc("name"))
                .list();
        
        if ( classes == null && log.isWarnEnabled() )
            log.warn("getSyncClasses is null.");
        
        return classes;
    }
    
    /**
     * @see org.openmrs.module.sync.api.db.SyncDAO#getSyncClassByName(String)
     */
    public SyncClass getSyncClassByName(String className) throws DAOException {
    	Criteria crit = sessionFactory.getCurrentSession()
        .createCriteria(SyncClass.class)
        .add(Expression.eq("name", className));
    	
        return (SyncClass)crit.uniqueResult();
    }

    /**
     * @see org.openmrs.module.sync.api.db.SyncDAO#createDatabaseForChild(java.lang.String, java.io.Writer)
     * NOTE: THIS IS WORK IN PROGRESS *DO NOT* USE
     */
    @Deprecated
    public void createDatabaseForChild(String uuidForChild, OutputStream os) throws DAOException {
        PrintStream out = new PrintStream(os);
        Set<String> tablesToSkip = new HashSet<String>();
        {
            tablesToSkip.add("hl7_in_archive");
            tablesToSkip.add("hl7_in_queue");
            tablesToSkip.add("hl7_in_error");
            tablesToSkip.add("formentry_archive");
            tablesToSkip.add("formentry_queue");
            tablesToSkip.add("formentry_error");
            // TODO: figure out which other tables to skip
            tablesToSkip.add("obs");
            tablesToSkip.add("concept");
            tablesToSkip.add("patient");
        }
        List<String> tablesToDump = new ArrayList<String>();
        Session session = sessionFactory.getCurrentSession();
        
        String schema = (String) session.createSQLQuery("SELECT schema()").uniqueResult();
        log.warn("schema: " + schema);
        
        { // Get all tables that we'll need to dump
            Query query = session.createSQLQuery("SELECT tabs.table_name FROM INFORMATION_SCHEMA.TABLES tabs WHERE tabs.table_schema = '" + schema + "'");
            for (Object tn : query.list()) {
                String tableName = (String) tn;
                if (!tablesToSkip.contains(tableName.toLowerCase()))
                    tablesToDump.add(tableName);
            }
        }
        log.warn("tables to dump: " + tablesToDump);
        
        String thisServerUuid = getGlobalProperty(SyncConstants.PROPERTY_SERVER_UUID);
       
        { // write a header
            out.println("-- ------------------------------------------------------");
            out.println("-- Database dump to create an openmrs child server");
            out.println("-- Schema: " + schema);
            out.println("-- Parent UUID: " + thisServerUuid);
            out.println("-- Parent version: " + OpenmrsConstants.OPENMRS_VERSION);
            out.println("-- ------------------------------------------------------");
            out.println("");
            out.println("/*!40101 SET NAMES utf8 */;");
            out.println("/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;");
            out.println("/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;");
            out.println("");
        }
        try {
            //Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/" + schema, "test", "test");
        	Connection conn = sessionFactory.getCurrentSession().connection();
            try {
                Statement st = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
                
                // Get the create database statement
                ResultSet rs = st.executeQuery("SHOW CREATE DATABASE " + schema);
                while (rs.next())
                    out.println(rs.getString("Create Database") + ";");
                
                for (String tableName : tablesToDump) {
                    out.println();
                    out.println("--");
                    out.println("-- Table structure for table `" + tableName + "`");
                    out.println("--");
                    out.println("DROP TABLE IF EXISTS `" + tableName + "`");
                    
                    rs = st.executeQuery("SHOW CREATE TABLE " + tableName);
                    while (rs.next())
                        out.println(rs.getString("Create Table") + ";");
                    out.println();
                    
                    if (session.createSQLQuery("select count(*) from " + tableName).uniqueResult().toString().equals("0")) {
                        out.println("-- `" + tableName + "` has no data");
                    } else {
                        out.println("-- Dumping data for table `" + tableName + "`");
                        out.println("LOCK TABLES `" + tableName + "` WRITE;");
                        out.println("/*!40000 ALTER TABLE `" + tableName + "` DISABLE KEYS */;");
                        boolean first = true;
                        out.println("INSERT INTO `" + tableName + "` VALUES ");
                        
                        rs = st.executeQuery("select * from " + tableName);
                        ResultSetMetaData md = rs.getMetaData();
                        int numColumns = md.getColumnCount();
                        int rowNum = 0;
                        while (rs.next()) {
                            ++rowNum;
                            if (first)
                                first = false;
                            else
                                out.print(", ");
                            if (rowNum % 20 == 0)
                                out.println();
                            out.print("(");
                            for (int i = 1; i <= numColumns; ++i) {
                                if (i != 1)
                                    out.print(", ");
                                if (rs.getObject(i) == null)
                                    out.print("NULL");
                                else {
                                    switch (md.getColumnType(i)) {
                                    case Types.VARCHAR:
                                    case Types.CHAR:
                                    case Types.LONGVARCHAR:
                                        out.print("'");
                                        out.print(rs.getString(i).replaceAll("\n","\\\\n").replaceAll("'","\\\\'"));
                                        out.print("'");
                                        break;
                                    case Types.BIGINT:
                                    case Types.DECIMAL:
                                    case Types.NUMERIC:
                                        out.print(rs.getBigDecimal(i));
                                        break;
                                    case Types.BIT:
                                        out.print(rs.getBoolean(i));
                                        break;
                                    case Types.INTEGER:
                                    case Types.SMALLINT:
                                    case Types.TINYINT:
                                        out.print(rs.getInt(i));
                                        break;
                                    case Types.REAL:
                                    case Types.FLOAT:
                                    case Types.DOUBLE:
                                        out.print(rs.getDouble(i));
                                        break;
                                    case Types.BLOB:
                                    case Types.VARBINARY:
                                    case Types.LONGVARBINARY:
                                        Blob blob = rs.getBlob(i);
                                        out.print("'");
                                        InputStream in = blob.getBinaryStream();
                                        while (true) {
                                        	int b = in.read();
                                        	if (b < 0)
                                        		break;
                                        	char c = (char) b;
                                        	if (c == '\'')
                                        		out.print("\'");
                                        	else
                                        		out.print(c);
                                        }
                                        out.print("'");
                                        break;
                                    case Types.CLOB:
                                        //Reader r = rs.getClob(i).getCharacterStream();
                                        out.print("'");
                                        out.print(rs.getString(i).replaceAll("\n","\\\\n").replaceAll("'","\\\\'"));
                                        out.print("'");
                                        break;
                                    case Types.DATE:
                                        out.print("'" + rs.getDate(i) + "'");
                                        break;
                                    case Types.TIMESTAMP:
                                        out.print(rs.getTimestamp(i));
                                        break;
                                    default:
                                        // when it comes time to look at BLOBs, look here: http://www.wave2.org/svnweb/Wave2%20Repository/view%2Fbinarystor%2Ftrunk%2Fsrc%2Fjava%2Forg%2Fbinarystor%2Fmysql/MySQLDump.java
                                        throw new RuntimeException("TODO: handle type code " + md.getColumnType(i) + " (name " + md.getColumnTypeName(i) + ")");
                                    }
                                }
                                //out.print("'" + data[i].toString().replaceAll("\n","\\\\n").replaceAll("'","\\\\'") + "'");
                            }
                            out.print(")");
                        }
                        out.println(";");
                        
                        out.println("/*!40000 ALTER TABLE `" + tableName + "` ENABLE KEYS */;");
                        out.println("UNLOCK TABLES;");
                        out.println();
                    }
                }
            } finally {
                conn.close();
            }
            
            // Now we mark this as a child
            out.println("-- Now mark this as a child database");
            if (uuidForChild == null)
                uuidForChild = SyncUtil.generateUuid();
            out.println("update global_property set property_value = '" + uuidForChild + "' where property = '" + SyncConstants.PROPERTY_SERVER_UUID + "';");
            
            {
            	// TODO: Write a footer to undo the following two lines
                // out.println("/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;");
                // out.println("/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;");
            	// Maybe start from this as an example: 
            	// /*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;
            	// /*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
            	// /*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
            	// /*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
            	// /*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
            	// /*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
            	// /*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
            	// /*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
            }
            
        } catch (IOException ex) {
        	log.error("IOException", ex);
        	
        } catch (SQLException ex) {
            log.error("SQLException", ex);
        }
    }
    
    /**
     * @see org.openmrs.module.sync.api.db.SyncDAO#deleteOpenmrsObject(org.openmrs.synchronization.OpenmrsObject)
     */
    public void deleteOpenmrsObject(OpenmrsObject o) throws DAOException {
    	sessionFactory.getCurrentSession().delete(o);
    }

    /**
     * Sets hibernate flush mode to org.hibernate.FlushMode.MANUAL.
     * 
     * @see org.hibernate.FlushMode
     * @see org.openmrs.module.sync.api.db.SyncDAO#setFlushModeManual()
     */
    public void setFlushModeManual() throws DAOException {
    	sessionFactory.getCurrentSession().setFlushMode(org.hibernate.FlushMode.MANUAL);
    }
    
    /**
     * Sets hibernate flush mode to org.hibernate.FlushMode.AUTO.
     * 
     * @see org.hibernate.FlushMode
     * @see org.openmrs.module.sync.api.db.SyncDAO#setFlushModeAutomatic()
     */
    public void setFlushModeAutomatic() throws DAOException {
    	sessionFactory.getCurrentSession().setFlushMode(org.hibernate.FlushMode.AUTO);
    }

    /**
     * Executes hibernate flush.
     * 
     * @see org.hibernate.Session#flush()
     * @see org.openmrs.module.sync.api.db.SyncDAO#flushSession()
     */
    public void flushSession() throws DAOException {
    	sessionFactory.getCurrentSession().flush();
    }

    /**
     * Performs generic save of openmrs object using Hibernate session.saveorupdate.
     * 
     * @throws DAOException 
     */
	public void saveOrUpdate(Object object) throws DAOException {
		sessionFactory.getCurrentSession().saveOrUpdate(object);
	}
	
	

	/**
	 * @see org.openmrs.module.sync.api.db.SyncDAO#getSyncStatistics(java.util.Date, java.util.Date)
	 */
	@SuppressWarnings("unchecked")
	public Map<RemoteServer,Set<SyncStatistic>> getSyncStatistics(Date fromDate, Date toDate) throws DAOException {
				
		
		//first get the list of remote servers and make map out of it
		List<RemoteServer> servers = this.getRemoteServers();
		
		Map<RemoteServer,Set<SyncStatistic>> map = new HashMap<RemoteServer,Set<SyncStatistic>>();

		String hqlChild = "select rs.nickname, ssr.state, count(*) " +
		"from RemoteServer rs join rs.serverRecords as ssr "+
		"where rs.serverId = :server_id and ssr.state  <> '" + SyncRecordState.NOT_SUPPOSED_TO_SYNC.toString() + "' " +
		"group by rs.nickname, ssr.state "+
		"order by nickname, state";
		
		String hqlParent = "select count(*) from SyncRecord where originalUuid = uuid and state <> '" + SyncRecordState.COMMITTED.toString() + 
		"' and state <> '" + SyncRecordState.NOT_SUPPOSED_TO_SYNC.toString() + "'";
		
		//for each server configured, get its stats
		for(RemoteServer r : servers) {
			if (r.getServerType() == RemoteServerType.CHILD) {
				Query q = sessionFactory.getCurrentSession().createQuery(hqlChild);
				q.setParameter("server_id", r.getServerId());
				List<Object[]> rows = q.list();
				Set<SyncStatistic> props = new HashSet<SyncStatistic>();
				for (Object[] row : rows) {
					SyncStatistic stat = new SyncStatistic(SyncStatistic.Type.SYNC_RECORD_COUNT_BY_STATE,row[1].toString(),row[2]); //state/count
					props.add(stat); 
				}
				map.put(r,props);
			}
			else {
				//for parent servers, get the number of records in sync journal
				Query q = sessionFactory.getCurrentSession().createQuery(hqlParent);
				Long count = (Long)q.uniqueResult();
				Set<SyncStatistic> props = new HashSet<SyncStatistic>();
				if (count != null) {
					props.add(new SyncStatistic(SyncStatistic.Type.SYNC_RECORD_COUNT_BY_STATE,"AWAITING",count)); //count
				}
				map.put(r,props);
			}
		}
						
		return map;
	}	
	
	public boolean checkUuidsForClass(Class clazz) {
		
		//TODO: work in progres

		boolean ret = false;
		/*
		try {
			//now build the sql based on the hibernate mappings; to do this we need to load (at least once) the config
			if (HibernateSyncDAO.configuration == null) {
				synchronized (configurationLock) {
					HibernateSyncDAO.configuration = new org.hibernate.cfg.Configuration().configure();
				}
			}
			
			String selectSql = null;
			String columnName = null;
			String tableName = null;
			String catalogName = null;
	
			org.hibernate.mapping.PersistentClass pc = HibernateSyncDAO.configuration.getClassMapping(clazz.getName());
			
			if (pc == null) {
				log.error("cannot get hibernate class mapping for " + clazz.getName());
				return ret;
			}
			
			tableName = pc.getTable().getName();
			org.hibernate.mapping.Property p = pc.getProperty("uuid");
			if (p == null) {
				log.error("cannot get hibernate uuid column mapping for " + clazz.getName());
				return ret;			
			}
			
			java.util.Iterator<org.hibernate.mapping.Column> columns = p.getColumnIterator();
			if (columns.hasNext()) {
				columnName = columns.next().getName();
			} else {
				log.info("column mapping not found for property uuid.");
				return ret;
			}
			
			//now compare this to database metadata
			java.sql.DatabaseMetaData meta = sessionFactory.getCurrentSession().connection().getMetaData();
			java.sql.ResultSet rs = meta.getColumns(null, null,tableName, columnName);
			ResultSetMetaData rmd = rs.getMetaData();
			
			if (!rs.first()) {
				log.error("didn't find uuid in database!");
				return ret;
			}
			
			log.debug("done");
		}
		catch (Exception e) {
			//TODO
			log.error("Ouch: ", e);
			ret = false;
		} */
		
		return ret;

	}

	public <T extends OpenmrsObject> T getOpenmrsObjectByUuid(Class<T> clazz, String uuid) {
		Criteria crit = sessionFactory.getCurrentSession().createCriteria(clazz);
		crit.add(Restrictions.eq("uuid", uuid));
		return (T) crit.uniqueResult();
    }
}