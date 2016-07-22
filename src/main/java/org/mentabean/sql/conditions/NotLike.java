package org.mentabean.sql.conditions;

import org.mentabean.sql.param.Param;

public class NotLike extends SimpleComparison {

	public NotLike(Param param) {
		super(param);
	}
	
	public NotLike(Object value) {
		super(value);
	}

	@Override
	public String comparisonSignal() {
		return "NOT LIKE";
	}


}
