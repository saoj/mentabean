package org.mentabean.sql.functions;

import org.mentabean.sql.Function;
import org.mentabean.sql.Parametrizable;
import org.mentabean.sql.param.Param;
import org.mentabean.sql.param.ParamValue;

public class Nullif extends Parametrizable implements Function {

	private Param p1, p2;
	
	public Nullif(Param p1, Param p2) {
		this.p1 = p1;
		this.p2 = p2;
	}
	
	public Nullif(Object p1, Object p2) {
		this.p1 = new ParamValue(p1);
		this.p2 = new ParamValue(p2);
	}
	
	@Override
	public Param[] getParams() {
		return new Param[] {p1, p2};
	}
	
	@Override
	public String name() {
		return "NULLIF";
	}
	
}
