package org.mentabean.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;

import org.joda.time.DateTime;
import org.mentabean.DBType;

public class DateTimeJodaType implements DBType<DateTime> {

	@Override
	public DateTime getFromResultSet(ResultSet rset, int index)
			throws SQLException {
		
		return rset.getTimestamp(index) == null ? null : new DateTime(rset.getTimestamp(index));
	}

	@Override
	public void bindToStmt(PreparedStatement stmt, int index, DateTime value)
			throws SQLException {
		
		if (value == null)
			stmt.setNull(index, Types.TIMESTAMP);
		else
			stmt.setTimestamp(index, new Timestamp(value.getMillis()));
	}

	@Override
	public DateTime getFromResultSet(ResultSet rset, String field)
			throws SQLException {
				
		return rset.getTimestamp(field) == null ? null : new DateTime(rset.getTimestamp(field));
	}

	@Override
	public Class<? extends Object> getTypeClass() {
		
		return DateTime.class;
	}

	@Override
	public boolean canBeNull() {

		return true;
	}

	@Override
	public String getAnsiType() {
		
		return "timestamp";
	}

}
