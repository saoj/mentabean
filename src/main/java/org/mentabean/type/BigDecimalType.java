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

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.mentabean.DBType;

public class BigDecimalType implements DBType<BigDecimal> {

	private boolean canBeNull = true;

	@Override
	public boolean canBeNull() {
		return canBeNull;
	}

	public BigDecimalType nullable(boolean flag) {
		// clone:
		BigDecimalType b = new BigDecimalType();
		canBeNull = flag;
		return b;
	}

	@Override
	public String getAnsiType() {
		return "double precision";
	}

	@Override
	public String toString() {

		return this.getClass().getSimpleName();
	}

	@Override
	public BigDecimal getFromResultSet(final ResultSet rset, final int index) throws SQLException {

		BigDecimal bd = rset.getBigDecimal(index);
		
		if (rset.wasNull()) {
			return null;
		}

		return bd;
	}

	@Override
	public BigDecimal getFromResultSet(final ResultSet rset, final String field) throws SQLException {
		
		BigDecimal bd = rset.getBigDecimal(field);
		
		if (rset.wasNull()) {
			return null;
		}

		return bd;
	}

	@Override
	public Class<? extends Object> getTypeClass() {

		return BigDecimal.class;
	}

	@Override
	public void bindToStmt(final PreparedStatement stmt, final int index, final BigDecimal value) throws SQLException {

		if (value == null) {

			stmt.setNull(index, java.sql.Types.DECIMAL);

		} else {

			stmt.setBigDecimal(index, value);
		}
	}
}
