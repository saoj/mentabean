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
package org.mentabean.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class AbstractBeanSessionTest {

	protected static Connection getConnection() {
		try {
			Class.forName("org.h2.Driver");
			return DriverManager.getConnection("jdbc:h2:mem:MentaBean", "sa", "");
		} catch (Exception e) {
			throw new IllegalStateException("Cannot connect to H2 database!", e);
		}
	}

	protected static void execUpdate(Connection conn, String query) throws SQLException {

		Statement stmt = null;

		try {

			stmt = conn.createStatement();

			stmt.executeUpdate(query);

		} finally {

			close(stmt);
		}
	}

	static void close(PreparedStatement stmt) {
		close(stmt, null);
	}

	static void close(PreparedStatement stmt, ResultSet rset) {

		if (rset != null) {
			try {
				rset.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (stmt != null) {
			try {
				stmt.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	static void close(Statement stmt) {
		close(stmt, null);
	}

	static void close(Statement stmt, ResultSet rset) {

		if (rset != null) {
			try {
				rset.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (stmt != null) {
			try {
				stmt.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	static void close(Connection conn) {

		if (conn != null) {
			try {
				conn.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}