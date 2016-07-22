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
package org.mentabean.util;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class FindProperties {

	public static Map<String, Class<? extends Object>> all(Class<? extends Object> clazz) {

		Map<String, Class<? extends Object>> all = new HashMap<String, Class<? extends Object>>();

		for (Method method : clazz.getMethods()) {
			String name = method.getName();
			Class<? extends Object> returnType = method.getReturnType();
			if (name.length() > 3 && (name.startsWith("get") || name.startsWith("is")) && !name.equals("getClass") && method.getParameterTypes().length == 0 && returnType != null && !returnType.equals(Void.class)) {

				String propName = name.startsWith("get") ? name.substring(3) : name.substring(2);
				propName = propName.substring(0, 1).toLowerCase() + propName.substring(1);
				
				all.put(propName, returnType);
			}
		}
		return all;
	}
}