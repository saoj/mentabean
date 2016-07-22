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

public class DoubleType implements DBType<Double> {

	private boolean canBeNull = true;

	@Override
	public boolean canBeNull() {
		return canBeNull;
	}

	public DoubleType nullable(boolean flag) {
		DoubleType d = new DoubleType();
		d.canBeNull = flag;
		return d;
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
	public Double getFromResultSet(final ResultSet rset, final int index) throws SQLException {

		Double x =  rset.getDouble(index);
		
		if (rset.wasNull()) {
			return null;
		}
		
		return x;
	}

	@Override
	public Double getFromResultSet(final ResultSet rset, final String field) throws SQLException {

		Double x = rset.getDouble(field);
		
		if (rset.wasNull()) {
			return null;
		}
		
		return x;
	}

	@Override
	public Class<? extends Object> getTypeClass() {

		return Double.class;
	}

	@Override
	public void bindToStmt(final PreparedStatement stmt, final int index, final Double value) throws SQLException {

		if (value == null || value.isNaN() || value.isInfinite()) {

			stmt.setNull(index, java.sql.Types.DOUBLE);

		} else {

			stmt.setDouble(index, value.doubleValue());
		}
	}
}