package org.mentabean.sql;

import org.mentabean.BeanConfig;
import org.mentabean.BeanSession;
import org.mentabean.DBField;
import org.mentabean.util.PropertiesProxy;

/**
 * 
 * This class encapsulates a proxy to help construct queries that are fully refactorable.
 * 
 * @author Sergio Oliveira Jr.
 *
 * @param <E>
 */
public class TableAlias<E> {
	
	private final Class<? extends E> beanClass;
	private final String prefix;
	private final BeanSession session;
	private final BeanConfig config;
	private final E proxy;
	
	public TableAlias(BeanSession session, BeanConfig config, Class<? extends E> beanClass) {
		this(session, config, beanClass, null);
	}
	
	public TableAlias(BeanSession session, BeanConfig config, Class<? extends E> beanClass, String prefix) {
		this.beanClass = beanClass;
		this.prefix = prefix;
		this.session = session;
		this.config = config;
		this.proxy = PropertiesProxy.create(beanClass);
	}
	
	@Override
    public String toString() {
	    return "TableAlias [beanClass=" + beanClass.getSimpleName() + ", prefix=" + prefix + "]";
    }

	/**
	 * Return the db columns of a select statements.
	 * 
	 * @return the columns to build a select statement
	 */
	public String columns(Object... props) {
		if (prefix != null) {
			return session.buildSelect(beanClass, prefix, props);
		} else {
			return session.buildSelect(beanClass, props);
		}
	}
	
	public String columnsMinus(Object... props) {
		if (prefix != null) {
			return session.buildSelectMinus(beanClass, prefix, props);
		} else {
			return session.buildSelectMinus(beanClass, props);
		}
	}
	
	/**
	 * Return the table name.
	 * 
	 * @return the table name
	 */
	public String tableName() {
		if (prefix != null) {
			return config.getTableName() + " " + prefix;
		} else {
			return config.getTableName();
		}
	}
	
	/**
	 * Return the db column name for this bean property.
	 * 
	 * @param prop this is a filler parameter because a proxy call will be performed!
	 * @return the db column name of this property
	 */
	public String column(Object prop) {
		
		String propName = PropertiesProxy.getPropertyName();
		
		DBField field = config.getField(propName);
		
		if (field == null) throw new IllegalStateException("Cannot find field for property \"" + propName + "\" on beanconfig: " + config);
		
		if (prefix != null) {
			return prefix + "." + field.getDbName();
		} else {
			return field.getDbName();
		}
	}
	
	public E pxy() {
		return proxy;
	}
	
	public E proxy() {
		return proxy;
	}
	
	public String prefix() {
		return prefix;
	}
	
	public Class<? extends E> beanClass() {
		return beanClass;
	}
	
}