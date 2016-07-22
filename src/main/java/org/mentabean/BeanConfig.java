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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.mentabean.event.TriggerDispatcher;
import org.mentabean.event.TriggerListener;
import org.mentabean.type.AutoIncrementType;
import org.mentabean.type.SequenceType;
import org.mentabean.util.PropertiesProxy;

/**
 * A class representing a bean configuration, like table name, primary keys and fields in the database.
 * 
 * @author sergio.oliveira.jr@gmail.com
 */
public class BeanConfig {

	private final Map<String, DBField> fieldList = new LinkedHashMap<String, DBField>();

	private final Map<String, DBField> pkList = new LinkedHashMap<String, DBField>();
	
	private final Map<String, Class<? extends Object>> abstractInstances = new LinkedHashMap<String, Class<? extends Object>>();
	
	private final Class<? extends Object> beanClass;

	private final String tableName;

	private DBField sequence = null;
	
	private String sequenceName = null;

	private DBField autoincrement = null;
	
	private TriggerDispatcher dispatcher = new TriggerDispatcher();

	/**
	 * Creates a configuration for a bean represented by the given class.
	 * 
	 * @param beanClass
	 *            The bean klass
	 * @param tableName
	 *            The database table where the bean properties will be stored.
	 */
	public BeanConfig(final Class<? extends Object> beanClass, final String tableName) {

		this.beanClass = beanClass;

		this.tableName = tableName;
	}

	/**
	 * Return the table name where the bean properties are stored.
	 * 
	 * @return The database table name.
	 */
	public String getTableName() {

		return tableName;
	}

	/**
	 * Return the bean class.
	 * 
	 * @return The bean class.
	 */
	public Class<? extends Object> getBeanClass() {

		return beanClass;
	}

	/**
	 * Add the sequence name *in the database* that will be used for this field.
	 * 
	 * NOTE: A field of type SEQUENCE must have been defined before or an IllegalStateException is thrown.
	 * 
	 * @param seqNameInDb the name of the sequence in the database
	 * @return this bean config
	 */
	public BeanConfig addSequenceName(String seqNameInDb) {
		
		if (sequence == null) {
			throw new IllegalStateException("There is no sequence field defined!");
		}
		
		sequenceName = seqNameInDb;
		return this;
	}
	
	/**
	 * Alias for method addSequence
	 * 
	 * @param seqNameInDb
	 * @return this bean config
	 */
	public BeanConfig seq(String seqNameInDb) {
		return addSequenceName(seqNameInDb);
	}

	/**
	 * Returns the name of the sequence in the database.
	 * 
	 * NOTE: The name returned is the name of the sequence in the database.
	 * 
	 * @return the name of the sequence in the database
	 */
	public String getSequenceName() {
		return sequenceName;
	}
	
	public BeanConfig remove(final String name) {
		
		if (name == null) {
			return remove((Object) null);
		}
		
		fieldList.remove(name);
		pkList.remove(name);
		return this;
	}
	
	public BeanConfig remove(Object propValueFromGetter) {
		return remove(PropertiesProxy.getPropertyName());
	}
	
	private BeanConfig addField(final String name, final String dbName, final DBType<? extends Object> type, final boolean isPK) {

		if (!isPK) {

			if (type instanceof SequenceType) {
				throw new IllegalStateException("A sequence type can only be a primary key!");
			}

			if (type instanceof AutoIncrementType) {
				throw new IllegalStateException("A auto-increment type can only be a primary key!");
			}
		}

		final DBField f = new DBField(name, dbName, type, isPK);

		fieldList.remove(name); // just in case we are re-adding it...

		fieldList.put(name, f);
		
		if (isPK) {

			pkList.remove(name); // just in case we are re-adding it...

			pkList.put(name, f);

			if (type instanceof SequenceType) {

				if (sequence != null) {
					throw new IllegalStateException("A bean can have only one sequence field!");
				}

				sequence = f;

			} else if (type instanceof AutoIncrementType) {

				if (autoincrement != null) {
					throw new IllegalStateException("A bean can have only one auto-increment field!");
				}

				autoincrement = f;
			}
		}

		return this;
	}
	
	public DBField getField(String name) {
		
		return fieldList.get(name);
	}

	/**
	 * Return an auto-increment field, if one was configured for this bean.
	 * 
	 * Note: A bean can have only one auto-increment field configured for this bean. Attempting to configure more than one will throw an exception.
	 * 
	 * @return the auto-increment field configured for this bean or null if it was not defined
	 */
	public DBField getAutoIncrementField() {

		return autoincrement;
	}

	/**
	 * Return a sequence field, if one was configured for this bean.
	 * 
	 * Note: A bean can have only one sequence field configured for this bean. Attempting to configure more than one will throw an exception.
	 * 
	 * @return the sequence field configured for this bean or null if it was not defined
	 */
	public DBField getSequenceField() {

		return sequence;
	}

	/**
	 * Add a database field for the given property with the given database type. It assumes that the property name is the SAME as the database column name. If they are different, use the other addField method.
	 * 
	 * @param name
	 *            The bean property name (same as the database column name)
	 * @param type
	 *            The database type
	 * @return This BeanConfig (Fluent API)
	 */
	public BeanConfig field(final String name, final DBType<? extends Object> type) {

		if (name == null) {
			return field((Object) null, type);
		}
		
		return addField(name, name.replace(".", "_"), type, false);
	}
	
