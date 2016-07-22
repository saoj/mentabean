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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mentabean.AutoBeanConfig;
import org.mentabean.BeanConfig;
import org.mentabean.BeanException;
import org.mentabean.BeanManager;
import org.mentabean.BeanSession;
import org.mentabean.DBTypes;
import org.mentabean.sql.TableAlias;
import org.mentabean.type.EnumIdType;
import org.mentabean.util.PropertiesProxy;
import org.mentabean.util.SQLBuilder;
import org.mentabean.util.SQLUtils;

@SuppressWarnings("unused")
public class AnsiSQLBeanSessionTest extends AbstractBeanSessionTest {

	private static final SimpleDateFormat BD_FORMATTER = new SimpleDateFormat("yyyy-MM-dd");

	@BeforeClass
	public static void setup() {
		AnsiSQLBeanSession.DEBUG = false; // turn on to see SQL generated
	}

	private static class User {

		public static enum Status {
			BASIC, PREMIUM, GOLD
		}

		private long id;
		private String username;
		private Date birthdate;
		private Status status;
		private boolean deleted;
		private Date insertTime;

		public User() {
		}

		public User(long id) {
			this.id = id;
		}

		public User(String username, String birthdate) {
			this.username = username;
			this.birthdate = fromString(birthdate);
			this.status = Status.BASIC;
		}

		private static Date fromString(String date) {
			try {
				return BD_FORMATTER.parse(date);
			} catch (Exception e) {
				throw new IllegalArgumentException("Cannot parse date: " + date);
			}
		}

		public void setBirthdate(String date) {
			setBirthdate(fromString(date));
		}

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public Date getBirthdate() {
			return birthdate;
		}

		public void setBirthdate(Date birthdate) {
			this.birthdate = birthdate;
		}

		public boolean isDeleted() {
			return deleted;
		}

		public void setDeleted(boolean deleted) {
			this.deleted = deleted;
		}

		public Date getInsertTime() {
			return insertTime;
		}

		public void setInsertTime(Date insertTime) {
			this.insertTime = insertTime;
		}

		public void setStatus(Status status) {
			this.status = status;
		}

		public Status getStatus() {
			return status;
		}
	}

	private void createTables(Connection conn) throws SQLException {

		execUpdate(conn, "create table Users(id integer primary key auto_increment, username varchar(25), bd datetime, status varchar(20), deleted tinyint, insert_time timestamp)");
		execUpdate(conn, "create table Posts(id integer primary key auto_increment, user_id integer, title varchar(200), body text, insert_time timestamp)");
	}

	private BeanConfig getUserBeanConfig() {

		// programmatic configuration for the bean... (no annotation or XML)

		BeanConfig config = new BeanConfig(User.class, "Users");
		
		User userProps = PropertiesProxy.create(User.class);
		
		config.pk(userProps.getId(), DBTypes.AUTOINCREMENT)
			.field(userProps.getUsername(), DBTypes.STRING)
			.field(userProps.getBirthdate(), "bd", DBTypes.DATE) // note that the database column name is different
			.field(userProps.getStatus(), DBTypes.ENUMVALUE.from(User.Status.class))
			.field(userProps.isDeleted(), DBTypes.BOOLEANINT)
			.field(userProps.getInsertTime(), "insert_time", DBTypes.NOW_ON_INSERT_TIMESTAMP);

		return config;
	}

	@Test
	public void testCRUD() throws SQLException { // CRUD = ISUD = Insert, Select, Update, Delete

		BeanManager beanManager = new BeanManager();
		BeanConfig userConfig = getUserBeanConfig();
		beanManager.addBeanConfig(userConfig);

		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rset = null;

		try {

			conn = getConnection();
			BeanSession session = new H2BeanSession(beanManager, conn);

			createTables(conn);

			// INSERT:

			User u = new User("saoj", "1980-03-01");
			u.setStatus(User.Status.GOLD);

			session.insert(u);

			Assert.assertEquals(1, u.getId());
			Assert.assertEquals("saoj", u.getUsername());
			Assert.assertEquals("1980-03-01", BD_FORMATTER.format(u.getBirthdate()));
			Assert.assertEquals(false, u.isDeleted());
			Assert.assertEquals(User.Status.GOLD, u.getStatus());

			// SELECT:

			u = new User(1);

			boolean loaded = session.load(u);

			Assert.assertEquals(true, loaded);

			Assert.assertEquals(1, u.getId());
			Assert.assertEquals("saoj", u.getUsername());
			Assert.assertEquals("1980-03-01", BD_FORMATTER.format(u.getBirthdate()));
			Assert.assertEquals(false, u.isDeleted());
			Assert.assertEquals(User.Status.GOLD, u.getStatus());
			Assert.assertTrue((new Date()).getTime() >= u.getInsertTime().getTime());

			// UPDATE:

			u.setUsername("soliveira");

			int modified = session.update(u);

			Assert.assertEquals(1, modified);

			Assert.assertEquals(1, u.getId());
			Assert.assertEquals("soliveira", u.getUsername());
			Assert.assertEquals("1980-03-01", BD_FORMATTER.format(u.getBirthdate()));
			Assert.assertEquals(false, u.isDeleted());
			Assert.assertEquals(User.Status.GOLD, u.getStatus());
			Assert.assertTrue((new Date()).getTime() >= u.getInsertTime().getTime());

			// make sure the new username was saved in the database

			u = new User(1);

			loaded = session.load(u);

			Assert.assertEquals(true, loaded);

			Assert.assertEquals(1, u.getId());
			Assert.assertEquals("soliveira", u.getUsername());
			Assert.assertEquals("1980-03-01", BD_FORMATTER.format(u.getBirthdate()));
			Assert.assertEquals(false, u.isDeleted());
			Assert.assertEquals(User.Status.GOLD, u.getStatus());
			Assert.assertTrue((new Date()).getTime() >= u.getInsertTime().getTime());

			// DELETE:

			boolean deleted = session.delete(u);

			Assert.assertEquals(true, deleted);

			// make sure the bean is deleted from the database...

			u = new User(1);

			loaded = session.load(u);

			Assert.assertEquals(false, loaded);

		} finally {

			close(stmt, rset);
			close(conn);
		}
	}

