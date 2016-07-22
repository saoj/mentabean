package org.mentabean.sql;

import org.mentabean.sql.param.Param;

public interface HasParams {

	public Param[] getParams();
	
	public String build();
	
}
