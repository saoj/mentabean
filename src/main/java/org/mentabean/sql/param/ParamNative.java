package org.mentabean.sql.param;

public class ParamNative implements Param {

	private Object param;
	
	public ParamNative(Object param) {
		this.param = param;
	}

	@Override
	public String paramInQuery() {
		return param.toString();
	}

	@Override
	public Object[] values() {
		return null;
	}
	
}
