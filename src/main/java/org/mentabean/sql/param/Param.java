package org.mentabean.sql.param;

public interface Param {

	/**
	 * Represents the parameters in query. In other words, this method returns a String
	 * with the expression exactly as will be shown in SQL before its execution.
	 * @return String
	 */
	public String paramInQuery();
	
	/**
	 * The parameter's values
	 * @return Object[]
	 */
	public Object[] values();
	
}
