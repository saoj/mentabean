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

import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.mentabean.BeanException;
import org.mentabean.DBType;

public class EnumIdType implements DBType<Enum<?>> {

	private final Class<? extends Enum<?>> enumType;
	private final Method fromIdMethod;
	private final Method getIdMethod;

	private boolean canBeNull = false;

	@Override
	public boolean canBeNull() {
		return canBeNull;
	}

	public EnumIdType nullable(boolean flag) {
		EnumIdType d = new EnumIdType(enumType);
		d.canBeNull = flag;
		return d;
	}

	public EnumIdType(final Class<? extends Enum<?>> enumType) {
		this.enumType = enumType;
		this.fromIdMethod = getFromIdMethod(enumType);
		this.getIdMethod = getGetIdMethod(enumType);
	}

	@Override
	public String getAnsiType() {
		return "smallint";
	}

	private static Method getFromIdMethod(Class<? extends Enum<?>> enumType) {

		try {

			return enumType.getMethod("fromId", int.class);
		} catch (Exception e) {

			throw new BeanException("Cannot find fromId(int) method from enum class: " + enumType, e);
		}
	}

	private static Method getGetIdMethod(Class<? extends Enum<?>> enumType) {

		try {

			return enumType.getMethod("getId", (Class[]) null);
		} catch (Exception e) {

			throw new BeanException("Cannot find getId() method from enum class: " + enumType, e);
		}
	}

	private Enum<?> fromId(int id) {
		try {
			return (Enum<?>) fromIdMethod.invoke(null, id);
		} catch (Exception e) {
			throw new BeanException(e);
		}
	}

	private int getId(Enum<?> theEnum) {
		try {
			return (Integer) getIdMethod.invoke(theEnum, (Object[]) null);
		} catch (Exception e) {
			throw new BeanException(e);
		}
	}

	@Override
	public String toString() {

		return this.getClass().getSimpleName() + ": " + enumType.getSimpleName();
	}

	@Override
	public Enum<?> getFromResultSet(final ResultSet rset, final int index) throws SQLException {

		final int id = rset.getInt(index);

		if (rset.wasNull()) {
			return null;
		}

		return fromId(id);
	}

	@Override
	public Enum<?> getFromResultSet(final ResultSet rset, final String field) throws SQLException {

		final int id = rset.getInt(field);

		if (rset.wasNull()) {
			return null;
		}

		return fromId(id);
	}

	@Override
	public Class<? extends Object> getTypeClass() {

		return enumType;
	}

	@Override
	public void bindToStmt(final PreparedStatement stmt, final int index, final Enum<?> value) throws SQLException {

		if (value == null) {

			stmt.setNull(index, Types.INTEGER);

			//Support for overwritten enums (superclass on left)
		} else if (enumType.isAssignableFrom(value.getClass())) {

			final int id = getId(value);

			stmt.setInt(index, id);

		} else {

			throw new IllegalArgumentException("Value '"+value+"' from '"+value.getClass()+"' is not an enum!");
		}
	}

	public static enum Test {

		T1(1), T2(2), T3(3);

		private final int id;

		private Test(int id) {
			this.id = id;
		}

		public int getId() {
			return id;
		}

		public static Test fromId(int id) {
			for (Test t : Test.values()) {
				if (t.getId() == id) {
					return t;
				}
			}
			return null;
		}
	}

	public static void main(String[] args) throws Exception {

		EnumIdType eit = new EnumIdType(Test.class);

		System.out.println(eit.fromId(2));

		System.out.println(eit.getId(Test.T3));

	}
}