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

public class BooleanIntType implements DBType<Boolean> {

	private boolean canBeNull = true;

	@Override
	public boolean canBeNull() {
		return canBeNull;
	}

	public BooleanIntType nullable(boolean flag) {
		// clone
		BooleanIntType b = new BooleanIntType();
		b.canBeNull = flag;
		return b;
	}

	@Override
	public String getAnsiType() {
		return "smallint";
	}

	@Override
	public String toString() {

		return this.getClass().getSimpleName();
	}

	private static Boolean getBoolValue(final int x) throws SQLException {

		if (x == 1) {
			return Boolean.TRUE;
		}

		if (x == 0) {
			return Boolean.FALSE;
		}

		throw new SQLException("integer is not 0 or 1: " + x);
	}

	@Override
	public Boolean getFromResultSet(final ResultSet rset, final int index) throws SQLException {

		final int x = rset.getInt(index);
		
		if (rset.wasNull()) {
			return null;
		}

		return getBoolValue(x);
	}

	@Override
	public Boolean getFromResultSet(final ResultSet rset, final String field) throws SQLException {

		final int x = rset.getInt(field);
		
		if (rset.wasNull()) {
			return null;
		}

		return getBoolValue(x);
	}

	@Override
	public Class<? extends Object> getTypeClass() {

		return Boolean.class;
	}

	@Override
	public void bindToStmt(final PreparedStatement stmt, final int index, final Boolean value) throws SQLException {

		if (value == null) {

			stmt.setInt(index, 0);

		} else if (value instanceof Boolean) {

			final int x = value ? 1 : 0;

			stmt.setInt(index, x);
		}

	}
}