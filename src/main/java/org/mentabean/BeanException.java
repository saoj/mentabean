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
 * A runtime exception that can happen when working with MentaBean.
 * 
 * @author Sergio Oliveira Jr.
 */
public class BeanException extends RuntimeException {

	private static final long serialVersionUID = -6402197033079197979L;
	
	protected final Throwable rootCause;

	public BeanException() {
		super();
		this.rootCause = null;
	}

	public BeanException(Throwable e) {
		super(getMsg(e), e);
		Throwable root = getRootCause(e);
		this.setStackTrace(root.getStackTrace());

		this.rootCause = root == this ? null : root;
	}

	public BeanException(String msg) {
		super(msg);

		this.rootCause = null;
	}

	public BeanException(String msg, Throwable e) {

		super(msg, e);
		Throwable root = getRootCause(e);
		this.setStackTrace(root.getStackTrace());

		this.rootCause = root == this ? null : root;
	}

	private static String getMsg(Throwable t) {

		Throwable root = getRootCause(t);

		String msg = root.getMessage();

		if (msg == null || msg.length() == 0) {

			msg = t.getMessage();

			if (msg == null || msg.length() == 0) {
				return root.getClass().getName();
			}
		}

		return msg;
	}

	private static Throwable getRootCause(Throwable t) {

		Throwable root = t.getCause();

		if (root == null) {
			return t;
		}

		while (root.getCause() != null) {

			root = root.getCause();
		}

		return root;

	}

	@Override
	public Throwable getCause() {

		return rootCause;
	}
}
