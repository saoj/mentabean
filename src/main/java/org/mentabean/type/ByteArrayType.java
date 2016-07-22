package org.mentabean.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.mentabean.DBType;

/**
 * ByteArrayType that uses a get/setBytes from ResultSet/PreparedStatement respectively
 * 
 * @author erico
 *
 */
public class ByteArrayType implements DBType<byte[]> {

	private boolean canBeNull = true;

	@Override
	public boolean canBeNull() {
		return canBeNull;
	}

	public ByteArrayType nullable(boolean flag) {
		ByteArrayType d = new ByteArrayType();
		d.canBeNull = flag;
		return d;
	}

	@Override
	public String getAnsiType() {
		return "blob";
	}

	@Override
	public String toString() {

		return ByteArrayType.class.getSimpleName();
	}

	@Override
	public byte[] getFromResultSet(final ResultSet rset, final int index) throws SQLException {

		byte[] x = rset.getBytes(index);
		
		if (rset.wasNull()) {
			return null;
		}
		
		return x;
	}

	@Override
	public byte[] getFromResultSet(final ResultSet rset, final String field) throws SQLException {

		byte[] x = rset.getBytes(field);
		
		if (rset.wasNull()) {
			return null;
		}
		
		return x;
	}

	@Override
	public Class<? extends Object> getTypeClass() {

		return byte[].class;
	}

	@Override
	public void bindToStmt(final PreparedStatement stmt, final int index, final byte[] value) throws SQLException {

		stmt.setBytes(index, value);
	}
}