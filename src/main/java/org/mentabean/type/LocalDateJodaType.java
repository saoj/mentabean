package org.mentabean.type;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.joda.time.LocalDate;
import org.mentabean.DBType;

public class LocalDateJodaType implements DBType<LocalDate> {

	@Override
	public LocalDate getFromResultSet(ResultSet rset, int index) throws SQLException {
		return rset.getDate(index) == null ? null : new LocalDate(rset.getDate(index));
	}

	@Override
	public void bindToStmt(PreparedStatement stmt, int index, LocalDate value) throws SQLException {
		
		if (value == null)
			stmt.setNull(index, Types.DATE);
		else
			stmt.setDate(index, new Date(value.toDate().getTime()));
	}

	@Override
	public LocalDate getFromResultSet(ResultSet rset, String field) throws SQLException {
				
		return rset.getDate(field) == null ? null : new LocalDate(rset.getDate(field));
	}

	@Override
	public Class<? extends Object> getTypeClass() {
		
		return LocalDate.class;
	}

	@Override
	public boolean canBeNull() {

		return true;
	}

	@Override
	public String getAnsiType() {
		
		return "date";
	}

}
