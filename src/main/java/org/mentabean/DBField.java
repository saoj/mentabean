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
 * A class representing a database field. It has the name of the bean property, the name of the column in the database, the database type, whether it is a PK or not and whether it default to now.
 * 
 * @author sergio.oliveira.jr@gmail.com
 */
public class DBField {

	private final String name;
	private final DBType<?> type;
	private final String dbName;
	private final boolean isPK;

	public DBField(final String name, final String dbName, final DBType<?> type, final boolean isPK) {
		this.name = name;
		this.dbName = dbName;
		this.type = type;
		this.isPK = isPK;
	}

	@Override
	public String toString() {

		final StringBuilder sb = new StringBuilder(32);

		sb.append("DBField: ").append(name).append(" type=").append(type).append(" dbName=").append(dbName);

		return sb.toString();
	}

	@Override
	public int hashCode() {

		return name.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {

		if (obj instanceof DBField) {

			final DBField f = (DBField) obj;

			if (f.name.equalsIgnoreCase(this.name)) {
				return true;
			}
		}

		return false;
	}

	public String getName() {

		return name;

	}

	@SuppressWarnings("rawtypes")
	public DBType getType() {

		return type;
	}

	public String getDbName() {

		return dbName;
	}

	public boolean isPK() {

		return isPK;
	}

}
