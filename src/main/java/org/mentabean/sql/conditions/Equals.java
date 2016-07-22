package org.mentabean.sql.conditions;

import org.mentabean.sql.Condition;
import org.mentabean.sql.param.Param;
import org.mentabean.sql.param.ParamValue;

public class Equals implements Condition {

	protected Param param;
	
	public Equals(Object value) {
		if (value != null) {
			this.param = new ParamValue(value);
		}
	}
	
	public Equals(Param param) {
		this.param = param;
	}

	@Override
	public Param[] getParams() {
		return new Param[] {param};
	}

	@Override
	public String build() {
		
		if (param == null)
			return "IS NULL";
		
		return "= "+param.paramInQuery();
	}
	
}
