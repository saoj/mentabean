package org.mentabean.jdbc;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;
import org.mentabean.BeanConfig;
import org.mentabean.BeanManager;
import org.mentabean.BeanSession;
import org.mentabean.DBTypes;
import org.mentabean.util.PropertiesProxy;
import org.mentabean.util.SQLUtils;

public class UpdateDiffTest extends AbstractBeanSessionTest {

	static class User implements Cloneable {

		private int id;
		private String name;
		private Integer age;
		private boolean active;
		private Group group;

		public User() {}

		public User(int id) {
			this.id = id;
		}

		public User(String name, Integer age, boolean active, Group group) {
			super();
			this.name = name;
			this.age = age;
			this.group = group;
		}

		public int getId() {
			return id;
		}
		public void setId(int id) {
			this.id = id;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public Group getGroup() {
			return group;
		}
		public void setGroup(Group group) {
			this.group = group;
		}
		public Integer getAge() {
			return age;
		}
		public void setAge(Integer age) {
			this.age = age;
		}
		public boolean isActive() {
			return active;
		}
		public void setActive(boolean active) {
			this.active = active;
		}

		@Override
		public Object clone() throws CloneNotSupportedException {
			return super.clone();
		}
	}

	static class Group implements Cloneable {

		private int id;
		private String name;

		public Group(String name) {
			this.name = name;
		}

		public Group(int id) {
			this.id = id;;
		}
		
		public Group(int id, String name) {
			this.id = id;;
			this.name = name;
		}

		public Group() {}

		public int getId() {
			return id;
		}
		public void setId(int id) {
			this.id = id;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Group) {
				return id == ((Group) obj).id;
			}
			return false;
		}
		
		@Override
		public int hashCode() {
			return id;
		}

		@Override
		public Object clone() throws CloneNotSupportedException {
			return super.clone();
		}
	}
	
	static class TypeTest {
		
		private long id;
		private int intPrimitive;
		private double doublePrimitive;
		private Double doubleWrapper;
		private Integer intWrapper;
		private String string;
		private Group group;
		
		public long getId() {
			return id;
		}
		public TypeTest setId(long id) {
			this.id = id;
			return this;
		}
		public int getIntPrimitive() {
			return intPrimitive;
		}
		public TypeTest setIntPrimitive(int intPrimitive) {
			this.intPrimitive = intPrimitive;
			return this;
		}
		public double getDoublePrimitive() {
			return doublePrimitive;
		}
		public TypeTest setDoublePrimitive(double doublePrimitive) {
			this.doublePrimitive = doublePrimitive;
			return this;
		}
		public Double getDoubleWrapper() {
			return doubleWrapper;
		}
		public TypeTest setDoubleWrapper(Double doubleWrapper) {
			this.doubleWrapper = doubleWrapper;
			return this;
		}
		public Integer getIntWrapper() {
			return intWrapper;
		}
		public TypeTest setIntWrapper(Integer intWrapper) {
			this.intWrapper = intWrapper;
			return this;
		}
		public String getString() {
			return string;
		}
		public TypeTest setString(String string) {
			this.string = string;
			return this;
		}
		public Group getGroup() {
			return group;
		}
		public TypeTest setGroup(Group group) {
			this.group = group;
			return this;
		}
	}

	private BeanManager configureManager1() {

		BeanManager manager = new BeanManager();

		User userProxy = PropertiesProxy.create(User.class);
		BeanConfig userCfg = new BeanConfig(User.class, "users")
		.pk(userProxy.getId(), DBTypes.AUTOINCREMENT)
		.field(userProxy.getGroup().getId(), "idgroups", DBTypes.INTEGER)
		.field(userProxy.getName(), DBTypes.STRING.size(0))
		.field(userProxy.getAge(), DBTypes.INTEGER)
		.field(userProxy.isActive(), DBTypes.BOOLEAN);
		manager.addBeanConfig(userCfg);

		Group groupProxy = PropertiesProxy.create(Group.class);
		BeanConfig groupCfg = new BeanConfig(Group.class, "groups")
		.pk(groupProxy.getId(), DBTypes.AUTOINCREMENT)
		.field(groupProxy.getName(), DBTypes.STRING.size(0));
		manager.addBeanConfig(groupCfg);

		return manager;
	}
	
	private BeanManager configureManager2() {
		
		BeanManager manager = new BeanManager();
		
		TypeTest t = PropertiesProxy.create(TypeTest.class);
		BeanConfig conf = new BeanConfig(TypeTest.class, "test")
		.pk(t.getId(), DBTypes.AUTOINCREMENT)
		.field(t.getIntPrimitive(), DBTypes.INTEGER)
		.field(t.getIntWrapper(), DBTypes.INTEGER)
		.field(t.getDoublePrimitive(), DBTypes.DOUBLE)
		.field(t.getDoubleWrapper(), DBTypes.DOUBLE)
		.field(t.getGroup().getId(), "idgroups", DBTypes.INTEGER)
		.field(t.getString(), DBTypes.STRING);
		manager.addBeanConfig(conf);
		
		return manager;
	}

