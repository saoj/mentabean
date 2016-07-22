package org.mentabean.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.mentabean.BeanConfig;
import org.mentabean.BeanException;
import org.mentabean.BeanManager;
import org.mentabean.DBField;
import org.mentabean.DBType;
import org.mentabean.type.ByteArrayType;
import org.mentabean.type.LongType;
import org.mentabean.util.Limit;
import org.mentabean.util.OrderBy;

/**
 * 
 * Now in PostgreSQL is 'current_timestamp'.
 * 
 * PostgreSQL only supports sequences for primary keys.
 * 
 * @author erico_kl
 * 
 */
public class PostgreSQLBeanSession extends AnsiSQLBeanSession {

	public PostgreSQLBeanSession(BeanManager beanManager, Connection conn) {
		
		super(beanManager, conn);
	}
	
	@Override
	protected String getCurrentTimestampCommand() {
		
		return "current_timestamp";
	}
	
	/**
	 * PostgreSQL will sort first then apply limit
	 */
	@Override
	protected StringBuilder handleLimit(final StringBuilder sb, final OrderBy orderBy, final Limit limit) {
		
		if (limit == null || limit.intValue() <= 0) {
			return sb;
		}
		
		final StringBuilder sbLimit = new StringBuilder(sb.length() + 32);
		
		sbLimit.append(sb.toString()).append(" LIMIT ").append(limit);
		
		return sbLimit;
	}

	@Override
	public void insert(final Object bean) {

		// find sequence field...
		final BeanConfig bc = beanManager.getBeanConfig(bean.getClass());
		
		if (bc == null) {
			
			throw new BeanException("Cannot find bean config: " + bean.getClass());
		}
			
		final DBField seqField = bc.getSequenceField();
		
		if (seqField == null) {
			
			//find autoincrement field
			
			super.insert(bean);
			
			final DBField autoIncrement = bc.getAutoIncrementField();

			if (autoIncrement == null) {

				dispatchAfterInsert(bean);
				
				return;
			}
			
			PreparedStatement stmt = null;

			ResultSet rset = null;
			
			StringBuilder sb = new StringBuilder("select lastval();");
			
			try {

				stmt = conn.prepareStatement(sb.toString());

				rset = stmt.executeQuery();

				if (rset.next()) {

					final long id = rset.getLong(1);
					
					try {

						injectValue(bean, autoIncrement.getName(), id, Integer.class);
						
					} catch(Exception e) {
						
						// try long as well:
						injectValue(bean, autoIncrement.getName(), id, Long.class);
					}
					
					dispatchAfterInsert(bean);
					
					return;
				}
				

			} catch (Exception e) {

				throw new BeanException(e);

			} finally {

				close(stmt, rset);
			}
		}
		
		String seqName = bc.getSequenceName();
		
		if (seqName == null) {
		
			seqName = bc.getTableName() + "_seq";
			
		}

		PreparedStatement stmt = null;
		ResultSet rset = null;

		StringBuilder sb = new StringBuilder();
		
		//get the sequence's next value in pgsql way
		sb.append("select nextval ('").append(seqName).append("')");
		
		try {
			
			stmt = conn.prepareStatement(sb.toString());
			
			rset = stmt.executeQuery();
			
			if (rset.next()) {
				
				final long id = rset.getLong(1);
				
				try {

					injectValue(bean, seqField.getName(), id, Integer.class);
					
				} catch(Exception e) {
					
					// try long as well:
					injectValue(bean, seqField.getName(), id, Long.class);
				}
					
			}

		}catch (Exception e) {
			
			throw new BeanException("Error preparing statement to insert in PostgreSQL", e);
			
		} finally {
			//if (stmt != null) try { stmt.close(); } catch(Exception e) { }
			close(stmt, rset);
		}

		super.insert(bean);
		
		dispatchAfterInsert(bean);
		
	}
	
	protected boolean isVarcharUnlimitedSupported() {
		
		return true;
	}
	
	@Override
	protected String getDatabaseType(DBType<?> dbType) {

		if (dbType.getClass().equals(ByteArrayType.class))
			return "bytea";
		
		if (dbType.getClass().equals(LongType.class))
			return "bigint";
		
		return super.getDatabaseType(dbType);
		
	}
}