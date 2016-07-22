package org.mentabean.jdbc;

import static org.mentabean.jdbc.AnsiSQLBeanSession.DEBUG;
import static org.mentabean.util.SQLUtils.lim;
import static org.mentabean.util.SQLUtils.orderByAsc;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mentabean.BeanConfig;
import org.mentabean.BeanManager;
import org.mentabean.BeanSession;
import org.mentabean.DBTypes;
import org.mentabean.jdbc.QueryBuilder.Alias;
import org.mentabean.jdbc.QueryBuilder.Query;
import org.mentabean.sql.conditions.GreaterThan;
import org.mentabean.sql.conditions.Like;
import org.mentabean.sql.functions.Coalesce;
import org.mentabean.sql.functions.Lower;
import org.mentabean.sql.param.ParamField;
import org.mentabean.sql.param.ParamFunction;
import org.mentabean.sql.param.ParamValue;
import org.mentabean.util.PropertiesProxy;
import org.mentabean.util.SQLUtils;

public class RecursivePropertiesTest extends AbstractBeanSessionTest {
	
	public static class Post {
		
		private int id;
		private String title;
		private User user;
		
		public Post() {
			
		}
		
		public Post(int id) {
			this.id = id;
		}
		
		public Post(String title, User user) {
			this.title = title;
			this.user = user;
		}
		
		public int getId() {
			return id;
		}
		public void setId(int id) {
			this.id = id;
		}
		public String getTitle() {
			return title;
		}
		public void setTitle(String title) {
			this.title = title;
		}
		public User getUser() {
			return user;
		}
		public void setUser(User user) {
			this.user = user;
		}
	}
	
	public static class User {
		
		private int id;
		private String username;
		private Address address;
		
		public User() {
			
		}
		
		public User(int id) {
			this.id = id;
		}
		
		public User(String username, Address addr) {
			this.username = username;
			this.address = addr;
		}
		
		public int getId() {
			return id;
		}
		public void setId(int id) {
			this.id = id;
		}
		public Address getAddress() {
			return address;
		}
		public void setAddress(Address address) {
			this.address = address;
		}
		public String getUsername() {
			return username;
		}
		public void setUsername(String username) {
			this.username = username;
		}
	}
	
	public static class Address {
		
		private int id;
		private City city;
		
		public Address() { 
			
		}
		
		public Address(int id) {
			this.id = id;
		}
		
		public Address(City city) {
			this.city = city;
		}
		
