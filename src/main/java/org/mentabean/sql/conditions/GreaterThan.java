package org.mentabean.sql.conditions;

import org.mentabean.sql.param.Param;

public class GreaterThan extends SimpleComparison {

	public GreaterThan(Param param) {
		super(param);
	}
	
	public GreaterThan(Object param) {
		super(param);
	}

	@Override
	public String comparisonSignal() {
		return ">";
	}

}
