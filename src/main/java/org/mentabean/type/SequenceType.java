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

public class SequenceType implements DBType<Number> {

	@Override
	public boolean canBeNull() {
		return false;
	}

	@Override
	public String toString() {

		return this.getClass().getSimpleName();
	}

	@Override
	public String getAnsiType() {
		return "integer";
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
	public void bindToStmt(final PreparedStatement stmt, final int index, final Number value) throws SQLException {

		if (value == null) {

			stmt.setNull(index, java.sql.Types.INTEGER);

		} else {

			stmt.setLong(index, value.longValue());
		}
	}
}