package org.mentabean.sql.conditions;

import org.mentabean.sql.param.Param;

public class NotIn extends In {

	public NotIn(Param param) {
		super(param);
	}

	@Override
	public String name() {
		return "NOT IN";
	}
	
}
