/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * MentaBean => http://www.mentabean.org
 * Author: Sergio Oliveira Jr. (sergio.oliveira.jr@gmail.com)
 */
package org.mentabean.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.mentabean.BeanConfig;
import org.mentabean.BeanException;
import org.mentabean.BeanManager;
import org.mentabean.DBField;
import org.mentabean.util.Limit;
import org.mentabean.util.OrderBy;

/**
 * When using limit, Oracle will NOT sort first before limiting, so a trick must be used.
 * 
 * Now in Oracle is 'sysdate'.
 * 
 * Oracle only supports sequences for primary keys.
 * 
 * @author soliveira
 * 
 */
public class OracleBeanSession extends AnsiSQLBeanSession {

	public OracleBeanSession(final BeanManager beanManager, final Connection conn) {

		super(beanManager, conn);
	}

	@Override
	protected String getCurrentTimestampCommand() {

		return "sysdate";
	}

	/**
	 * Oracle will not SORT first then apply LIMIT.
	 */
	@Override
	protected StringBuilder handleLimit(final StringBuilder sb, final OrderBy orderBy, final Limit limit) {

		if (limit == null || limit.intValue() <= 0) {
			return sb;
		}

		final StringBuilder sbLimit = new StringBuilder(sb.length() + 32);

		if (orderBy != null && !orderBy.isEmpty()) {

			sbLimit.append("SELECT * FROM (").append(sb.toString()).append(") where rowcount <= ").append(limit);

		} else {

			sbLimit.append(sb.toString()).append(" AND rownum <= ").append(limit);
		}

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

			super.insert(bean);
			
			dispatchAfterInsert(bean);

			return;
		}

		PreparedStatement stmt = null;

		ResultSet rset = null;

		final StringBuilder sb = new StringBuilder(128);
		
		sb.append("select ");
		
		String seqName = bc.getSequenceName();
		
		if (seqName != null) {
			// sequence name was defined defined
			sb.append(seqName);
		} else {
			// use the convention for sequence name
			sb.append("seq_").append(seqField.getDbName()).append("_").append(bc.getTableName());	
		}

		sb.append(".nextval from ").append(bc.getTableName());

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

		} catch (Exception e) {

			throw new BeanException(e);

		} finally {

			close(stmt, rset);
		}

		super.insert(bean);
		
		dispatchAfterInsert(bean);
	}

}