package org.mentabean.jdbc;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.mentabean.util.DefaultProxy;
import org.mentabean.util.PropertiesProxy;

public class PropertiesProxyTest {
	
	public static class User {
		
		private String blah1;
		private int blah2;
		private Integer blah3;
		private Address address;
		
		public String getBlah1() {
			return blah1;
		}
		public void setBlah1(String blah1) {
			this.blah1 = blah1;
		}
		public int getBlah2() {
			return blah2;
		}
		public void setBlah2(int blah2) {
			this.blah2 = blah2;
		}
		public Integer getBlah3() {
			return blah3;
		}
		public void setBlah3(Integer blah3) {
			this.blah3 = blah3;
		}
		public Address getAddress() {
			return address;
		}
		public void setAddress(Address address) {
			this.address = address;
		}
	}
	
	public static class Address {
		
		private int foo1;
		private String foo2;
		private City city;
		
		public int getFoo1() {
			return foo1;
		}
		public void setFoo1(int foo1) {
			this.foo1 = foo1;
		}
		public String getFoo2() {
			return foo2;
		}
		public void setFoo2(String foo2) {
			this.foo2 = foo2;
		}
		public City getCity() {
			return city;
		}
		public void setCity(City city) {
			this.city = city;
		}
	}
	
	public static class City {
		
		private int id;
		private String name;
		
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
	}
	
	private static boolean check(String[] array, String s) {
		for(String x : array) {
			if (s.equals(x)) return true;
		}
		return false;
	}
	
	@Before
	public void setUp() {
		PropertiesProxy.INSTANCE = new DefaultProxy();
	}
	
	@Test
	public void testRegular() {
		
		User userProp = PropertiesProxy.create(User.class);
		
		userProp.getBlah1();
		Assert.assertEquals("blah1", PropertiesProxy.getPropertyName());
		
		userProp.getBlah3();
		Assert.assertEquals("blah3", PropertiesProxy.getPropertyName());
		
		userProp.getBlah1();
		userProp.getBlah2();
		String[] array = PropertiesProxy.getPropertyNames();
		Assert.assertEquals(2, array.length);
		Assert.assertTrue(check(array, "blah1"));
		Assert.assertTrue(check(array, "blah2"));
	}
	
	@Test
	public void testNonFinal() {
		
		User userProp = PropertiesProxy.create(User.class);
		
		userProp.getBlah1();
		Assert.assertEquals("blah1", PropertiesProxy.getPropertyName());
		
		userProp.getAddress();
		Assert.assertEquals("address", PropertiesProxy.getPropertyName());
		
		userProp.getAddress();
		userProp.getBlah3();
		String[] array = PropertiesProxy.getPropertyNames();
		Assert.assertEquals(2, array.length);
		Assert.assertTrue(check(array, "address"));
		Assert.assertTrue(check(array, "blah3"));
	}
	
	@Test
	public void testNestedProperties() {
		
		User userProp = PropertiesProxy.create(User.class);
		
		userProp.getBlah1();
		userProp.getAddress().getFoo1();
		String[] array = PropertiesProxy.getPropertyNames();
		Assert.assertEquals(2, array.length);
		Assert.assertTrue(check(array, "blah1"));
		Assert.assertTrue(check(array, "address.foo1"));
		
		userProp.getAddress().getFoo2();
		userProp.getBlah3();
		array = PropertiesProxy.getPropertyNames();
		Assert.assertEquals(2, array.length);
		Assert.assertTrue(check(array, "address.foo2"));
		Assert.assertTrue(check(array, "blah3"));
	}
	
	@Test
	public void testThreeLevels() {
		
		User userProp = PropertiesProxy.create(User.class);
		
		userProp.getBlah2();
		userProp.getAddress().getCity().getId();
		userProp.getBlah1();
		String[] array = PropertiesProxy.getPropertyNames();
		Assert.assertEquals(3, array.length);
		Assert.assertTrue(check(array, "blah2"));
		Assert.assertTrue(check(array, "address.city.id"));
		Assert.assertTrue(check(array, "blah1"));

		userProp.getAddress().getFoo2();
		userProp.getBlah3();
		userProp.getAddress().getCity().getId();
		userProp.getBlah1();
		array = PropertiesProxy.getPropertyNames();
		Assert.assertEquals(4, array.length);
		Assert.assertTrue(check(array, "address.foo2"));
		Assert.assertTrue(check(array, "blah3"));
		Assert.assertTrue(check(array, "address.city.id"));
		Assert.assertTrue(check(array, "blah1"));
	}
	
