package org.mentabean.sql.param;

import org.mentabean.jdbc.QueryBuilder;
import org.mentabean.jdbc.QueryBuilder.Alias;
import org.mentabean.jdbc.QueryBuilder.Query;
import org.mentabean.sql.Function;
import org.mentabean.util.PropertiesProxy;

public class DefaultParamHandler implements ParamHandler {

	@Override
	public Param findBetter(QueryBuilder builder, Object property) {
		
		if (property instanceof Param)
			return (Param) property;
		
		if (property instanceof Query)
			return new ParamSubQuery((Query) property);
		
		if (property instanceof Function)
			return new ParamFunction((Function) property);
		
		if (PropertiesProxy.hasBeanInstance()) {
			
			Object proxy = PropertiesProxy.getBeanInstances()[0];
			for (Alias<?> a : builder.getCreatedAliases()) {
				if (a.pxy() == proxy) {
					return new ParamField(a, property);
				}
			}
		}
		
		return new ParamValue(property);
	}
	
}
