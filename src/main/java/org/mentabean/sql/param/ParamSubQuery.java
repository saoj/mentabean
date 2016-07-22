package org.mentabean.sql.param;

import org.mentabean.jdbc.QueryBuilder.Query;

public class ParamSubQuery implements Param {

	private Query query;
	
	public ParamSubQuery(Query query) {
		this.query = query;
	}
	
	@Override
	public String paramInQuery() {
		return "("+query.getSQL()+")";
	}

	@Override
	public Object[] values() {
		return query.getParamValues().toArray();
	}

}
