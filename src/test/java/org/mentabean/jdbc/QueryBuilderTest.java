package org.mentabean.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mentabean.BeanException;
import org.mentabean.BeanManager;
import org.mentabean.BeanSession;
import org.mentabean.DBTypes;
import org.mentabean.jdbc.QueryBuilder.Alias;
import org.mentabean.jdbc.QueryBuilder.Query;
import org.mentabean.sql.Sentence;
import org.mentabean.sql.conditions.Between;
import org.mentabean.sql.conditions.Equals;
import org.mentabean.sql.conditions.GreaterThan;
import org.mentabean.sql.conditions.In;
import org.mentabean.sql.conditions.LessThan;
import org.mentabean.sql.conditions.Like;
import org.mentabean.sql.conditions.NotEquals;
import org.mentabean.sql.conditions.NotIn;
import org.mentabean.sql.functions.Avg;
import org.mentabean.sql.functions.Count;
import org.mentabean.sql.functions.Length;
import org.mentabean.sql.functions.Lower;
import org.mentabean.sql.functions.Substring;
import org.mentabean.sql.functions.Upper;
import org.mentabean.sql.operations.Add;
import org.mentabean.sql.param.DefaultParamHandler;
import org.mentabean.sql.param.Param;
import org.mentabean.sql.param.ParamField;
import org.mentabean.sql.param.ParamFunction;
import org.mentabean.sql.param.ParamHandler;
import org.mentabean.sql.param.ParamSubQuery;
import org.mentabean.sql.param.ParamValue;
import org.mentabean.util.PropertiesProxy;
import org.mentabean.util.SQLUtils;

public class QueryBuilderTest extends AbstractBeanSessionTest {

	public static class Country {
		
		private String name;

		public Country(String name) {
			this.name = name;
		}
		
		public Country() {
			
		}
		
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
		
	}
	
	public static class City {
		
		private int code;
		private String name;
		private Country country;
		
		public City(int code, String name, Country country) {
			this.code = code;
			this.name = name;
			this.country = country;
		}
		
		public City() {
			
		}
		
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}

		public int getCode() {
			return code;
		}

		public void setCode(int code) {
			this.code = code;
		}

		public Country getCountry() {
			return country;
		}

