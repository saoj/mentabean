package org.mentabean.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Types;

import org.joda.time.LocalTime;
import org.mentabean.DBType;

public class LocalTimeJodaType implements DBType<LocalTime> {

	@Override
	public LocalTime getFromResultSet(ResultSet rset, int index) throws SQLException {
		
		return rset.getTime(index) == null ? null : new LocalTime(rset.getTime(index));
	}

	@Override
	public void bindToStmt(PreparedStatement stmt, int index, LocalTime value) throws SQLException {
		
		if (value == null)
			stmt.setNull(index, Types.TIME);
		else
			stmt.setTime(index, new Time(value.toDateTimeToday().getMillis()));
	}

	@Override
	public LocalTime getFromResultSet(ResultSet rset, String field) throws SQLException {
				
		return rset.getTime(field) == null ? null : new LocalTime(rset.getTime(field));
	}

	@Override
	public Class<? extends Object> getTypeClass() {
		
		return LocalTime.class;
	}

	@Override
	public boolean canBeNull() {

		return true;
	}

	@Override
	public String getAnsiType() {
		
		return "time";
	}

}
