/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * MentaBean => http://www.mentabean.org
 * Author: Sergio Oliveira Jr. (sergio.oliveira.jr@gmail.com)
 */
package org.mentabean.jdbc;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mentabean.BeanConfig;
import org.mentabean.BeanException;
import org.mentabean.BeanManager;
import org.mentabean.BeanSession;
import org.mentabean.DBField;
import org.mentabean.DBType;
import org.mentabean.event.TriggerDispatcher;
import org.mentabean.event.TriggerDispatcher.Type;
import org.mentabean.event.TriggerEvent;
import org.mentabean.event.TriggerListener;
import org.mentabean.sql.TableAlias;
import org.mentabean.type.AutoIncrementType;
import org.mentabean.type.AutoTimestampType;
import org.mentabean.type.NowOnInsertAndUpdateTimestampType;
import org.mentabean.type.NowOnInsertTimestampType;
import org.mentabean.type.NowOnUpdateTimestampType;
import org.mentabean.type.SizedType;
import org.mentabean.util.InjectionUtils;
import org.mentabean.util.Limit;
import org.mentabean.util.OrderBy;
import org.mentabean.util.PropertiesProxy;
import org.mentabean.util.SQLUtils;

/**
 * The bean session implementation based on JDBC and SQL.
 * 
 * @author soliveira
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class AnsiSQLBeanSession implements BeanSession {

	protected static boolean DEBUG = false;
	
	protected static boolean DEBUG_NATIVE = false;

	/* The loaded map will be cleared when the session dies */
	protected IdentityHashMap<Object, Map<String, Value>> loaded = new IdentityHashMap<Object, Map<String, Value>>();

	protected Connection conn;

	protected final BeanManager beanManager;
	
	protected final TriggerDispatcher dispatcher = new TriggerDispatcher();

	/**
	 * Creates a JdbcBeanSession with a BeanManager and a Connection.
	 * 
	 * @param beanManager
	 *            The bean manager
	 * @param conn
	 *            The database connection
	 */
	public AnsiSQLBeanSession(final BeanManager beanManager, final Connection conn) {
		this.beanManager = beanManager;
		this.conn = conn;
	}

	/**
	 * Turn SQL debugging on and off.
	 * 
	 * @param b
	 *            true if it should be debugged
	 */
	public static void debugSql(boolean b) {
		DEBUG = b;
	}
	
	/**
	 * Turn SQL native queries debugging on and off.
	 * 
	 * @param b
	 *            true if it should be debugged
	 */
	public static void debugNativeSql(boolean b) {
		DEBUG_NATIVE = b;
	}

	/**
	 * Get the connection associated with this JdbcBeanSession.
	 * 
	 * @return the database connection
	 */
	@Override
	public Connection getConnection() {

		return conn;
	}

	/**
	 * Get the command representing 'now' in this database. This base implementation returns null, in other words, no now command will be used.
	 * 
	 * @return the command for now in this database (now(), sysdate, etc)
	 */
	protected String getCurrentTimestampCommand() {

		return null;
	}

	/**
	 * Get a value from a bean through reflection.
	 * 
	 * @param bean
	 * @param fieldName
	 * @return The value of a bean property
	 */
	protected Object getValueFromBean(final Object bean, final String fieldName) {

		return getValueFromBean(bean, fieldName, null);

	}
	
	public static String[] getProperties(Object[] names) {
		if (names != null) {
    		for(Object o : names) {
    			if (o instanceof String) {
    				PropertiesProxy.addPropertyName((String) o);
    			}
    		}
		}
		
		if (PropertiesProxy.hasProperties()) {
			return PropertiesProxy.getPropertyNames();
		} else {
			return null;
		}
	}
	
	/**
	 * Get a value from a bean through reflection.
	 * 
	 * @param bean
	 * @param fieldName
	 * @param m
	 * @return The value of a bean property
	 */
	protected Object getValueFromBean(final Object bean, final String fieldName, Method m) {
		
		int index;
		
		if ((index = fieldName.lastIndexOf(".")) > 0) {
			
			String chain = fieldName.substring(0, index);
			
			String lastField = fieldName.substring(index + 1);
			
			Object deepestBean = getDeepestBean(bean, chain, false);
			
			if (deepestBean == null) return null;
			
			return getValueFromBean(deepestBean, lastField, m);
		}
		
		if (bean == null) {
			return null;
		}

		if (m == null) {
			m = InjectionUtils.findMethodToGet(bean.getClass(), fieldName);
		}

		if (m == null) {
			throw new BeanException("Cannot find method to get field from bean: " +
					"Class: " + bean.getClass() + ", field: " + fieldName);
		}

		Object value = null;

		try {

			value = m.invoke(bean, (Object[]) null);

			return value;

		} catch (Exception e) {
			throw new BeanException(e);
		}
	}

	private static void checkPK(final Object value, final DBField dbField) {

		if (value == null) {
			throw new BeanException("pk is missing: " + dbField);
		} else if (value instanceof Number) {

			final Number n = (Number) value;

			if (n.doubleValue() <= 0) {
				throw new BeanException("Number pk is missing: " + dbField);
			}
		}
	}
	
	@Override
	public boolean load(Object bean) {
		return loadImpl(bean, null, null);
	}
	
	@Override
	public boolean load(Object bean, Object... properties) {
		
		return loadImpl(bean, getProperties(properties), null);
	}
	
	
	@Override
	public boolean loadMinus(Object bean, Object... minus) {
		
		return loadImpl(bean, null, getProperties(minus));
		
	}
	
	protected boolean loadImpl(final Object bean, String[] properties, String[] minus) {

		final BeanConfig bc = getConfigFor(bean.getClass());

		if (bc == null) {
			throw new BeanException("Cannot find bean config: " + bean.getClass());
		}

		if (bc.getNumberOfFields() == 0) {
			throw new BeanException("BeanConfig has zero fields: " + bc);
		}

		final StringBuilder sb = new StringBuilder(32 * bc.getNumberOfFields());

		sb.append("SELECT ");

		Iterator<DBField> iter = bc.fields();

		int count = 0;

		while (iter.hasNext()) {

			DBField field = iter.next();
			
			final String fieldName = field.getDbName();
			
			if (!field.isPK()) { // always load the PK...
				
    			if (properties != null && !checkArray(fieldName, properties, bc)) {
    				continue;
    			}
    
    			if (minus != null && checkArray(fieldName, minus, bc)) {
    				continue;
    			}
			}

			if (count++ > 0) {
				sb.append(',');
			}

			sb.append(fieldName);

		}

		sb.append(" FROM ").append(bc.getTableName()).append(" WHERE ");

		if (!bc.hasPK()) {
			throw new BeanException("Cannot load bean without a PK!");
		}

		iter = bc.pks();

		count = 0;

		final List<Value> values = new LinkedList<Value>();

		while (iter.hasNext()) {

			final DBField dbField = iter.next();

			final String fieldName = dbField.getName();

			final String dbFieldName = dbField.getDbName();

			final Object value = getValueFromBean(bean, fieldName);

			checkPK(value, dbField);

			if (count++ > 0) {
				sb.append(" AND ");
			}

			sb.append(dbFieldName).append("=?");

			values.add(new Value(dbField, value));

		}

		if (values.isEmpty()) {
			throw new BeanException("Bean is empty: " + bean + " / " + bc);
		}

		if (conn == null) {
			throw new BeanException("Connection is null!");
		}

		PreparedStatement stmt = null;

		ResultSet rset = null;

		try {
			
			if (DEBUG) {
				System.out.println("LOAD SQL: " + sb.toString());
			}
			
			stmt = conn.prepareStatement(sb.toString());

			final Iterator<Value> iter2 = values.iterator();

			int index = 0;

			while (iter2.hasNext()) {

				final Value v = iter2.next();
				
				v.field.getType().bindToStmt(stmt, ++index, v.value);

			}

			rset = stmt.executeQuery();
			
			if (DEBUG_NATIVE) {
				System.out.println("LOAD SQL (NATIVE): " + stmt);
			}

			index = 0;

			final Map<String, Value> fieldsLoaded = new HashMap<String, Value>();

			if (rset.next()) {

				iter = bc.fields();

				while (iter.hasNext()) {

					final DBField f = iter.next();

					final String fieldName = f.getName();
					
					if (!f.isPK()) {
					
    					if (properties != null && !checkArray(fieldName, properties, bc)) {
    						continue;
    					}
    
    					if (minus != null && checkArray(fieldName, minus, bc)) {
    						continue;
    					}
					}

					final DBType type = f.getType();

					final Object value = type.getFromResultSet(rset, ++index);

					injectValue(bean, fieldName, value, type.getTypeClass());

					fieldsLoaded.put(fieldName, new Value(f, value));
				}

			} else {
				return false;
			}

			if (rset.next()) {
				throw new BeanException("Load returned more than one row!");
			}

			loaded.put(bean, fieldsLoaded);

			return true;

		} catch (Exception e) {

			throw new BeanException(e);

		} finally {

			close(stmt, rset);
		}
	}
	
	private Object getDeepestBean(Object target, String name, boolean create) {
		
		int index;
		
		if ((index = name.indexOf('.')) > 0) {
			
			String fieldName = name.substring(0, index);

			String remainingName = name.substring(index + 1);
			
			Object bean = getPropertyBean(target, fieldName, create);
			
			return getDeepestBean(bean, remainingName, create);
		}
		
		return getPropertyBean(target, name, create);
	}
	
	/**
	 * Get a value from target through reflection and tries to create a new instance if create parameter is true
	 * @param target
	 * @param name
	 * @param create
	 * @return The value from bean
	 */
	protected Object getPropertyBean(Object target, String name, boolean create) {
		
		Object value = getValueFromBean(target, name);
		
		if (value == null && create) {
			
			// try to instantiate, must have a default constructor!
			
			Class<?> beanClass = InjectionUtils.findPropertyType(target.getClass(), name);
			
			if (beanClass == null) {
				throw new BeanException("Cannot find property type: " + target.getClass() + " " + name);
			}
			
			try {
				
				value = beanClass.newInstance();
				
			} catch(Exception e) {
				
				value = getAbstractValue(target.getClass(), name);
			}
			
			// don't forget to inject in the target so next time it is there...
			
			injectValue(target, name, value, beanClass);
		}
		
		return value;
	}
	
	private Object getAbstractValue(Class<? extends Object> clazz, String name) {
		
		try {
			
			BeanConfig bc = getConfigFor(clazz);
			if (bc != null) {
				
				Class<? extends Object> instanceClass = bc.getAbstractProperty(name);
				if (instanceClass != null) {
					return instanceClass.newInstance();
				}
			}
			
			throw new BeanException("Cannot instantiate property name: " + name + " from "+clazz);					
			
		} catch (Exception e) {
			throw new BeanException("Cannot instantiate abstract value for "+clazz+" (field "+name+")", e);
		}
	}

	/**
	 * Inject a value in a bean through reflection.
	 * 
	 * @param bean
	 * @param fieldName
	 * @param value
	 * @param valueType
	 */
	protected void injectValue(final Object bean, final String fieldName, Object value, final Class<? extends Object> valueType) {
		
		// first check if we have a chain of fields...
		
		int index;
		
		if ((index = fieldName.lastIndexOf(".")) > 0) {
			
			if (value == null) {
				// there is nothing to do here, as we don't want to create any object since the id is null...
				return;
			}
			
			String chain = fieldName.substring(0, index);
			
			String lastField = fieldName.substring(index + 1);
			
			Object deepestBean = getDeepestBean(bean, chain, true);
			
			injectValue(deepestBean, lastField, value, valueType);
			
			return;
			
		}

		final Method m = InjectionUtils.findMethodToInject(bean.getClass(), fieldName, value == null ? valueType : value.getClass());

		if (m == null) {

			// try field...

			final Field field = InjectionUtils.findFieldToInject(bean.getClass(), fieldName, value == null ? valueType : value.getClass());
			
			if (field != null) {
				
				// if field is a primitive (not a wrapper or void), convert a null to its default value
				if (field.getType().isPrimitive() && value == null) {
					value = InjectionUtils.getDefaultValueForPrimitive(field.getType());
				}
				
				try {

					field.set(bean, value);

				} catch (final Exception e) {

					e.printStackTrace();

					throw new BeanException(e);
				}
				
			} else {
				
				// if Long and can be expressed as integer, try integer...
				if (value instanceof Long) {
					Long l = (Long) value;
					if (l.longValue() <= Integer.MAX_VALUE && l.longValue() >= Integer.MIN_VALUE) {
						injectValue(bean, fieldName, l.intValue(), Integer.class); // recursion...
						return;
					}
				}
				
				// Field can be a GenericType (Object). If value is null nothing will be injected..
				if (value != null)
					throw new BeanException("Cannot find field or method to inject: " + bean + " / " + fieldName);
			}

		} else {
			
			// if field is a primitive (not a wrapper or void), convert a null to its default value
			Class<?> paramType = m.getParameterTypes()[0];
			if (paramType.isPrimitive() && value == null) {
				value = InjectionUtils.getDefaultValueForPrimitive(paramType);
			}
			
			try {
			
				m.invoke(bean, value);

			} catch (final Exception e) {

				e.printStackTrace();

				throw new BeanException(e);
			}
		}
	}

	/**
	 * Some databases will sort before applying the limit (MySql), others will not (Oracle). Handle each one accordingly.
	 * 
	 * Note: This base implementation does nothing.
	 * 
	 * @param sb
	 * @param orderBy
	 * @param limit
	 * @return A string builder with the the SQL modified for the limit operation
	 */
	protected StringBuilder handleLimit(final StringBuilder sb, final OrderBy orderBy, final Limit limit) {

		return sb;
	}

	/**
	 * Build the column/field list for a SQL SELECT statement based on the bean configuration. Very useful to create select statements.
	 * 
	 * @param beanClass
	 *            the bean class
	 * @return the column/field list for a select
	 */
	@Override
	public String buildSelect(final Class<? extends Object> beanClass) {

		return buildSelectImpl(beanClass, null, null, null, true, true);
	}

	@Override
	public String buildSelect(final Class<? extends Object> beanClass, Object... properties) {
		
		return buildSelectImpl(beanClass, null, getProperties(properties), null, true, true);
	}

	/**
	 * Build a column/field list for a SQL SELECT statement based on the bean configuration. A table prefix will be used on each field. Very useful to create select statements on multiple tables (joins).
	 * 
	 * @param beanClass
	 *            the bean class
	 * @param tablePrefix
	 *            the table prefix to use before each field
	 * @return the column/field list for a select
	 */
	@Override
	public String buildSelect(final Class<? extends Object> beanClass, final String tablePrefix) {

		return buildSelectImpl(beanClass, tablePrefix, null, null, true, true);
	}

	@Override
	public String buildSelect(final Class<? extends Object> beanClass, final String tablePrefix, Object... properties) {
		
		return buildSelectImpl(beanClass, tablePrefix, getProperties(properties), null, true, true);
	}

	/**
	 * Like buildSelect but you can exclude some properties from the resulting list. Useful when you have a bean with too many properties and you just want to fetch a few.
	 * 
	 * Note: The list of properties to exclude contains 'property names' and NOT database column names.
	 * 
	 * @param beanClass
	 *            the bean class
	 * @param minus
	 *            a list for property names to exclude
	 * @return the column/field list for a select
	 */
	@Override
	public String buildSelectMinus(final Class<? extends Object> beanClass, final Object... minus) {
		
		return buildSelectImpl(beanClass, null, null, getProperties(minus), true, true);
	}

	/**
	 * Same as buildSelectMinus with support for a database table prefix that will be applied on each field.
	 * 
	 * @param beanClass
	 *            the bean class
	 * @param tablePrefix
	 *            the database table prefix
	 * @param minus
	 *            a list of property names to exclude
	 * @return the column/field list for a select
	 */
	@Override
	public String buildSelectMinus(final Class<? extends Object> beanClass, final String tablePrefix, final Object... minus) {
		
		return buildSelectImpl(beanClass, tablePrefix, null, getProperties(minus), true, true);
	}

	protected String buildSelectImpl(final Class<? extends Object> beanClass, final String tablePrefix, 
			final String[] properties, final String[] minus, final boolean includePK, final boolean addSuffix) {

		final BeanConfig bc = getConfigFor(beanClass);

		if (bc == null) {
			return null;
		}

		final StringBuilder sb = new StringBuilder(32 * bc.getNumberOfFields());

		final Iterator<DBField> iter = bc.fields();

		int count = 0;

		while (iter.hasNext()) {

			final DBField field = iter.next();

			final String dbField = field.getDbName();

			final String name = field.getName();

			if (!field.isPK() || !includePK) { // always include PK
				
    			if (properties != null && !checkArray(name, properties, bc)) {
    				continue;
    			}
    
    			if (minus != null && checkArray(name, minus, bc)) {
    				continue;
    			}
			}

			if (count++ > 0) {
				sb.append(",");
			}

			if (tablePrefix != null) {

				sb.append(tablePrefix).append('.').append(dbField);

				if (addSuffix) {
				
					sb.append(' ');

					sb.append(tablePrefix).append('_').append(dbField);
				}

			} else {
				sb.append(dbField);
			}
		}

		return sb.toString();

	}

	private boolean checkArray(final String value, final String[] array, final BeanConfig bc) {

		String column = propertyToColumn(bc, value);
		for (int i = 0; i < array.length; i++) {
			if (propertyToColumn(bc, array[i]).equals(column)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Populate a bean (insert all its properties) from the results in a result set, based on the bean configuration.
	 * 
	 * @param rset
	 *            the result set from where to get the property values
	 * @param bean
	 *            the bean to be populated
	 * @throws Exception
	 */
	@Override
	public void populateBean(final ResultSet rset, final Object bean) {

		populateBeanImpl(rset, bean, null, null, null, true);
	}

	@Override
	public void populateBean(final ResultSet rset, final Object bean, Object... properties) {
		
		populateBeanImpl(rset, bean, null, getProperties(properties), null, true);
	}

	/**
	 * Same as populateBean, but use a table prefix before fetching the values from the result set. Useful when there are multiple tables involved and you want to avoid field name clashing.
	 * 
	 * @param rset
	 *            the result set
	 * @param bean
	 *            the bean to be populated
	 * @param tablePrefix
	 *            the table prefix
	 */
	@Override
	public void populateBean(final ResultSet rset, final Object bean, final String tablePrefix) {

		populateBeanImpl(rset, bean, tablePrefix, null, null, true);
	}

	@Override
	public void populateBean(final ResultSet rset, final Object bean, final String tablePrefix, Object... properties) {
		
		populateBeanImpl(rset, bean, tablePrefix, getProperties(properties), null, true);
	}

	/**
	 * Same as populateBean, but exclude some fields when populating.
	 * 
	 * @param rset
	 * @param bean
	 * @param minus
	 */
	@Override
	public void populateBeanMinus(final ResultSet rset, final Object bean, final Object... minus) {
		
		populateBeanImpl(rset, bean, null, null, getProperties(minus), true);
	}

	/**
	 * Same as populateBean, but exclude some fields when populating and use a table prefix in front of the field names.
	 * 
	 * @param rset
	 * @param bean
	 * @param tablePrefix
	 * @param minus
	 */
	@Override
	public void populateBeanMinus(final ResultSet rset, final Object bean, final String tablePrefix, final Object... minus) {
		
		populateBeanImpl(rset, bean, tablePrefix, null, getProperties(minus), true);
	}

	protected void populateBeanImpl(final ResultSet rset, final Object bean, final String tablePrefix, final String[] properties, final String[] minus, boolean includePK) {

		final BeanConfig bc = getConfigFor(bean.getClass());

		if (bc == null) {
			throw new BeanException("Cannot find bean config: " + bean.getClass());
		}

		final Iterator<DBField> iter = bc.fields();

		final StringBuilder sbField = new StringBuilder(32);

		while (iter.hasNext()) {

			final DBField f = iter.next();

			final String fieldName = f.getName();
			
			if (!f.isPK() || !includePK) { // always populate PK
			
    			if (properties != null && !checkArray(fieldName, properties, bc)) {
    				continue;
    			}
    
    			if (minus != null && checkArray(fieldName, minus, bc)) {
    				continue;
    			}
			}

			final String dbFieldName = f.getDbName();

			final DBType type = f.getType();

			sbField.setLength(0);

			if (tablePrefix != null) {
				sbField.append(tablePrefix).append('_').append(dbFieldName);
			} else {
				sbField.append(dbFieldName);
			}

			try {

				final Object value = type.getFromResultSet(rset, sbField.toString());

				injectValue(bean, fieldName, value, type.getTypeClass());

			} catch (Exception e) {

				throw new BeanException(e);
			}
		}
	}

	/**
	 * Load a list of beans, but exclude some fields.
	 * 
	 * @param <E>
	 * @param bean
	 * @param minus
	 * @param orderBy
	 * @param limit
	 * @return A list of beans
	 */
	@Override
	public <E> List<E> loadListMinus(final E bean, final OrderBy orderBy, final Limit limit, final Object... minus) {
		
		return loadListImpl(bean, orderBy, limit, null, getProperties(minus));
	}

	private <E> E checkUnique(final List<E> list) {

		if (list == null || list.size() == 0) {
			return null;
		} else if (list.size() > 1) {
			throw new BeanException("Query returned more than one bean!");
		} else {
			return list.get(0);
		}
	}

	@Override
	public <E> List<E> loadList(final E bean, final OrderBy orderBy, final Limit limit) {

		return loadListImpl(bean, orderBy, limit, null, null);
	}

	@Override
	public <E> List<E> loadList(final E bean, final OrderBy orderBy, final Limit limit, Object... properties) {
		
		return loadListImpl(bean, orderBy, limit, getProperties(properties), null);
	}

	private <E> StringBuilder prepareListQuery(StringBuilder sb, BeanConfig bc, E bean, OrderBy orderBy, Limit limit, List<Value> values) {

		sb.append(" FROM ").append(bc.getTableName()).append(" ");

		Iterator<DBField> iter = bc.fields();

		int count = 0;

		while (iter.hasNext()) {

			final DBField field = iter.next();

			final String dbField = field.getDbName();

			final Method m = findMethodToGet(bean, field.getName());
			
			boolean isNestedProperty = field.getName().contains(".");

			if (m == null) {
				if (!isNestedProperty) {
					throw new BeanException("Cannot find method to get field from bean: " + field.getName());
				} else {
					continue; // nested property not set!
				}
			}

			final Class<? extends Object> returnType = m.getReturnType();

			final Object value = getValueFromBean(bean, field.getName(), m);

			if (!isSet(value, returnType)) {
				continue;
			}

			if (count++ > 0) {
				sb.append(" AND ");
			} else {
				sb.append(" WHERE ");
			}

			sb.append(dbField).append("=?");

			values.add(new Value(field, value));
		}

		sb.append(buildOrderBy(orderBy, bc));

		sb = handleLimit(sb, orderBy, limit);

		return sb;
	}
	
	private String buildOrderBy(OrderBy orderBy, BeanConfig bc) {
		
		if (orderBy != null && !orderBy.isEmpty()) {
			
			String orderByString = orderBy.toString();
			
			String[] orders = orderByString.trim().split("\\s*,\\s*");
			
			for (String order : orders) {
				if (order.contains(" ")) {
					order = order.substring(0, order.indexOf(" "));
				}
				orderByString = orderByString.replace(order, propertyToColumn(bc, order));
			}
			
			StringBuilder sb = new StringBuilder();
			sb.append(" order by ").append(orderByString).append(" ");
			
			return sb.toString();
		}
		return " ";
	}
	
	/**
	 * Returns a database column name for a bean attribute.
	 * @param 	bc - The <code>BeanConfig</code> object 
	 * @param 	property - A bean property
	 * @return	The database column name found if exists, otherwise will return the
	 * given bean <code>property</code>
	 */
	public String propertyToColumn(BeanConfig bc, Object property) {
		
		Iterator<DBField> it = bc.fields();
		
		String propertyName = getProperties(new Object[] {property})[0];
		
		while (it.hasNext()) {
			DBField field = it.next();
			if (propertyName.equalsIgnoreCase(field.getName()))
				return field.getDbName();
		}
		
		return propertyName;
	}
	
	@Override
	public String propertyToColumn(Class<? extends Object> clazz, Object property) {
		
		return propertyToColumn(clazz, property, null);
	}
	
	@Override
	public String propertyToColumn(Class<? extends Object> clazz, Object property, String alias) {
		
		BeanConfig bc = getConfigFor(clazz);
		
		if (alias == null)
			return propertyToColumn(bc, property);
		
		return alias+"."+propertyToColumn(bc, property);
	}
	
	@Override
	public String buildTableName(Class<? extends Object> clazz) {
		
		return getConfigFor(clazz).getTableName();
	}
	
	@Override
	public QueryBuilder buildQuery() {

		return new QueryBuilder(this);
	}

	@Override
	public int countList(Object bean) {
		return countListImpl(bean, null, null);
	}

	private int countListImpl(final Object bean, final OrderBy orderBy, final Limit limit) {

		if (limit != null && limit.intValue() == 0) {
			return 0;
		}

		final BeanConfig bc = getConfigFor(bean.getClass());

		if (bc == null) {
			throw new BeanException("Cannot find bean config: " + bean.getClass());
		}

		StringBuilder sb = new StringBuilder(32 * bc.getNumberOfFields());

		sb.append("SELECT count(1)");

		final List<Value> values = new LinkedList<Value>();

		sb = prepareListQuery(sb, bc, bean, orderBy, limit, values);

		PreparedStatement stmt = null;

		ResultSet rset = null;

		try {

			final String sql = sb.toString();

			if (DEBUG) {
				System.out.println("COUNT LIST: " + sql);
			}

			stmt = conn.prepareStatement(sql);

			final Iterator<Value> iter2 = values.iterator();

			int index = 0;

			while (iter2.hasNext()) {

				final Value v = iter2.next();

				v.field.getType().bindToStmt(stmt, ++index, v.value);

			}

			rset = stmt.executeQuery();
			
			if (DEBUG_NATIVE) {
				System.out.println("COUNT LIST (NATIVE): "+stmt);
			}

			rset.next();

			return rset.getInt(1);

		} catch (Exception e) {

			throw new BeanException(e);

		} finally {

			close(stmt, rset);
		}
	}

	private <E> List<E> loadListImpl(final E bean, final OrderBy orderBy, final Limit limit, final String[] properties, final String[] minus) {

		if (limit != null && limit.intValue() == 0) {
			return new ArrayList<E>();
		}

		final BeanConfig bc = getConfigFor(bean.getClass());

		if (bc == null) {
			throw new BeanException("Cannot find bean config: " + bean.getClass());
		}

		StringBuilder sb = new StringBuilder(32 * bc.getNumberOfFields());

		Iterator<DBField> iter = bc.fields();

		sb.append("SELECT ");

		int count = 0;

		while (iter.hasNext()) {

			final DBField field = iter.next();

			final String dbField = field.getDbName();

			final String name = field.getName();

			if (!field.isPK()) {
			
    			if (properties != null && !checkArray(name, properties, bc)) {
    				continue;
    			}
    
    			if (minus != null && checkArray(name, minus, bc)) {
    				continue;
    			}
			}

			if (count++ > 0) {
				sb.append(",");
			}

			sb.append(dbField);
		}

		final List<Value> values = new LinkedList<Value>();

		sb = prepareListQuery(sb, bc, bean, orderBy, limit, values);

		PreparedStatement stmt = null;

		ResultSet rset = null;

		try {

			final String sql = sb.toString();
			
			if (DEBUG) {
				System.out.println("LOAD LIST: "+sql);
			}
			
			stmt = conn.prepareStatement(sql);

			final Iterator<Value> iter2 = values.iterator();

			int index = 0;

			while (iter2.hasNext()) {

				final Value v = iter2.next();

				v.field.getType().bindToStmt(stmt, ++index, v.value);

			}

			rset = stmt.executeQuery();
			
			if (DEBUG_NATIVE) {
				System.out.println("LOAD LIST (NATIVE): " + stmt);
			}

			final List<E> results = new LinkedList<E>();

			final Class<? extends Object> beanKlass = bean.getClass();

			int total = 0;

			while (rset.next()) {

				iter = bc.fields();

				index = 0;

				final E item = (E) beanKlass.newInstance(); // not sure how to
															// handle generics
															// here...

				while (iter.hasNext()) {

					final DBField f = iter.next();

					final String fieldName = f.getName();
					
					if (!f.isPK()) {

    					if (properties != null && !checkArray(fieldName, properties, bc)) {
    						continue;
    					}
    
    					if (minus != null && checkArray(fieldName, minus, bc)) {
    						continue;
    					}
					}

					final DBType type = f.getType();

					final Object value = type.getFromResultSet(rset, ++index);

					injectValue(item, fieldName, value, type.getTypeClass());
				}

				results.add(item);

				total++;

				if (limit != null && limit.intValue() > 0 && total == limit.intValue()) {
					return results;
				}
			}

			return results;

		} catch (Exception e) {

			throw new BeanException(e);

		} finally {

			close(stmt, rset);
		}
	}

	/**
	 * if Boolean consider TRUE to be set and FALSE to be not set.
	 * 
	 * if Character, cast to integer and assume it is set if different than 0
	 * 
	 * if Number consider everything different than zero to be set.
	 * 
	 * Otherwise returns TRUE for anything different than null and FALSE for null.
	 * 
	 * @param value
	 * @param returnType
	 * @return true if is set
	 */
	protected boolean isSet(final Object value, final Class<? extends Object> returnType) {

		if (value != null) {
			if (returnType.equals(boolean.class) && value instanceof Boolean) {

				// if Boolean consider TRUE to be set and FALSE to be not set
				// (false = default value)

				final boolean b = ((Boolean) value).booleanValue();

				return b;

			} else if (returnType.equals(char.class) && value instanceof Character) {

				// if Character, cast to int and assume set if different than
				// 0...

				final int c = ((Character) value).charValue();

				return c != 0;

			} else if (returnType.isPrimitive() && !returnType.equals(boolean.class) && !returnType.equals(char.class) && value instanceof Number) {

				// if number consider everything different than zero to be set...

				final Number n = (Number) value;

				if (n.doubleValue() != 0d) {
					return true;
				}

			} else {
				return true;
			}
		}

		return false;
	}

	@Override
	public int update(final Object bean, Object... forceNull) {

		return update(bean, true, getProperties(forceNull));
	}

	@Override
	public int updateAll(final Object bean) {

		return update(bean, false, null);
	}

	private int update(final Object bean, final boolean dynUpdate, String[] nullProps) {

		final Map<String, Value> fieldsLoaded = loaded.get(bean);

		final BeanConfig bc = getConfigFor(bean.getClass());

		if (bc == null) {
			throw new BeanException("Cannot find bean config: " + bean.getClass());
		}

		if (bc.getNumberOfFields() == 0) {
			throw new BeanException("BeanConfig has zero fields: " + bc);
		}

		final StringBuilder sb = new StringBuilder(32 * bc.getNumberOfFields());

		sb.append("UPDATE ").append(bc.getTableName()).append(" SET ");

		Iterator<DBField> iter = bc.fields();

		int count = 0;

		final List<Value> values = new LinkedList<Value>();
		
		while (iter.hasNext()) {

			final DBField dbField = iter.next();

			if (dbField.isPK()) {
				continue;
			}

			final DBType type = dbField.getType();

			if (type instanceof AutoIncrementType) {
				continue;
			}

			if (type instanceof AutoTimestampType) {
				continue;
			}

			boolean isNowOnUpdate = type instanceof NowOnUpdateTimestampType || type instanceof NowOnInsertAndUpdateTimestampType;

			final String fieldName = dbField.getName();

			final String dbFieldName = dbField.getDbName();

			if (!isNowOnUpdate) {
				
				Method m = findMethodToGet(bean, fieldName);
				
				Object value = null;
				Class<? extends Object> returnType = null;
				
				boolean isNestedProperty = fieldName.contains(".");
				
				if (m == null && !isNestedProperty) {
					throw new BeanException("Cannot find method to get field from bean: " + fieldName);
				}

				if (m != null) {
					returnType = m.getReturnType();
					value = getValueFromBean(bean, fieldName, m);
				}

				boolean update = false;

				if (!dynUpdate) {

					// if this is NOT a dynUpdate then update all properties with
					// whatever value they have

					update = true;

				} else if (fieldsLoaded != null) {

					// this is a dynUpdate, check if value is dirty, in other words,
					// if it has changed since it was loaded...

					final Value v = fieldsLoaded.get(fieldName);

					if (v != null) {
						if (value == null && v.value != null) {
							update = true;
						} else if (value != null && v.value == null) {
							update = true;
						} else if (value == null && v.value == null) {
							update = false;
						} else {
							update = !value.equals(v.value);
						}
					}

				} else {

					// this is a dynUpdate, but bean was not previously loaded from
					// the database...
					// in this case only update if the property is considered to be
					// SET...

					update = isSet(value, returnType);
					
					if (!update && nullProps != null) {
						
						update = checkArray(fieldName, nullProps, bc);
						
						//null in database column
						value = null;
					}
				}

				if (update) {

					if (count++ > 0) {
						sb.append(',');
					}

					sb.append(dbFieldName).append("=?");

					values.add(new Value(dbField, value));

				}

			} else {

				if (count++ > 0) {
					sb.append(',');
				}
				
				sb.append(dbFieldName).append("=");

				String nowCommand = getCurrentTimestampCommand();

				if (nowCommand == null) {

					sb.append("?");

					values.add(new Value(dbField, new java.util.Date()));

				} else {

					sb.append(nowCommand);
				}
			}
		}

		if (count == 0) {
			return 0;
		}

		sb.append(" WHERE ");

		if (!bc.hasPK()) {
			throw new BeanException("Cannot update bean without a PK!");
		}

		iter = bc.pks();

		count = 0;

		while (iter.hasNext()) {

			final DBField dbField = iter.next();

			final String fieldName = dbField.getName();

			final String dbFieldName = dbField.getDbName();

			final Object value = getValueFromBean(bean, fieldName);

			if (value == null) {
				throw new BeanException("pk is missing: " + dbField);
			} else if (value instanceof Number) {

				final Number n = (Number) value;

				if (n.doubleValue() <= 0) {
					throw new BeanException("Number pk is missing: " + dbField);
				}

			}

			if (count++ > 0) {
				sb.append(" AND ");
			}

			sb.append(dbFieldName).append("=?");

			values.add(new Value(dbField, value));

		}

		if (values.isEmpty()) {
			throw new BeanException("Bean is empty: " + bean + " / " + bc);
		}

		if (conn == null) {
			throw new BeanException("Connection is null!");
		}

		PreparedStatement stmt = null;

		try {

			if (DEBUG) {
				System.out.println("UPDATE SQL: " + sb.toString());
			}

			dispatchBeforeUpdate(bean);
			
			stmt = conn.prepareStatement(sb.toString());

			Iterator<Value> iter2 = values.iterator();

			int index = 0;

			while (iter2.hasNext()) {

				final Value v = iter2.next();

				v.field.getType().bindToStmt(stmt, ++index, v.value);

			}

			final int x = stmt.executeUpdate();
			
			if (DEBUG_NATIVE) {
				System.out.println("UPDATE SQL (NATIVE): " + stmt);
			}

			if (x > 1) {
				throw new BeanException("update modified more than one line: " + x);
			}

			if (x == 0) {
				return 0;
			}

			if (fieldsLoaded != null) {

				iter2 = values.iterator();

				while (iter2.hasNext()) {

					final Value v = iter2.next();

					if (v.field.isPK()) {
						continue;
					}

					final Value vv = fieldsLoaded.get(v.field.getName());

					if (vv != null) {
						vv.value = v.value;
					}
				}
			}
			
			dispatchAfterUpdate(bean);

			return 1;

		} catch (Exception e) {

			throw new BeanException(e);

		} finally {

			close(stmt);
		}
	}
	
	@Override
	public <E> E createBasicInstance(E bean) {

		try {
			BeanConfig bc = getConfigFor(bean.getClass());

			Iterator<DBField> pks = bc.pks();
			DBField pk = null;
			Object value = null;
			E basic = (E) bean.getClass().newInstance();

			while (pks.hasNext()) {
				
				pk = pks.next();
				
				value = getValueFromBean(bean, pk.getName());
				
				checkPK(value, pk);
				
				injectValue(basic, pk.getName(), value, null);
			}
			
			return basic;

		}catch(Exception e) {
			throw new BeanException(e);
		}
	}
	
	@Override
	public <E> int updateDiff(E newBean, E oldBean) {
		
		List<String> nullProps = new LinkedList<String>();
		
		E diff = compareDifferences(newBean, oldBean, nullProps);
		
		return diff == null ? 0 : update(diff, nullProps.toArray());
	}
	
	@Override
	public <E> E compareDifferences(E bean, E another, List<String> nullProps) {
		
		try {
			BeanConfig bc = getConfigFor(bean.getClass());
			
			Iterator<DBField> fields = bc.fields();
			DBField field;
			Object valueBean, valueAnother;
			
			E diff = (E) bean.getClass().newInstance();
			
			boolean hasDiff = false;
			
			while (fields.hasNext()) {
				
				field = fields.next();
				
				Method m = findMethodToGet(bean, field.getName());
				
				boolean isNestedProperty = field.getName().contains(".");
				
				if (m == null) {
					if (!isNestedProperty) {
						
						throw new BeanException("Cannot find method to get field from bean: " + field.getName());
						
					} else {
						
						int index = field.getName().lastIndexOf(".")+1;
						
						Object deepest = getDeepestBean(bean, field.getName().substring(0, index-1), true);
						
						String lastField = field.getName().substring(index);
						
						m = findMethodToGet(deepest, lastField);
					}
				}

				final Class<? extends Object> returnType = m.getReturnType();
				
				valueBean = getValueFromBean(bean, field.getName());

				if (field.isPK()) {
					
					injectValue(diff, field.getName(), valueBean, null);
					
					continue;
				}
				
				valueAnother = getValueFromBean(another, field.getName());
				
				if (!isSet(valueBean, returnType) && !isSet(valueAnother, returnType)) {
					
					continue;
				}

				if (!isSet(valueBean, returnType)) {
					
					nullProps.add(field.getName());
					
					hasDiff = true;
					
					continue;
				}
				
				if (!valueBean.equals(valueAnother)) {
					
					hasDiff = true;
					
					injectValue(diff, field.getName(), valueBean, returnType);
				}
				
			}
			
			return hasDiff ? diff : null;
			
		}catch(Exception e) {
			throw new BeanException(e);
		}
	}
	
	@Override
	public int save(final Object bean, Object... forceNull) {
		
		return save(bean, true, getProperties(forceNull));
	}
	
	@Override
	public int saveAll(final Object bean) {
		
		return save(bean, false);
	}
	
	/**
	 * Update or insert a bean into database. It tries to update first and then insert
	 * @param 	bean Object to update or insert
	 * @param 	dynUpdate flag indicating a dynamic update
	 * @return	A value <b>0 (zero)</b> if operation was an <code>update</code>, 
	 * <b>1 (one) if</b> <code>insert</code> method was executed
	 * @see #saveAll(Object)
	 * @see #save(Object, Object...)
	 */
	protected int save(Object bean, boolean dynUpdate, String nullProps[]) {

		try {

			if (secureLoadUnique(bean) != null) {
				
				update(bean, dynUpdate, nullProps);

				return UPDATE;
				
			} else {
				
				insert(bean);
				
				return INSERT;
			}

		}catch (BeanException e) {

			throw e;
		}
	}
	
	protected Object secureLoadUnique(Object bean) {
		
		try {
			
			return loadUnique(createBasicInstance(bean));
			
		} catch (Exception e) {
			
			return null;
		}
	}
	
	private Method findMethodToGet(Object bean, String fieldName) {
		
		Method m = null;
		
		int index;
		
		if ((index = fieldName.lastIndexOf(".")) > 0) {
			
			String chain = fieldName.substring(0, index);
			
			String lastField = fieldName.substring(index + 1);
			
			Object deepestBean = getDeepestBean(bean, chain, false);
			
			if (deepestBean != null) {
				
				m = InjectionUtils.findMethodToGet(deepestBean.getClass(), lastField);
			}
			
		} else {

			m = InjectionUtils.findMethodToGet(bean.getClass(), fieldName);
		}
		
		return m;
	}

	protected class QueryAndValues {

		public QueryAndValues(StringBuilder sb, List<Value> values) {
			this.sb = sb;
			this.values = values;
		}

		public StringBuilder sb;
		public List<Value> values;
	}

	protected QueryAndValues prepareInsertQuery(Object bean) {

		final BeanConfig bc = getConfigFor(bean.getClass());

		if (bc == null) {
			throw new BeanException("Cannot find bean config: " + bean.getClass());
		}

		if (bc.getNumberOfFields() == 0) {
			throw new BeanException("BeanConfig has zero fields: " + bc);
		}

		final StringBuilder sb = new StringBuilder(32 * bc.getNumberOfFields());

		sb.append("INSERT INTO ").append(bc.getTableName()).append("(");

		Iterator<DBField> iter = bc.pks();

		int count = 0;

		final List<Value> values = new LinkedList<Value>();

		while (iter.hasNext()) {

			final DBField dbField = iter.next();

			final String fieldName = dbField.getName();

			final String dbFieldName = dbField.getDbName();

			final DBType type = dbField.getType();

			if (type instanceof AutoIncrementType) {
				continue;
			}

			if (type instanceof AutoTimestampType) {
				continue;
			}

			if (type instanceof NowOnUpdateTimestampType) {
				continue;
			}
			
			final Object value = getValueFromBean(bean, fieldName);

			if (count++ > 0) {
				sb.append(',');
			}

			sb.append(dbFieldName);

			values.add(new Value(dbField, value));
		}

		iter = bc.fields();

		while (iter.hasNext()) {

			final DBField dbField = iter.next();

			if (dbField.isPK()) {
				continue;
			}

			final String fieldName = dbField.getName();

			final String dbFieldName = dbField.getDbName();

			final DBType type = dbField.getType();

			if (type instanceof AutoIncrementType) {
				continue;
			}

			if (type instanceof AutoTimestampType) {
				continue;
			}

			if (type instanceof NowOnUpdateTimestampType) {
				continue;
			}

			boolean isNowOnInsert = type instanceof NowOnInsertTimestampType || type instanceof NowOnInsertAndUpdateTimestampType;

			if (!isNowOnInsert) {

				Object value = getValueFromBean(bean, fieldName);

				if (count++ > 0) {
					sb.append(',');
				}

				sb.append(dbFieldName);

				values.add(new Value(dbField, value));

			} else {

				if (count++ > 0) {
					sb.append(',');
				}

				sb.append(dbFieldName);

				String cmd = getCurrentTimestampCommand();

				if (cmd == null) {
					values.add(new Value(dbField, new java.util.Date()));
				} else {
					values.add(new Value(dbField, true));
				}
			}
		}

		if (count == 0) {
			throw new BeanException("There is nothing to insert!");
		}

		sb.append(") VALUES(");

		final Iterator<Value> valuesIter = values.iterator();

		int i = 0;

		while (valuesIter.hasNext()) {

			final Value v = valuesIter.next();

			if (i > 0) {
				sb.append(',');
			}

			if (v.isSysdate) {
				sb.append(getCurrentTimestampCommand());
			} else {
				sb.append('?');
			}

			i++;
		}

		sb.append(')');

		if (values.isEmpty()) {
			throw new BeanException("Bean is empty: " + bean + " / " + bc);
		}

		return new QueryAndValues(sb, values);
	}

	protected Map<String, Value> bindToInsertStatement(PreparedStatement stmt, List<Value> values) {

		final Iterator<Value> iter2 = values.iterator();

		int index = 0;

		final Map<String, Value> fieldsLoaded = new HashMap<String, Value>();

		while (iter2.hasNext()) {

			final Value v = iter2.next();

			if (v.isSysdate && getCurrentTimestampCommand() != null) {
				continue;
			}

			try {

				v.field.getType().bindToStmt(stmt, ++index, v.value);

			} catch (Exception e) {
				throw new BeanException(e);
			}

			fieldsLoaded.put(v.field.getName(), v);
		}

		return fieldsLoaded;
	}

	@Override
	public void insert(final Object bean) {

		QueryAndValues qav = prepareInsertQuery(bean);

		StringBuilder sb = qav.sb;

		List<Value> values = qav.values;

		if (conn == null) {
			throw new BeanException("Connection is null!");
		}

		PreparedStatement stmt = null;

		try {

			if (DEBUG) {
				System.out.println("INSERT SQL: " + sb.toString());
			}

			stmt = conn.prepareStatement(sb.toString());

			Map<String, Value> fieldsLoaded = bindToInsertStatement(stmt, values);

			dispatchBeforeInsert(bean);
			
			final int x = stmt.executeUpdate();
			
			if (DEBUG_NATIVE) {
				System.out.println("INSERT SQL (NATIVE): " + stmt);
			}

			if (x > 1) {
				throw new BeanException("insert modified more than one line: " + x);
			}

			if (x == 0) {
				throw new BeanException("Nothing was inserted! Insert returned 0 rows!");
			}
			
			loaded.put(bean, fieldsLoaded);

		} catch (Exception e) {

			throw new BeanException(e);

		} finally {
			close(stmt);
		}
	}
	
	@Override
	public int deleteAll(final Object bean) {
		
		final BeanConfig bc = getConfigFor(bean.getClass());
		
		if (bc.getNumberOfFields() == 0) {
			throw new BeanException("BeanConfig has zero fields: " + bc);
		}
		
		final StringBuilder sb = new StringBuilder(32 * bc.getNumberOfFields());
		
		sb.append("DELETE ");
		
		List<Value> values = new LinkedList<Value>();
		
		prepareListQuery(sb, bc, bean, null, null, values);
		
		if (conn == null) {
			throw new BeanException("Connection is null!");
		}

		PreparedStatement stmt = null;

		try {

			if (DEBUG) {
				System.out.println("DELETE SQL: " + sb.toString());
			}

			stmt = conn.prepareStatement(sb.toString());

			final Iterator<Value> iter2 = values.iterator();

			int index = 0;

			while (iter2.hasNext()) {

				final Value v = iter2.next();

				v.field.getType().bindToStmt(stmt, ++index, v.value);

			}

			dispatchBeforeDelete(bean);
			
			final int x = stmt.executeUpdate();
			
			if (DEBUG_NATIVE) {
				System.out.println("DELETE SQL (NATIVE): " + stmt);
			}

			dispatchAfterDelete(bean);
			
			return x;

		} catch (Exception e) {

			throw new BeanException(e);

		} finally {

			close(stmt);
		}
	}

	@Override
	public boolean delete(final Object bean) {

		final BeanConfig bc = getConfigFor(bean.getClass());

		if (bc.getNumberOfFields() == 0) {
			throw new BeanException("BeanConfig has zero fields: " + bc);
		}

		final StringBuilder sb = new StringBuilder(32 * bc.getNumberOfFields());

		sb.append("DELETE FROM ").append(bc.getTableName()).append(" WHERE ");

		if (!bc.hasPK()) {
			throw new BeanException("Cannot delete bean without a PK!");
		}

		final Iterator<DBField> iter = bc.pks();

		final List<Value> values = new LinkedList<Value>();

		int count = 0;

		while (iter.hasNext()) {

			final DBField dbField = iter.next();

			final String fieldName = dbField.getName();

			final String dbFieldName = dbField.getDbName();

			final Object value = getValueFromBean(bean, fieldName);

			if (value == null) {
				throw new BeanException("pk is missing: " + dbField);
			} else if (value instanceof Number) {

				final Number n = (Number) value;

				if (n.doubleValue() <= 0) {
					throw new BeanException("Number pk is missing: " + dbField);
				}

			}

			if (count++ > 0) {
				sb.append(" AND ");
			}

			sb.append(dbFieldName).append("=?");

			values.add(new Value(dbField, value));

		}

		if (values.isEmpty()) {
			throw new BeanException("Bean is empty: " + bean + " / " + bc);
		}

		if (conn == null) {
			throw new BeanException("Connection is null!");
		}

		PreparedStatement stmt = null;

		try {

			if (DEBUG) {
				System.out.println("DELETE SQL: " + sb.toString());
			}

			stmt = conn.prepareStatement(sb.toString());

			final Iterator<Value> iter2 = values.iterator();

			int index = 0;

			while (iter2.hasNext()) {

				final Value v = iter2.next();

				v.field.getType().bindToStmt(stmt, ++index, v.value);

			}

			dispatchBeforeDelete(bean);
			
			final int x = stmt.executeUpdate();
			
			if (DEBUG_NATIVE) {
				System.out.println("DELETE SQL (NATIVE): " + stmt);
			}

			if (x > 1) {
				throw new BeanException("delete modified more than one line: " + x);
			}

			if (x == 0) {
				return false;
			}

			loaded.remove(bean);

			dispatchAfterDelete(bean);
			
			return true;

		} catch (Exception e) {

			throw new BeanException(e);

		} finally {

			close(stmt);
		}
	}

	@Override
	public <E> List<E> loadList(final E bean) {

		return loadListImpl(bean, null, null, null, null);
	}

	@Override
	public <E> List<E> loadList(final E bean, Object... properties) {
		
		return loadListImpl(bean, null, null, getProperties(properties), null);
	}
	
	@Override
	public <E> E loadUnique(E bean) {
		return loadUniqueImpl(bean, null, null);
	}
	
	@Override
	public <E> E loadUnique(E bean, Object... properties) {
		return loadUniqueImpl(bean, getProperties(properties), null);
	}
	
	@Override
	public <E> E loadUniqueMinus(E bean, Object... minus) {
		return loadUniqueImpl(bean, null, getProperties(minus));
	}

	protected <E> E loadUniqueImpl(final E bean, String[] properties, String[] minus) {
		
		E o = checkUnique(loadListImpl(bean, null, new Limit(2), properties, minus));

		if (o != null) {
			loadImpl(o, properties, minus); // load twice to attach to session so dynamic update is by default!
		}
		
		return o;
	}

	@Override
	public <E> List<E> loadList(final E bean, final OrderBy orderBy) {

		return loadListImpl(bean, orderBy, null, null, null);
	}

	@Override
	public <E> List<E> loadList(final E bean, final OrderBy orderBy, Object... properties) {
		return loadListImpl(bean, orderBy, null, getProperties(properties), null);
	}

	@Override
	public <E> List<E> loadList(final E bean, final Limit limit) {

		return loadList(bean, null, limit);
	}

	@Override
	public <E> List<E> loadList(final E bean, final Limit limit, Object... properties) {
		return loadListImpl(bean, null, limit, getProperties(properties), null);
	}

	/**
	 * Load a list of beans, but exclude some fields. Useful when the bean has too many properties and you don't want to fetch everything from the database.
	 * 
	 * @param <E>
	 * @param bean
	 * @param minus
	 * @return A list of beans
	 */
	@Override
	public <E> List<E> loadListMinus(final E bean, final Object... minus) {
		
		return loadListMinus(bean, null, null, minus);
	}

	/**
	 * Load a list of beans, but exclude some fields. Useful when the bean has too many properties and you don't want to fetch everything from the database.
	 * 
	 * @param <E>
	 * @param bean
	 * @param minus
	 * @param orderBy
	 * @return A list of beans
	 */
	@Override
	public <E> List<E> loadListMinus(final E bean, final OrderBy orderBy, final Object... minus) {

		return loadListMinus(bean, orderBy, null, minus);
	}

	/**
	 * Load a list of beans, but exclude some fields. Useful when the bean has too many properties and you don't want to fetch everything from the database.
	 * 
	 * @param <E>
	 * @param bean
	 * @param minus
	 * @param limit
	 * @return A list of beans
	 */
	@Override
	public <E> List<E> loadListMinus(final E bean, final Limit limit, final Object... minus) {
		
		return loadListMinus(bean, null, limit, minus);
	}

	/**
	 * Each dialect can override this to return the database column type it supports other than the ANSI standard.
	 * 
	 * @param dbType
	 * @return The string representation of this database type to be used with create table statement
	 */
	protected String getDatabaseType(DBType<?> dbType) {
		return dbType.getAnsiType();
	}
	
	/**
	 * Each dialect can override this to return true if the <i>VARCHAR</i> type supports unlimited size
	 * @return <b>true</b> if database supports <i>VARCHAR</i> with no limit, <b>false</b> otherwise
	 */
	protected boolean isVarcharUnlimitedSupported() {
		return false;
	}

	@Override
	public void createTable(Class<? extends Object> beanKlass) {
		BeanConfig bc = getConfigFor(beanKlass);
		if (bc == null) {
			throw new BeanException("Cannot find bean config: " + beanKlass);
		}

		if (bc.getNumberOfFields() == 0) {
			throw new BeanException("Cannot create table with zero columns: " + beanKlass);
		}

		StringBuilder sb = new StringBuilder(1024);

		sb.append("create table ").append(bc.getTableName()).append(" (");

		Iterator<DBField> iter = bc.fields();

		int count = 0;

		while (iter.hasNext()) {
			DBField dbField = iter.next();
			DBType<?> dbType = dbField.getType();

			if (count++ > 0) {
				sb.append(", ");
			}

			String dbTypeStr = getDatabaseType(dbType);
			
			if (dbTypeStr == null)
				throw new BeanException("Invalid ANSI type for column '"+
			dbField.getDbName()+"' in table '"+bc.getTableName()+"'. Maybe you're using a GenericType.");
			
			sb.append(dbField.getDbName()).append(" ").append(dbTypeStr);

			if (dbType instanceof SizedType) {
				int size = ((SizedType) dbType).getSize();
				
				if (size <= 0 && !isVarcharUnlimitedSupported()) {

					// no limit is not supported, so get the default size..
					size = SizedType.DEFAULT_SIZE;
				}

				if (size > 0)
					sb.append("(").append(size).append(")");
			}

			if (dbType.canBeNull() == false || dbField.isPK()) {
				sb.append(" NOT NULL");
			}
		}

		sb.append(")");

		if (DEBUG) {
			System.out.println("CREATE TABLE SQL: " + sb.toString());
		}

		PreparedStatement stmt = null;

		boolean autoCommit = false;
		
		try {
			autoCommit = conn.getAutoCommit();
			
			conn.setAutoCommit(false);
			
			stmt = conn.prepareStatement(sb.toString());
			stmt.executeUpdate();
			
			if (DEBUG_NATIVE) {
				System.out.println("CREATE TABLE SQL (NATIVE): " + stmt);
			}
			
			close(stmt);
			
			String pkConstraintQuery = createPKConstraintQuery(bc.getTableName(), bc.pks());
			
			if (DEBUG) {
				System.out.println("PK CONSTRAINT QUERY: "+pkConstraintQuery);
			}
			
			stmt = conn.prepareStatement(pkConstraintQuery);
			stmt.executeUpdate();
			
			if (DEBUG_NATIVE) {
				System.out.println("PK CONSTRAINT QUERY (NATIVE): " + stmt);
			}
			
			if (autoCommit)
				conn.commit();

		} catch (Exception e) {
			
			if (autoCommit) {
				try {
					conn.rollback();
					conn.setAutoCommit(true);
				}catch (Exception e2) {
					throw new BeanException(e2);
				}
			}
			
			throw new BeanException(e);
			
		} finally {
			
			if (autoCommit) {
				try {
					conn.setAutoCommit(true);
				}catch (Exception e) {
					throw new BeanException(e);
				}
			}
			
			close(stmt);
		}
		
	}

	@Override
	public void createTables() {
		Set<BeanConfig> all = beanManager.getBeanConfigs();
		for (BeanConfig bc : all) {
			createTable(bc.getBeanClass());
		}
	}
	
	@Override
	public void dropTable(Class<? extends Object> beanKlass) {
		
		StringBuilder sb = new StringBuilder("DROP TABLE ");
		String tableName = buildTableName(beanKlass); 
		sb.append(tableName);
		
		if (DEBUG) {
			System.out.println("DROP TABLE QUERY: "+sb.toString());
		}
		
		PreparedStatement ppst = null;
		
		try {
			
			ppst = SQLUtils.prepare(conn, sb.toString());
			ppst.executeUpdate();
			
			if (DEBUG_NATIVE) {
				System.out.println("DROP TABLE QUERY (NATIVE): "+ppst);				
			}
			
		}catch (SQLException e) {
			
			throw new BeanException("Unable to drop table '"+tableName+"'", e);
			
		}finally {
			
			SQLUtils.close(ppst);
		}
	}
	
	/**
	 * Create a SQL query to add the primary key constraint
	 * @param 	table - The table name 
	 * @param 	pks - An iterator of all primary key fields that will be added to the table
	 * @return	A String containing the resulting <i>alter table</i> constraint query
	 */
	protected String createPKConstraintQuery(String table, Iterator<DBField> pks) {
		
		StringBuilder sb = new StringBuilder("alter table ");
		sb.append(table);
		sb.append(" add primary key (");
		
		if (pks.hasNext())
			sb.append(pks.next().getDbName());
		
		while (pks.hasNext()) {
			DBField dbField = pks.next();
			
			sb.append(", ").append(dbField.getDbName());
		}
		
		return sb.append(")").toString();
		
	}

//	private int getSize(DBType<?> dbType) {
//		if (dbType instanceof SizedType) {
//			SizedType st = (SizedType) dbType;
//			return st.getSize();
//		}
//		throw new IllegalStateException("Cannot get size from type: " + dbType);
//	}
	
	public BeanConfig getConfigFor(Class<? extends Object> clazz) {
		return beanManager.getBeanConfig(clazz);
	}
	
	@Override
	public void addTrigger(TriggerListener trigger) {
		
		dispatcher.addTrigger(trigger);
	}
	
	@Override
	public void removeTrigger(TriggerListener trigger) {
		
		dispatcher.removeTrigger(trigger);
	}
	
	protected void dispatchBeforeInsert(Object bean) {
		
		dispatchTrigger(Type.BEFORE_INSERT, bean);
	}
	
	protected void dispatchAfterInsert(Object bean) {
		
		dispatchTrigger(Type.AFTER_INSERT, bean);
	}
	
	protected void dispatchBeforeUpdate(Object bean) {
		
		dispatchTrigger(Type.BEFORE_UPDATE, bean);
	}
	
	protected void dispatchAfterUpdate(Object bean) {
		
		dispatchTrigger(Type.AFTER_UPDATE, bean);
	}
	
	protected void dispatchBeforeDelete(Object bean) {
		
		dispatchTrigger(Type.BEFORE_DELETE, bean);
	}
	
	protected void dispatchAfterDelete(Object bean) {
		
		dispatchTrigger(Type.AFTER_DELETE, bean);
	}
	
	/**
	 * Dispatch all triggers from actual <code>BeanSession</code> and respective <code>BeanConfig</code>.  
	 * This method is called when <b>insert</b>, <b>update</b> or <b>delete</b> operation occurs.
	 * @param type - TriggerType indicating what trigger event will be dispatched
	 * @param bean - Bean that will be set into TriggerEvent object
	 */
	protected void dispatchTrigger(Type type, Object bean) {
		
		TriggerEvent evt = new TriggerEvent(this, bean);
		
		dispatcher.dispatch(type, evt);
		getConfigFor(bean.getClass()).getDispatcher().dispatch(type, evt);
	}

	protected class Value {

		public Object value;

		public DBField field;

		public boolean isSysdate;

		private Value(final DBField field, Object value, final boolean isSysdate) {

			this.field = field;

			this.value = value;

			this.isSysdate = isSysdate;
		}

		public Value(final DBField field, final Object value) {

			this(field, value, false);
		}

		public Value(final DBField field, final boolean isSysdate) {

			this(field, null, isSysdate);
		}
	}

	static void close(PreparedStatement stmt) {
		SQLUtils.close(stmt);
	}

	static void close(PreparedStatement stmt, ResultSet rset) {
		SQLUtils.close(rset, stmt);
	}
	
	@Override
	public <E> TableAlias<E> createTableAlias(Class<? extends E> beanClass) {
		return new TableAlias<E>(this, beanManager.getBeanConfig(beanClass), beanClass);
	}
	
	@Override
	public <E> TableAlias<E> createTableAlias(Class<? extends E> beanClass, String prefix) {
		return new TableAlias<E>(this, beanManager.getBeanConfig(beanClass), beanClass, prefix);
	}

}