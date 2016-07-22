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
package org.mentabean;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.List;

import org.mentabean.event.TriggerListener;
import org.mentabean.jdbc.QueryBuilder;
import org.mentabean.sql.TableAlias;
import org.mentabean.util.Limit;
import org.mentabean.util.OrderBy;

/**
 * Describe a simple ORM interface that can perform CRUD for Beans according to properties defined programmatically on BeanManager. It can also load lists and unique beans based on properties set on a given bean. It also supports dynamic update, in other words, it will only update fields from a bean
 * loaded from the database that were actually modified during the session. The behavior can be turned off when necessary ,in other words, when you want to blindly update all properties from a bean in the database regarding of whether they were modified or not.
 * 
 * @author Sergio Oliveira Jr.
 */
public interface BeanSession {

	public static final int UPDATE = 0, INSERT = 1;
	
	/**
	 * Get the database connection being used by this bean session.
	 * 
	 * @return the database connection
	 */
	public Connection getConnection();

	/**
	 * Load the bean from the database, injecting all its properties through reflection. Note that the bean passed MUST have its primary key set otherwise there is no way we can load it from the database.
	 * 
	 * @param bean
	 *            The bean we want to load from the DB.
	 * @return true if the bean was found in the database, false otherwise
	 */
	public boolean load(Object bean);
	
	public boolean load(Object bean, Object... properties);
	
	public boolean loadMinus(Object bean, Object... minus);

	/**
	 * Update the bean in the database. Only the bean fields that have been modified (dirty) will be updated.
	 * 
	 * It will return 1 if an update did happen or 0 if the bean could not be found in the database or if there was nothing modified in bean.
	 * 
	 * The bean MUST have its primary key set, otherwise it is impossible to update the bean in the database, and an exception will be thrown.
	 * 
	 * @param bean
	 *            The bean to be updated
	 * @param forceNull
	 * 			  Database columns that will be forced to null if the bean property is not set
	 * @return 1 if update was successful, 0 if the update did not happen or was not necessary
	 */
	public int update(Object bean, Object... forceNull);

	/**
	 * Same as update(bean) but here you can turn off the default dynamic update behavior and force all bean properties to be updated regardless whether they have been modified or not.
	 * 
	 * @param bean
	 * @return the number of rows that were updated
	 * @throws Exception
	 */
	public int updateAll(Object bean);
	
	/**
	 * Updates an object using only the differences between newBean and oldBean instances. 
	 * This method is useful in distributed environments like RMI or web services because these approaches should serialize objects to send/receive informations from client to server and vice versa 
	 * @param newBean
	 * @param oldBean
	 * @return 1 if update was successful, 0 if the update did not happen or was not necessary (when bean instances have no differences)
	 */
	public <E> int updateDiff(E newBean, E oldBean);

	/**
	 * Insert the bean in the database.
	 * 
	 * Depending on the type of PK, the generation of the PK can and should be taken care by the DB itself. The generated PK should be inserted in the bean by reflection, before the method returns.
	 * 
	 * The default, database-independent implementation of this method, insert all fields in the database not worrying about PK generation strategies.
	 * 
	 * @param bean
	 *            The bean to insert
	 */
	public void insert(Object bean);
	
	/**
	 * Tries to update or insert a bean using the <code>update</code> method.
	 * 
	 * @param bean The bean to update or insert
	 * @param forceNull Database columns that will be forced to null (or zero) if the bean property is not set
	 * @return	A value <b>0 (zero)</b> if operation executed an <code>update</code> and <b>1 (one)</b> if <code>insert</code> method was executed
	 * @see #saveAll(Object)
	 * @see #update(Object, Object...)
	 */
	public int save(Object bean, Object... forceNull);
	
	/**
	 * Tries to update or insert a bean object into database. The update uses the <code>updateAll</code> method.
	 * 
	 * @param bean The bean to update or insert
	 * @return	A value <b>0 (zero)</b> if operation executed an <code>update</code> and <b>1 (one)</b> if <code>insert</code> method was executed
	 * @see #updateAll(Object)
	 */
	public int saveAll(Object bean);

	/**
	 * Delete the bean from the database.
	 * 
	 * The PK of the bean MUST be set. The bean can only be deleted by its PK.
	 * 
	 * @param bean
	 * @return true if it was deleted or false if it did not exist
	 * @throws Exception
	 */
	public boolean delete(Object bean);
	
	/**
	 * Delete all data based on the properties present in the bean passed
	 * 
	 * The PK doesn't need to be set
	 * 
	 * @param bean
	 * @return The total of tuples removed from database
	 */
	public int deleteAll(final Object bean);

	/**
	 * Count the number of beans that would be returned by a loadList method.
	 * 
	 * @param bean
	 *            The bean holding the properties used by the list query.
	 * @return the number of beans found in the database
	 */
	public int countList(Object bean);

	/**
	 * Load a list of beans based on the properties present in the bean passed. For example, if you want to load all users with lastName equal to 'saoj' you would instantiate a bean and set its lastName property to 'saoj' before passing it as an argument to this method.
	 * 
	 * @param <E>
	 * @param bean
	 *            The bean holding the properties used by the list query.
	 * @return A list of beans the match the properties in the given bean. Or an empty list if nothing was found.
	 */
	public <E> List<E> loadList(E bean);

	public <E> List<E> loadList(E bean, Object... properties);

	public <E> List<E> loadListMinus(E bean, Object... minus);

	/**
	 * Same as loadList(bean) except that you can order the list returned by passing an SQL orderBy clause.
	 * 
	 * @param <E>
	 * @param bean
	 *            The bean holding the properties used by the list query.
	 * @param orderBy
	 *            The orderBy SQL clause.
	 * @return A list of beans the match the properties in the given bean. Or an empty list if nothing was found.
	 */
	public <E> List<E> loadList(E bean, OrderBy orderBy);

