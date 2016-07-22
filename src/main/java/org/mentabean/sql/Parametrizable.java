package org.mentabean.sql;

import java.util.ArrayList;
import java.util.List;

import org.mentabean.sql.param.Param;

public abstract class Parametrizable implements HasParams {

	protected List<Param> params = new ArrayList<Param>();
	
	public abstract String name();
	
	@Override
	public Param[] getParams() {
		return params.toArray(new Param[0]);
	}
	
	protected Parametrizable addParam(Param param) {
		params.add(param);
		return this;
	}
	
	@Override
	public String build() {
		
		StringBuilder sb = new StringBuilder(name());
		
		sb.append(" (");
		
		for (Param param : getParams()) {
			sb.append(param.paramInQuery()).append(',');
		}
		
		sb.setCharAt(sb.length()-1, ')');
		
		return sb.toString();
	}
	
}
