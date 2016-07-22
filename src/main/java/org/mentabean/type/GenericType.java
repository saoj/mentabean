package org.mentabean.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.mentabean.DBType;

/**
 * Generic type that uses a get/setObject from ResultSet/PreparedStatement respectively
 * <br><br><i>Note this class must NOT be used to create tables. Databases doesn't have a generic type</i>
 * @author erico
 *
 */
public class GenericType implements DBType<Object> {

	private boolean canBeNull = true;

	@Override
	public boolean canBeNull() {
		return canBeNull;
	}

	public GenericType nullable(boolean flag) {
		GenericType d = new GenericType();
		d.canBeNull = flag;
		return d;
	}

	@Override
	public String getAnsiType() {
		return null; // we don't have generic types in database level
	}

	@Override
	public String toString() {

		return this.getClass().getSimpleName();
	}

	@Override
	public Object getFromResultSet(final ResultSet rset, final int index) throws SQLException {

		Object x = rset.getObject(index);
		
		if (rset.wasNull()) {
			return null;
		}
		
		return x;
	}

	@Override
	public Object getFromResultSet(final ResultSet rset, final String field) throws SQLException {

		Object x = rset.getObject(field);
		
		if (rset.wasNull()) {
			return null;
		}
		
		return x;
	}

	@Override
	public Class<? extends Object> getTypeClass() {

		return Object.class;
	}

	@Override
	public void bindToStmt(final PreparedStatement stmt, final int index, final Object value) throws SQLException {

		stmt.setObject(index, value);
	}
}