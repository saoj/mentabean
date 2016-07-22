package org.mentabean.sql.conditions;

import org.mentabean.sql.Condition;
import org.mentabean.sql.param.Param;
import org.mentabean.sql.param.ParamValue;

public abstract class AbstractBetween implements Condition {
	
	protected Param begin, end;
	
	public AbstractBetween(Param begin, Param end) {
		this.begin = begin;
		this.end = end;
	}
	
	public AbstractBetween(Object beginValue, Object endValue) {
		if (beginValue != null)
			begin = new ParamValue(beginValue);
		if (endValue != null)
			end = new ParamValue(endValue);
	}
	
	@Override
	public Param[] getParams() {
		return new Param[] {begin, end};
	}

}