		public int getId() {
			return id;
		}
		public void setId(int id) {
			this.id = id;
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
		
		public City() {
			
		}
		
		public City(int id) {
			this.id = id;
		}
		
		public City(int id, String name) {
			this.id = id;
			this.name = name;
		}
		
		public City(String name) {
			this.name = name;
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
	}
	
	@BeforeClass
	public static void setup() {
		DEBUG = false; // turn on to see SQL generated
	}
	
	private BeanManager getBeanManager1() {

		// programmatic configuration for the bean... (no annotation or XML)
		
		BeanManager beanManager = new BeanManager();

		BeanConfig postConfig = new BeanConfig(Post.class, "Posts");
		postConfig.pk("id", DBTypes.AUTOINCREMENT);
		postConfig.field("title", DBTypes.STRING);
		postConfig.field("user.id", "user_id", DBTypes.INTEGER); // note that the database column name is different
		beanManager.addBeanConfig(postConfig);
		
		BeanConfig userConfig = new BeanConfig(User.class, "Users");
		userConfig.pk("id", DBTypes.AUTOINCREMENT);
		userConfig.field("username", DBTypes.STRING);
		userConfig.field("address.id", "address_id", DBTypes.INTEGER);
		beanManager.addBeanConfig(userConfig);
		
		BeanConfig addressConfig = new BeanConfig(Address.class, "Addresses");
		addressConfig.pk("id", DBTypes.AUTOINCREMENT);
		addressConfig.field("city.id", "city_id", DBTypes.INTEGER);
		beanManager.addBeanConfig(addressConfig);
		
		BeanConfig cityConfig = new BeanConfig(City.class, "Cities");
		cityConfig.pk("id", DBTypes.INTEGER);
		cityConfig.field("name", DBTypes.STRING);
		beanManager.addBeanConfig(cityConfig);
		
		return beanManager;
	}
	
	@Test
	public void testTwoLevels() throws SQLException {
		
		BeanManager beanManager = getBeanManager1();
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rset = null;

		try {

			conn = getConnection();
//			createTables1(conn);
			
			BeanSession session = new H2BeanSession(beanManager, conn);
			session.createTables();
			
			// first insert...
			
			City rj = new City(1, "Rio de Janeiro");
			City chi = new City(2, "Chicago");
			
			session.insert(rj);
			session.insert(chi);
			
			Address addr1 = new Address(rj);
			Address addr2 = new Address(chi);
			
			session.insert(addr1);
			session.insert(addr2);
			
			User user1 = new User("saoj", addr1);
			User user2 = new User("julia", addr2);
			
			session.insert(user1);
			session.insert(user2);
			
			Post post1 = new Post("Title1", user1);
			
			session.insert(post1);
			
			// now load and test;
			
			City c1 = new City(1);
			session.load(c1);
			Assert.assertEquals(c1.getId(), rj.getId());
			Assert.assertEquals(c1.getName(), rj.getName());

			Address a1 = new Address(1);
			session.load(a1);
			Assert.assertEquals(a1.getId(), addr1.getId());
			Assert.assertEquals(a1.getCity().getId(), addr1.getCity().getId());
			
			User u1 = new User(1);
			session.load(u1);
			Assert.assertEquals(u1.getId(), user1.getId());
			Assert.assertEquals(u1.getUsername(), user1.getUsername());
			Assert.assertEquals(u1.getAddress().getId(), user1.getAddress().getId());
			
			// now above the second level will be null due to lazy loading:
			Assert.assertEquals(u1.getAddress().getCity(), null);
			
			// let's test this again:
			Post p1 = new Post(1);
			session.load(p1);
			Assert.assertEquals(p1.getId(), post1.getId());
			Assert.assertEquals(p1.getUser().getId(), post1.getUser().getId());
			// but the only thing the user object has is the ID... everything else is null (lazy loading)
			Assert.assertEquals(p1.getUser().getUsername(), null);
			
			// when you need the user of the post, you can manually load it:
			session.load(p1.getUser()); // manual lazy loading!
			
			Assert.assertEquals(p1.getUser().getUsername(), post1.getUser().getUsername());
			
			// now update!
			
			// change the user of post1
			post1.setUser(user2);
			session.update(post1);
			
			Post updatedPost = new Post(1);
			session.load(updatedPost);
			Assert.assertEquals(post1.getId(), updatedPost.getId());
			Assert.assertEquals(post1.getTitle(), updatedPost.getTitle());
			Assert.assertEquals(post1.getUser().getId(), updatedPost.getUser().getId());
			
			// load list:
			
			Post post2 = new Post("Title2", user1);
			session.insert(post2);
			Post post3 = new Post("Title3", user2);
			session.insert(post3);
			
			// we should have two posts for user2 now: (remember the first one was updated to user2)
			Post p = new Post();
			p.setUser(user2);
			
			List<Post> posts = session.loadList(p);
			Assert.assertEquals(posts.size(), 2);
			
			int beansDeleted = session.deleteAll(p);
			Assert.assertEquals(2, beansDeleted);
			
			p.setUser(user1);
			posts = session.loadList(p);
			Assert.assertEquals(posts.size(), 1);
			
			beansDeleted = session.deleteAll(p);
			Assert.assertEquals(1, beansDeleted);
			
		}  finally {
			close(stmt, rset);
			close(conn);
		}
	}
	
	private BeanManager getBeanManager2() {

		// programmatic configuration for the bean... (no annotation or XML)
		
		BeanManager beanManager = new BeanManager();
		
		Post postProps = PropertiesProxy.create(Post.class);

		BeanConfig postConfig = new BeanConfig(Post.class, "Posts");
		postConfig.pk(postProps.getId(), DBTypes.AUTOINCREMENT);
		postConfig.field(postProps.getTitle(), DBTypes.STRING);
		postConfig.field(postProps.getUser().getId(), "user_id", DBTypes.INTEGER); // note that the database column name is different
		postConfig.field(postProps.getUser().getAddress().getId(), "address_id", DBTypes.INTEGER);
		beanManager.addBeanConfig(postConfig);
		
		User userProps = PropertiesProxy.create(User.class);
		
		BeanConfig userConfig = new BeanConfig(User.class, "Users");
		userConfig.pk(userProps.getId(), DBTypes.AUTOINCREMENT);
		userConfig.field("username", DBTypes.STRING);
		userConfig.field("address.id", "address_id", DBTypes.INTEGER);
		beanManager.addBeanConfig(userConfig);
		
		Address addressProps = PropertiesProxy.create(Address.class);
		
		BeanConfig addressConfig = new BeanConfig(Address.class, "Addresses");
		addressConfig.pk("id", DBTypes.AUTOINCREMENT);
		addressConfig.field(addressProps.getCity().getId(), "city_id", DBTypes.INTEGER);
		beanManager.addBeanConfig(addressConfig);
		
		BeanConfig cityConfig = new BeanConfig(City.class, "Cities");
		cityConfig.pk("id", DBTypes.INTEGER);
		cityConfig.field("name", DBTypes.STRING);
		beanManager.addBeanConfig(cityConfig);
		
		return beanManager;
	}
	
	@Test
	public void testThreeLevels() throws SQLException {
		
		BeanManager beanManager = getBeanManager2();
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rset = null;

		try {

			conn = getConnection();
//			createTables2(conn);
			
			BeanSession session = new H2BeanSession(beanManager, conn);
			session.createTables();

			// first insert...
			
			City rj = new City(1, "Rio de Janeiro");
			City chi = new City(2, "Chicago");
			
			session.insert(rj);
			session.insert(chi);
			
			Address addr1 = new Address(rj);
			Address addr2 = new Address(chi);
			
			session.insert(addr1);
			session.insert(addr2);
			
			User user1 = new User("saoj", addr1);
			User user2 = new User("julia", addr2);
			
			session.insert(user1);
			session.insert(user2);
			
			Post post1 = new Post("Title1", user1);
			
			session.insert(post1);
			
			// now load and test;
			
			Post p1 = new Post(1);
			session.load(p1);
			
			// check if we have the address id
			Assert.assertEquals(p1.getUser().getAddress().getId(), post1.getUser().getAddress().getId());
			
			// and everything from user and from address must be NULL except the ids (remember the Posts also have user_id, so the id of the user will NOT be null)
			Assert.assertEquals(p1.getUser().getId(), post1.getUser().getId());
			Assert.assertEquals(p1.getUser().getUsername(), null);
			Assert.assertEquals(p1.getUser().getAddress().getCity(), null);
			
			// change the address of the post (i.e. of its user)
			
			p1.getUser().setAddress(addr2);
			
			session.update(p1);
			
			Post updatedPost = new Post(1);
			session.load(updatedPost);
			
			Assert.assertEquals(updatedPost.getUser().getAddress().getId(), addr2.getId());
			
			// NOTE: This will create an inconsistency in the database because we did NOT update the user, only the address_id in the Post table...
			
			// the correct way of doing it:
			
			session.load(p1.getUser());
			Assert.assertEquals(p1.getUser().getAddress().getId(), addr1.getId()); // still address 1 because we never updated user...
			// now update:
			p1.getUser().setAddress(addr2);
			session.update(p1.getUser());
			
			// now just call update on the post and its address_id will be updated correctly:
			session.update(p1);
			
			Post p = new Post(1);
			session.load(p);
			Assert.assertEquals(p.getUser().getAddress().getId(), addr2.getId());
			// and now the user is also correct:
			User u = new User(1);
			session.load(u);
			Assert.assertEquals(u.getAddress().getId(),	addr2.getId());
			
			Assert.assertEquals(1, session.deleteAll(new Post()));
			Assert.assertEquals(2, session.deleteAll(new User()));
			Assert.assertEquals(2, session.deleteAll(new Address()));
			Assert.assertEquals(2, session.deleteAll(new City()));
			
		}  finally {
			close(stmt, rset);
			close(conn);
		}
	}
	
	@Test
	public void testAnotherOperations() throws SQLException {

		final Connection conn = getConnection();
		
		try {
			BeanSession beanSession = new H2BeanSession(getBeanManager1(), conn);
			beanSession.createTables();

			//let's play
			City c1 = new City(1, "Santa Rosa");
			Address a1 = new Address(c1);
			User u1 = new User("erico", a1);
			User u2 = new User("jessica", a1);

			beanSession.insert(c1);
			beanSession.insert(a1);
			beanSession.insert(u1);
			beanSession.insert(u2);

			//simple check
			City c1DB = new City(c1.getId());
			beanSession.load(c1DB);
			Assert.assertEquals(c1.getName(), c1DB.getName());

			User u1DB = new User(u1.getId());
			beanSession.load(u1DB);
			Assert.assertEquals(u1.getId(), u1DB.getId());
			Assert.assertEquals(u1.getAddress().getId(), u1DB.getAddress().getId());

			beanSession.insert(u2);

			//retrieving all users in base WITHOUT address
			List<User> list = beanSession.loadListMinus(new User(), orderByAsc("username"), "address.id");
			for (User userDB : list) {
				Assert.assertEquals(null, userDB.getAddress());
			}
			
			User userProps = PropertiesProxy.create(User.class);
			
			list = beanSession.loadListMinus(new User(), orderByAsc("username"), userProps.getAddress().getId());
			for (User userDB : list) {
				Assert.assertEquals(null, userDB.getAddress());
			}

			//another way to get the list above
			list = beanSession.loadList(new User(), orderByAsc("username"), "id", "username");
			for (User userDB : list) {
				Assert.assertNull(userDB.getAddress());
				Assert.assertNotNull(userDB.getId());
				Assert.assertNotNull(userDB.getUsername());
			}
			
			// test proxy for list of properties
			list = beanSession.loadList(new User(), orderByAsc("username"), userProps.getId(), userProps.getUsername());
			for (User userDB : list) {
				Assert.assertNull(userDB.getAddress());
				Assert.assertNotNull(userDB.getId());
				Assert.assertNotNull(userDB.getUsername());
			}
			
			list = beanSession.loadList(new User(), orderByAsc("username"), userProps.getId() );
			for (User userDB : list) {
				Assert.assertNull(userDB.getAddress());
				Assert.assertNotNull(userDB.getId());
				Assert.assertNull(userDB.getUsername());
			}
			
			list = beanSession.loadList(new User(), orderByAsc(userProps.getUsername()), userProps.getId());
			for (User userDB : list) {
				Assert.assertNull(userDB.getAddress());
				Assert.assertNotNull(userDB.getId());
				Assert.assertNull(userDB.getUsername());
			}
			
			list = beanSession.loadList(new User(), orderByAsc(userProps.getUsername()), lim(1), userProps.getId());
			Assert.assertEquals(1, list.size());
			
			Assert.assertEquals(2, beanSession.deleteAll(new User("jessica", null)));
			
			list = beanSession.loadList(new User());
			Assert.assertEquals(1, list.size());
			
			Assert.assertNotNull(list.get(0).getAddress());
			Assert.assertNotNull(list.get(0).getId());
			Assert.assertEquals("erico", list.get(0).getUsername());
			
		}finally {
			close(conn);
		}
	}
	
	
	/*
	 * Another situation
	 */
	public static class Customer {
		
		private int code;
		private String name;
		private Boolean active;
		
		public Customer() {
		}
		public Customer(int code, String name, Boolean active) {
			this.code = code;
			this.name = name;
			this.active = active;
		}
		public Customer(int code) {
			this.code = code;
		}
		public Customer(String name, Boolean active) {
			this.name = name;
			this.active = active;
		}
		public int getCode() {
			return code;
		}
		public void setCode(int code) {
			this.code = code;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public Boolean getActive() {
			return active;
		}
		public void setActive(Boolean active) {
			this.active = active;
		}
		
	}
	
	public static class Sale {
		
		private long id;
		private Customer customer;
		private List<Item> items = new ArrayList<Item>();
		
		public Sale() {}
		
		public Sale(Customer customer) {
			this.customer = customer;
		}
		public Sale(long id) {
			this.id = id;
		}
		public Sale(Customer customer, List<Item> items) {
			this.customer = customer;
			this.items = items;
		}
		public Sale(long id, Customer customer, List<Item> items) {
			this.id = id;
			this.customer = customer;
			this.items = items;
		}
		public long getId() {
			return id;
		}
		public void setId(long id) {
			this.id = id;
		}
		public Customer getCustomer() {
			return customer;
		}
		public void setCustomer(Customer customer) {
			this.customer = customer;
		}
		public BigDecimal getTotalPrice() {
			BigDecimal totalPrice = BigDecimal.ZERO;
			for (Item item : items)
				totalPrice = totalPrice.add(item.getTotalPrice());
			return totalPrice;
				
		}
		public List<Item> getItems() {
			return items;
		}
		public void setItems(List<Item> items) {
			this.items = items;
		}
		public void addItem(Item item) {
			item.setSale(this);
			this.items.add(item);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Sale)
				return ((Sale)obj).id == id;
			return false;
		}
		
		@Override
		public int hashCode() {
			return (int) id;
		}
	}
	
	public static class Item {
		
		private Sale sale;
		private Product product;
		private BigDecimal amount;
		private Double actualPrice;
		
		public Item() {}
		
		public Item(Sale sale, Product product, BigDecimal amount,
				Double actualPrice) {
			this.actualPrice = actualPrice;
			this.sale = sale;
			this.product = product;
			this.amount = amount;
		}
		public Item(Sale sale, Product product, BigDecimal amount) {
			this.sale = sale;
			this.product = product;
			this.amount = amount;
			this.actualPrice = product.getPrice();
		}
		public Item(Product product, BigDecimal amount) {
			this.product = product;
			this.amount = amount;
			this.actualPrice = product.getPrice();
		}
		public Item(Sale sale, Product product) {
			this.product = product;
			this.sale = sale;
			this.actualPrice = product.getPrice();
		}
		public Item(Sale sale) {
			this.sale = sale;
		}
		public Sale getSale() {
			return sale;
		}
		public void setSale(Sale sale) {
			this.sale = sale;
		}
		public Product getProduct() {
			return product;
		}
		public void setProduct(Product product) {
			this.product = product;
		}
		public BigDecimal getAmount() {
			return amount;
		}
		public void setAmount(BigDecimal amount) {
			this.amount = amount;
		}
		public Double getActualPrice() {
			return actualPrice;
		}
		public void setActualPrice(Double actualPrice) {
			this.actualPrice = actualPrice;
		}
		public BigDecimal getTotalPrice() {
			if (actualPrice <=0)
				throw new IllegalArgumentException("Invalid price");
			if (amount.signum() <= 0)
				throw new IllegalArgumentException("Invalid amount");
			return BigDecimal.valueOf(actualPrice).multiply(amount);
		}
	}
	
	public static class Product {
		
		private int id;
		private String description;
		private double price;
		
		public Product() {}
		
		public Product(String description, double price) {
			this.description = description;
			this.price = price;
		}
		public Product(int id, String description, double price) {
			this.id = id;
			this.description = description;
			this.price = price;
		}
		public Product(int id) {
			this.id = id;
		}
		public int getId() {
			return id;
		}
		public void setId(int id) {
			this.id = id;
		}
		public String getDescription() {
			return description;
		}
		public void setDescription(String description) {
			this.description = description;
		}
		public double getPrice() {
			return price;
		}
		public void setPrice(double price) {
			this.price = price;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Product)
				return ((Product)obj).id == id;
			return false;
		}
		
		@Override
		public int hashCode() {
			return (int) id;
		}
	}
	