	@Test
	public void testDynUpdate() throws SQLException {

		BeanManager beanManager = new BeanManager();
		BeanConfig userConfig = getUserBeanConfig();
		beanManager.addBeanConfig(userConfig);

		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rset = null;

		try {

			conn = getConnection();
			BeanSession session = new H2BeanSession(beanManager, conn);

			createTables(conn);

			// First insert, so we can test the update after...
			User u = new User("saoj", "1980-03-01");
			session.insert(u);

			Assert.assertEquals(1, u.getId());
			Assert.assertEquals("saoj", u.getUsername());
			Assert.assertEquals("1980-03-01", BD_FORMATTER.format(u.getBirthdate()));
			Assert.assertEquals(false, u.isDeleted());

			// UNATTACHED UPDATE:

			u = new User(1); // note that bean was NOT loaded, in other words,
								// it was NOT attached to session
			u.setUsername("soliveira");

			int modified = session.update(u); // only properties that are
												// considered SET will be
												// updated
			Assert.assertEquals(1, modified);

			// make sure it was written to the database
			u = new User(1);
			boolean loaded = session.load(u);
			Assert.assertEquals(true, loaded);

			Assert.assertEquals("soliveira", u.getUsername());

			// ATTACHED UPDATE:

			u = new User(1);
			loaded = session.load(u);
			Assert.assertEquals(true, loaded);

			u.setUsername(u.getUsername()); // setting, but nothing is
											// changing...

			modified = session.update(u); // nothing should happen here...
			Assert.assertEquals(0, modified);

			// UNATTACHED UPDATED ALL:

			u = new User(1); // note that bean was NOT loaded, in other words,
								// it was NOT attached to session
			u.setUsername("julia");

			modified = session.updateAll(u);

			Assert.assertEquals(1, modified);

			// everything was written to the database, even the fields that were
			// NOT set
			// as a result, some fields were nullified

			u = new User(1);
			loaded = session.load(u);
			Assert.assertEquals(true, loaded);
			Assert.assertEquals("julia", u.getUsername());
			Assert.assertNull(u.getBirthdate());
			Assert.assertNull(u.getInsertTime());

			// ATTACHED UPDATE ALL:

			u = new User(1);
			loaded = session.load(u);
			Assert.assertEquals(true, loaded);

			u.setBirthdate("1980-02-02");

			modified = session.updateAll(u);

			Assert.assertEquals(1, modified);

			// everything was written to the database, even the fields that were
			// NOT set

			u = new User(1);
			loaded = session.load(u);
			Assert.assertEquals(true, loaded);
			Assert.assertEquals("julia", u.getUsername());
			Assert.assertEquals("1980-02-02", BD_FORMATTER.format(u.getBirthdate()));
			Assert.assertNull(u.getInsertTime());

		} finally {

			close(stmt, rset);
			close(conn);
		}
	}

	@Test
	public void testMultipleInserts() throws SQLException {

		BeanManager beanManager = new BeanManager();
		BeanConfig userConfig = getUserBeanConfig();
		beanManager.addBeanConfig(userConfig);

		Connection conn = null;

		try {

			conn = getConnection();
			BeanSession session = new H2BeanSession(beanManager, conn);

			createTables(conn);

			for (int i = 0; i < 10; i++) {
				User u = new User();
				u.setUsername("saoj" + (i + 1));
				u.setBirthdate("1990-01-1" + i);

				session.insert(u);

				Assert.assertEquals(i + 1, u.getId());
			}
		} finally {
			close(conn);
		}
	}

	@Test
	public void testLoadList() throws SQLException {

		BeanManager beanManager = new BeanManager();
		BeanConfig userConfig = getUserBeanConfig();
		beanManager.addBeanConfig(userConfig);

		Connection conn = null;

		try {

			conn = getConnection();
			BeanSession session = new H2BeanSession(beanManager, conn);

			createTables(conn);

			for (int i = 0; i < 10; i++) {
				User u = new User();
				u.setUsername("saoj" + (i + 1));
				u.setBirthdate("1990-01-1" + i);
				u.setStatus(User.Status.BASIC);

				session.insert(u);

				Assert.assertEquals(i + 1, u.getId());
			}

			// now set one user to GOLD

			User u = new User(5);
			boolean loaded = session.load(u);
			Assert.assertEquals(true, loaded);
			u.setStatus(User.Status.GOLD);
			int modified = session.update(u);
			Assert.assertEquals(1, modified);

			// first count to see if we are excluding the golad...

			u = new User();
			u.setStatus(User.Status.BASIC);

			int total = session.countList(u);

			Assert.assertEquals(9, total);

			// now load a list of all BASIC users

			u = new User();
			u.setStatus(User.Status.BASIC);

			List<User> users = session.loadList(u);

			Assert.assertEquals(9, users.size());

			// check that the GOLD user was not loaded...

			for (User user : users) {
				Assert.assertTrue(user.getId() != 5);
			}

		} finally {
			close(conn);
		}
	}

	private static class Post {

		private int id;
		private User user;
		private String title;
		private String body;
		private Date insertTime;

		public Post() {
		}

		public Post(int id) {
			this.id = id;
		}

		public Post(User user, String title, String text) {
			this.user = user;
			this.title = title;
			this.body = text;
		}

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public User getUser() {
			return user;
		}

		public void setUser(User user) {
			this.user = user;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getBody() {
			return body;
		}

		public void setBody(String body) {
			this.body = body;
		}

		public Date getInsertTime() {
			return insertTime;
		}

		public void setInsertTime(Date insertTime) {
			this.insertTime = insertTime;
		}
	}

	private void getPostBeanConfig(BeanManager beanManager) {

		// Fluent API

		beanManager
		.bean(Post.class, "Posts")
		.pk("id", DBTypes.AUTOINCREMENT)
		.field("user.id", "user_id", DBTypes.LONG)
		.field("title", DBTypes.STRING).field("body", DBTypes.STRING)
		.field("insertTime", "insert_time", DBTypes.NOW_ON_INSERT_TIMESTAMP);
	}

