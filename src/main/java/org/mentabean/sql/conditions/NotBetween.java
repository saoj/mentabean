package org.mentabean.sql.conditions;

import org.mentabean.sql.param.Param;

public class NotBetween extends AbstractBetween {

	public NotBetween(Param begin, Param end) {
		super(begin, end);
	}
	
	public NotBetween(Object beginValue, Object endValue) {
		super(beginValue, endValue);
	}

	@Override
	public String build() {
		
		if (begin == null && end == null)
			return "";
		
		if (begin == null) {
			return new GreaterThan(end).build();
		}
		if (end == null) {
			return new LessThan(begin).build();
		}
		
		return "NOT BETWEEN "+begin.paramInQuery()+" AND "+end.paramInQuery();
	}

}
