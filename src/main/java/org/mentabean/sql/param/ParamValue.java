package org.mentabean.sql.param;

public class ParamValue implements Param {

	private Object value;
	
	public ParamValue(Object value) {
		this.value = value;
	}
	
	@Override
	public String paramInQuery() {
		return "?";
	}

	@Override
	public Object[] values() {
		return new Object[] {value};
	}

}