	//fluent mode
	private BeanManager getBeanManagerCustomer() {

		// programmatic configuration for the bean... (no annotation or XML)
		BeanManager beanManager = new BeanManager();

//		BeanConfig customerConfig = new BeanConfig(Customer.class, "customers")
//			.pk("code", "idcustomers", DBTypes.AUTOINCREMENT)
//			.field("name", DBTypes.STRING);
//			
//		BeanConfig saleConfig = new BeanConfig(Sale.class, "sales")
//			.pk("id", "idsales", DBTypes.AUTOINCREMENT)
//			.field("customer.code", "idcustomers", DBTypes.INTEGER);
//		
//		BeanConfig itemConfig = new BeanConfig(Item.class, "items")
//			
//			// the "sale.id" is a reference for field "id" in object "sale"
//			.pk("sale.id", "idsales", DBTypes.LONG)
//			
//			// same as above, the "product.id" is a reference for field "id" in object "product"
//			// note that it's a composite primary key with no sequence or autoincrement field
//			.pk("product.id", "idproducts", DBTypes.INTEGER)
//			
//			.field("amount", "amount_db", DBTypes.DOUBLE)
//			.field("actualPrice", "actual_price_db", DBTypes.DOUBLE);
//		
//		BeanConfig productConfig = new BeanConfig(Product.class, "products")
//			.pk("id", "idproducts", DBTypes.AUTOINCREMENT)
//			.field("description", DBTypes.STRING)
//			.field("price", DBTypes.DOUBLE);
		
		
		Customer customerProxy = PropertiesProxy.create(Customer.class);
		BeanConfig customerConfig = new BeanConfig(Customer.class, "customers")
			.pk(customerProxy.getCode(), "idcustomers", DBTypes.AUTOINCREMENT)
			.field(customerProxy.getName(), DBTypes.STRING.size(-1))
			.field(customerProxy.getActive(), DBTypes.BOOLEAN);
			
		Sale saleProxy = PropertiesProxy.create(Sale.class);
		BeanConfig saleConfig = new BeanConfig(Sale.class, "sales")
			.pk(saleProxy.getId(), "idsales", DBTypes.AUTOINCREMENT)
			.field(saleProxy.getCustomer().getCode(), "idcustomers", DBTypes.INTEGER);
		
		Item itemProxy = PropertiesProxy.create(Item.class);
		BeanConfig itemConfig = new BeanConfig(Item.class, "items")
			
			// the "sale.id" is a reference for field "id" in object "sale"
			.pk(itemProxy.getSale().getId(), "idsales", DBTypes.LONG)
			
			// same as above, the "product.id" is a reference for field "id" in object "product"
			// note that it's a composite primary key with no sequence or autoincrement field
			.pk(itemProxy.getProduct().getId(), "idproducts", DBTypes.INTEGER)
			
			.field(itemProxy.getAmount(), "amount_db", DBTypes.BIGDECIMAL)
			
			.field(itemProxy.getActualPrice(), "actual_price_db", DBTypes.DOUBLE);
		
		Product productProxy = PropertiesProxy.create(Product.class);
		BeanConfig productConfig = new BeanConfig(Product.class, "products")
			.pk(productProxy.getId(), "idproducts", DBTypes.AUTOINCREMENT)
			.field(productProxy.getDescription(), DBTypes.STRING.size(-1))
			.field(productProxy.getPrice(), DBTypes.DOUBLE);

		//add configurations in beanManager
		beanManager.addBeanConfig(customerConfig);
		beanManager.addBeanConfig(saleConfig);
		beanManager.addBeanConfig(itemConfig);
		beanManager.addBeanConfig(productConfig);
		
		return beanManager;
	}
	
