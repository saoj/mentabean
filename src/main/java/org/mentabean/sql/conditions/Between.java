package org.mentabean.sql.conditions;

import org.mentabean.sql.param.Param;

public class Between extends AbstractBetween {

	private boolean not;
	
	public Between(Param begin, Param end) {
		super(begin, end);
	}
	
	public Between(Object beginValue, Object endValue) {
		super(beginValue, endValue);
	}
	
	public Between not() {
		not = true;
		return this;
	}
	
	@Override
	public String build() {
		
		if (begin == null && end == null) {
			
			return "";
		}
		
		if (begin == null) {
			
			if (not) {
				
				return new GreaterThan(end).build();
			}
			
			return new LessThanEquals(end).build();
		}
		
		if (end == null) {
			
			if (not) {
				
				return new LessThan(begin).build();
			}
			
			return new GreaterThanEquals(begin).build();
		}
		
		String str = "BETWEEN "+begin.paramInQuery()+" AND "+end.paramInQuery();
		return not ? "NOT "+str : str;
	}
	
}