	public BeanConfig field(Object propValueFromGetter, final DBType<? extends Object> type) {
		
		return field(PropertiesProxy.getPropertyName(), type);
	}

	/**
	 * Add a database field for the given property with the given database type.
	 * 
	 * @param name
	 *            The bean property name
	 * @param dbName
	 *            The name of the database column holding this property
	 * @param type
	 *            The database type
	 * @return This BeanConfig (Fluent API)
	 */
	public BeanConfig field(final String name, final String dbName, final DBType<? extends Object> type) {
		
		if (name == null) {
			return field((Object) null, dbName, type);
		}

		return addField(name, dbName, type, false);
	}
	
	public BeanConfig field(Object propValueFromGetter, String dbName, final DBType<? extends Object> type) {
		
		return field(PropertiesProxy.getPropertyName(), dbName, type);
	}

	/**
	 * Add a bean property that is the primary key in the database. The column name is the same as the property bean name. If they are different use the other pk method. All beans must have a primary key and you can call this method multiple times to support composite primary keys.
	 * 
	 * @param name
	 *            The bean property name
	 * @param type
	 *            The database type
	 * @return This BeanConfig (Fluent API)
	 */
	public BeanConfig pk(final String name, final DBType<? extends Object> type) {
		
		if (name == null) {
			return pk((Object) null, type);
		}

		return addField(name, name.replace(".", "_"), type, true);
	}
	
	public BeanConfig pk(Object propValueFromGetter, final DBType<? extends Object> type) {
		
		return pk(PropertiesProxy.getPropertyName(), type);
	}

	/**
	 * Add a property that is the primary key in the database. All beans must have a primary key and you can call this method multiple times to support composite primary keys.
	 * 
	 * @param name
	 *            The bean property name
	 * @param dbName
	 *            The name of the database column holding this property
	 * @param type
	 *            The database type
	 * @return This BeanConfig (Fluent API)
	 */
	public BeanConfig pk(final String name, final String dbName, final DBType<? extends Object> type) {
		
		if (name == null) {
			return pk((Object) null, dbName, type);
		}

		return addField(name, dbName, type, true);
	}
	
	public BeanConfig pk(Object propValueFromGetter, String dbName, final DBType<? extends Object> type) {
		
		return pk(PropertiesProxy.getPropertyName(), dbName, type);
	}

	/**
	 * Return the number of fields configured for this bean. It includes the PK.
	 * 
	 * @return The number of fields configured for this bean.
	 */
	public int getNumberOfFields() {

		return fieldList.size();
	}

	public int getNumberOfPKs() {

		return pkList.size();
	}

	@Override
	public String toString() {

		final StringBuilder sb = new StringBuilder(64);

		sb.append("BeanConfig: ").append(beanClass.getSimpleName()).append(" tableName=").append(tableName);

		return sb.toString();
	}

	/**
	 * Return all DBFields configured for this bean. It includes the PK as well.
	 * 
	 * @return An Iterator with all DBFields configured for this bean.
	 */
	public Iterator<DBField> fields() {

		return fieldList.values().iterator();
	}

	/**
	 * Check whether the primary key was defined.
	 * 
	 * @return true if a primary key was defined.
	 */
	public boolean hasPK() {

		return !pkList.isEmpty();
	}

	/**
	 * Return an iterator with the DBFields for the PK configured for this bean.
	 * 
	 * Note: A bean can have more than one property as its primary key, in that case it has a composite primary key.
	 * 
	 * @return An iterator with the DBFields for the PK.
	 */
	public Iterator<DBField> pks() {

		return pkList.values().iterator();
	}
	
	public BeanConfig trigger(TriggerListener trigger) {
		
		dispatcher.addTrigger(trigger);
		
		return this;
	}
	
	public BeanConfig remove(TriggerListener trigger) {
		
		dispatcher.removeTrigger(trigger);
		
		return this;
	}
	
	public TriggerDispatcher getDispatcher() {
		return dispatcher;
	}
	
	/**
	 * Configures a class that should be used instead of property type to create instances
	 * through {@link Class#newInstance()} method. It's useful when working with abstract objects
	 * @param abstractProperty - The property name
	 * @param clazz - The concrete class
	 * @return this
	 */
	public BeanConfig abstractInstance(String abstractProperty, Class<? extends Object> clazz) {
		
		if (abstractProperty == null) {
			return abstractInstance((Object) abstractProperty, clazz);
		}
		
		abstractInstances.put(abstractProperty, clazz);
		
		return this;
		
	}	
	
	/**
	 * Configures a class that should be used instead of property type to create instances
	 * through {@link Class#newInstance()} method. It's useful when working with abstract objects
	 * @param abstractProperty - The property name (through proxy)
	 * @param clazz - The concrete class
	 * @return this
	 */
	public <E> BeanConfig abstractInstance(E abstractProperty, Class<? extends E> clazz) {
		
		return abstractInstance(PropertiesProxy.getPropertyName(), clazz);
		
	}
	
	public Class<? extends Object> getAbstractProperty(String key) {
		
		return abstractInstances.get(key);
	}
	
}
