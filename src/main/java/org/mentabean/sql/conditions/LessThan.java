package org.mentabean.sql.conditions;

import org.mentabean.sql.param.Param;

public class LessThan extends SimpleComparison {

	public LessThan(Param param) {
		super(param);
	}
	
	public LessThan(Object param) {
		super(param);
	}

	@Override
	public String comparisonSignal() {
		return "<";
	}

}
