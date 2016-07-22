package org.mentabean.sql.functions;

import org.mentabean.sql.Function;
import org.mentabean.sql.Parametrizable;
import org.mentabean.sql.param.Param;
import org.mentabean.sql.param.ParamFunction;
import org.mentabean.sql.param.ParamValue;

public class Substring extends Parametrizable implements Function {

	private Param str, beginIndex, endIndex;
	
	public Substring(Param str) {
		this.str = str;
	}
	
	public Substring endIndex(Param param) {
		endIndex = param;
		return this;
	}
	
	public Substring beginIndex(Param param) {
		beginIndex = param;
		return this;
	}
	
	@Override
	public Param[] getParams() {
		
		if (beginIndex == null)
			beginIndex = new ParamValue(0);
		if (endIndex == null)
			endIndex = new ParamFunction(new Length(str));
		
		return new Param[] {str, beginIndex, endIndex};
	}
	
	@Override
	public String name() {
		return "SUBSTRING";
	}

}
