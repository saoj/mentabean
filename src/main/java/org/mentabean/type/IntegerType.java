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

public class IntegerType implements DBType<Integer> {

	private boolean canBeNull = true;

	@Override
	public boolean canBeNull() {
		return canBeNull;
	}

	public IntegerType nullable(boolean flag) {
		IntegerType d = new IntegerType();
		d.canBeNull = flag;
		return d;
	}

	@Override
	public String getAnsiType() {
		return "integer";
	}

	@Override
	public String toString() {

		return this.getClass().getSimpleName();
	}

	@Override
	public Integer getFromResultSet(final ResultSet rset, final int index) throws SQLException {

		Integer x = rset.getInt(index);
		
		if (rset.wasNull()) {
			return null;
		}
		
		return x;
	}

	@Override
	public Integer getFromResultSet(final ResultSet rset, final String field) throws SQLException {

		Integer x = rset.getInt(field);
		
		if (rset.wasNull()) {
			return null;
		}
		
		return x;
	}

	@Override
	public Class<? extends Object> getTypeClass() {

		return Integer.class;
	}

	@Override
	public void bindToStmt(final PreparedStatement stmt, final int index, final Integer value) throws SQLException {

		if (value == null) {

			stmt.setNull(index, java.sql.Types.INTEGER);

		} else {

			stmt.setInt(index, value.intValue());
		}
	}
}