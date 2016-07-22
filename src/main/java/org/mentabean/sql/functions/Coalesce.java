package org.mentabean.sql.functions;

import org.mentabean.sql.Function;
import org.mentabean.sql.Parametrizable;
import org.mentabean.sql.param.Param;

public class Coalesce extends Parametrizable implements Function {

	@Override
	public String name() {
		return "COALESCE";
	}
	
	@Override
	public Coalesce addParam(Param param) {
		super.addParam(param);
		return this;
	}
	
}
