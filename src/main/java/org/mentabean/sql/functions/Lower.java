package org.mentabean.sql.functions;

import org.mentabean.sql.Function;
import org.mentabean.sql.Parametrizable;
import org.mentabean.sql.param.Param;

public class Lower extends Parametrizable implements Function {

	private Param param;
	
	public Lower(Param param) {
		this.param = param;
	}
	
	@Override
	public Param[] getParams() {
		return new Param[] {param};
	}
	
	@Override
	public String name() {
		return "LOWER";
	}
	
}