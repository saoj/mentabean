package org.mentabean.sql;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.mentabean.DBType;
import org.mentabean.jdbc.AnsiSQLBeanSession;
import org.mentabean.jdbc.QueryBuilder.Query;
import org.mentabean.sql.param.Param;
import org.mentabean.sql.param.ParamFunction;
import org.mentabean.sql.param.ParamSubQuery;
import org.mentabean.type.GenericType;

public class Sentence implements Function, Condition {

	private Param param;
	private String name, property;
	private DBType<?> returnType = new GenericType();
	
	public Sentence(Param param) {
		this.param = param;
	}
	
	public Sentence(Query query) {
		this.param = new ParamSubQuery(query);
	}
	
	public Sentence(Function function) {
		this.param = new ParamFunction(function);
	}
	
	/**
	 * Specify the property of <code>FROM</code> alias that will be populated with this sentence
	 * @param property
	 * @return this
	 */
	public Sentence fromProperty(Object property) {
		
		this.property = AnsiSQLBeanSession.getProperties(new Object[] {property})[0];
		
		if (name == null) {
			name = this.property.replace('.', '_');
		}
		return this;
	}
	
	public Sentence name(String name) {
		this.name = name;
		return this;
	}
	
	public Sentence returnType(DBType<?> type) {
		this.returnType = type;
		return this;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getValue(ResultSet rset) throws SQLException {
		return (T) returnType.getFromResultSet(rset, name);
	}
	
	public DBType<?> getReturnType() {
		return returnType;
	}
	
	public String getProperty() {
		return property;
	}
	
	public String getName() {
		return name;
	}
	
	@Override
	public Param[] getParams() {
		return new Param[] {param};
	}

	@Override
	public String build() {
		return "("+param.paramInQuery()+")";
	}

}