	@Test
	public void testOneToOneRelationshipCRUD() throws SQLException {

		BeanManager beanManager = new BeanManager();
		BeanConfig userConfig = getUserBeanConfig();
		getPostBeanConfig(beanManager);
		beanManager.addBeanConfig(userConfig);

		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rset = null;

		try {

			conn = getConnection();
			BeanSession session = new H2BeanSession(beanManager, conn);

			createTables(conn);

			User u = new User("saoj", "1980-01-02");
			session.insert(u);

			Assert.assertEquals(1, u.getId());

			// Now insert a post for this user...
			Post p = new Post(new User(1), "Test", "This is a test!");
			session.insert(p);

			Assert.assertEquals(1, p.getId());

			// Load from the database...
			p = new Post(1);
			boolean loaded = session.load(p);
			Assert.assertEquals("Test", p.getTitle());
			Assert.assertEquals(1, p.getUser().getId());
			Assert.assertNotNull(p.getUser()); // loads user with id only
			Assert.assertEquals(1, p.getUser().getId());
			Assert.assertNull(p.getUser().getUsername());

			// Load user for this post... (let's do our manual lazy loading)
			u = new User(p.getUser().getId());
			loaded = session.load(u);
			Assert.assertEquals(true, loaded);
			p.setUser(u); // manual lazy loading (forget about automatic lazy loading, you want control!)

			// Use JOIN to load all dependencies with a single query... (you know how to make a join, right?)

			p = new Post(1);

			StringBuilder query = new StringBuilder(256);
			query.append("select ");
			query.append(session.buildSelect(Post.class, "p"));
			query.append(", ");
			query.append(session.buildSelect(User.class, "u"));
			query.append(" from Posts p join Users u on p.user_id = u.id");
			query.append(" where p.id = ?");

			stmt = SQLUtils.prepare(conn, query.toString(), p.getId());
			
			rset = stmt.executeQuery();

			if (rset.next()) {

				session.populateBean(rset, p, "p");

				u = new User();

				session.populateBean(rset, u, "u");

				p.setUser(u); // manual lazy loading (we prefer to have control!)
			}

			Assert.assertEquals(1, p.getId());
			Assert.assertEquals("Test", p.getTitle());
			Assert.assertEquals(1, u.getId());
			Assert.assertEquals("saoj", p.getUser().getUsername());
			Assert.assertTrue((new Date()).getTime() >= p.getInsertTime().getTime());

			rset.close();
			stmt.close();

			// Deleting => No cascade deletes, if you want that implement in the database level...

			u = new User(1);
			boolean deleted = session.delete(u);
			Assert.assertEquals(true, deleted);

			// Post of course is still there...
			p = new Post(1);
			loaded = session.load(p);
			Assert.assertEquals(true, loaded);
			Assert.assertEquals(1, p.getUser().getId()); // of course this user is gone!

			u = new User(1);
			loaded = session.load(u);
			Assert.assertEquals(false, loaded); // use was deleted above...

		} finally {
			close(stmt, rset);
			close(conn);
		}
	}

	@Test
	public void testOneToOneRelationshipCRUDWithTableAlias() throws SQLException {

		BeanManager beanManager = new BeanManager();
		BeanConfig userConfig = getUserBeanConfig();
		getPostBeanConfig(beanManager);
		beanManager.addBeanConfig(userConfig);

		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rset = null;

		try {

			conn = getConnection();
			BeanSession session = new H2BeanSession(beanManager, conn);

			createTables(conn);

			User u = new User("saoj", "1980-01-02");
			session.insert(u);

			Assert.assertEquals(1, u.getId());

			// Now insert a post for this user...
			Post p = new Post(new User(1), "Test", "This is a test!");
			session.insert(p);

			Assert.assertEquals(1, p.getId());

			// Load from the database...
			p = new Post(1);
			boolean loaded = session.load(p);
			Assert.assertEquals("Test", p.getTitle());
			Assert.assertEquals(1, p.getUser().getId());
			Assert.assertNotNull(p.getUser());
			Assert.assertEquals(1, p.getUser().getId());
			Assert.assertNull(p.getUser().getUsername());

			// Load user for this post... (let's do our manual lazy loading)
			u = new User(p.getUser().getId());
			loaded = session.load(u);
			Assert.assertEquals(true, loaded);
			p.setUser(u); // manual lazy loading (forget about automatic lazy loading, you want control!)

			// Use JOIN to load all dependencies with a single query... (you know how to make a join, right?)

			p = new Post(1);
			
			TableAlias<Post> postAlias = session.createTableAlias(Post.class, "p");
			TableAlias<User> userAlias = session.createTableAlias(User.class, "u");

			StringBuilder query = new StringBuilder(256);
			query.append("select ");
			query.append(postAlias.columns());
			query.append(", ");
			query.append(userAlias.columns());
			query.append(" from ").append(postAlias.tableName());
			query.append(" join ").append(userAlias.tableName());
			query.append(" on ");
			query.append(postAlias.column(postAlias.pxy().getUser().getId())).append(" = ").append(userAlias.column(userAlias.pxy().getId()));
			query.append(" where ");
			query.append(postAlias.column(postAlias.pxy().getId()));
			query.append(" = ?");
			
			stmt = SQLUtils.prepare(conn, query.toString(), p.getId());
			
			rset = stmt.executeQuery();

			if (rset.next()) {

				session.populateBean(rset, p, "p");

				u = new User();

				session.populateBean(rset, u, "u");

				p.setUser(u); // manual lazy loading (we prefer to have control!)
			}

			Assert.assertEquals(1, p.getId());
			Assert.assertEquals("Test", p.getTitle());
			Assert.assertEquals(1, u.getId());
			Assert.assertEquals("saoj", p.getUser().getUsername());
			Assert.assertTrue((new Date()).getTime() >= p.getInsertTime().getTime());

			rset.close();
			stmt.close();

			// Deleting => No cascade deletes, if you want that implement in the database level...

			u = new User(1);
			boolean deleted = session.delete(u);
			Assert.assertEquals(true, deleted);

			// Post of course is still there...
			p = new Post(1);
			loaded = session.load(p);
			Assert.assertEquals(true, loaded);
			Assert.assertEquals(1, p.getUser().getId()); // of course this user is gone!

			u = new User(1);
			loaded = session.load(u);
			Assert.assertEquals(false, loaded); // use was deleted above...

		} finally {
			close(stmt, rset);
			close(conn);
		}
	}

