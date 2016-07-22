package org.mentabean.sql.conditions;

import org.mentabean.sql.Condition;
import org.mentabean.sql.param.Param;
import org.mentabean.sql.param.ParamValue;

public abstract class SimpleComparison implements Condition {

	private Param param;
	
	public abstract String comparisonSignal();
	
	public SimpleComparison(Param param) {
		this.param = param;
	}
	
	public SimpleComparison(Object value) {
		this.param = new ParamValue(value);
	}
	
	@Override
	public Param[] getParams() {
		return new Param[] {param};
	}

	@Override
	public String build() {
		return comparisonSignal()+" "+param.paramInQuery();
	}

}
