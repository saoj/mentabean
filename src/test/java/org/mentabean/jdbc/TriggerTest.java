package org.mentabean.jdbc;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.mentabean.jdbc.AnsiSQLBeanSession.DEBUG;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;

import junit.framework.Assert;

import org.junit.Test;
import org.mentabean.BeanConfig;
import org.mentabean.BeanManager;
import org.mentabean.BeanSession;
import org.mentabean.DBTypes;
import org.mentabean.event.TriggerAdapter;
import org.mentabean.event.TriggerEvent;
import org.mentabean.util.PropertiesProxy;

public class TriggerTest extends AbstractBeanSessionTest {
	
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
		
		private Long code;
		private Boolean active;
		private BigDecimal height;
		private int age;
		private byte[] picture;
		
		public Customer() {
		}
		public Customer(Long code, String name, Boolean active) {
			this.code = code;
			setName(name);
			this.active = active;
		}
		public Customer(Long code) {
			this.code = code;
		}
		public Customer(String name, Boolean active, BigDecimal height) {
			setName(name);
			this.active = active;
			this.height = height;
		}
		public Long getCode() {
			return code;
		}
		public void setCode(Long code) {
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
		public byte[] getPicture() {
			return picture;
		}
		public void setPicture(byte[] picture) {
			this.picture = picture;
		}
		@Override
		public String toString() {
			return "Customer [code=" + code + ", active=" + active
					+ ", height=" + height + ", age=" + age + "]";
		}
		
	}
	
	private BeanManager getBeanManagerCustomer() {

		BeanManager beanManager = new BeanManager();

		Customer customerProxy = PropertiesProxy.create(Customer.class);
		BeanConfig customerConfig = new BeanConfig(Customer.class, "customers")
			.pk(customerProxy.getCode(), "idcustomers", DBTypes.AUTOINCREMENT)
			.field(customerProxy.getName(), "name_db", DBTypes.STRING)
			.field(customerProxy.getActive(), "active_db", DBTypes.BOOLEAN)
			.field(customerProxy.getHeight(), "height_db", DBTypes.BIGDECIMAL)
			.field(customerProxy.getPicture(), "picture_db", DBTypes.BYTE_ARRAY)
			.field(customerProxy.getAge(), "age_db", DBTypes.INTEGER);
		
		customerConfig.trigger(new TriggerAdapter() {
			
			@Override
			public void beforeInsert(TriggerEvent evt) {
				Customer c = evt.getBean();
				showMsg("===> BeanConfig trigger => Before insert for: "+c);
				assertNull(c.getCode());
			}
			
			@Override
			public void afterInsert(TriggerEvent evt) {
				Customer c = evt.getBean();
				showMsg("===> BeanConfig trigger => After insert for: "+c);
				assertNotNull(c.getCode());
			}
			
		});
			
		//add configurations in beanManager
		beanManager.addBeanConfig(customerConfig);
		
		return beanManager;
	}
	
	@Test
	public void test() {
		
		DEBUG = false;
		
		final Connection conn = getConnection();
		PreparedStatement stmt = null;
		
		try {
			
			BeanSession session = new H2BeanSession(getBeanManagerCustomer(), conn);
			session.createTables();
			
			session.addTrigger(new TriggerAdapter() {
				
				@Override
				public void beforeInsert(TriggerEvent evt) {
					
					showMsg("Before insert for: "+evt.getBean());
					Customer c = evt.getBean();
					assertNull(c.getCode());
				}
				
				@Override
				public void afterInsert(TriggerEvent evt) {
					showMsg("After insert for: "+evt.getBean());
					Customer c = evt.getBean();
					assertNotNull(c.getCode());
				}
				
				@Override
				public void beforeUpdate(TriggerEvent evt) {
					showMsg("Before update for: "+evt.getBean());
				}
				
				@Override
				public void afterUpdate(TriggerEvent evt) {
					showMsg("After update for: "+evt.getBean());
				}
				
				@Override
				public void beforeDelete(TriggerEvent evt) {
					showMsg("Before delete for: "+evt.getBean());
					assertNotNull(evt.getSession().loadUnique(evt.getBean()));
				}
				
				@Override
				public void afterDelete(TriggerEvent evt) {
					showMsg("After delete for: "+evt.getBean());
					assertNull(evt.getSession().loadUnique(evt.getBean()));
				}
				
			});
			
			//let's play...
			Customer c1 = new Customer("Erico", true, new BigDecimal("3.5"));
			c1.setAge(22);
			c1.setPicture(new byte[1024]);
			Customer c2 = new Customer("Jessica", true, new BigDecimal("692.894"));
			Customer c3 = new Customer("Inactive customer", false, null);
			session.insert(c1);
			session.insert(c2);
			session.insert(c3);
			
			session.load(c1);
			
			Customer basic = session.createBasicInstance(c1);
			Assert.assertNotNull(basic.getCode());
			Assert.assertNull(basic.getName());
			Assert.assertNull(basic.getActive());
			Assert.assertNull(basic.getHeight());
			Assert.assertNull(basic.getPicture());
			Assert.assertEquals(0, basic.getAge());
			
			session.load(c2);
			assertNotNull(c1.getPicture());
			assertNull(c2.getPicture());
			
			Customer pxy = PropertiesProxy.create(Customer.class);
			
			Customer cDB = new Customer(c3.getCode());
			session.loadMinus(cDB, pxy.getActive());
			session.loadList(new Customer(), pxy.getName(), pxy.getActive());
			
			session.load(c1);
			assertEquals(22, c1.getAge());
			
			Customer upd = new Customer(c1.getCode());
			upd.setName("Erico KL");
			session.update(upd, pxy.getHeight(), "age");
			
			session.load(upd);
			assertNull(upd.getHeight());
			assertEquals(0, upd.getAge());
			assertNotNull(upd.getActive());
			
			session.delete(upd);
			
		} catch (Exception e) {
			
			throw new RuntimeException(e);
			
		}finally {
			close(stmt);
			close(conn);
		}
	}
	
	private void showMsg(String msg) {
		if (DEBUG) {
			showMsg(msg);
		}
	}

}