	@Test
	public void testSQLBuilder() throws SQLException {

		BeanManager beanManager = new BeanManager();
		BeanConfig userConfig = getUserBeanConfig();
		getPostBeanConfig(beanManager);
		beanManager.addBeanConfig(userConfig);

		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rset = null;

		try {

			conn = getConnection();
			BeanSession session = new H2BeanSession(beanManager, conn);

			createTables(conn);

			User u = new User("saoj", "1980-01-02");
			session.insert(u);

			Assert.assertEquals(1, u.getId());

			// Now insert a post for this user...
			Post p = new Post(new User(1), "Test", "This is a test!");
			session.insert(p);

			Assert.assertEquals(1, p.getId());

			// Load from the database...
			p = new Post(1);
			boolean loaded = session.load(p);
			Assert.assertEquals("Test", p.getTitle());
			Assert.assertEquals(1, p.getUser().getId());
			Assert.assertNotNull(p.getUser());
			Assert.assertEquals(1, p.getUser().getId());
			Assert.assertNull(p.getUser().getUsername());

			// Load user for this post... (let's do our manual lazy loading)
			u = new User(p.getUser().getId());
			loaded = session.load(u);
			Assert.assertEquals(true, loaded);
			p.setUser(u); // manual lazy loading (forget about automatic lazy loading, you want control!)

			// Use JOIN to load all dependencies with a single query... (you know how to make a join, right?)

			p = new Post(1);
			
			TableAlias<Post> postAlias = session.createTableAlias(Post.class, "p");
			TableAlias<User> userAlias = session.createTableAlias(User.class, "u");
			Post post = postAlias.pxy();
			User user = userAlias.pxy();

			SQLBuilder query = new SQLBuilder(256, postAlias, userAlias);
			query.append("select ");
			query.append(postAlias.columns());
			query.append(", ");
			query.append(userAlias.columns());
			query.append(" from ").append(postAlias.tableName());
			query.append(" join ").append(userAlias.tableName());
			query.append(" on ");
			// query.append(postAlias.column(postAlias.pxy().getUser().getId())).append(" = ").append(userAlias.column(userAlias.pxy().getId()));
			query.column(post.getUser().getId()).append(" = ").column(user.getId());
			query.append(" where ");
			// query.append(postAlias.column(postAlias.pxy().getId()));
			query.column(post.getId());
			query.append(" = ?");
			
			stmt = SQLUtils.prepare(conn, query.toString(), p.getId());
			
			rset = stmt.executeQuery();

			if (rset.next()) {

				session.populateBean(rset, p, "p");

				u = new User();

				session.populateBean(rset, u, "u");

				p.setUser(u); // manual lazy loading (we prefer to have control!)
			}

			Assert.assertEquals(1, p.getId());
			Assert.assertEquals("Test", p.getTitle());
			Assert.assertEquals(1, u.getId());
			Assert.assertEquals("saoj", p.getUser().getUsername());
			Assert.assertTrue((new Date()).getTime() >= p.getInsertTime().getTime());

			rset.close();
			stmt.close();

			// Deleting => No cascade deletes, if you want that implement in the database level...

			u = new User(1);
			boolean deleted = session.delete(u);
			Assert.assertEquals(true, deleted);

			// Post of course is still there...
			p = new Post(1);
			loaded = session.load(p);
			Assert.assertEquals(true, loaded);
			Assert.assertEquals(1, p.getUser().getId()); // of course this user is gone!

			u = new User(1);
			loaded = session.load(u);
			Assert.assertEquals(false, loaded); // use was deleted above...

		} finally {
			close(stmt, rset);
			close(conn);
		}
	}
	
	@Test
	public void testSQLBuilder2() throws SQLException {

		BeanManager beanManager = new BeanManager();
		BeanConfig userConfig = getUserBeanConfig();
		getPostBeanConfig(beanManager);
		beanManager.addBeanConfig(userConfig);

		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rset = null;

		try {

			conn = getConnection();
			BeanSession session = new H2BeanSession(beanManager, conn);

			createTables(conn);

			User u = new User("saoj", "1980-01-02");
			session.insert(u);

			Assert.assertEquals(1, u.getId());

			// Now insert a post for this user...
			Post p = new Post(new User(1), "Test", "This is a test!");
			session.insert(p);

			Assert.assertEquals(1, p.getId());

			// Load from the database...
			p = new Post(1);
			boolean loaded = session.load(p);
			Assert.assertEquals("Test", p.getTitle());
			Assert.assertEquals(1, p.getUser().getId());
			Assert.assertNotNull(p.getUser());
			Assert.assertEquals(1, p.getUser().getId());
			Assert.assertNull(p.getUser().getUsername());

			// Load user for this post... (let's do our manual lazy loading)
			u = new User(p.getUser().getId());
			loaded = session.load(u);
			Assert.assertEquals(true, loaded);
			p.setUser(u); // manual lazy loading (forget about automatic lazy loading, you want control!)

			// Use JOIN to load all dependencies with a single query... (you know how to make a join, right?)

			p = new Post(1);
			
			TableAlias<Post> postAlias = session.createTableAlias(Post.class, "p");
			TableAlias<User> userAlias = session.createTableAlias(User.class, "u");
			Post post = postAlias.pxy();
			User user = userAlias.pxy();

			SQLBuilder query = new SQLBuilder(256, postAlias, userAlias);
			query.append("select ");
			query.append(postAlias.columnsMinus(post.getTitle()));
			query.append(", ");
			query.append(userAlias.columns());
			query.append(" from ").append(postAlias.tableName());
			query.append(" join ").append(userAlias.tableName());
			query.append(" on ");
			// query.append(postAlias.column(postAlias.pxy().getUser().getId())).append(" = ").append(userAlias.column(userAlias.pxy().getId()));
			query.column(post.getUser().getId()).append(" = ").column(user.getId());
			query.append(" where ");
			// query.append(postAlias.column(postAlias.pxy().getId()));
			query.column(post.getId());
			query.append(" = ?");
			
			stmt = SQLUtils.prepare(conn, query.toString(), p.getId());
			
			rset = stmt.executeQuery();

			if (rset.next()) {

				//XXX aqui daria erro pois a session não vai conseguir popular p_title
				//É por isso que o Alias do QueryBuilder tem o seu próprio populateBean,
				//pois ele já sabe quais propriedades foram informadas..
				//session.populateBean(rset, p, "p");
				
				//XXX "minus" repetitivo...
				session.populateBeanMinus(rset, p, "p", post.getTitle());

				//possível solução.. fazer um populateBean no TableAlias que já pegasse
				//o prefix e também as properties que devem retornar
				//algo como:
				//postAlias.populateBean(rset, p);
				
				u = new User();

				session.populateBean(rset, u, "u");

				p.setUser(u); // manual lazy loading (we prefer to have control!)
			}

			Assert.assertEquals(1, p.getId());
			Assert.assertNull(p.getTitle());
			Assert.assertEquals(1, u.getId());
			Assert.assertEquals("saoj", p.getUser().getUsername());
			Assert.assertTrue((new Date()).getTime() >= p.getInsertTime().getTime());

			rset.close();
			stmt.close();

			// Deleting => No cascade deletes, if you want that implement in the database level...

			u = new User(1);
			boolean deleted = session.delete(u);
			Assert.assertEquals(true, deleted);

			// Post of course is still there...
			p = new Post(1);
			loaded = session.load(p);
			Assert.assertEquals(true, loaded);
			Assert.assertEquals(1, p.getUser().getId()); // of course this user is gone!

			u = new User(1);
			loaded = session.load(u);
			Assert.assertEquals(false, loaded); // use was deleted above...

		} finally {
			close(stmt, rset);
			close(conn);
		}
	}
	
