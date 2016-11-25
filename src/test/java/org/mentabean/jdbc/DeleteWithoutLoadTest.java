package org.mentabean.jdbc;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mentabean.BeanConfig;
import org.mentabean.BeanManager;
import org.mentabean.BeanSession;
import org.mentabean.DBTypes;
import org.mentabean.util.PropertiesProxy;
import org.mentabean.util.SQLUtils;

import static org.junit.Assert.*;

public class DeleteWithoutLoadTest extends AbstractBeanSessionTest {
	
	public static class User {
		
		private int id;
		private String name;
		
		public User() { }
		
		public User(int id) { this.id = id; }
		
		public void setId(int id) { this.id = id; }
		public int getId() { return id; }
		
		public void setName(String name) { this.name = name; }
		public String getName() { return name; }
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
			.field(user.getName(), DBTypes.STRING);
		
		beanManager.addBeanConfig(userConfig);
		
		return beanManager;
	}
	
	@Test
	public void testRegularDelete() {
		
		User user = new User();
		user.setName("Sergio");
		session.insert(user);
		
		assertTrue(user.getId() > 0);
		
		User u = new User(user.getId());
		assertTrue(session.load(u));
		
		assertTrue(session.delete(u));
		
		u = new User(user.getId());
		assertFalse(session.load(u)); // was deleted!
	}
	
	@Test
	public void testDeleteWithoutLoad() {
		
		User user = new User();
		user.setName("Sergio");
		session.insert(user);
		
		assertTrue(user.getId() > 0);
		
		User u = new User(user.getId());
		
		assertTrue(session.delete(u)); // delete without load
		
		u = new User(user.getId());
		assertFalse(session.load(u)); // was deleted!
	}
}