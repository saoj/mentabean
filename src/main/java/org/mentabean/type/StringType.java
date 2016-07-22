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

public class StringType implements DBType<String>, SizedType {

	private int size = DEFAULT_SIZE;

	private boolean canBeNull = true;

	@Override
	public boolean canBeNull() {
		return canBeNull;
	}

	public StringType nullable(boolean flag) {
		StringType d = new StringType();
		d.canBeNull = flag;
		d.size = this.size;
		return d;
	}

	@Override
	public String getAnsiType() {
		return "varchar"; // if your database do not support that you are in trouble !!!
	}

	@Override
	public String toString() {

		return this.getClass().getSimpleName();
	}

	public StringType size(int size) {
		// clone so we can use this with DBTypes (cleaner API)
		StringType clone = new StringType();
		clone.size = size;
		return clone;
	}

	@Override
	public int getSize() {
		return size;
	}

	@Override
	public String getFromResultSet(final ResultSet rset, final int index) throws SQLException {

		return rset.getString(index);
	}

	@Override
	public String getFromResultSet(final ResultSet rset, final String field) throws SQLException {

		return rset.getString(field);
	}

	@Override
	public Class<? extends Object> getTypeClass() {

		return String.class;
	}

	@Override
	public void bindToStmt(final PreparedStatement stmt, final int index, final String value) throws SQLException {

		if (value == null) {

			stmt.setString(index, null);

		} else {

			stmt.setString(index, value);
		}
	}
}