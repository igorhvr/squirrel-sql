
package net.sourceforge.squirrel_sql.jdbcproxy;

/*
 * Copyright (C) 2006 Rob Manning
 * manningr@users.sourceforge.net
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.Map;

public class ProxyConnection implements Connection {

    Connection _con = null; 
    ProxyDatabaseMetaData _data = null;
    
    public ProxyConnection(Connection con) throws SQLException {
        _con = con;
        _data = new ProxyDatabaseMetaData(con.getMetaData());
    }
    
    public int getHoldability() throws SQLException {
        ProxyMethodManager.check("ProxyConnection", "getHoldability");
        return _con.getHoldability();
    }

    public int getTransactionIsolation() throws SQLException {
        ProxyMethodManager.check("ProxyConnection", "getTransactionIsolation");
        return _con.getTransactionIsolation();
    }

    public void clearWarnings() throws SQLException {
        ProxyMethodManager.check("ProxyConnection", "clearWarnings");
        _con.clearWarnings();
    }

    public void close() throws SQLException {
        ProxyMethodManager.printMethodsCalled();
        ProxyMethodManager.check("ProxyConnection", "close");
        _con.close();
    }

    public void commit() throws SQLException {
        ProxyMethodManager.check("ProxyConnection", "commit");
        _con.commit();
    }

    public void rollback() throws SQLException {
        ProxyMethodManager.check("ProxyConnection", "rollback");
        _con.rollback();
    }

    public boolean getAutoCommit() throws SQLException {
        ProxyMethodManager.check("ProxyConnection", "getAutoCommit");
        return _con.getAutoCommit();
    }

    public boolean isClosed() throws SQLException {
        ProxyMethodManager.check("ProxyConnection", "isClosed");
        return _con.isClosed();
    }

    public boolean isReadOnly() throws SQLException {
        ProxyMethodManager.check("ProxyConnection", "isReadOnly");
        return _con.isReadOnly();
    }

    public void setHoldability(int holdability) throws SQLException {
        ProxyMethodManager.check("ProxyConnection", "setHoldability");
        _con.setHoldability(holdability);
    }

    public void setTransactionIsolation(int level) throws SQLException {
        ProxyMethodManager.check("ProxyConnection", "setTransactionIsolation");
        _con.setTransactionIsolation(level);
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException {
        ProxyMethodManager.check("ProxyConnection", "setAutoCommit");
        _con.setAutoCommit(autoCommit);
    }

    public void setReadOnly(boolean readOnly) throws SQLException {
        ProxyMethodManager.check("ProxyConnection", "setReadOnly");
        _con.setReadOnly(readOnly);
    }

    public String getCatalog() throws SQLException {
        ProxyMethodManager.check("ProxyConnection", "getCatalog");
        return _con.getCatalog();
    }

    public void setCatalog(String catalog) throws SQLException {
        ProxyMethodManager.check("ProxyConnection", "setCatalog");
        _con.setCatalog(catalog);
    }

    public DatabaseMetaData getMetaData() throws SQLException {
        ProxyMethodManager.check("ProxyConnection", "getMetaData");
        return _data;
    }

    public SQLWarning getWarnings() throws SQLException {
        ProxyMethodManager.check("ProxyConnection", "getWarnings");
        return _con.getWarnings();
    }

    public Savepoint setSavepoint() throws SQLException {
        ProxyMethodManager.check("ProxyConnection", "setSavepoint");
        return _con.setSavepoint();
    }

    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        ProxyMethodManager.check("ProxyConnection", "releaseSavepoint");
        _con.releaseSavepoint(savepoint);
    }

    public void rollback(Savepoint savepoint) throws SQLException {
        ProxyMethodManager.check("ProxyConnection", "rollback");
        _con.rollback(savepoint);
    }

    public Statement createStatement() throws SQLException {
        ProxyMethodManager.check("ProxyConnection","createStatement");
        return new ProxyStatement(this, _con.createStatement());
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency)
            throws SQLException 
    {
        ProxyMethodManager.check("ProxyConnection", "createStatement");
        return new ProxyStatement(this, _con.createStatement(resultSetType, resultSetConcurrency));
    }

    public Statement createStatement(int resultSetType,
            int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        ProxyMethodManager.check("ProxyConnection", "createStatement");
        return new ProxyStatement(this, _con.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability));
    }

    public Map getTypeMap() throws SQLException {
        ProxyMethodManager.check("ProxyConnection", "getTypeMap");
        return _con.getTypeMap();
    }

    public void setTypeMap(Map map) throws SQLException {
        ProxyMethodManager.check("ProxyConnection", "setTypeMap");
        _con.setTypeMap(map);
    }

    public String nativeSQL(String sql) throws SQLException {
        ProxyMethodManager.check("ProxyConnection", "nativeSQL");
        return _con.nativeSQL(sql);
    }

    public CallableStatement prepareCall(String sql) throws SQLException {
        ProxyMethodManager.check("ProxyConnection", "prepareCall");
        return _con.prepareCall(sql);
    }

    public CallableStatement prepareCall(String sql, int resultSetType,
            int resultSetConcurrency) throws SQLException 
    {
        ProxyMethodManager.check("ProxyConnection", "prepareCall");
        return _con.prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    public CallableStatement prepareCall(String sql, int resultSetType,
            int resultSetConcurrency, int resultSetHoldability)
            throws SQLException 
    {
        ProxyMethodManager.check("ProxyConnection", "prepareCall");
        return _con.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException {
        ProxyMethodManager.check("ProxyConnection", "prepareStatement");
        return _con.prepareStatement(sql);
    }

    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
            throws SQLException {
        ProxyMethodManager.check("ProxyConnection", "prepareStatement");
        return _con.prepareStatement(sql, autoGeneratedKeys);
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType,
            int resultSetConcurrency) throws SQLException {
        ProxyMethodManager.check("ProxyConnection", "prepareStatement");
        return _con.prepareStatement(sql, resultSetType, resultSetConcurrency);
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType,
            int resultSetConcurrency, int resultSetHoldability)
            throws SQLException 
    {
        ProxyMethodManager.check("ProxyConnection", "prepareStatement");
        return _con.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    public PreparedStatement prepareStatement(String sql, int[] columnIndexes)
            throws SQLException 
    {
        ProxyMethodManager.check("ProxyConnection", "prepareStatement");
        return _con.prepareStatement(sql, columnIndexes);
    }

    public Savepoint setSavepoint(String name) throws SQLException {
        ProxyMethodManager.check("ProxyConnection", "setSavepoint");
        return _con.setSavepoint();
    }

    public PreparedStatement prepareStatement(String sql, String[] columnNames)
            throws SQLException {
        ProxyMethodManager.check("ProxyConnection", "prepareStatement");
        return _con.prepareStatement(sql, columnNames);
    }

}
