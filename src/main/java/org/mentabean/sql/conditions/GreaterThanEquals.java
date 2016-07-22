package org.mentabean.sql.conditions;

import org.mentabean.sql.param.Param;

public class GreaterThanEquals extends SimpleComparison {

	public GreaterThanEquals(Param param) {
		super(param);
	}
	
	public GreaterThanEquals(Object param) {
		super(param);
	}

	@Override
	public String comparisonSignal() {
		return ">=";
	}

}
