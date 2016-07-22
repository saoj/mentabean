package org.mentabean.sql.functions;

import org.mentabean.sql.Function;
import org.mentabean.sql.Parametrizable;
import org.mentabean.sql.param.Param;

public class Count extends Parametrizable implements Function {

	public Count(Param param) {
		addParam(param);
	}
	
	@Override
	public String name() {
		return "COUNT";
	}

}