	@Test
	public void testVarargs() {
		
		User userProp = PropertiesProxy.create(User.class);
		
		Object[] obj = new Object[] { "blah1", "blah2" };
		String[] array = AnsiSQLBeanSession.getProperties(obj);
		Assert.assertEquals(2, array.length);
		Assert.assertTrue(check(array, "blah1"));
		Assert.assertTrue(check(array, "blah2"));
		
		obj = new Object[] { "blah1", userProp.getBlah3(), "blah2" };
		array = AnsiSQLBeanSession.getProperties(obj);
		Assert.assertEquals(3, array.length);
		Assert.assertTrue(check(array, "blah1"));
		Assert.assertTrue(check(array, "blah2"));
		Assert.assertTrue(check(array, "blah3"));
		
		obj = new Object[] { userProp.getBlah3() };
		array = AnsiSQLBeanSession.getProperties(obj);
		Assert.assertEquals(1, array.length);
		Assert.assertTrue(check(array, "blah3"));
		
		obj = new Object[] { userProp.getBlah2(), userProp.getBlah3() };
		array = AnsiSQLBeanSession.getProperties(obj);
		Assert.assertEquals(2, array.length);
		Assert.assertTrue(check(array, "blah2"));
		Assert.assertTrue(check(array, "blah3"));
		
		obj = new Object[] { userProp.getBlah3(), userProp.getAddress() };
		array = AnsiSQLBeanSession.getProperties(obj);
		Assert.assertEquals(2, array.length);
		Assert.assertTrue(check(array, "blah3"));
		Assert.assertTrue(check(array, "address"));
		
		obj = new Object[] { userProp.getBlah3(), userProp.getAddress().getCity(), "blah1" };
		array = AnsiSQLBeanSession.getProperties(obj);
		Assert.assertEquals(3, array.length);
		Assert.assertTrue(check(array, "blah3"));
		Assert.assertTrue(check(array, "blah1"));
		Assert.assertTrue(check(array, "address.city"));
		
		obj = new Object[] { userProp.getBlah3(), userProp.getAddress().getFoo1(), "blah1" };
		array = AnsiSQLBeanSession.getProperties(obj);
		Assert.assertEquals(3, array.length);
		Assert.assertTrue(check(array, "blah3"));
		Assert.assertTrue(check(array, "blah1"));
		Assert.assertTrue(check(array, "address.foo1"));
		
		obj = new Object[] { userProp.getBlah3(), userProp.getAddress().getCity().getId() };
		array = AnsiSQLBeanSession.getProperties(obj);
		Assert.assertEquals(2, array.length);
		Assert.assertTrue(check(array, "blah3"));
		Assert.assertTrue(check(array, "address.city.id"));
		
		obj = new Object[] { "blah3", userProp.getAddress().getCity().getId(), userProp.getAddress().getFoo2() };
		array = AnsiSQLBeanSession.getProperties(obj);
		Assert.assertEquals(3, array.length);
		Assert.assertTrue(check(array, "blah3"));
		Assert.assertTrue(check(array, "address.city.id"));
		Assert.assertTrue(check(array, "address.foo2"));
		
		obj = new Object[] { "blah3", userProp.getAddress().getFoo2(), userProp.getAddress().getCity().getId() };
		array = AnsiSQLBeanSession.getProperties(obj);
		Assert.assertEquals(3, array.length);
		Assert.assertTrue(check(array, "blah3"));
		Assert.assertTrue(check(array, "address.city.id"));
		Assert.assertTrue(check(array, "address.foo2"));
	}
	
	@Test
	public void testProxyInstances() {
		
		Address address = PropertiesProxy.create(Address.class);
		City city = PropertiesProxy.create(City.class);
		
		address.getFoo1();
		city.getId();
		address.getFoo2();
		city.getName();
		
		//first we have to get the proxy instances, cause getPropertyNames will clear them 
		Object instances[] = PropertiesProxy.getBeanInstances();
		//now we can work with property names...
		String properties[] = PropertiesProxy.getPropertyNames();
		
		Assert.assertEquals(properties.length, instances.length);
		Assert.assertEquals("foo1", properties[0]);
		Assert.assertEquals(address, instances[0]);
		Assert.assertEquals("id", properties[1]);
		Assert.assertEquals(city, instances[1]);
		Assert.assertEquals("foo2", properties[2]);
		Assert.assertEquals(address, instances[2]);
		Assert.assertEquals("name", properties[3]);
		Assert.assertEquals(city, instances[3]);
	}
}