	@Test
	public void test() throws Exception {

		Connection conn = getConnection();
		
		try {
			
			BeanSession session = new H2BeanSession(configureManager1(), conn);
			session.createTables();

			Group g1 = new Group("Common");
			Group g2 = new Group("Super");
			Group g3 = new Group("Admin");
			session.insert(g1);
			session.insert(g2);
			session.insert(g3);

			User u1 = new User("John", 40, true, g1);
			User u2 = new User("Ralph", 30, false, g2);
			User u3 = new User("Matt", 20, true, g3);
			session.insert(u1);
			session.insert(u2);
			session.insert(u3);

			User ralph = new User(2);
			session.load(ralph);
			assertEquals("Ralph", ralph.getName());

			User clone = (User) ralph.clone();
			assertNotNull(clone.getGroup());
			assertEquals(2, clone.getGroup().getId());

			int updated = session.updateDiff(clone, ralph);
			assertEquals(0, updated);

			clone.setGroup(null);
			updated = session.updateDiff(clone, ralph);
			assertEquals(1, updated);
			clone = session.createBasicInstance(clone);
			session.load(clone);
			assertNull(clone.getGroup());
			assertEquals("Ralph", clone.getName());
			assertFalse(clone.isActive());

			User old = (User) clone.clone();
			clone.setGroup(g3);
			updated = session.updateDiff(clone, old);
			assertEquals(1, updated);
			clone = session.createBasicInstance(clone);
			session.load(clone);
			assertNotNull(clone.getGroup());
			assertEquals("Ralph", clone.getName());

			old = (User) clone.clone();
			clone.setGroup(new Group());
			clone.setAge(0);
			clone.setActive(true);
			updated = session.updateDiff(clone, old);
			assertEquals(1, updated);
			clone = session.createBasicInstance(clone);
			session.load(clone);
			assertNull(clone.getGroup());
			assertEquals(new Integer(0), clone.getAge());
			assertTrue(clone.isActive());

			old = (User) clone.clone();
			clone.setAge(null);
			updated = session.updateDiff(clone, old);
			assertEquals(1, updated);
			clone = session.loadUnique(clone);
			assertNull(clone.getAge());

			old = (User) clone.clone();
			clone.setAge(10);
			clone.setActive(false);
			updated = session.updateDiff(clone, old);
			assertEquals(1, updated);
			clone = session.createBasicInstance(clone);
			session.load(clone);
			assertEquals(new Integer(10), clone.getAge());
			assertFalse(clone.isActive());

		} finally {
			
			SQLUtils.close(conn);
		}
	}
	
	@Test
	public void testDifferences() throws Exception {

		Connection conn = getConnection();

		try {

			BeanSession session = new H2BeanSession(configureManager2(), conn);
			session.createTables();

			Group g1 = new Group(1, "Group one");
			Group g2 = new Group(2, "Group two");
			
			TypeTest newObj = new TypeTest()
				.setDoublePrimitive(0.6)
				.setDoubleWrapper(0.6)
				.setIntPrimitive(1)
				.setIntWrapper(1)
				.setGroup(g1)
				.setString("Test one");
			
			TypeTest oldObj = new TypeTest()
				.setDoublePrimitive(0.7)
				.setDoubleWrapper(0.6)
				.setIntPrimitive(1)
				.setIntWrapper(1)
				.setGroup(g1)
				.setString("Test one");
			
			List<String> diffs = new LinkedList<String>();
			TypeTest diff = session.compareDifferences(newObj, oldObj, diffs);
			Assert.assertNotNull(diff);
			Assert.assertEquals(diff.getDoublePrimitive(), newObj.getDoublePrimitive());
			Assert.assertEquals(0, diffs.size());
			
			diffs.clear();
			oldObj.setDoublePrimitive(0.6);
			diff = session.compareDifferences(newObj, oldObj, diffs);
			Assert.assertNull(diff);
			Assert.assertEquals(0, diffs.size());

			diffs.clear();
			newObj.setGroup(g2);
			diff = session.compareDifferences(newObj, oldObj, diffs);
			Assert.assertNotNull(diff);
			Assert.assertEquals(0, diffs.size());
			Assert.assertEquals(g2, diff.getGroup());
			
			diffs.clear();
			newObj.setGroup(null);
			diff = session.compareDifferences(newObj, oldObj, diffs);
			Assert.assertEquals(1, diffs.size());
			Assert.assertEquals("group.id", diffs.get(0));
			
			diffs.clear();
			newObj.setGroup(g2);
			newObj.setDoublePrimitive(0);
			newObj.setString(null);
			diff = session.compareDifferences(newObj, oldObj, diffs);
			Assert.assertEquals(g2, diff.getGroup());
			Assert.assertEquals(2, diffs.size());
			Assert.assertEquals("doublePrimitive", diffs.get(0));
			Assert.assertEquals("string", diffs.get(1));
			
			diffs.clear();
			newObj.setGroup(oldObj.getGroup());
			newObj.setDoublePrimitive(0.000000001d);
			oldObj.setDoublePrimitive(0.000000001d);
			newObj.setString(oldObj.getString());
			diff = session.compareDifferences(newObj, oldObj, diffs);
			Assert.assertEquals(0, diffs.size());
			Assert.assertNull(diff);
			
			diffs.clear();
			oldObj.setDoublePrimitive(0);
			diff = session.compareDifferences(newObj, oldObj, diffs);
			Assert.assertNotNull(diff);
			Assert.assertEquals(newObj.getDoublePrimitive(), diff.getDoublePrimitive());
			
			diffs.clear();
			newObj.setDoublePrimitive(0);
			oldObj.setDoublePrimitive(0);
			diff = session.compareDifferences(newObj, oldObj, diffs);
			Assert.assertNull(diff);
			
			diffs.clear();
			newObj.setIntPrimitive(0);
			diff = session.compareDifferences(newObj, oldObj, diffs);
			Assert.assertNotNull(diff);
			Assert.assertEquals(0, diff.getIntPrimitive());
			Assert.assertEquals(1, diffs.size());
			Assert.assertEquals("intPrimitive", diffs.get(0));

		} finally {

			SQLUtils.close(conn);
		}
	}

}
