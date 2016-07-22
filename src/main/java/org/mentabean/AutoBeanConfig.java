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

import java.util.Map;

import org.mentabean.util.FindProperties;

/**
 * A bean config that uses reflection to try to guess the database column type from the bean properties.
 * 
 * @author soliveira
 */
public class AutoBeanConfig extends BeanConfig {

	public AutoBeanConfig(Class<? extends Object> klass, String tableName) {
		super(klass, tableName);

		// now use reflection to configure this guy...

		Map<String, Class<? extends Object>> props = FindProperties.all(klass);

		for (Map.Entry<String, Class<? extends Object>> entry : props.entrySet()) {
			
			DBType<?> columnType = DBTypes.from(entry.getValue());
			if (columnType != null) {
				this.field(entry.getKey(), columnType);
			}
		}
	}
}