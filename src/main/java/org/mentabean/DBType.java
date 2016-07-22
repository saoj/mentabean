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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * An interface defining a database type. You can add more database types by implementing this interface.
 * 
 * @author sergio.oliveira.jr@gmail.com
 */
public interface DBType<E> {

	/**
	 * Do what you have to do to get and return this database type from a result set.
	 * 
	 * @param rset
	 *            The result set
	 * @param index
	 *            The index in the result set
	 * @return The value from the result set
	 * @throws SQLException
	 */
	public E getFromResultSet(ResultSet rset, int index) throws SQLException;

	/**
	 * Do what you have to do to bind a value to a prepared statement.
	 * 
	 * @param stmt
	 *            The prepared statement
	 * @param index
	 *            The index in the prepared statement
	 * @param value
	 *            The value to be bound to the prepared statement
	 * @throws SQLException
	 */
	public void bindToStmt(PreparedStatement stmt, int index, E value) throws SQLException;

	/**
	 * Do what you have to do to get and return this database type from a result set.
	 * 
	 * @param rset
	 *            The result set
	 * @param field
	 *            The name of the field in the result set
	 * @return The value from the result set
	 * @throws SQLException
	 */
	public E getFromResultSet(ResultSet rset, String field) throws SQLException;

	/**
	 * Return the java type representing this database type.
	 * 
	 * @return The java type of this database type.
	 */
	public Class<? extends Object> getTypeClass();

	/**
	 * Returns whether this type can be NULL in the database. This is used by the session.createTable() method.
	 * 
	 * @return true if field can be NULL in the database.
	 */
	public boolean canBeNull();

	/**
	 * Return the best ANSI type for this database type. This is used by the session.createTable() method.
	 * 
	 * @return the ANSI type for this database type.
	 */
	public String getAnsiType();

}