	@Test
	public void testSQLBuilder3() throws SQLException {
		
		BeanManager beanManager = new BeanManager();
		BeanConfig userConfig = getUserBeanConfig();
		getPostBeanConfig(beanManager);
		beanManager.addBeanConfig(userConfig);

		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rset = null;

		try {

			conn = getConnection();
			BeanSession session = new H2BeanSession(beanManager, conn);

			createTables(conn);

			User u = new User("erico", "1991-01-31");
			session.insert(u);

			TableAlias<User> userAlias1 = session.createTableAlias(User.class, "u");
			TableAlias<User> userAlias2 = session.createTableAlias(User.class, "u2");
			User user1 = userAlias1.pxy();
			User user2 = userAlias2.pxy();

			SQLBuilder query = new SQLBuilder(userAlias1, userAlias2);
			query.append("select ");
			query.append(userAlias1.columns());
			query.append(" from ").append(userAlias1.tableName());
			query.append(" where ").column(user1.getId());
			query.append(" in (select ").column(user2.getId());
			query.append(" from ").append(userAlias2.tableName());
			query.append(")");
			
			Assert.assertTrue(query.toString().contains("(select u2.id"));
			stmt = SQLUtils.prepare(conn, query.toString());
			
			rset = stmt.executeQuery();

			if (rset.next()) {

				u = new User();

				session.populateBean(rset, u, "u");
			}

			Assert.assertEquals(1, u.getId());
			Assert.assertEquals("erico", u.getUsername());

		} finally {
			close(stmt);
			close(conn);
		}
	}
	
	@Test
	public void testLoadUnique() throws SQLException {

		BeanManager beanManager = new BeanManager();
		BeanConfig userConfig = getUserBeanConfig();
		beanManager.addBeanConfig(userConfig);

		Connection conn = null;

		try {

			conn = getConnection();
			BeanSession session = new H2BeanSession(beanManager, conn);

			createTables(conn);

			User u1 = new User("saoj", "1983-01-02");
			session.insert(u1);

			User u = new User();
			u.setUsername("saoj");

			u = session.loadUnique(u);

			Assert.assertEquals(1, u.getId());
			Assert.assertEquals("saoj", u.getUsername());

			// now add another one and try again...

			User u2 = new User("saoj", "1967-01-03");
			session.insert(u2);

			u = new User();
			u.setUsername("saoj");

			boolean ok = false;

			try {

				u = session.loadUnique(u);

				ok = true; // cannot come here...

			} catch (BeanException e) {

				ok = false;
			}

			Assert.assertEquals(false, ok);
			
			//loadUnique specifying properties
			u = new User();
			u.setBirthdate("1967-01-03");
			
			User proxy = PropertiesProxy.create(User.class);
			u = session.loadUnique(u, proxy.getUsername()); //only username
			
			Assert.assertNull(u.getBirthdate());
			Assert.assertNull(u.getInsertTime());
			Assert.assertNull(u.getStatus());
			Assert.assertEquals(2, u.getId()); //always load the pk	
			Assert.assertNotNull(u.getUsername());
			
			//loadUnique specifying 'minus' properties
			u = new User();
			u.setBirthdate("1967-01-03");
			u = session.loadUniqueMinus(u, proxy.getUsername(), proxy.getId()); //not load username nor pk (mentabean must ignore this)
			
			Assert.assertNotNull(u.getBirthdate());
			Assert.assertNotNull(u.getInsertTime());
			Assert.assertNotNull(u.getStatus());
			Assert.assertEquals(2, u.getId()); //always load the pk... I mean, always
			Assert.assertNull(u.getUsername());	

		} finally {
			close(conn);
		}
	}

	@Test
	public void testSettingProperties() throws SQLException {

		BeanManager beanManager = new BeanManager();
		BeanConfig userConfig = getUserBeanConfig();
		beanManager.addBeanConfig(userConfig);

		Connection conn = null;

		try {

			conn = getConnection();
			BeanSession session = new H2BeanSession(beanManager, conn);

			createTables(conn);

			User saoj = new User("saoj", "1980-01-01");
			session.insert(saoj);

			User julia = new User("julia", "1980-03-03");
			session.insert(julia);

			// delete "saoj" by making deleted equals to false
			saoj.setDeleted(true);
			int modified = session.update(saoj);
			Assert.assertEquals(1, modified);

			// load all non-deleted users...
			User u = new User();
			u.setStatus(User.Status.BASIC);			
			u.setDeleted(false);
			List<User> nonDeletedUsers = session.loadList(u);
			Assert.assertFalse(nonDeletedUsers.size() == 1); // THIS DOES NOT WORK because isDeleted() returns a boolean primitive
			Assert.assertEquals(2, nonDeletedUsers.size()); // it will return everything because the deleted = false condition was never detected

			// to fix, let's change the property to return a Boolean
			beanManager.addBeanConfig(getUser2BeanConfig());

			// now try again
			User2 u2 = new User2();
			u2.setDeleted(false);
			List<User2> nonDeletedUsers2 = session.loadList(u2);
			Assert.assertEquals(1, nonDeletedUsers2.size()); // now only ONE is returned, the non-deleted one...

		} finally {
			close(conn);
		}
	}

	private static class User2 {

		public static enum Status {
			BASIC, PREMIUM, GOLD
		}

		private int id;
		private String username;
		private Date birthdate;
		private Status status = Status.BASIC;
		private boolean deleted;
		private Date insertTime;

		public User2() {
		}

		public User2(int id) {
			this.id = id;
		}

		public User2(String username, String birthdate) {
			this.username = username;
			this.birthdate = fromString(birthdate);
		}

		private static Date fromString(String date) {
			try {
				return BD_FORMATTER.parse(date);
			} catch (Exception e) {
				throw new IllegalArgumentException("Cannot parse date: " + date);
			}
		}

