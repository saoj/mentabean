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
 * A abstract implementation of the MentaBean interface. Your beans might choose to inherit from this class to be able to persist themselves.
 * 
 * @author sergio.oliveira.jr@gmail.com
 */
public abstract class AbstractMentaBean implements MentaBean {

	private BeanSession session = null;
	
	public AbstractMentaBean() {
		// mandatory...
	}

	/**
	 * You can inject the bean session through the constructor.
	 * 
	 * @param session the bean session to inject
 	 */
	public AbstractMentaBean(BeanSession session) {
		this.session = session;
	}
	
	/**
	 * You can inject the bean session through the setter.
	 * 
	 * @param session the bean session to inject
	 */
	public void setBeanSession(BeanSession session) {
		this.session = session;
	}
	
	@Override
	public BeanSession getBeanSession() {
		return session;
	}

	@Override
	public void insert() {
		session.insert(this);
	}

	@Override
	public boolean load() {
		return session.load(this);
	}

	@Override
	public boolean update() {
		return session.update(this);
	}

	@Override
	public boolean updateAll() {
		return session.updateAll(this);
	}

	@Override
	public boolean delete() {
		return session.delete(this);
	}
	
	@Override
	public int save(Object... forceNull) {
		return session.save(this, forceNull);
	}
}