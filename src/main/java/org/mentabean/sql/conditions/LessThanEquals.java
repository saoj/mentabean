package org.mentabean.sql.conditions;

import org.mentabean.sql.param.Param;

public class LessThanEquals extends SimpleComparison {

	public LessThanEquals(Param param) {
		super(param);
	}
	
	public LessThanEquals(Object param) {
		super(param);
	}

	@Override
	public String comparisonSignal() {
		return "<=";
	}

}
