package org.mentabean.jdbc;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.mentabean.jdbc.AnsiSQLBeanSession.DEBUG;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.mentabean.BeanConfig;
import org.mentabean.BeanManager;
import org.mentabean.BeanSession;
import org.mentabean.DBTypes;
import org.mentabean.util.PropertiesProxy;

public class AnotherProxyTest extends AbstractBeanSessionTest {

	
	public static class Person {
		
		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
	
	public static class Customer extends Person {
		
		private int code;
		private Boolean active;
		private BigDecimal height;
		private int age;
		private Type type;
		private TypeTwo typeTwo;
		
		public Customer() {
		}
		public Customer(int code, String name, Boolean active) {
			this.code = code;
			setName(name);
			this.active = active;
		}
		public Customer(int code) {
			this.code = code;
		}
		public Customer(String name, Boolean active, BigDecimal height) {
			setName(name);
			this.active = active;
			this.height = height;
		}
		public int getCode() {
			return code;
		}
		public void setCode(int code) {
			this.code = code;
		}
		public Boolean getActive() {
			return active;
		}
		public void setActive(Boolean active) {
			this.active = active;
		}
		public BigDecimal getHeight() {
			return height;
		}
		public void setHeight(BigDecimal height) {
			this.height = height;
		}
		public int getAge() {
			return age;
		}
		public void setAge(int age) {
			this.age = age;
		}
		public Type getType() {
			return type;
		}
		public void setType(Type type) {
			this.type = type;
		}
		public TypeTwo getTypeTwo() {
			return typeTwo;
		}
		public void setTypeTwo(TypeTwo typeTwo) {
			this.typeTwo = typeTwo;
		}
		
	}
	
	public enum Type {
		
		A, B, C;
	}
	
	public enum TypeTwo {
		
		A {}, B {}, C {};
	}
	
	private BeanManager getBeanManagerCustomer() {

		BeanManager beanManager = new BeanManager();

		Customer customerProxy = PropertiesProxy.create(Customer.class);
		BeanConfig customerConfig = new BeanConfig(Customer.class, "customers")
			.pk(customerProxy.getCode(), "idcustomers", DBTypes.AUTOINCREMENT)
			.field(customerProxy.getName(), "name_db", DBTypes.STRING.size(-1))
			.field(customerProxy.getActive(), "active_db", DBTypes.BOOLEAN)
			.field(customerProxy.getHeight(), "height_db", DBTypes.BIGDECIMAL)
			.field(customerProxy.getAge(), "age_db", DBTypes.INTEGER)
			.field(customerProxy.getType(), DBTypes.ENUMVALUE.from(Type.class).nullable(true))
			.field(customerProxy.getTypeTwo(), DBTypes.ENUMVALUE.from(TypeTwo.class).nullable(true));
			
		//add configurations in beanManager
		beanManager.addBeanConfig(customerConfig);
		
		return beanManager;
	}
	
	@Test
	public void test() {
		
		DEBUG = false;
		
		final Connection conn = getConnection();
		
		try {
			
			BeanSession session = new H2BeanSession(getBeanManagerCustomer(), conn);
			session.createTables();
			
			//let's play...
			Customer c1 = new Customer("Erico", true, new BigDecimal("3.5"));
			c1.setAge(22);
			c1.setType(Type.A);
			c1.setTypeTwo(TypeTwo.B);
			Customer c2 = new Customer("Jessica", true, new BigDecimal("692.894"));
			Customer c3 = new Customer("Inactive customer", false, null);
			session.insert(c1);
			session.insert(c2);
			session.insert(c3);
			
			Customer pxy = PropertiesProxy.create(Customer.class);
			
			Customer cDB = new Customer(c3.getCode());
			session.loadMinus(cDB, pxy.getActive());
			session.loadList(new Customer(), pxy.getName(), pxy.getActive());
			
			c1 = new Customer(c1.getCode());
			session.load(c1);
			assertEquals(22, c1.getAge());
			assertEquals(Type.A, c1.getType());
			assertEquals(TypeTwo.B, c1.getTypeTwo());
			
			Customer upd = new Customer(c1.getCode());
			upd.setName("Erico KL");
			session.update(upd, pxy.getHeight(), "age");
			
			session.load(upd);
			assertNull(upd.getHeight());
			assertEquals(0, upd.getAge());
			assertNotNull(upd.getActive());
			
			c1 = new Customer(1, "Test", true);
			c2 = new Customer(2, "Test", true);
			
			List<String> nullProps = new LinkedList<String>();
			Customer merged = session.compareDifferences(c1, c2, nullProps);
			assertNull(merged);
			assertEquals(0, nullProps.size());
			
			c1 = new Customer(1, "John", null);
			c1.setAge(20);
			c2 = new Customer(1, "Test", true);
			c2.setAge(20);
			
			merged = session.compareDifferences(c1, c2, nullProps);
			assertEquals(1, merged.getCode());
			assertEquals("John", merged.getName());
			assertNull(merged.getActive());
			assertEquals(0, merged.getAge());
			assertEquals(1, nullProps.size());
			assertEquals("active", nullProps.get(0));
			
			c1 = new Customer(1, "John", null);
			c1.setAge(0);
			session.insert(c1);
			c2 = new Customer(1, "John", true);
			c2.setAge(20);
			
			nullProps.clear();
			merged = session.compareDifferences(c1, c2, nullProps);
			assertEquals(c1.getCode(), merged.getCode());
			assertNull(merged.getName());
			assertNull(merged.getActive());
			assertEquals(0, merged.getAge());
			assertEquals(2, nullProps.size());
			assertEquals("active", nullProps.get(0));
			assertEquals("age", nullProps.get(1));
			assertEquals(1, session.updateDiff(c1, c2));
			Customer db = session.loadUnique(new Customer(c1.getCode()));
			assertEquals(null, db.getActive());
			assertEquals(0, db.getAge());
			assertEquals(c1.getCode(), db.getCode());
			assertEquals(null, merged.getHeight());
			assertEquals("John", db.getName());
			
			c1 = new Customer(1, "John", true);
			c1.setAge(20);
			c2 = new Customer(1, "John", true);
			c2.setAge(0);
			
			nullProps.clear();
			merged = session.compareDifferences(c1, c2, nullProps);
			assertEquals(1, merged.getCode());
			assertNull(merged.getName());
			assertNull(merged.getActive());
			assertEquals(20, merged.getAge());
			assertEquals(0, nullProps.size());
			assertEquals(1, session.updateDiff(c1, c2));
			
			c1 = new Customer(1, "John", true);
			c1.setAge(20);
			c2 = new Customer(1, "John", true);
			c2.setAge(20);
			
			nullProps.clear();
			merged = session.compareDifferences(c1, c2, nullProps);
			assertNull(merged);
			assertEquals(0, nullProps.size());
			
		} catch (Exception e) {
			
			throw new RuntimeException(e);
			
		}finally {
			close(conn);
		}
	}
	
}
