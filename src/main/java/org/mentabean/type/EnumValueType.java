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

public class EnumValueType implements DBType<Enum<?>>, SizedType {

	private final Class<? extends Enum<?>> enumType;
	private final int size;

	private boolean canBeNull = false;

	@Override
	public boolean canBeNull() {
		return canBeNull;
	}

	public EnumValueType nullable(boolean flag) {
		EnumValueType d = new EnumValueType(enumType);
		d.canBeNull = flag;
		return d;
	}

	public EnumValueType(final Class<? extends Enum<?>> enumType) {

		this.enumType = enumType;
		this.size = calcSize();
	}

	@Override
	public String getAnsiType() {
		return "varchar";
	}

	public EnumValueType size(int size) {
		throw new UnsupportedOperationException("Cannot set the size on a " + this);
	}

	@Override
	public int getSize() {
		return size;
	}

	private int calcSize() {
		Enum<?>[] all = enumType.getEnumConstants();
		int max = 0;
		for (Enum<?> o : all) {
			max = Math.max(max, o.name().length());
		}
		return max;
	}

	@Override
	public String toString() {

		return this.getClass().getSimpleName() + ": " + enumType.getSimpleName();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Enum<?> getFromResultSet(final ResultSet rset, final int index) throws SQLException {

		final String s = rset.getString(index);

		if (s == null) {
			return null;
		}

		return Enum.valueOf((Class) enumType, s);

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Enum<?> getFromResultSet(final ResultSet rset, final String field) throws SQLException {

		final String s = rset.getString(field);

		if (s == null) {
			return null;
		}

		return Enum.valueOf((Class) enumType, s);

	}

	@Override
	public Class<? extends Object> getTypeClass() {

		return enumType;
	}

	@Override
	public void bindToStmt(final PreparedStatement stmt, final int index, final Enum<?> value) throws SQLException {

		if (value == null) {

			stmt.setString(index, null);

			//Support for overwritten enums (superclass on left)
		} else if (enumType.isAssignableFrom(value.getClass())) {

			final String s = value.name();

			stmt.setString(index, s);

		} else {

			throw new IllegalArgumentException("Value '"+value+"' from '"+value.getClass()+"' is not an enum!");
		}

	}
}