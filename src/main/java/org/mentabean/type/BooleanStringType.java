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

import org.mentabean.BeanException;
import org.mentabean.DBType;

public class BooleanStringType implements DBType<Boolean>, SizedType {

	private final String sTrue;
	private final String sFalse;
	private boolean canBeNull = true;

	public BooleanStringType() {
		this("T", "F");
	}

	public BooleanStringType(final String sTrue, final String sFalse) {
		this.sTrue = sTrue;
		this.sFalse = sFalse;
	}
	
	public static BooleanStringType values(String sTrue, String sFalse) {
		return new BooleanStringType(sTrue, sFalse);
	}

	@Override
	public boolean canBeNull() {
		return canBeNull;
	}

	@Override
	public String getAnsiType() {
		return "varchar";
	}

	public BooleanStringType nullable(boolean flag) {
		// clone:
		BooleanStringType b = new BooleanStringType(this.sTrue, this.sFalse);
		b.canBeNull = flag;
		return b;
	}

	public BooleanStringType size(int size) {
		throw new UnsupportedOperationException("Cannot set size of a " + this);
	}

	@Override
	public int getSize() {
		return Math.max(sTrue.length(), sFalse.length());
	}

	@Override
	public boolean equals(final Object obj) {

		if (obj instanceof BooleanStringType) {

			final BooleanStringType bt = (BooleanStringType) obj;

			if (bt.sTrue.equals(this.sTrue) && bt.sFalse.equals(this.sFalse)) {

				return true;
			}
		}

		return false;
	}

	@Override
	public String toString() {

		return this.getClass().getSimpleName();
	}

	protected Boolean getBooleanValue(final String s) {

		if (s.equals(sTrue)) {
			return true;
		}

		if (s.equals(sFalse)) {
			return false;
		}

		return null;
	}

	private static Boolean getBoolValue(final boolean b) {

		if (b) {
			return Boolean.TRUE;
		}

		return Boolean.FALSE;
	}

	@Override
	public Boolean getFromResultSet(final ResultSet rset, final int index) throws SQLException {

		final String s = rset.getString(index);
		
		if (rset.wasNull() || s == null) {
			return null;
		}

		final Boolean b = getBooleanValue(s);
		
		if (b == null) {
			throw new BeanException("Don't know how to convert String to boolean:" + s);
		}

		return getBoolValue(b);
	}

	@Override
	public Boolean getFromResultSet(final ResultSet rset, final String field) throws SQLException {

		final String s = rset.getString(field);
		
		if (rset.wasNull() || s == null) {
			return null;
		}

		final Boolean b = getBooleanValue(s);
		
		if (b == null) {
			throw new BeanException("Don't know how to convert String to boolean: " + s);
		}

		return getBoolValue(b);
	}

	@Override
	public Class<? extends Object> getTypeClass() {

		return Boolean.class;
	}

	@Override
	public void bindToStmt(final PreparedStatement stmt, final int index, final Boolean value) throws SQLException {

		if (value == null) {

			stmt.setString(index, null);

		} else {

			final String s = value ? sTrue : sFalse;

			stmt.setString(index, s);
		}
	}
}