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
import org.mentabean.DBType;
import org.mentabean.type.AutoIncrementType;
import org.mentabean.util.Limit;
import org.mentabean.util.OrderBy;

/**
 * MySQL only supports auto-increment.
 * 
 * Now in mysql is 'now()'
 * 
 * @author soliveira
 * 
 */
public class MySQLBeanSession extends AnsiSQLBeanSession {

	public MySQLBeanSession(final BeanManager beanManager, final Connection conn) {

		super(beanManager, conn);
	}

	@Override
	protected String getCurrentTimestampCommand() {

		return "now()";
	}

	@Override
	protected String getDatabaseType(DBType<?> dbType) {
		if (dbType instanceof AutoIncrementType) {
			return "integer AUTO_INCREMENT";
		}
		return super.getDatabaseType(dbType);
	}

	/**
	 * MySQL is not like Oracle. It will SORT everything first and then apply LIMIT.
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

		super.insert(bean);

		// find autoincrement field...

		final BeanConfig bc = beanManager.getBeanConfig(bean.getClass());

		final DBField autoIncrement = bc.getAutoIncrementField();

		if (autoIncrement == null) {

			dispatchAfterInsert(bean);
			
			return;
		}

		PreparedStatement stmt = null;

		ResultSet rset = null;

		final StringBuilder sb = new StringBuilder("select last_insert_id() from ");

		sb.append(bc.getTableName());
		sb.append("limit 1");

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
			}

		} catch (Exception e) {

			throw new BeanException(e);

		} finally {

			close(stmt, rset);
		}
	}

}
