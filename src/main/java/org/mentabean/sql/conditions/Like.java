package org.mentabean.sql.conditions;

import org.mentabean.sql.param.Param;

public class Like extends SimpleComparison {

	public Like(Param param) {
		super(param);
	}
	
	public Like(Object value) {
		super(value);
	}

	@Override
	public String comparisonSignal() {
		return "LIKE";
	}


}
