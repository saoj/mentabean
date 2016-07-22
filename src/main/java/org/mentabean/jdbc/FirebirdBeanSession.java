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
import java.util.List;
import java.util.Map;

import org.mentabean.BeanConfig;
import org.mentabean.BeanException;
import org.mentabean.BeanManager;
import org.mentabean.DBField;
import org.mentabean.util.Limit;
import org.mentabean.util.OrderBy;

/**
 * Firebird support.
 * 
 * @author soliveira
 */
public class FirebirdBeanSession extends AnsiSQLBeanSession {

	public FirebirdBeanSession(final BeanManager beanManager, final Connection conn) {

		super(beanManager, conn);
	}

	@Override
	protected String getCurrentTimestampCommand() {

		return "current_timestamp";
	}

	/**
	 * MySQL is not like Oracle. It will SORT everything first and then apply LIMIT.
	 */
	@Override
	protected StringBuilder handleLimit(final StringBuilder sb, final OrderBy orderBy, final Limit limit) {

		if (limit == null || limit.intValue() <= 0) {
			return sb;
		}

		String query = sb.toString();

		if (query.toLowerCase().startsWith("select ")) {
			throw new BeanException("Got a limit query that does not start with select: " + query);
		}

		String withoutSelect = query.substring("select".length());

		final StringBuilder sbLimit = new StringBuilder(withoutSelect.length() + 64);

		sbLimit.append("SELECT first(").append(limit).append(")").append(withoutSelect);

		return sbLimit;
	}

	@Override
	public void insert(final Object bean) {

		// find autoincrement field...

		final BeanConfig bc = beanManager.getBeanConfig(bean.getClass());

		final DBField autoIncrement = bc.getAutoIncrementField();

		if (autoIncrement == null) {

			super.insert(bean);
			
			dispatchAfterInsert(bean);

			return;
		}

		QueryAndValues qav = prepareInsertQuery(bean);

		StringBuilder sb = qav.sb;

		List<Value> values = qav.values;

		if (conn == null) {
			throw new BeanException("Connection is null!");
		}

		PreparedStatement stmt = null;
		ResultSet rset = null;

		try {

			// add the returning...

			sb.append(" returning ").append(autoIncrement.getDbName());

			if (DEBUG) {
				System.out.println("INSERT SQL: " + sb.toString());
			}

			stmt = conn.prepareStatement(sb.toString());

			Map<String, Value> fieldsLoaded = bindToInsertStatement(stmt, values);

			rset = stmt.executeQuery();

			if (!rset.next()) {
				throw new BeanException("Nothing was inserted! Insert returned no result set!");
			}

			int id = rset.getInt(autoIncrement.getDbName());

			injectValue(bean, autoIncrement.getName(), id, Integer.class);

			loaded.put(bean, fieldsLoaded);
			
			dispatchAfterInsert(bean);

		} catch (Exception e) {

			throw new BeanException(e);

		} finally {
			close(stmt, rset);
		}
	}
}