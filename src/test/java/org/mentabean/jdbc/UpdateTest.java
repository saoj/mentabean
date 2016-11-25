package org.mentabean.jdbc;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mentabean.BeanConfig;
import org.mentabean.BeanManager;
import org.mentabean.BeanSession;
import org.mentabean.DBTypes;
import org.mentabean.util.PropertiesProxy;
import org.mentabean.util.SQLUtils;

public class UpdateTest extends AbstractBeanSessionTest {
	
	public static class User {
		
		private int id;
		private String name;
		private int age;
		
		public User() { }
		
		public User(int id) { this.id = id; }
		
		public void setId(int id) { this.id = id; }
		public int getId() { return id; }
		
		public void setName(String name) { this.name = name; }
		public String getName() { return name; }
		
		public void setAge(int age) { this.age = age; }
		public int getAge() { return age; }
	}
	
	private BeanSession session;
	
	@Before
	public void setUp() {
		
		AnsiSQLBeanSession.debugSql(false);
		AnsiSQLBeanSession.debugNativeSql(false);
		
		session = new H2BeanSession(configure(), getConnection());
		session.createTables();
	}
	
	@After
	public void tearDown() {
		SQLUtils.close(session.getConnection());
	}
	
	private BeanManager configure() {
		
		BeanManager beanManager = new BeanManager();
		
		User user = PropertiesProxy.create(User.class);
		
		BeanConfig userConfig = new BeanConfig(User.class, "users")
			.pk(user.getId(), DBTypes.AUTOINCREMENT)
			.field(user.getName(), DBTypes.STRING)
			.field(user.getAge(), DBTypes.INTEGER);
		
		beanManager.addBeanConfig(userConfig);
		
		return beanManager;
	}
	
	@Test
	public void testUpdateWithoutLoad() {
		
		User user = new User();
		user.setName("Sergio");
		user.setAge(33);
		session.insert(user);
		
		assertTrue(user.getId() > 0);
		
		User u = new User(user.getId()); // update by PK
		u.setName("Julia"); // change name...
		
		assertTrue(session.update(u) == 1); // update without load...
		
		u = new User(user.getId());
		
		assertTrue(session.load(u));
		
		assertEquals("Julia", u.getName());
		assertEquals(33, u.getAge());
	}
	
	@Test
	public void testUpdateWithoutLoad2() {
		
		User user = new User();
		user.setName("Sergio");
		user.setAge(33);
		session.insert(user);
		
		assertTrue(user.getId() > 0);
		
		User u = new User(user.getId()); // update by PK
		u.setName("Julia"); // change name...
		u.setAge(32);
		
		assertTrue(session.update(u) == 1); // update without load...
		
		u = new User(user.getId());
		
		assertTrue(session.load(u));
		
		assertEquals("Julia", u.getName());
		assertEquals(32, u.getAge());
	}
	
	@Test
	public void testUpdateWithoutLoadWithForceNull() {
		
		User user = new User();
		user.setName("Sergio");
		user.setAge(34);
		session.insert(user);
		
		assertTrue(user.getId() > 0);
		
		User u = new User(user.getId()); // update by PK
		u.setName("Julia"); // change name...
		
		User userProxy = PropertiesProxy.create(User.class);
		
		assertTrue(session.update(u, userProxy.getAge()) == 1); // update without load...
		
		u = new User(user.getId());
		
		assertTrue(session.load(u));
		
		assertEquals("Julia", u.getName());
		assertEquals(0, u.getAge()); // null => 0 for integers
	}
	
	@Test
	public void testUpdateWithoutLoadWithForceNull2() {
		
		User user = new User();
		user.setName("Sergio");
		user.setAge(34);
		session.insert(user);
		
		assertTrue(user.getId() > 0);
		
		User u = new User(user.getId()); // update by PK
		u.setAge(22); // change age...
		
		User userProxy = PropertiesProxy.create(User.class);
		
		assertTrue(session.update(u, userProxy.getName()) == 1); // update without load...
		
		u = new User(user.getId());
		
		assertTrue(session.load(u));
		
		assertEquals(null, u.getName()); // was forced to null
		assertEquals(22, u.getAge());
	}
	
	@Test
	public void testUpdateAllWithoutLoad() {
		
		User user = new User();
		user.setName("Sergio");
		user.setAge(34);
		session.insert(user);
		
		assertTrue(user.getId() > 0);
		
		User u = new User(user.getId()); // update by PK
		u.setAge(22); // change age...
		
		assertTrue(session.updateAll(u) == 1); // updateAll without load...
		
		u = new User(user.getId());
		
		assertTrue(session.load(u));
		
		assertEquals(null, u.getName()); // was forced to null
		assertEquals(22, u.getAge());
	}
}	
