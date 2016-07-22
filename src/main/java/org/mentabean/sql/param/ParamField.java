package org.mentabean.sql.param;

import org.mentabean.jdbc.QueryBuilder.Alias;

public class ParamField implements Param {

	private String column;
	
	public ParamField(Alias<?> alias, Object property) {
		column = alias.toColumn(property);
	}
	
	@Override
	public String paramInQuery() {
		return column;
	}

	@Override
	public Object[] values() {
		return null;
	}

}
