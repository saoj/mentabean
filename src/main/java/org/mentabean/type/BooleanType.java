package org.mentabean.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.mentabean.DBType;

public class BooleanType implements DBType<Boolean> {

	private boolean canBeNull = true;

	@Override
	public boolean canBeNull() {
		return canBeNull;
	}

	public BooleanType nullable(boolean flag) {
		BooleanType bt = new BooleanType();
		bt.canBeNull = flag;
		return bt;
	}

	@Override
	public Boolean getFromResultSet(ResultSet rset, int index) throws SQLException {

		Boolean b = rset.getBoolean(index);

		if (rset.wasNull()) {
			
			return null;
		}

		return b;
	}

	@Override
	public void bindToStmt(PreparedStatement stmt, int index, Boolean value)throws SQLException {
		
		if (value == null) {
			
			stmt.setNull(index, Types.BOOLEAN);
			
		}else {
			
			stmt.setBoolean(index, value);
		}
	}

	@Override
	public Boolean getFromResultSet(ResultSet rset, String field) throws SQLException {
		
		Boolean b = rset.getBoolean(field);

		if (rset.wasNull()) {
			
			return null;
		}

		return b;
	}

	@Override
	public Class<? extends Object> getTypeClass() {
		
		return Boolean.class;
	}

	@Override
	public String getAnsiType() {
		
		return "boolean";
	}

}