	public <E> List<E> loadList(E bean, OrderBy orderBy, Object... properties);

	public <E> List<E> loadListMinus(E bean, OrderBy orderBy, Object... minus);

	/**
	 * Same as loadList(bean) except that you can limit the number of beans returned in the list.
	 * 
	 * @param <E>
	 * @param bean
	 *            The bean holding the properties used by the list query.
	 * @param limit
	 *            The max number of beans returned in the list.
	 * @return A list of beans the match the properties in the given bean. Or an empty list if nothing was found.
	 */
	public <E> List<E> loadList(E bean, Limit limit);

	public <E> List<E> loadList(E bean, Limit limit, Object... properties);

	public <E> List<E> loadListMinus(E bean, Limit limit, Object... minus);

	/**
	 * Same as loadList(bean) except that you can limit the number of beans returned in the list as well as sort them by passing a orderBy SQL clause.
	 * 
	 * @param <E>
	 * @param bean
	 *            The bean holding the properties used by the list query.
	 * @param orderBy
	 *            The orderBy SQL clause.
	 * @param limit
	 *            The max number of beans returned in the list.
	 * @return A list of beans the match the properties in the given bean. Or an empty list if nothing was found.
	 */
	public <E> List<E> loadList(E bean, OrderBy orderBy, Limit limit);

	public <E> List<E> loadList(E bean, OrderBy orderBy, Limit limit, Object... properties);

	public <E> List<E> loadListMinus(E bean, OrderBy orderBy, Limit limit, Object... minus);

	/**
	 * Same as loadList(bean) but it attempts to load a single bean only. If more than one bean is found it throws an exception.
	 * 
	 * NOTE: The returned bean will be attached by the session so only the modified properties will be updated in case update() is called.
	 * 
	 * @param <E>
	 * @param bean
	 *            The bean holding the properties used by the load query.
	 * @return A unique bean that match the properties in the given bean. Or null if nothing was found.
	 * @throws BeanException
	 *             if more than one bean is found by the query.
	 */
	public <E> E loadUnique(E bean);
	
	public <E> E loadUnique(E bean, Object... properties);
	
	public <E> E loadUniqueMinus(E bean, Object... minus);

	public String buildSelect(final Class<? extends Object> beanClass);

	public String buildSelect(final Class<? extends Object> beanClass, Object... properties);

	public String buildSelectMinus(final Class<? extends Object> beanClass, Object... minus);

	public String buildSelect(final Class<? extends Object> beanClass, String tableName);

	public String buildSelect(final Class<? extends Object> beanClass, String tableName, Object... properties);

	public String buildSelectMinus(final Class<? extends Object> beanClass, String tableName, Object... minus);

	public void populateBean(final ResultSet rset, final Object bean);

	public void populateBean(final ResultSet rset, final Object bean, Object... properties);

	public void populateBeanMinus(final ResultSet rset, final Object bean, Object... minus);

	public void populateBean(final ResultSet rset, final Object bean, String tableName);

	public void populateBean(final ResultSet rset, final Object bean, String tableName, Object... properties);

	public void populateBeanMinus(final ResultSet rset, final Object bean, String tableName, Object... minus);

	// some experiment with metadata...

	public void createTable(Class<? extends Object> beanKlass);
	
	public void dropTable(Class<? extends Object> beanKlass);

	public void createTables();
	
	// some useful methods to handle with manual queries
	
	public String propertyToColumn(Class<? extends Object> clazz, Object property, String alias);
	
	public String propertyToColumn(Class<? extends Object> clazz, Object property);
	
	/**
	 * Creates an single instance only with primary keys according the given object
	 * @param bean - The bean with all primary key set
	 * @return A new instance of the bean
	 */
	public <E> E createBasicInstance(E bean);
	
	/**
	 * Compare differences between two beans holding the properties of first bean passed and
	 * adding null properties in nullProps list. This method returns null when beans has no differences
	 * @param
	 * 		bean
	 * @param
	 * 		another
	 * @param 
	 * 		nullProps An empty list
	 * @return A new instance of the bean
	 */
	public <E> E compareDifferences(E bean, E another, List<String> nullProps);
	
	/**
	 * Returns the table name configured for the given class
	 * @param clazz - The bean class
	 * @return Table name
	 */
	public String buildTableName(Class<? extends Object> clazz);
	
	/**
	 * Returns the BeanConfig object from the given bean class
	 * @param clazz
	 * @return BeanConfig
	 */
	public BeanConfig getConfigFor(Class<?> clazz);
	
	/**
	 * Builds a new QueryBuilder so it's possible to create fluent custom SQL queries
	 * @return QueryBuilder object
	 */
	public QueryBuilder buildQuery();
	
	//trigger..
	/**
	 * Add a TriggerListener in this session. Remember that it's not added in BeanConfig, only in current session
	 * @param trigger
	 */
	public void addTrigger(TriggerListener trigger);
	
	/**
	 * Removes the TriggerListener from session
	 * @param trigger
	 */
	public void removeTrigger(TriggerListener trigger);
	
	/**
	 * Return a table alias for this bean class
	 * 
	 * @param beanClass
	 * @return TableAlias
	 */
	public <E> TableAlias<E> createTableAlias(Class<? extends E> beanClass);
	
	/**
	 * Return a table alias for this bean class with a prefix.
	 * 
	 * @param beanClass
	 * @param prefix
	 * @return TableAlias
	 */
	public <E> TableAlias<E> createTableAlias(Class<? extends E> beanClass, String prefix);
	
}