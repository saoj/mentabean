package org.mentabean.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.mentabean.BeanException;

public class SQLUtils {
	
	public static final String UNIQUE_KEY_VIOLATED_STATE = "23505";
	public static final String FOREIGN_KEY_VIOLATED_STATE = "23503";
	
	public static void executeScript(Connection conn, String file, String charset) {
		
		FileInputStream fis = null;
		BufferedReader br = null;
		
		try {

			ScriptRunner script = new ScriptRunner(conn);
			
			fis = new FileInputStream(file);
			
			if (charset != null) {
			
				br = new BufferedReader(new InputStreamReader(fis, charset));
				
			} else {
				
				br = new BufferedReader(new InputStreamReader(fis));
			}
			
			script.runScript(br);
			
		} catch(Exception e) {
			throw new BeanException(e);
		}
	}
	
	public static boolean checkIfTableExists(Connection conn, String tableName) {
		ResultSet rset = null;
		try {
			DatabaseMetaData dbm = conn.getMetaData();
		    rset = dbm.getTables(null, null, null, new String[] { "TABLE" });
		    while(rset.next()) {
		    	String tn = rset.getString("TABLE_NAME");
		    	if (tn.equalsIgnoreCase(tableName)) return true;
		    }
		    return false;
		} catch(Exception e) {
			throw new BeanException(e);
		} finally {
			if (rset != null) {
				try { 
					rset.close(); 
				} catch(Exception e) {
					throw new BeanException(e);
				}
			}
		}
	}
	
	public static void close(Statement stmt) {
		if (stmt != null) {
			try {
				stmt.close();
			} catch(SQLException e) {
				throw new BeanException(e);
			}
		}
	}
	
	public static void close(ResultSet rset, Statement stmt) {
		
		SQLException bad = null;
		
		if (rset != null) {
			try {
				rset.close();
			} catch(SQLException e) {
				bad = e;
			}
		}
		if (stmt != null) {
			try {
				stmt.close();
			} catch(SQLException e) {
				bad = e;
			}
		}
		if (bad != null) throw new BeanException(bad);
	}
	
	public static void close(Statement stmt, Connection conn) {
		SQLException bad = null;
		
		if (stmt != null) {
			try {
				stmt.close();
			} catch(SQLException e) {
				bad = e;
			}
		}
		if (conn != null) {
			try {
				conn.close();
			} catch(SQLException e) {
				bad = e;
			}
		}
		if (bad != null) throw new BeanException(bad);
	}
	
	public static void close(Connection conn) {
		
		if (conn != null) {
			try {
				conn.close();
			} catch(SQLException e) {
				throw new BeanException(e);
			}
		}
		
	}
	
	public static void close(ResultSet rset, Statement stmt, Connection conn) {
		SQLException bad = null;
		
		if (rset != null) {
			try {
				rset.close();
			} catch(SQLException e) {
				bad = e;
			}
		}
		if (stmt != null) {
			try {
				stmt.close();
			} catch(SQLException e) {
				bad = e;
			}
		}
		if (conn != null) {
			try {
				conn.close();
			} catch(SQLException e) {
				bad = e;
			}
		}
		if (bad != null) throw new BeanException(bad);
	}
	
	/**
	 * Fill the given {@code PreparedStatement} object with the specified parameters.<br><br>
	 * <i>Note that this method uses the {@code setObject} method which depends 
	 * of a specified JDBC driver implementation to work properly.</i><br>
	 * 
	 * @param stmt - The {@code PreparedStatement} object
	 * @param params - Values that will be added in statement
	 * @return The last index added plus 1. It can be useful for continuous statement filling operations.
	 * @see 	#fillStatementIndx(PreparedStatement, int, Object...)
	 * @see		PreparedStatement#setObject(int, Object)
	 */
	public static int fillStatement(PreparedStatement stmt, Object... params) {
		return fillStatementIndx(stmt, 1, params);
	}

	/**
	 * Fill the given {@code PreparedStatement} object with the specified parameters 
	 * starting at {@code indx} position.<br><br>
	 * <i>Note that this method uses the {@code setObject} method which depends 
	 * of a specified JDBC driver implementation to work properly.</i><br>
	 * 
	 * @param stmt - The {@code PreparedStatement} object
	 * @param indx - Initial index for setting attributes in statement
	 * @param params - Values that will be added in statement
	 * @return The next index (last added plus 1). It can be useful for continuous statement filling operations.
	 * @see 	#fillStatement(PreparedStatement, Object...)
	 * @see		PreparedStatement#setObject(int, Object)
	 */
	public static int fillStatementIndx(PreparedStatement stmt, int indx, Object... params) {

		try {
			for (Object obj : params)
				stmt.setObject(indx++, obj);

			return indx;
			
		}catch (Exception e) {
			throw new BeanException("Error setting values for statement", e);
		}
	}
	
	/**
	 * Prepare a statement (PreparedStatement) with the query and set the parameters.
	 * 
	 * @param conn the connection
	 * @param query the query to prepare
	 * @param params the parameters to set in the query
	 * @return the statement ready to be executed
	 * @throws SQLException
	 */
	public static PreparedStatement prepare(Connection conn, String query, Object... params) throws SQLException {
		PreparedStatement stmt = conn.prepareStatement(query);
		fillStatement(stmt, params);
		return stmt;
	}
	
	public static OrderBy orderByAsc(Object field) {
		OrderBy orderBy = new OrderBy();
		if (field instanceof String) {
			orderBy.orderByAsc((String) field); // i am surprised this is necessary
		} else {
			orderBy.orderByAsc(field);
		}
		return orderBy;
	}
	
	public static OrderBy orderByDesc(Object field) {
		OrderBy orderBy = new OrderBy();
		if (field instanceof String) {
			orderBy.orderByDesc((String) field);  // i am surprised this is necessary
		} else {
			orderBy.orderByDesc(field);
		}
		return orderBy;
	}
	
	public static Limit lim(int x) {
		return new Limit(x);
	}
	
	
	public static void beginTransaction(Connection conn) {
		try {
			if (conn.getAutoCommit())
				conn.setAutoCommit(false);
		} catch (Exception e) {
			throw new BeanException("Unable to begin transaction", e);
		}
	}
	
	public static void commitTransaction(Connection conn) {
		try {
			conn.commit();
		} catch (Exception e) {
			throw new BeanException("Unable to commit transaction", e);
		}
	}
	
	public static void rollbackTransaction(Connection conn) {
		try {
			conn.rollback();
		} catch (Exception e) {
			throw new BeanException("Unable to rollback transaction", e);
		}
	}
}