package org.mentabean.sql.functions;

import org.mentabean.sql.Function;
import org.mentabean.sql.Parametrizable;
import org.mentabean.sql.param.Param;

public class Upper extends Parametrizable implements Function {

	public Upper(Param param) {
		addParam(param);
	}
	
	@Override
	public String name() {
		return "UPPER";
	}
	
}