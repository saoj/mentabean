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
package org.mentabean.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.mentabean.DBType;

public class LongType implements DBType<Long> {

	private boolean canBeNull = true;

	@Override
	public boolean canBeNull() {
		return canBeNull;
	}

	public LongType nullable(boolean flag) {
		LongType d = new LongType();
		d.canBeNull = flag;
		return d;
	}

	@Override
	public String getAnsiType() {
		return "integer"; // most database will not support BIGINT
	}

	@Override
	public String toString() {

		return this.getClass().getSimpleName();
	}

	@Override
	public Long getFromResultSet(final ResultSet rset, final int index) throws SQLException {

		Long x = rset.getLong(index);
		
		if (rset.wasNull()) {
			return null;
		}
		
		return x;
	}

	@Override
	public Long getFromResultSet(final ResultSet rset, final String field) throws SQLException {

		Long x = rset.getLong(field);
		
		if (rset.wasNull()) {
			return null;
		}
		
		return x;
	}

	@Override
	public Class<? extends Object> getTypeClass() {

		return Long.class;
	}

	@Override
	public void bindToStmt(final PreparedStatement stmt, final int index, final Long value) throws SQLException {

		if (value == null) {

			stmt.setNull(index, java.sql.Types.BIGINT);

		} else {

			stmt.setLong(index, value.longValue());
		}
	}
}