		public void setBirthdate(String date) {
			setBirthdate(fromString(date));
		}

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public Date getBirthdate() {
			return birthdate;
		}

		public void setBirthdate(Date birthdate) {
			this.birthdate = birthdate;
		}

		public Boolean isDeleted() {
			return deleted;
		}

		public void setDeleted(boolean deleted) {
			this.deleted = deleted;
		}

		public Date getInsertTime() {
			return insertTime;
		}

		public void setInsertTime(Date insertTime) {
			this.insertTime = insertTime;
		}

		public void setStatus(Status status) {
			this.status = status;
		}

		public Status getStatus() {
			return status;
		}
	}

	private BeanConfig getUser2BeanConfig() {

		// programmatic configuration for the bean... (no annotation or XML)

		BeanConfig config = new BeanConfig(User2.class, "Users");
		config.pk("id", DBTypes.AUTOINCREMENT);
		config.field("username", DBTypes.STRING);
		config.field("birthdate", "bd", DBTypes.DATE); // note that the database column name is different
		config.field("status", DBTypes.ENUMVALUE.from(User2.Status.class));
		config.field("deleted", DBTypes.BOOLEANINT);
		config.field("insertTime", "insert_time", DBTypes.NOW_ON_INSERT_TIMESTAMP);

		return config;
	}

	@Test
	public void testEnumIdType() throws SQLException {

		BeanManager beanManager = new BeanManager();
		BeanConfig userConfig = getUser3BeanConfig();
		beanManager.addBeanConfig(userConfig);

		Connection conn = null;

		try {

			conn = getConnection();
			BeanSession session = new H2BeanSession(beanManager, conn);

			execUpdate(conn, "create table Users(id integer primary key auto_increment, username varchar(25), bd datetime, status integer, deleted tinyint, insert_time timestamp)");

			User3 u = new User3("saoj", "1980-03-03");
			u.setStatus(User3.Status.GOLD);
			session.insert(u);

			// now load and see if we get the same status...
			u = new User3(1);
			boolean loaded = session.load(u);
			Assert.assertEquals(true, loaded);
			Assert.assertEquals(User3.Status.GOLD, u.getStatus());

		} finally {
			close(conn);
		}
	}

	private static class User3 {

		public static enum Status {
			BASIC(1), PREMIUM(2), GOLD(3);

			private final int id;

			private Status(int id) {
				this.id = id;
			}

			public int getId() {
				return id;
			}

			public static Status fromId(int id) {
				for (Status s : Status.values()) {
					if (s.getId() == id) {
						return s;
					}
				}
				return null;
			}
		}

		private int id;
		private String username;
		private Date birthdate;
		private Status status = Status.BASIC;
		private boolean deleted;
		private Date insertTime;

		public User3() {
		}

		public User3(int id) {
			this.id = id;
		}

		public User3(String username, String birthdate) {
			this.username = username;
			this.birthdate = fromString(birthdate);
		}

		private static Date fromString(String date) {
			try {
				return BD_FORMATTER.parse(date);
			} catch (Exception e) {
				throw new IllegalArgumentException("Cannot parse date: " + date);
			}
		}

		public void setBirthdate(String date) {
			setBirthdate(fromString(date));
		}

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public Date getBirthdate() {
			return birthdate;
		}

		public void setBirthdate(Date birthdate) {
			this.birthdate = birthdate;
		}

		public Boolean isDeleted() {
			return deleted;
		}

		public void setDeleted(boolean deleted) {
			this.deleted = deleted;
		}

		public Date getInsertTime() {
			return insertTime;
		}

		public void setInsertTime(Date insertTime) {
			this.insertTime = insertTime;
		}

		public void setStatus(Status status) {
			this.status = status;
		}

		public Status getStatus() {
			return status;
		}
	}

	private BeanConfig getUser3BeanConfig() {

		// programmatic configuration for the bean... (no annotation or XML)

		BeanConfig config = new BeanConfig(User3.class, "Users");
		config.pk("id", DBTypes.AUTOINCREMENT);
		config.field("username", DBTypes.STRING);
		config.field("birthdate", "bd", DBTypes.DATE); // note that the database column name is different
		config.field("status", new EnumIdType(User3.Status.class));
		config.field("deleted", DBTypes.BOOLEANINT);
		config.field("insertTime", "insert_time", DBTypes.NOW_ON_INSERT_TIMESTAMP);

		return config;
	}

	@Test
	public void testCreateTable() throws SQLException {

		BeanManager beanManager = new BeanManager();
		BeanConfig userConfig = getUser4BeanConfig();
		beanManager.addBeanConfig(userConfig);

		Connection conn = null;

		try {

			conn = getConnection();
			BeanSession session = new H2BeanSession(beanManager, conn);

			Assert.assertFalse(SQLUtils.checkIfTableExists(conn, "Users"));

			session.createTables();

			Assert.assertTrue(SQLUtils.checkIfTableExists(conn, "Users"));

		} finally {
			close(conn);
		}
	}

	@Test
	public void testAutoBeanConfig() throws SQLException {

		BeanManager beanManager = new BeanManager();
		BeanConfig userConfig = new AutoBeanConfig(User4.class, "Users");
		beanManager.addBeanConfig(userConfig);

		userConfig.pk("id", DBTypes.AUTOINCREMENT); // override since PK and autoincrement cannot be discovered by reflection...

		Connection conn = null;

		try {

			conn = getConnection();
			BeanSession session = new H2BeanSession(beanManager, conn);

			// execUpdate(conn, "create table Users(id integer primary key auto_increment, username varchar(25), birthdate datetime, status integer, deleted tinyint, insert_time timestamp, update_time timestamp)");
			session.createTables();

			User4 u = new User4("saoj", "1980-03-03");
			u.setStatus(User4.Status.GOLD);
			session.insert(u);

			// now load and see if we get the same status...
			u = new User4(1);
			boolean loaded = session.load(u);
			Assert.assertEquals(true, loaded);
			Assert.assertEquals(User4.Status.GOLD, u.getStatus());

			// update_time must be null
			Assert.assertNull(u.getUpdateTime());

		} finally {
			close(conn);
		}
	}

