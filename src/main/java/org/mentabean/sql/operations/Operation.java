package org.mentabean.sql.operations;

import java.util.ArrayList;
import java.util.List;

import org.mentabean.BeanException;
import org.mentabean.jdbc.QueryBuilder.Alias;
import org.mentabean.jdbc.QueryBuilder.Query;
import org.mentabean.sql.Condition;
import org.mentabean.sql.Function;
import org.mentabean.sql.param.Param;
import org.mentabean.sql.param.ParamField;
import org.mentabean.sql.param.ParamFunction;
import org.mentabean.sql.param.ParamSubQuery;
import org.mentabean.sql.param.ParamValue;

public abstract class Operation implements Function, Condition {

	private List<Param> params = new ArrayList<Param>();
	
	public abstract String operationSignal();
	
	public Operation param(Param p) {
		this.params.add(p);
		return this;
	}
	
	public Operation param(Function function) {
		this.params.add(new ParamFunction(function));
		return this;
	}
	
	public Operation param(Object value) {
		this.params.add(new ParamValue(value));
		return this;
	}
	
	public Operation param(Query query) {
		this.params.add(new ParamSubQuery(query));
		return this;
	}
	
	public Operation param(Alias<?> alias, Object property) {
		this.params.add(new ParamField(alias, property));
		return this;
	}
	
	@Override
	public Param[] getParams() {
		return params.toArray(new Param[0]);
	}

	@Override
	public String build() {
		
		if (params.size() < 2)
			throw new BeanException("An operation needs 2 (two) or more parameters");
		
		StringBuilder sb = new StringBuilder();
		sb.append("(").append(params.get(0).paramInQuery());
		
		for (int i=1; i < params.size(); i++) {
			sb.append(operationSignal()).append(params.get(i).paramInQuery());
		}
		
		return sb.append(")").toString();
	}

}
