package org.mentabean.sql.conditions;

import org.mentabean.sql.param.Param;

public class NotEquals extends Equals {

	public NotEquals(Param param) {
		super(param);
	}
	
	public NotEquals(Object param) {
		super(param);
	}

	@Override
	public String build() {
		
		if (param == null)
			return "IS NOT NULL";
		
		return "<> "+param.paramInQuery();
	}
	
}