	@Test
	public void testNowOnUpdate() throws SQLException {

		BeanManager beanManager = new BeanManager();
		BeanConfig userConfig = getUser4BeanConfig();
		beanManager.addBeanConfig(userConfig);

		Connection conn = null;

		try {

			conn = getConnection();
			BeanSession session = new H2BeanSession(beanManager, conn);

			execUpdate(conn, "create table Users(id integer primary key auto_increment, username varchar(25), bd datetime, status integer, deleted tinyint, insert_time timestamp, update_time timestamp)");

			User4 u = new User4("saoj", "1980-03-03");
			u.setStatus(User4.Status.GOLD);
			session.insert(u);

			// now load and see if we get the same status...
			u = new User4(1);
			boolean loaded = session.load(u);
			Assert.assertEquals(true, loaded);
			Assert.assertEquals(User4.Status.GOLD, u.getStatus());

			// update_time must be null
			Assert.assertNull(u.getUpdateTime());

			// now update..
			u.setUsername("saoj1");
			session.update(u);

			// update_time is still null, you need to load !!!
			Assert.assertNull(u.getUpdateTime());

			session.load(u);
			Assert.assertNotNull(u.getUpdateTime());

		} finally {
			close(conn);
		}
	}

	private static class User4 {

		public static enum Status {
			BASIC(1), PREMIUM(2), GOLD(3);

			private final int id;

			private Status(int id) {
				this.id = id;
			}

			public int getId() {
				return id;
			}

			public static Status fromId(int id) {
				for (Status s : Status.values()) {
					if (s.getId() == id) {
						return s;
					}
				}
				return null;
			}
		}

		private int id;
		private String username;
		private Date birthdate;
		private Status status = Status.BASIC;
		private boolean deleted;
		private Date insertTime;
		private Date updateTime;

		public Date getUpdateTime() {
			return updateTime;
		}

		public void setUpdateTime(Date updateTime) {
			this.updateTime = updateTime;
		}

		public User4() {
		}

		public User4(int id) {
			this.id = id;
		}

		public User4(String username, String birthdate) {
			this.username = username;
			this.birthdate = fromString(birthdate);
		}

		private static Date fromString(String date) {
			try {
				return BD_FORMATTER.parse(date);
			} catch (Exception e) {
				throw new IllegalArgumentException("Cannot parse date: " + date);
			}
		}

		public void setBirthdate(String date) {
			setBirthdate(fromString(date));
		}

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public Date getBirthdate() {
			return birthdate;
		}

		public void setBirthdate(Date birthdate) {
			this.birthdate = birthdate;
		}

		public Boolean isDeleted() {
			return deleted;
		}

		public void setDeleted(boolean deleted) {
			this.deleted = deleted;
		}

		public Date getInsertTime() {
			return insertTime;
		}

		public void setInsertTime(Date insertTime) {
			this.insertTime = insertTime;
		}

		public void setStatus(Status status) {
			this.status = status;
		}

		public Status getStatus() {
			return status;
		}
	}

	private BeanConfig getUser4BeanConfig() {

		// programmatic configuration for the bean... (no annotation or XML)

		BeanConfig config = new BeanConfig(User4.class, "Users");
		config.pk("id", DBTypes.AUTOINCREMENT);
		config.field("username", DBTypes.STRING.size(50).nullable(false));
		config.field("birthdate", "bd", DBTypes.DATE); // note that the database column name is different
		config.field("status", new EnumIdType(User4.Status.class));
		config.field("deleted", DBTypes.BOOLEANINT);
		config.field("insertTime", "insert_time", DBTypes.NOW_ON_INSERT_TIMESTAMP);
		config.field("updateTime", "update_time", DBTypes.NOW_ON_UPDATE_TIMESTAMP);

		return config;
	}
	
	
	/*
	 * Testing sublevels
	 */
	
	public static class Foo {
		
		private long id;
		private Bar bar;
		private String test;
		private Bean bean;
		
		public Foo() {}
		
		public Foo(long id, String test, Bean bean) {
			super();
			this.id = id;
			this.test = test;
			this.bean = bean;
		}

		public Foo(long id) {
			this.id = id;
		}
		
		public long getId() {
			return id;
		}
		public void setId(long id) {
			this.id = id;
		}
		public Bar getBar() {
			return bar;
		}
		public void setBar(Bar bar) {
			this.bar = bar;
		}
		public String getTest() {
			return test;
		}
		public void setTest(String test) {
			this.test = test;
		}
		public Bean getBean() {
			return bean;
		}
		public void setBean(Bean bean) {
			this.bean = bean;
		}
	}
	
	public static class Bean {
		
		private long id;

		public Bean(long id) {
			this.id = id;
		}

		public Bean() {}

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}
		
		@Override
		public String toString() {
			return "Bean: "+id;
		}
	}
	
	public static class Bar {
		
		private String name;
		private Item item;
		
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public Item getItem() {
			return item;
		}
		public void setItem(Item item) {
			this.item = item;
		}
	}
	
	public static class Item {
		
		private SubItem subItem;

		public SubItem getSubItem() {
			return subItem;
		}

		public void setSubItem(SubItem subItem) {
			this.subItem = subItem;
		}
	}
	
	public static class SubItem {
		
		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
	
	private BeanConfig getSublevelsBeanConfig() {
		
		Foo foo = PropertiesProxy.create(Foo.class);
		BeanConfig config = new BeanConfig(Foo.class, "foo")
		.pk(foo.getId(), "idfoo", DBTypes.LONG)
		.field(foo.getTest(), DBTypes.STRING)
		.field(foo.getBean().getId(), "idbean", DBTypes.LONG)
		.field(foo.getBar().getItem().getSubItem().getName(), "namebar", DBTypes.STRING);
		
		return config;
	}
	
	private Foo createEmptyFoo() {
		
		Foo foo = new Foo();
		foo.setBar(new Bar());
		foo.getBar().setItem(new Item());
		foo.getBar().getItem().setSubItem(new SubItem());
		return foo;
	}
	
