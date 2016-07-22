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

/**
 * The functionality of a MentaBean, in other words, the methods you can call on a mentabean to perform database operations like insert, load, reload, save, update and delete.
 * 
 * @author Sergio Oliveira Jr.
 */
public interface MentaBean {

	/**
	 * Attempt to insert a bean in the database.
	 * 
	 * Note: This method can be called multiple times in the same bean. Of course the PK constraints will be enforced by the database, so you can change the PK by hand and call this method multiple times, to insert multiple beans. If the PK is an auto-generated database field (e.g. auto-increment),
	 * you will not change the PK by hand and a new PK will be automatically assigned to the bean.
	 */
	public void insert();

	/**
	 * Attempt to load the bean properties from the database.
	 * 
	 * This method will throw an exception if you try to load a bean without its primary key set.
	 * 
	 * @return true if the bean was loaded
	 */
	public boolean load();

	/**
	 * Attempt to update the bean properties in the database. If this bean was previously loaded, this method will update only the properties that were modified (dirty).
	 * 
	 * @return true if it was updated
	 */
	public boolean update();

	/**
	 * Attempt to update ALL the bean properties in the database, not just the ones that have been changed.
	 * 
	 * @return true if it was updated
	 */
	public boolean updateAll();

	/**
	 * Attempt to delete a bean from the database. It will throw an exception if the bean does not have its PK set.
	 * 
	 * @return true if it was deleted
	 */
	public boolean delete();
	
	/**
	 * Attempt to save a bean in the database. It will try update first, then insert.
	 * 
	 * @return <b>0</b> when UPDATE and <b>1</b> if operation was an INSERT
	 * @see BeanSession#save(Object, Object...)
	 */
	public int save(Object... forceNull);
	
	/**
	 * Return the bean session being used by this MentaBean.
	 * 
	 * @return the bean session
	 */
	public BeanSession getBeanSession();
}