		public void setCountry(Country country) {
			this.country = country;
		}
	}
	
	public static class Company {
		
		private String id;
		private String name;
		private City city;
		private int employeesCount;
		
		public Company(String name) {
			this.name = name;
		}
		
		public Company() {
			
		}
		
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		}
		public City getCity() {
			return city;
		}

		public void setCity(City city) {
			this.city = city;
		}

		public int getEmployeesCount() {
			return employeesCount;
		}

		public void setEmployeesCount(int employeesCount) {
			this.employeesCount = employeesCount;
		}

		@Override
		public String toString() {
			return "Company [id=" + id + ", name=" + name + ", city=" + city
					+ "]";
		}

	}
	
	public static class Employee {
		
		private long number;
		private String name;
		private double salary;
		
		public Employee(long number, String name, double salary) {
			this.number = number;
			this.name = name;
			this.salary = salary;
		}

		public Employee() {
			
		}
		
		public long getNumber() {
			return number;
		}
		public void setNumber(long number) {
			this.number = number;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public double getSalary() {
			return salary;
		}
		public void setSalary(double salary) {
			this.salary = salary;
		}

		@Override
		public String toString() {
			return "\n"+number + " - " + name + " - " + salary;
		}
		
	}
	
	public static class Post {
		
		private Employee employee;
		private Company company;
		private String description;
		
		public Post(Employee employee, Company company, String description) {
			this.employee = employee;
			this.company = company;
			this.description = description;
		}
		
		public Post() {
			
		}
		
		public Employee getEmployee() {
			return employee;
		}
		public void setEmployee(Employee employee) {
			this.employee = employee;
		}
		public Company getCompany() {
			return company;
		}
		public void setCompany(Company company) {
			this.company = company;
		}
		public String getDescription() {
			return description;
		}
		public void setDescription(String description) {
			this.description = description;
		}

		@Override
		public String toString() {
			return "Post [employee=" + employee + ", company=" + company
					+ ", description=" + description + "]";
		}
		
	}
	
	private BeanManager configure() {
		
		BeanManager manager = new BeanManager();
		
		Company comPxy = PropertiesProxy.create(Company.class);
		manager.bean(Company.class, "company")
		.pk(comPxy.getId(), "idcompany", DBTypes.STRING)
		.field(comPxy.getCity().getCode(), "c_code", DBTypes.INTEGER)
		.field(comPxy.getName(), DBTypes.STRING);
		
		Employee empPxy = PropertiesProxy.create(Employee.class);
		manager.bean(Employee.class, "employee")
		.pk(empPxy.getNumber(), "idemployee", DBTypes.LONG)
		.field(empPxy.getName(), DBTypes.STRING)
		.field(empPxy.getSalary(), DBTypes.DOUBLE);
		
		Post postPxy = PropertiesProxy.create(Post.class);
		manager.bean(Post.class, "post")
		.pk(postPxy.getEmployee().getNumber(), "idemployee", DBTypes.LONG)
		.pk(postPxy.getCompany().getId(), "idcompany", DBTypes.STRING)
		.field(postPxy.getDescription(), DBTypes.STRING);
		
		City cityPxy = PropertiesProxy.create(City.class);
		manager.bean(City.class, "cities")
		.pk(cityPxy.getCode(), "city_code", DBTypes.INTEGER)
		.field(cityPxy.getCountry().getName(), "country_name", DBTypes.STRING)
		.field(cityPxy.getName(), "city_name", DBTypes.STRING);
		
		Country countryPxy = PropertiesProxy.create(Country.class);
		manager.bean(Country.class, "countries")
		.pk(countryPxy.getName(), "country_ident", DBTypes.STRING);
		
		return manager;
		
	}
	
	@Test
	public void test() {
		
		PreparedStatement ppst = null;
		
		try {
			
			Company comp = new Company();
			comp.setId("4356136");
			comp.setName("W3C");
			session.insert(comp);
			
			Company basicComp = session.createBasicInstance(comp);
			assertFalse(comp == basicComp);
			assertNotNull(basicComp.getId());
			assertNull(basicComp.getName());
			
			if (AnsiSQLBeanSession.DEBUG) {
				System.out.println("Company:             " + comp);
				System.out.println("Company (only pks):  " + basicComp);
			}
			
			Employee emp = new Employee();
			emp.setName("Érico");
			emp.setNumber(391);
			emp.setSalary(9999);
			session.insert(emp);
			
			Employee basicEmp = session.createBasicInstance(emp);
			assertNull(basicEmp.getName());
			assertEquals(0d, basicEmp.getSalary(), 0);
			assertEquals(emp.getNumber(), basicEmp.getNumber());
			
			Post post = new Post();
			post.setCompany(comp);
			post.setEmployee(emp);
			post.setDescription("Programmer");
			session.insert(post);
			
			QueryBuilder builder = session.buildQuery();
			Alias<Employee> e = builder.aliasTo(Employee.class, "emp");
			Alias<Company> c = builder.aliasTo(Company.class, "com");
			Alias<Post> p = builder.aliasTo(Post.class, "p");
			Alias<Employee> e2 = builder.aliasTo(Employee.class, "emp2");
			e2.setReturns(e2.proxy().getNumber());
			
			Query query = builder.select(p, c, e)
			.from(p)
			
			.join(c).pkOf(c).in(p)
			.join(e).pkOf(e).in(p)
			
			.where()
			.clause(new Substring(new ParamField(e, e.proxy().getName()))
				
				/*
				 * it's joke (only for showing that's possible to do that)
				 */
				.beginIndex(new ParamFunction(new Length(
						new ParamFunction(new Lower(
								new ParamFunction(new Upper(
										new ParamValue("c"))))))))
				.endIndex(new ParamFunction(new Length(
						new ParamFunction(new Lower(
								new ParamField(e, e.proxy().getName())))))))
				.condition(new Like(new ParamValue("%o"))).and()
				
			.clause(e.proxy().getNumber()).condition(
					new In(new ParamSubQuery(builder.subQuery()
							.select(e2)
							.from(e2)
							.where()
							.clause(new Substring(new ParamValue("teste")).endIndex(
									new ParamFunction(new Length(new ParamValue("CA")))))
									.condition(new Equals(new ParamValue("te")))
							))).and()
			
			.clause(new Lower(new ParamField(c, c.proxy().getName()))).condition( 
							new Equals(new ParamFunction(new Lower(new ParamValue("W3c")))))
			.orderBy().asc(p, p.proxy().getDescription())
			.limit(10);
			
			ppst = query.prepare();
			
			if (AnsiSQLBeanSession.DEBUG_NATIVE) {
				System.out.println("CUSTOM: "+ppst);
			}
			
			ResultSet rs = ppst.executeQuery();
			
			List<Post> list = new ArrayList<Post>();
			
			while (rs.next()) {
				
				Post pObj = new Post();
				p.populateBean(rs, pObj);
				c.populateBean(rs, pObj.getCompany());
				e.populateBean(rs, pObj.getEmployee());
				
				list.add(pObj);
			}
			
			assertEquals(1, list.size());
			assertNotNull(list.get(0));
			assertNotNull(list.get(0).getCompany());
			assertNotNull(list.get(0).getEmployee());
			
		}catch (Exception e) {
			
			throw new BeanException(e);
			
		}finally {
			
			SQLUtils.close(ppst);
		}
	}
	
	private BeanSession session;
	
	@Before
	public void setUp() {
		AnsiSQLBeanSession.DEBUG = false;
		AnsiSQLBeanSession.DEBUG_NATIVE = false;
		
		session = new H2BeanSession(configure(), getConnection());
		session.createTables();
		
		prepareData();
	}
	
	@After
	public void tearDown() {
		SQLUtils.close(session.getConnection());
	}
	
	public void prepareData() {

		Company comp;
		Employee emp;
		Post post;

		Country usa = new Country("United States");
		session.insert(usa);

		City ny = new City(123, "New York", usa);
		session.insert(ny);
		City sf = new City(1020, "San Francisco", usa);
		session.insert(sf);

		/*
		 * Companies
		 */
		comp = new Company();
		comp.setId("1");
		comp.setName("Google");
		comp.setCity(ny);
		session.insert(comp);

		comp = new Company();
		comp.setId("2");
		comp.setName("IBM");
		session.insert(comp);

		comp = new Company();
		comp.setId("3");
		comp.setName("Oracle");
		comp.setCity(sf);
		session.insert(comp);


		/*
		 * Google employees
		 */
		comp = session.loadList(new Company("Google")).get(0);

		emp = new Employee(19, "Maile Ohye", 19750);
		session.insert(emp);
		post = new Post(emp, comp, "Developer Programs Tech Lead");
		session.insert(post);

		emp = new Employee(12, "Ilya Grigorik", 10000);
		session.insert(emp);
		post = new Post(emp, comp, "Developer Advocate");
		session.insert(post);

		emp = new Employee(15, "Michael Manoochehri", 18100);
		session.insert(emp);
		post = new Post(emp, comp, "Developer Programs Engineer");
		session.insert(post);

		emp = new Employee(18, "Brian Cairns", 12500);
		session.insert(emp);
		post = new Post(emp, comp, "Software Engineer");
		session.insert(post);


		/*
		 * Oracle employees
		 */
		comp = session.loadList(new Company("Oracle")).get(0);

		emp = new Employee(2, "Lawrence J. Ellison", 38900);
		session.insert(emp);
		post = new Post(emp, comp, "Chief Executive Officer");
		session.insert(post);

		emp = new Employee(1, "Mark V. Hurd", 63250);
		session.insert(emp);
		post = new Post(emp, comp, "President");
		session.insert(post);

		/*
		 * No company employee
		 */
		emp = new Employee();
		emp.setName("No company");
		emp.setNumber(999);
		session.insert(emp);

	}
	
	/**
	 * Retrieve all employees
	 */
	@Test
	public void query01() throws Exception {

		QueryBuilder builder = session.buildQuery();
		Alias<Employee> e = builder.aliasTo(Employee.class, "emp");

		Query query = builder
				.select(e)
				.from(e);

		List<Employee> list = query.executeQuery();

		assertEquals(7, list.size());
	}
	
	/**
	 * Retrieve all employees which salary is greater then $15000 sorting by name
	 */
	@Test
	public void query02() throws Exception {

		QueryBuilder builder = session.buildQuery();
		Alias<Employee> e = builder.aliasTo(Employee.class, "emp");

		Query query = builder
				.select(e)
				.from(e)
				.where()
				.clause(e.proxy().getSalary())
				.condition(new GreaterThan(15000))
				.orderBy().asc(e, e.proxy().getName());

		List<Employee> list = query.executeQuery();

		assertEquals(4, list.size());
		assertEquals(list.get(0).getName(), "Lawrence J. Ellison"); //first
		assertEquals(list.get(3).getName(), "Michael Manoochehri"); //last
	}
	
	/**
	 * Retrieve all Google employees which name starts with "M" sorting by name
	 */
	@Test
	public void query03() throws Exception {

		QueryBuilder builder = session.buildQuery();
		Alias<Employee> e = builder.aliasTo(Employee.class, "emp");
		Alias<Company> c = builder.aliasTo(Company.class, "com");
		Alias<Post> p = builder.aliasTo(Post.class);

		Query query = builder
				.select(e)
				.from(e)
				.join(p).pkOf(e).in(p)
				.join(c).pkOf(c).in(p)
				.where()
				.clause(c.proxy().getName())
				.condition(new Equals("Google"))
				.and()
				.clause(e.proxy().getName())
				.condition(new Like("M%"))
				.orderBy().asc(e, e.proxy().getName());

		List<Employee> list = query.executeQuery();

		assertEquals(2, list.size());
		assertEquals(list.get(0).getName(), "Maile Ohye"); //first
		assertEquals(list.get(1).getName(), "Michael Manoochehri"); //last
	}
	
	/**
	 * Retrieve all Google employees which salary between 10000 and 15000 sorting by salary
	 */
	@Test
	public void query04() throws Exception {

		QueryBuilder builder = session.buildQuery();
		Alias<Employee> e = builder.aliasTo(Employee.class, "emp");
		Alias<Company> c = builder.aliasTo(Company.class, "com");
		Alias<Post> p = builder.aliasTo(Post.class);

		Query query = builder
				.select(e)
				.from(e)
				.join(p).pkOf(e).in(p)
				.join(c).pkOf(c).in(p)
				.where()
				.clause(c.proxy().getId())
				.condition(new Equals("1"))
				.and()
				.clause(e.proxy().getSalary())
				.condition(new Between(10000, 15000))
				.orderBy().asc(e, e.proxy().getSalary());

		List<Employee> list = query.executeQuery();

		assertEquals(2, list.size());
		assertEquals(list.get(0).getName(), "Ilya Grigorik"); //first
		assertEquals(list.get(1).getName(), "Brian Cairns"); //last
	}
	
	/**
	 * Retrieve all posts (with employees and companies) sorting by company name ASC and employee name DESC
	 */
	@Test
	public void query05() throws Exception {

		QueryBuilder builder = session.buildQuery();
		Alias<Employee> e = builder.aliasTo(Employee.class, "emp");
		Alias<Company> c = builder.aliasTo(Company.class, "com");
		Alias<Post> p = builder.aliasTo(Post.class);

		Query query = builder
				.select(p, e, c)
				.from(p)
				
				.join(e).pkOf(e).in(p)
				//here we set the bean where 'e' alias (employee) will be populate
				.inProperty(p.proxy().getEmployee())
				
				.join(c).pkOf(c).in(p)
				//here we set the bean where 'c' alias (company) will be populate				
				.inProperty(p.proxy().getCompany())
				
				.orderBy()
				.asc(c, c.proxy().getName())
				.desc(e, e.proxy().getName());

		List<Post> list = query.executeQuery();
		
		assertNotNull(list.get(0).getCompany().getName());
		assertEquals(6, list.size());
		assertEquals(list.get(0).getEmployee().getName(), "Michael Manoochehri"); //first
		assertEquals(list.get(0).getCompany().getName(), "Google"); //first
		assertEquals(list.get(5).getEmployee().getName(), "Lawrence J. Ellison"); //last
		assertEquals(list.get(5).getCompany().getName(), "Oracle"); //last
	}
	
	/**
	 * Retrieve all employees that have no company
	 */
	@Test
	public void query06() {
		
		QueryBuilder builder = session.buildQuery();
		Alias<Employee> e = builder.aliasTo(Employee.class, "emp");
		Alias<Employee> eSub = builder.aliasTo(Employee.class, "emp_sub");
		eSub.setReturns(eSub.proxy().getNumber());
		Alias<Post> p = builder.aliasTo(Post.class);
		
		Query query = builder
				.select(e)
				.from(e)
				.where()
				
				.clause(e.proxy().getNumber())
				
				//sub query in 'NOT IN' condition
				.condition(new NotIn(new ParamSubQuery(
						builder.subQuery()
						.select(eSub)
						.from(eSub)
						.join(p).pkOf(eSub).in(p))
				));
		
		List<Employee> list = query.executeQuery();
		
		assertEquals(1, list.size());
		assertEquals(list.get(0).getName(), "No company");
	}
	
	/**
	 * Retrieve all employees that have no company (same as above, just another way to build the SQL query)
	 */
	@Test
	public void query07() {
		
		QueryBuilder builder = session.buildQuery();
		Alias<Employee> e = builder.aliasTo(Employee.class, "emp");
		Alias<Post> p = builder.aliasTo(Post.class);
		
		Query query = builder
				.select(e)
				.from(e)
				.leftJoin(p).pkOf(e).in(p)
				.where()
				.clause(p.proxy().getDescription())
				
				//this will be converted to 'IS NULL' in query. See the Equals condition
				.condition(new Equals(null));
		
		List<Employee> list = query.executeQuery();
		
		assertEquals(1, list.size());
		assertEquals(list.get(0).getName(), "No company");
	}
	
	/**
	 * Retrieve all employees which name length is greater then 15 sorting by length ASC, name DESC
	 */
	@Test
	public void query08() {
		
		QueryBuilder builder = session.buildQuery();
		Alias<Employee> e = builder.aliasTo(Employee.class, "emp");
		
		Query query = builder
				.select(e)
				.from(e)
				.where()
				
				.clause(new Length(new ParamField(e, e.proxy().getName())))
				.condition(new GreaterThan(15))
				
				.orderBy()
				.asc(new ParamFunction(new Length(new ParamField(e, e.proxy().getName()))))
				.desc(e, e.proxy().getName());
		
		List<Employee> list = query.executeQuery();
		
		assertEquals(2, list.size());
		assertEquals(list.get(0).getName(), "Michael Manoochehri");
		assertEquals(list.get(1).getName(), "Lawrence J. Ellison");
	}
	
	/**
	 * Retrieve all companies with a count of employees sorting by company name ASC
	 */
	@Test
	public void query09() {
		
		QueryBuilder builder = session.buildQuery();
		Alias<Company> c = builder.aliasTo(Company.class, "c");
		Alias<Employee> e = builder.aliasTo(Employee.class, "e");
		Alias<Post> p = builder.aliasTo(Post.class, "p");
		
		Sentence count = new Sentence(new Count(new ParamField(e, e.proxy().getNumber())))
			.fromProperty(c.proxy().getEmployeesCount());
		
		Query query = builder
				.select(c)
				.add(count)
				.from(c)
				.leftJoin(p).pkOf(c).in(p)
				.leftJoin(e).pkOf(e).in(p)
				.groupBy(c)
				.orderBy()
				.asc(c, c.proxy().getName());
		
		List<Company> list = query.executeQuery();
		
		assertEquals(3, list.size());
		assertEquals(list.get(0).getName(), "Google");
		assertEquals(list.get(0).getEmployeesCount(), 4);
		assertEquals(list.get(1).getName(), "IBM");
		assertEquals(list.get(1).getEmployeesCount(), 0);
		assertEquals(list.get(2).getName(), "Oracle");
		assertEquals(list.get(2).getEmployeesCount(), 2);
	}
	
	/**
	 * Retrieve all companies having count of employees greater than 0 (zero) sorting by number of employees ASC
	 */
	@Test
	public void query10() {
		
		QueryBuilder builder = session.buildQuery();
		Alias<Company> c = builder.aliasTo(Company.class, "c");
		Alias<Employee> e = builder.aliasTo(Employee.class, "e");
		Alias<Post> p = builder.aliasTo(Post.class, "p");
		
		Sentence count = new Sentence(new Count(new ParamField(e, e.proxy().getNumber())))
			.fromProperty(c.proxy().getEmployeesCount());
		
		Query query = builder
				.select(c)
				.add(count)
				.from(c)
				.leftJoin(p).pkOf(c).in(p)
				.leftJoin(e).pkOf(e).in(p)
				.groupBy(c)
					.having()
					.clause(count)
					.condition(new GreaterThan(0))
				.orderBy()
				.asc(new ParamFunction(count));
		
		List<Company> list = query.executeQuery();
		
		assertEquals(2, list.size());
		assertEquals(list.get(0).getName(), "Oracle");
		assertEquals(list.get(0).getEmployeesCount(), 2);
		assertEquals(list.get(1).getName(), "Google");
		assertEquals(list.get(1).getEmployeesCount(), 4);
	}
	
	/**
	 * Retrieve all companies with a count of employees sorting by company name ASC
	 */
	@Test
	public void query11() {
		
		QueryBuilder builder = session.buildQuery();
		Alias<Company> c = builder.aliasTo(Company.class, "com");
		Alias<Employee> e = builder.aliasTo(Employee.class, "emp");
		Alias<Post> p = builder.aliasTo(Post.class, "p");
		Alias<Post> p2 = builder.aliasTo(Post.class, "p2");
		
		Query sub = builder.subQuery()
				.select(new Sentence(new Count(new ParamField(p2, p2.proxy().getDescription()))).name("count_sub"))
				.from(p2)
				.where()
				.clause(p2.proxy().getCompany().getId())
				.condition(new Equals(new ParamField(c, c.proxy().getId())));
		
		Query query = builder
				.select(p, c)
					.add(new Sentence(sub)
					.fromProperty(p.proxy().getCompany().getEmployeesCount()))
				.from(p)
				.leftJoin(c).pkOf(c).in(p)
				.inProperty(p.proxy().getCompany())
				.leftJoin(e).pkOf(e).in(p)
				.inProperty(p.proxy().getEmployee())
				.orderBy()
				.asc(c, c.proxy().getName());
		
		List<Post> list = query.executeQuery();
		
		assertEquals(6, list.size());
		assertEquals(list.get(0).getCompany().getName(), "Google");
		assertEquals(list.get(0).getCompany().getEmployeesCount(), 4);
		assertEquals(list.get(5).getCompany().getName(), "Oracle");
		assertEquals(list.get(5).getCompany().getEmployeesCount(), 2);
	}
	
	/**
	 * Retrieve all companies with name upper case
	 */
	@Test
	public void query12() {
		
		QueryBuilder builder = session.buildQuery();
		Alias<Company> c = builder.aliasTo(Company.class, "c");
		
		Sentence nameUpper = new Sentence(new Upper(new ParamField(c, c.proxy().getName())))
			.fromProperty(c.proxy().getName());
		
		Query query = builder
				.select(c)
				.add(nameUpper)
				.from(c)
				.orderBy()
				.asc(c, c.proxy().getName());
		
		List<Company> list = query.executeQuery();
		
		assertEquals(3, list.size());
		assertEquals(list.get(0).getName(), "GOOGLE");
		assertEquals(list.get(1).getName(), "IBM");
		assertEquals(list.get(2).getName(), "ORACLE");
	}
	
	/**
	 * Retrieve all companies with name upper case and a count of employees
	 */
	@Test
	public void query13() {
		
		QueryBuilder builder = session.buildQuery();
		Alias<Company> c = builder.aliasTo(Company.class, "c");
		c.setReturnMinus(c.proxy().getName());
		Alias<Employee> e = builder.aliasTo(Employee.class, "e");
		Alias<Post> p = builder.aliasTo(Post.class, "p");
		
		Sentence nameUpper = new Sentence(new Upper(new ParamField(c, c.proxy().getName())))
		.fromProperty(c.proxy().getName());
		
		Sentence count = new Sentence(new Count(new ParamField(e, e.proxy().getNumber())))
		.fromProperty(c.proxy().getEmployeesCount());
		
		Query query = builder
				.select(c)
				.add(count)
				.add(nameUpper)
				.from(c)
				.leftJoin(p).pkOf(c).in(p)
				.leftJoin(e).pkOf(e).in(p)
				.groupBy();
		
		List<Company> list = query.executeQuery();
		
		assertEquals(3, list.size());
		
		assertEquals("ORACLE", list.get(0).getName());
		assertEquals(2, list.get(0).getEmployeesCount());
		
		assertEquals("GOOGLE", list.get(1).getName());
		assertEquals(4, list.get(1).getEmployeesCount());
		
		assertEquals("IBM", list.get(2).getName());
		assertEquals(0, list.get(2).getEmployeesCount());
		
	}
	
	/**
	 * Retrieve all posts
	 */
	@Test
	public void query14() {
		
		QueryBuilder builder = session.buildQuery();
		Alias<Post> p = builder.aliasTo(Post.class, "p");
		
		Query query = builder.selectFrom(p);
		
		List<Post> list = query.executeQuery();
		
		assertEquals(6, list.size());
		for (Post post : list) {
			assertNotNull(post.getCompany());
			assertNotNull(post.getEmployee());
			assertNotNull(post.getCompany().getId());
			assertTrue(post.getEmployee().getNumber() > 0);
			assertNull(post.getCompany().getName());
			assertNull(post.getEmployee().getName());
		}
	}
	
	/**
	 * Retrieve all posts with company and employee using <code>executeQuery()</code> method
	 */
	@Test
	public void queryExecuteQuery() {
		
		QueryBuilder builder = session.buildQuery();
		Alias<Company> c = builder.aliasTo(Company.class, "c");
		Alias<Employee> e = builder.aliasTo(Employee.class, "e");
		Alias<Post> p = builder.aliasTo(Post.class, "p");
		
		Query query = builder
				.select(p, e, c)
				.from(p)
				.leftJoin(c).pkOf(c).in(p)
					.inProperty(p.proxy().getCompany())
				.leftJoin(e).pkOf(e).in(p)
					.inProperty(p.proxy().getEmployee());
		
		List<Post> list = query.executeQuery();
		
		assertEquals(6, list.size());
		for (Post post : list) {
			assertNotNull(post.getCompany());
			assertNotNull(post.getEmployee());
			assertNotNull(post.getCompany().getId());
			assertTrue(post.getEmployee().getNumber() > 0);
			assertNotNull(post.getCompany().getName());
			assertNotNull(post.getEmployee().getName());
		}
	}
	
	/**
	 * Retrieve all posts using <code>prepare()</code> and <code>populateBean</code> methods.
	 */
	@Test
	public void queryPrepare() {

		PreparedStatement ppst = null;

		try {

			QueryBuilder builder = session.buildQuery();
			Alias<Company> c = builder.aliasTo(Company.class, "c");
			Alias<Employee> e = builder.aliasTo(Employee.class, "e");
			Alias<Post> p = builder.aliasTo(Post.class, "p");

			Query query = builder
					.select(p, e, c)
					.from(p)
					.leftJoin(c).pkOf(c).in(p)
					.leftJoin(e).pkOf(e).in(p);
					
			List<Post> list = new ArrayList<Post>();
			ppst = query.prepare();
			ResultSet rs = ppst.executeQuery();
			Post obj;
			while (rs.next()) {
				obj = new Post();
				p.populateBean(rs, obj);
				
				if (obj.getCompany() != null) {
					c.populateBean(rs, obj.getCompany());
				}
				
				if (obj.getEmployee() != null) {
					e.populateBean(rs, obj.getEmployee());
				}
				list.add(obj);
			}

			assertEquals(6, list.size());
			
			for (Post post : list) {
				assertNotNull(post.getCompany());
				assertNotNull(post.getEmployee());
				assertNotNull(post.getCompany().getId());
				assertTrue(post.getEmployee().getNumber() > 0);
				assertNotNull(post.getCompany().getName());
				assertNotNull(post.getEmployee().getName());
			}
			
		}catch (Exception e) {
			throw new BeanException(e);
		}finally {
			SQLUtils.close(ppst);
		}
	}
	
	/**
	 * Retrieve all companies with a count of employees (manual way)
	 */
	@Test
	public void query15() {

		PreparedStatement ppst = null;

		try {
			QueryBuilder builder = session.buildQuery();
			Alias<Company> c = builder.aliasTo(Company.class, "c");
			Alias<Employee> e = builder.aliasTo(Employee.class, "e");
			Alias<Post> p = builder.aliasTo(Post.class, "p");

			Sentence count = new Sentence(new Count(
					new ParamField(e, e.proxy().getNumber())))
					.returnType(DBTypes.INTEGER)
					.name("count");

			Query query = builder
					.select(c)
					.add(count)
					.from(c)
					.leftJoin(p).pkOf(c).in(p)
					.leftJoin(e).pkOf(e).in(p)
					.groupBy();

			List<Company> list = new ArrayList<Company>();
			ppst = query.prepare();
			ResultSet rs = ppst.executeQuery();
			Company obj;

			while (rs.next()) {
				obj = new Company();
				c.populateBean(rs, obj);

				Integer employeesCount = query.getValueFromResultSet(rs, "count");
				obj.setEmployeesCount(employeesCount);

				list.add(obj);
			}

			assertEquals(3, list.size());

			assertEquals("Oracle", list.get(0).getName());
			assertEquals(2, list.get(0).getEmployeesCount());
			
			assertEquals("IBM", list.get(1).getName());
			assertEquals(0, list.get(1).getEmployeesCount());

			assertEquals("Google", list.get(2).getName());
			assertEquals(4, list.get(2).getEmployeesCount());

		}catch (Exception e) {
			throw new BeanException(e);
		}finally {
			SQLUtils.close(ppst);
		}
	}
	
	/**
	 * Returns only the count of posts in database
	 */
	@Test
	public void queryCount() {
		
		QueryBuilder builder = session.buildQuery();
		Alias<Post> p = builder.aliasTo(Post.class, "p");
		
		Sentence count = new Sentence(
				new Add()
				.param(new Count(new ParamField(p, p.proxy().getDescription())))
				.param(4))
		.name("count")
		.returnType(DBTypes.INTEGER);
		
		Query query = builder
				.select(count)
				.from(p);
		
		Integer postCount = query.executeSentence();
		
		assertEquals(6 + 4, (int)postCount);
	}
	
	/**
	 * Throws an exception because there are no sentences in query
	 */
	@Test (expected = BeanException.class)
	public void queryCountWithNoSentence() {
		
		QueryBuilder builder = session.buildQuery();
		Alias<Post> p = builder.aliasTo(Post.class, "p");
		
		Query query = builder
				.selectFrom(p);
		
		query.executeSentence();
	}
	
	/**
	 * Retrieve all posts where company or employe name contains 'O' and salary less than 50000, 
	 * order by post description
	 */
	@Test
	public void query16() {
		
		QueryBuilder builder = session.buildQuery();
		Alias<Post> p = builder.aliasTo(Post.class, "p");
		Alias<Employee> e = builder.aliasTo(Employee.class, "e");
		Alias<Company> c = builder.aliasTo(Company.class, "c");
		
		Query query = builder
				.select(p, e, c)
				.from(p)
				.leftJoin(c).pkOf(c).in(p)
					.inProperty(p.proxy().getCompany())
				.leftJoin(e).pkOf(e).in(p)
					.inProperty(p.proxy().getEmployee())
				.where()
				.openPar()
					.clause(c.proxy().getName())
					.condition(new Like("%O%"))
					.or()
					.clause(e.proxy().getName())
					.condition(new Like("%O%"))
				.closePar()
				.and()
				.clause(e.proxy().getSalary())
				.condition(new LessThan(50000))
				.orderBy()
				.asc(p, p.proxy().getDescription());
		
		List<Post> list = query.executeQuery();
		
		assertEquals(2, list.size());
		
		assertEquals("Lawrence J. Ellison", list.get(0).getEmployee().getName());
		assertEquals("Maile Ohye", list.get(1).getEmployee().getName());
	}
	
	/**
	 * Retrieve posts using parenthesis
	 */
	@Test
	public void queryParenthesis() {
		
		QueryBuilder builder = session.buildQuery();
		Alias<Company> c = builder.aliasTo(Company.class, "c");
		Alias<Employee> e = builder.aliasTo(Employee.class, "e");
		Alias<Post> p = builder.aliasTo(Post.class, "p");
		
		assertEquals(6, builder
				.select(p, e, c)
				.from(p)
				.leftJoin(c).on(c.proxy().getId()).eq(p.proxy().getCompany().getId())
					.eqProperty(p.proxy().getCompany())
				.leftJoin(e).on(e.proxy().getNumber()).eq(p.proxy().getEmployee().getNumber())
					.eqProperty(p.proxy().getEmployee())
				.where()
				.openPar()
				.clauseIf(false, c.proxy().getName())
				.condition(new Like("Google"))
				.or()
				.clauseIf(false, c.proxy().getName())
				.condition(new Like("Oracle"))
				.closePar()
				.executeQuery().size());
		
		assertEquals(6, builder
				.select(p, e, c)
				.from(p)
				.leftJoin(c).on(c.proxy().getId()).eq(p.proxy().getCompany().getId())
				.eqProperty(p.proxy().getCompany())
				.leftJoin(e).on(e.proxy().getNumber()).eq(p.proxy().getEmployee().getNumber())
				.eqProperty(p.proxy().getEmployee())
				.where()
				.openPar()
					.clauseIf(false, c.proxy().getName())
					.condition(new Like("Google"))
					.or()
					.clauseIf(true, c.proxy().getName())
					.condition(new Like("%"))
					.or()
					.clauseIf(false, c.proxy().getName())
					.condition(new Like("Oracle"))
				.closePar()
				.executeQuery().size());
		
		assertEquals(4, builder
				.select(p, e, c)
				.from(p)
				.leftJoin(c).on(c.proxy().getId()).eq(p.proxy().getCompany().getId())
				.eqProperty(p.proxy().getCompany())
				.leftJoin(e).on(e.proxy().getNumber()).eq(p.proxy().getEmployee().getNumber())
				.eqProperty(p.proxy().getEmployee())
				.where()
				.openPar()
					.clauseIf(true, c.proxy().getName())
					.condition(new Like("Google"))
					.or()
					.clauseIf(false, c.proxy().getName())
					.condition(new Like("Oracle"))
				.closePar()
				.and()
				.openPar()
					.clauseIf(false, c.proxy().getName())
					.condition(new Like("Oracle"))
				.closePar()
				.executeQuery().size());
		
		assertEquals(4, builder
				.select(p, e, c)
				.from(p)
				.leftJoin(c).on(c.proxy().getId()).eq(p.proxy().getCompany().getId())
				.eqProperty(p.proxy().getCompany())
				.leftJoin(e).on(e.proxy().getNumber()).eq(p.proxy().getEmployee().getNumber())
				.eqProperty(p.proxy().getEmployee())
				.where()
				.openPar()
					.clauseIf(true, c.proxy().getName())
					.condition(new Like("Google"))
					.or()
					.clauseIf(false, c.proxy().getName())
					.condition(new Like("Oracle"))
				.closePar()
				.and()
				.openPar()
					.clauseIf(false, c.proxy().getName())
					.condition(new Like("Oracle"))
					.and()
					.clauseIf(true, c.proxy().getName())
					.condition(new Like("Google"))
				.closePar()
				.executeQuery().size());
		
		assertEquals(4, builder
				.select(p, e, c)
				.from(p)
				.leftJoin(c).on(c.proxy().getId()).eq(p.proxy().getCompany().getId())
				.eqProperty(p.proxy().getCompany())
				.leftJoin(e).on(e.proxy().getNumber()).eq(p.proxy().getEmployee().getNumber())
				.eqProperty(p.proxy().getEmployee())
				.where()
				.openPar()
					.clauseIf(true, c.proxy().getName())
					.condition(new Like("Google"))
					.or()
					.clauseIf(false, c.proxy().getName())
					.condition(new Like("Oracle"))
				.closePar()
				.and()
				.openPar()
					.clauseIf(false, c.proxy().getName())
					.condition(new Like("Oracle"))
					.and()
					.clauseIf(false, c.proxy().getName())
					.condition(new Like("Google"))
				.closePar()
				.executeQuery().size());
		
		assertEquals(2, builder
				.select(p, e, c)
				.from(p)
				.leftJoin(c).on(c.proxy().getId()).eq(p.proxy().getCompany().getId())
				.eqProperty(p.proxy().getCompany())
				.leftJoin(e).on(e.proxy().getNumber()).eq(p.proxy().getEmployee().getNumber())
				.eqProperty(p.proxy().getEmployee())
				.where()
				.openPar()
					.clauseIf(false, c.proxy().getName())
					.condition(new Like("Google"))
					.or()
					.clauseIf(false, c.proxy().getName())
					.condition(new Like("Oracle"))
				.closePar()
				.or()
				.openPar()
					.clauseIf(true, c.proxy().getName())
					.condition(new Like("Oracle"))
					.and()
					.clauseIf(false, c.proxy().getName())
					.condition(new Like("Google"))
				.closePar()
				.executeQuery().size());
		
		assertEquals(6, builder
				.select(p, e, c)
				.from(p)
				.leftJoin(c).on(c.proxy().getId()).eq(p.proxy().getCompany().getId())
				.eqProperty(p.proxy().getCompany())
				.leftJoin(e).on(e.proxy().getNumber()).eq(p.proxy().getEmployee().getNumber())
				.eqProperty(p.proxy().getEmployee())
				.where()
				.openPar()
					.clauseIf(false, c.proxy().getName())
					.condition(new Like("Google"))
					.or()
					.clauseIf(false, c.proxy().getName())
					.condition(new Like("Oracle"))
				.closePar()
				.or()
				.openPar()
					.clauseIf(false, c.proxy().getName())
					.condition(new Like("Oracle"))
					.and()
					.clauseIf(false, c.proxy().getName())
					.condition(new Like("Google"))
				.closePar()
				.executeQuery().size());
		
		assertEquals(0, builder
				.select(p, e, c)
				.from(p)
				.leftJoin(c).on(c.proxy().getId()).eq(p.proxy().getCompany().getId())
				.eqProperty(p.proxy().getCompany())
				.leftJoin(e).on(e.proxy().getNumber()).eq(p.proxy().getEmployee().getNumber())
				.eqProperty(p.proxy().getEmployee())
				.where()
				.openPar()
					.clauseIf(true, c.proxy().getName())
					.condition(new Like("Google"))
					.or()
					.clauseIf(false, c.proxy().getName())
					.condition(new Like("Oracle"))
				.closePar()
				.and()
				.clause(c.proxy().getName())
				.condition(new Like("Oracle"))
				.executeQuery().size());
		
		assertEquals(4, builder
				.select(p, e, c)
				.from(p)
				.leftJoin(c).pkOf(c).in(p)
					.inProperty(p.proxy().getCompany())
				.leftJoin(e).pkOf(e).in(p)
					.inProperty(p.proxy().getEmployee())
				.where()
				.openPar()
				.clauseIf(true, c.proxy().getName())
				.condition(new Like("Google"))
				.or()
				.clauseIf(false, c.proxy().getName())
				.condition(new Like("Oracle"))
				.closePar()
				.executeQuery().size());
		
		assertEquals(2, builder
				.select(p, e, c)
				.from(p)
				.leftJoin(c).pkOf(c).in(p)
					.inProperty(p.proxy().getCompany())
				.leftJoin(e).pkOf(e).in(p)
					.inProperty(p.proxy().getEmployee())
				.where()
				.openPar()
				.clauseIf(false, c.proxy().getName())
				.condition(new Like("Google"))
				.or()
				.clauseIf(true, c.proxy().getName())
				.condition(new Like("Oracle"))
				.closePar()
				.executeQuery().size());
		
		assertEquals(0, builder
				.select(p, e, c)
				.from(p)
				.leftJoin(c).pkOf(c).in(p)
					.inProperty(p.proxy().getCompany())
				.leftJoin(e).pkOf(e).in(p)
					.inProperty(p.proxy().getEmployee())
				.where()
				.openPar()
				.clauseIf(true, c.proxy().getName())
				.condition(new Like("Google"))
				.and()
				.clauseIf(true, c.proxy().getName())
				.condition(new Like("Oracle"))
				.closePar()
				.executeQuery().size());
		
		assertEquals(6, builder
				.select(p, e, c)
				.from(p)
				.leftJoin(c).pkOf(c).in(p)
					.inProperty(p.proxy().getCompany())
				.leftJoin(e).pkOf(e).in(p)
					.inProperty(p.proxy().getEmployee())
				.where()
				.clauseIf(true, c.proxy().getName())
				.condition(new Like("Google"))
				.and()
				.openPar()
					.clauseIf(false, c.proxy().getName())
					.condition(new Like("Oracle"))
				.closePar()
				.or()
				.clauseIf(true, c.proxy().getName())
				.condition(new Like("Oracle"))
				.executeQuery().size());
		
		assertEquals(0, builder
				.select(p, e, c)
				.from(p)
				.leftJoin(c).pkOf(c).in(p)
					.inProperty(p.proxy().getCompany())
				.leftJoin(e).pkOf(e).in(p)
					.inProperty(p.proxy().getEmployee())
				.where()
				.clauseIf(true, c.proxy().getName())
				.condition(new Like("Google"))
				.and()
				.openPar()
					.openPar()
						.clauseIf(false, c.proxy().getName())
						.condition(new Like("Oracle"))
					.closePar()
					.or()
					.clauseIf(true, c.proxy().getName())
					.condition(new Like("Oracle"))
				.closePar()
				.executeQuery().size());
		
		assertEquals(6, builder
				.selectDistinct(p, e, c)
				.from(p)
				.leftJoin(c).pkOf(c).in(p)
					.inProperty(p.proxy().getCompany())
				.leftJoin(e).pkOf(e).in(p)
					.inProperty(p.proxy().getEmployee())
				.where()
				.openPar()
				.clauseIf(false, c.proxy().getName())
				.condition(new Like("Google"))
				.or()
				.clauseIf(false, c.proxy().getName())
				.condition(new Like("Oracle"))
				.closePar()
				.and()
				.clauseIf(false, e.proxy().getName())
				.condition(new Like("%"))
				.executeQuery().size());
		
		assertEquals(6, builder
				.selectDistinct(p, e, c)
				.from(p)
				.leftJoin(c).pkOf(c).in(p)
				.inProperty(p.proxy().getCompany())
				.leftJoin(e).pkOf(e).in(p)
				.inProperty(p.proxy().getEmployee())
				.where()
				.openPar()
				.clauseIf(true, c.proxy().getName())
				.condition(new Like("Google"))
				.or()
				.clauseIf(true, c.proxy().getName())
				.condition(new Like("Oracle"))
				.closePar()
				.and()
				.clauseIf(true, e.proxy().getName())
				.condition(new Like("%"))
				.executeQuery().size());
		
		assertEquals(4, builder
				.select(p, e, c)
				.from(p)
				.leftJoin(c).pkOf(c).in(p)
				.inProperty(p.proxy().getCompany())
				.leftJoin(e).pkOf(e).in(p)
				.inProperty(p.proxy().getEmployee())
				.where()
				.openPar()
					.openPar()
						.clauseIf(true, c.proxy().getName())
						.condition(new Like("Google"))
					.closePar()
					.and()
					.openPar()
						.clauseIf(false, c.proxy().getName())
						.condition(new Like("Oracle"))
						.or()
						.openPar()
							.clauseIf(true, c.proxy().getId())
							.condition(new NotEquals(null))
						.closePar()
					.closePar()
				.closePar()
				.and()
				.clauseIf(false, e.proxy().getName())
				.condition(new Like("%"))
				.limit(6)
				.executeQuery().size());
		
	}
	
	@Test
	public void limitNumberTest() {

		QueryBuilder builder = session.buildQuery();
		Alias<Company> c = builder.aliasTo(Company.class, "c");
		Alias<Employee> e = builder.aliasTo(Employee.class, "e");
		Alias<Post> p = builder.aliasTo(Post.class, "p");

		assertEquals(6, builder
				.selectDistinct(p, e, c)
				.from(p)
				.leftJoin(c).pkOf(c).in(p)
					.inProperty(p.proxy().getCompany())
				.leftJoin(e).pkOf(e).in(p)
					.inProperty(p.proxy().getEmployee())
				.where()
				.openPar()
				.clauseIf(false, c.proxy().getName())
				.condition(new Like("Google"))
				.or()
				.clauseIf(false, c.proxy().getName())
				.condition(new Like("Oracle"))
				.closePar()
				.limit(0)
				.executeQuery().size());
		
		assertEquals(3, builder
				.select(p, e, c)
				.from(p)
				.leftJoin(c).pkOf(c).in(p)
				.inProperty(p.proxy().getCompany())
				.leftJoin(e).pkOf(e).in(p)
				.inProperty(p.proxy().getEmployee())
				.where()
				.openPar()
				.clauseIf(false, c.proxy().getName())
				.condition(new Like("Google"))
				.or()
				.clauseIf(false, c.proxy().getName())
				.condition(new Like("Oracle"))
				.closePar()
				.limit(3)
				.executeQuery().size());
	}
	
	@Test
	public void paramHandlerTest() {
		
		QueryBuilder builder = session.buildQuery();
		Alias<Company> c = builder.aliasTo(Company.class, "c");
		Alias<Post> p = builder.aliasTo(Post.class, "p");
		
		ParamHandler paramHandler = new DefaultParamHandler();
		Param found;
		
		found = paramHandler.findBetter(builder, 3);
		Assert.assertTrue(found instanceof ParamValue);
		
		found = paramHandler.findBetter(builder, null);
		Assert.assertTrue(found instanceof ParamValue);
		
		found = paramHandler.findBetter(builder, builder.subQuery().selectFrom(c));
		Assert.assertTrue(found instanceof ParamSubQuery);
		
		builder.selectFrom(c).where().clause(c.proxy().getName()).condition(new NotEquals(null)).executeQuery();
		
		found = paramHandler.findBetter(builder, c.proxy().getName());
		Assert.assertTrue(found instanceof ParamField);	
		
		builder = builder.subQuery();
		
		builder.select(c, p)
			.from(c)
			.join(p).pkOf(c).in(p)
			.where().clause(p.proxy().getDescription())
			.condition(new NotEquals("aaaaaaaa"))
			.and()
			.clause(c.proxy().getName())
			.condition(new NotEquals("aaaaaaaaa"))
			.executeQuery();
		
		found = paramHandler.findBetter(builder, p.proxy().getDescription());
		Assert.assertTrue(found instanceof ParamField);	
		
		found = paramHandler.findBetter(builder, c.proxy().getId());
		Assert.assertTrue(found instanceof ParamField);	
		
		found = paramHandler.findBetter(builder, new Avg(null));
		Assert.assertTrue(found instanceof ParamFunction);	
	}
	
}