	@Test
	public void testSublevels() {
		
		Connection conn = null;
		BeanManager beanManager = new BeanManager();
		beanManager.addBeanConfig(getSublevelsBeanConfig());
		
		try {

			conn = getConnection();
			BeanSession session = new H2BeanSession(beanManager, conn);
			
			session.createTables();

			Foo foo = createEmptyFoo();
			foo.getBar().getItem().getSubItem().setName("Test one");
			foo.setId(1);
			foo.setTest("Test one");
			session.insert(foo);
			
			foo.getBar().getItem().getSubItem().setName("Test two");
			foo.setId(2);
			foo.setTest("Test two");
			session.insert(foo);
			
			foo.setId(0);
			foo.setTest(null);
			int count = session.countList(foo);
			Assert.assertEquals(1, count);
			
			//here, if we don't force the creation of instances, a NPE will thrown
			//getDeepestBean(bean, chain, true); --> findMethodToGet
			count = session.countList(new Foo());
			Assert.assertEquals(2, count);
			
			foo = createEmptyFoo();
			foo.setId(1);
			foo.setTest("Test three");
			session.update(foo);
			
			Foo fooBD = new Foo();
			fooBD.setId(1);
			session.load(fooBD);
			Assert.assertEquals("Test one", fooBD.getBar().getItem().getSubItem().getName());
			Assert.assertEquals("Test three", fooBD.getTest());
			
			foo = new Foo();
			foo.setId(1);
			foo.setTest("Test four");
			session.update(foo);
			
			fooBD = new Foo();
			fooBD.setId(1);
			session.load(fooBD);
			Assert.assertEquals("Test one", fooBD.getBar().getItem().getSubItem().getName());
			Assert.assertEquals("Test four", fooBD.getTest());
			
			Foo proto = new Foo();
			List<Foo> listBD = session.loadList(proto);
			Assert.assertEquals(2, listBD.size());
			Assert.assertNull(listBD.get(0).getBean());
			Assert.assertNull(listBD.get(1).getBean());
			
			Foo proxy = PropertiesProxy.create(Foo.class);
			foo = new Foo(1);
			foo.setTest("Test five");
			foo.setBean(new Bean());
			foo.getBean().setId(3);
			session.update(foo, proxy.getBean().getId());
			
			session.load(fooBD = new Foo(1));
			Assert.assertEquals("Test five", fooBD.getTest());
			Assert.assertEquals(3, fooBD.getBean().getId());
			
			foo = new Foo(1);
			//forcing bean to null 
			session.update(foo, proxy.getBean().getId());
			session.load(fooBD = new Foo(1));
			Assert.assertNull(fooBD.getBean());
			Assert.assertNotNull(fooBD.getBar().getItem().getSubItem().getName());
			
			foo = new Foo(1);
			session.update(foo, proxy.getBean().getId(), proxy.getBar().getItem().getSubItem().getName());
			session.load(fooBD = new Foo(1));
			Assert.assertNull(fooBD.getBean());
			Assert.assertNull(fooBD.getBar());
			
			foo = createEmptyFoo();
			foo.setId(1);
			foo.getBar().getItem().getSubItem().setName("Not null");
			session.update(foo, proxy.getBar().getItem().getSubItem().getName());
			session.load(fooBD = new Foo(1));
			Assert.assertNull(fooBD.getBean());
			Assert.assertEquals("Not null", fooBD.getBar().getItem().getSubItem().getName());
			
			foo = createEmptyFoo();
			foo.setId(1);
			session.update(foo, proxy.getBar().getItem().getSubItem().getName());
			session.load(fooBD = new Foo(1));
			Assert.assertNull(fooBD.getBar());
			
			/*
			 * testing using attached update
			 */
			foo = createEmptyFoo();
			foo.setId(5);
			foo.setTest("Attached test");
			foo.setBean(new Bean());
			foo.getBean().setId(5);
			foo.getBar().getItem().getSubItem().setName("Anything");
			session.insert(foo);
			
			fooBD = new Foo(foo.getId());
			session.load(fooBD);
			fooBD.setTest("Attached test - updated");
			session.update(fooBD);
			session.load(fooBD);
			Assert.assertEquals("Attached test - updated", fooBD.getTest());
			Assert.assertNotNull(fooBD.getBean());
			Assert.assertNotNull(fooBD.getBar());
			
		} catch (Exception e) {
			throw new BeanException(e);
		} finally {
			close(conn);
		}
	}
	
	private BeanManager getNullFKsManager() {
		
		BeanManager manager = new BeanManager();

		Foo fooPxy = PropertiesProxy.create(Foo.class);
		BeanConfig fooConfig = new BeanConfig(Foo.class, "foo")
		.pk(fooPxy.getId(), "idfoo", DBTypes.LONG)
		.field(fooPxy.getBean().getId(), "idbean", DBTypes.LONG)
		.field(fooPxy.getTest(), DBTypes.STRING);
		manager.addBeanConfig(fooConfig);

		Bean barPxy = PropertiesProxy.create(Bean.class);
		BeanConfig beanConfig = new BeanConfig(Bean.class, "bean")
		.pk(barPxy.getId(), DBTypes.LONG);
		manager.addBeanConfig(beanConfig);

		return manager;
	}
	
	@Test
	public void testNullFKs() {
		
		Connection conn = null;
		BeanManager beanManager = getNullFKsManager();
		
		try {

			conn = getConnection();
			BeanSession session = new H2BeanSession(beanManager, conn);
			
			session.createTables();
			SQLUtils.prepare(conn, "alter table foo add foreign key(idbean) references bean");
			
			Bean bean = new Bean(1);
			session.insert(bean);
			
			Foo foo = new Foo(1, "Foo UM", bean);
			session.insert(foo);
			
			foo = new Foo(2, "Foo DOIS", null);
			session.insert(foo);
			
			Foo fooBD = new Foo(2);
			session.load(fooBD);
			Assert.assertNull(fooBD.getBean());
			fooBD.setTest("Foo DOIS - updated");
			session.update(fooBD);
			session.load(fooBD = new Foo(2));
			Assert.assertEquals("Foo DOIS - updated", fooBD.getTest());
			Assert.assertNull(fooBD.getBean());
			
			fooBD = new Foo(1);
			session.load(fooBD);
			Assert.assertNotNull(fooBD.getBean());
			Assert.assertEquals(1, fooBD.getBean().getId());
			
			fooBD.setTest(null);
			session.update(fooBD);
			session.load(fooBD = new Foo(1));
			Assert.assertNull(fooBD.getTest());
			
			fooBD.setBean(null);
			fooBD.setTest("Foo UM");
			session.update(fooBD);
			session.load(fooBD = new Foo(1));
			Assert.assertEquals("Foo UM", fooBD.getTest());
			Assert.assertNull(fooBD.getBean());
			
			fooBD.setTest("Foo UM - alterado");
			session.update(fooBD);
			session.load(fooBD = new Foo(1));
			Assert.assertEquals("Foo UM - alterado", fooBD.getTest());
			Assert.assertNull(fooBD.getBean());
			
		} catch (Exception e) {
			throw new BeanException(e);
		} finally {
			close(conn);
		}
	}

}