	@Test
	public void testCustomer() {

		final Connection conn = getConnection();
		PreparedStatement stmt = null;
		
		try {
			AnsiSQLBeanSession session = new H2BeanSession(getBeanManagerCustomer(), conn);
			session.createTables();

			//let's play...
			Customer c1 = new Customer("Erico", true);
			Customer c2 = new Customer("Jessica", true);
			Customer c3 = new Customer("Inactive customer", false);
			session.insert(c1);
			session.insert(c2);
			session.insert(c3);
			
			Customer c3DB = new Customer(c3.getCode());
			session.load(c3DB);
			Assert.assertEquals(c3.getActive(), c3DB.getActive());

			Product p1 = new Product("Bean", 5.50);
			Product p2 = new Product("Rice", 4.32);
			Product p3 = new Product("Bread", 0.15);
			session.insert(p1);
			session.insert(p2);
			session.insert(p3);

			Sale s1 = new Sale(c1);

			// note that addItem method will set the respective sale in the given item
			s1.addItem(new Item(p1, BigDecimal.valueOf(1.0)));
			s1.addItem(new Item(p2, BigDecimal.valueOf(3)));
			s1.addItem(new Item(p3, BigDecimal.valueOf(0.175)));

			// we don't want magic here, so we have to insert the sale and the items separately
			session.insert(s1);
			for (Item item : s1.getItems())
				session.insert(item);

			//retrieving the stored sale
			Sale s1DB = new Sale(s1.getId());
			session.load(s1DB);
			
			// note that the s1DB sale has a costumer ONLY with code
			Assert.assertEquals(s1.getCustomer().getCode(), s1DB.getCustomer().getCode());
			
			// loading the customer
			session.load(s1DB.getCustomer());
			
			// now we have a sale with the full customer
			Assert.assertEquals(s1.getCustomer().getName(), s1DB.getCustomer().getName());

			// again, we have to load sale and items if we want to retrieve both of them
			Item itemProto = new Item(s1DB);
			
			Item itemProxy = PropertiesProxy.create(Item.class);
			s1DB.setItems(session.loadList(itemProto, orderByAsc(itemProxy.getProduct().getId()))); //ordering by idproducts

			// let's check
			Assert.assertEquals(s1.getId(), s1DB.getId());
			for (int i=0; i<s1.getItems().size(); i++) {
				
				// we're comparing the items in index order, so the s1 and s1DB's items order need to be equals
				Assert.assertEquals(s1.getItems().get(i).getActualPrice(), s1DB.getItems().get(i).getActualPrice());
				Assert.assertEquals(s1.getItems().get(i).getAmount().doubleValue(), s1DB.getItems().get(i).getAmount().doubleValue());
			}
			
			// updating the sale's customer
			s1DB.setCustomer(c2);
			session.update(s1DB);
			
			Sale updatedSale = new Sale(s1DB.getId());
			session.load(updatedSale);
			
			Assert.assertEquals(c2.getCode(), updatedSale.getCustomer().getCode());
			
			
			// let's do a more specific query
			// loading sales and customers with one query
			// first we'll insert a sale WITHOUT a customer
			Sale s2 = new Sale();
			session.insert(s2);
			
			Sale s2DB = new Sale(s2.getId());
			session.load(s2DB);
			
			//we don't have a customer
			Assert.assertNull(s2DB.getCustomer());
			
			List<Sale> sales = new ArrayList<Sale>();
			
			// Manual query using QueryBuilder
			QueryBuilder builder = session.buildQuery();
			
			// set the Alias just once
			Alias<Sale> s = builder.aliasTo(Sale.class, "s");
			Alias<Customer> c = builder.aliasTo(Customer.class, "c");
			
			// returns (will be removed from buildSelect and populateBean)
			c.setReturnMinus(c.pxy().getActive());
			
			Query query = builder.select(s, c)
			.from(s)
			.leftJoin(c).pkOf(c).in(s)
			.where().clause(s.pxy().getId()).condition(new GreaterThan(new ParamValue(0))).and()
			.clause(new Coalesce()
				.addParam(new ParamField(s, s.pxy().getId()))
				.addParam(new ParamValue(0)))
					.condition(new GreaterThan(new ParamValue(0))).and()
			.clause(new Coalesce()
				.addParam(new ParamFunction(new Lower(new ParamField(c, c.pxy().getName()))))
				.addParam(new ParamValue("i")))
					.condition(new Like(new ParamValue("%i%")))
			.orderBy().asc(s, s.pxy().getId())
			.desc(s, s.pxy().getCustomer().getCode())
			.limit(2).offset(0);
			
			stmt = query.prepare();
			ResultSet rs = stmt.executeQuery();
			
			Sale saleObj = null;
			while (rs.next()) {
				
				saleObj = new Sale();
				s.populateBean(rs, saleObj);
				
				if (saleObj.getCustomer() != null) { // left join might not return any customer...
					c.populateBean(rs, saleObj.getCustomer());
				}
				
				sales.add(saleObj);
			}
			
			Assert.assertNotNull(sales.get(0).getCustomer());
			Assert.assertNull(sales.get(1).getCustomer());
			
			// update or insert
			int result = -1;
			
			// must be updated
			saleObj.setCustomer(c1);
			result = session.save(saleObj);
			Assert.assertEquals(BeanSession.UPDATE, result);
			
			//must be inserted
			saleObj.setId(0);
			result = session.save(saleObj);
			Assert.assertEquals(BeanSession.INSERT, result);
			
		} catch (Exception e) {
			
			throw new RuntimeException(e);
			
		}finally {
			
			SQLUtils.close(stmt, conn);
		}
	}
	
}