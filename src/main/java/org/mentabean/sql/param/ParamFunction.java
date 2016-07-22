package org.mentabean.sql.param;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.mentabean.sql.Function;

public class ParamFunction implements Param {

	private Function function;
	
	public ParamFunction(Function function) {
		this.function = function;
	}
	
	@Override
	public String paramInQuery() {
		return function.build();
	}

	@Override
	public Object[] values() {
		
		List<Object> values = new ArrayList<Object>();
		
		Param[] params = function.getParams();
		
		if (params != null && params.length > 0) {
			for (Param p : params) {
				if (p.values() != null && p.values().length > 0) {
					values.addAll(Arrays.asList(p.values()));
				}
			}
		}
		
		return values.toArray();
	}

}
