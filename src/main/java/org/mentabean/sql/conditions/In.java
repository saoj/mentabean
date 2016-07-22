package org.mentabean.sql.conditions;

import org.mentabean.sql.Condition;
import org.mentabean.sql.Parametrizable;
import org.mentabean.sql.param.Param;

public class In extends Parametrizable implements Condition {
	
	@Override
	public String name() {
		return "IN";
	}
	
	public In (Param param) {
		addParam(param);
	}
	
	@Override
	public Parametrizable addParam(Param param) {
		return super.addParam(param);
	}
	
}
