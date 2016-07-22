package org.mentabean.sql.param;

import org.mentabean.jdbc.QueryBuilder;

public interface ParamHandler {

	public Param findBetter(QueryBuilder builder, Object property);
	
}
