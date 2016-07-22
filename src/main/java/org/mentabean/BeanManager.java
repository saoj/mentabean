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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.mentabean.util.DefaultProxy;
import org.mentabean.util.PropertiesProxy;

/**
 * The manager that keeps track of the configuration for all beans.
 * 
 * @author sergio.oliveira.jr@gmail.com
 */
public class BeanManager {

	private final Map<Class<? extends Object>, BeanConfig> beans = new HashMap<Class<? extends Object>, BeanConfig>();
	
	public BeanManager(PropertiesProxy proxy) {
		
		PropertiesProxy.INSTANCE = proxy == null ? new DefaultProxy() : proxy;			
	}
	
	public BeanManager() {
		this(null);
	}

	/**
	 * Add a bean configuration.
	 * 
	 * @param bc
	 *            The bean configuration to add.
	 * @return The BeanConfig added (Fluent API)
	 */
	public BeanConfig bean(final BeanConfig bc) {

		if (beans.containsKey(bc.getBeanClass())) {
			throw new IllegalStateException("A configuration was already added for this bean ("+bc.getBeanClass()+")");
		}

		beans.put(bc.getBeanClass(), bc);

		return bc;
	}

	/**
	 * Add a bean configuration.
	 * 
	 * @param bc
	 *            The bean configuration to add.
	 */
	public void addBeanConfig(final BeanConfig bc) {
		bean(bc);
	}

	/**
	 * Creates a bean configuration and add to this manager.
	 * 
	 * @param beanClass
	 *            The bean class
	 * @param tableName
	 *            The table name where the bean properties will be stored.
	 * @return The BeanConfig created (Fluent API)
	 */
	public BeanConfig bean(final Class<? extends Object> beanClass, final String tableName) {

		return bean(new BeanConfig(beanClass, tableName));
	}

	/**
	 * Get the bean configuration for the given bean class.
	 * 
	 * @param beanClass
	 *            The bean class
	 * @return The bean configuration for this bean or null if it was not defined
	 */
	public BeanConfig getBeanConfig(final Class<? extends Object> beanClass) {

		return beans.get(beanClass);
	}

	public Set<BeanConfig> getBeanConfigs() {
		Set<BeanConfig> all = new HashSet<BeanConfig>();
		all.addAll(beans.values());
		return all;
	}
}