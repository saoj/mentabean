package org.mentabean.jdbc;

import java.sql.Connection;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;
import org.mentabean.BeanConfig;
import org.mentabean.BeanException;
import org.mentabean.BeanManager;
import org.mentabean.BeanSession;
import org.mentabean.DBTypes;
import org.mentabean.jdbc.QueryBuilder.Alias;
import org.mentabean.jdbc.QueryBuilder.Query;
import org.mentabean.sql.Sentence;
import org.mentabean.sql.conditions.Equals;
import org.mentabean.sql.conditions.GreaterThan;
import org.mentabean.sql.conditions.GreaterThanEquals;
import org.mentabean.sql.conditions.In;
import org.mentabean.sql.conditions.Like;
import org.mentabean.sql.functions.Count;
import org.mentabean.sql.functions.Lower;
import org.mentabean.sql.param.ParamField;
import org.mentabean.sql.param.ParamFunction;
import org.mentabean.sql.param.ParamSubQuery;
import org.mentabean.sql.param.ParamValue;
import org.mentabean.util.PropertiesProxy;
import org.mentabean.util.SQLUtils;

public class AdvancedQueryBuilderTest extends AbstractBeanSessionTest {

	public static class Company {
		
		private String id;
		private long number;
		private String name;
		private int employeeCount;
		
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
		public long getNumber() {
			return number;
		}
		public void setNumber(long number) {
			this.number = number;
		}
		public int getEmployeeCount() {
			return employeeCount;
		}
		public void setEmployeeCount(int employeeCount) {
			this.employeeCount = employeeCount;
		}
		@Override
		public String toString() {
			return "Company [id=" + id + ", number=" + number + ", name="
					+ name + ", employeeCount=" + employeeCount + "]";
		}
		
	}
	
	public static class Employee {
		
		private Company company;
		private long number;
		private String name;
		private double salary;
		
		public Company getCompany() {
			return company;
		}
		public void setCompany(Company company) {
			this.company = company;
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
			return "Employee [company=" + company + ", number=" + number
					+ ", name=" + name + ", salary=" + salary + "]";
		}
		
	}
	
	public static class Post {
		
		private Employee employee;
		private Company company;
		private String description;
		
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
			return "=== POST "+description+" ===\n"+
					company+"\n"+employee;
		}
		
	}
	
	private BeanManager configure() {
		
		BeanManager manager = new BeanManager();
		
		Company comPxy = PropertiesProxy.create(Company.class);
		BeanConfig comConf = new BeanConfig(Company.class, "company")
		.pk(comPxy.getId(), DBTypes.STRING)
		.pk(comPxy.getNumber(), DBTypes.LONG)
		.field(comPxy.getName(), DBTypes.STRING);
		manager.addBeanConfig(comConf);
		
		Employee empPxy = PropertiesProxy.create(Employee.class);
		BeanConfig employeeConf = new BeanConfig(Employee.class, "employee")
		.pk(empPxy.getNumber(), DBTypes.LONG)
		.pk(empPxy.getCompany().getId(), "idcompany", DBTypes.STRING)
		.pk(empPxy.getCompany().getNumber(), "ncompany", DBTypes.LONG)
		.field(empPxy.getName(), DBTypes.STRING)
		.field(empPxy.getSalary(), DBTypes.DOUBLE);
		manager.addBeanConfig(employeeConf);
		
		Post postPxy = PropertiesProxy.create(Post.class);
		BeanConfig postConf = new BeanConfig(Post.class, "post")
		.pk(postPxy.getEmployee().getNumber(), "number_employee", DBTypes.LONG)
		.pk(postPxy.getEmployee().getCompany().getId(), "idcompany_employee", DBTypes.STRING)
		.pk(postPxy.getEmployee().getCompany().getNumber(), "ncompany_employee", DBTypes.LONG)
		.pk(postPxy.getCompany().getId(), "idcompany", DBTypes.STRING)
		.pk(postPxy.getCompany().getNumber(), "ncompany", DBTypes.LONG)
		.field(postPxy.getDescription(), DBTypes.STRING);
		manager.addBeanConfig(postConf);
		
		return manager;
		
	}
	
	@Test
	public void test() {
		
		final BeanManager manager = configure();
		final Connection conn = getConnection();
		
		try {
			
			AnsiSQLBeanSession.DEBUG = false;
			AnsiSQLBeanSession.DEBUG_NATIVE = false;
			
			BeanSession session = new H2BeanSession(manager, conn);
			session.createTables();
			
			Company comp = new Company();
			comp.setId("4356136");
			comp.setName("W3C");
			comp.setNumber(1);
			session.insert(comp);
			
			Company basicComp = session.createBasicInstance(comp);
			Assert.assertFalse(comp == basicComp);
			Assert.assertNotNull(basicComp.getId());
			Assert.assertNotNull(basicComp.getNumber());
			Assert.assertNull(basicComp.getName());
			
			if (AnsiSQLBeanSession.DEBUG) {
				System.out.println("Company:             " + comp);
				System.out.println("Company (only pks):  " + basicComp);
			}
			
			Employee emp = new Employee();
			emp.setCompany(comp);
			emp.setName("Érico");
			emp.setNumber(391);
			emp.setSalary(9999);
			session.insert(emp);
			
			Employee basicEmp = session.createBasicInstance(emp);
			Assert.assertNull(basicEmp.getName());
			Assert.assertNotNull(basicEmp.getCompany());
			Assert.assertEquals(0d, basicEmp.getSalary());
			Assert.assertEquals(emp.getNumber(), basicEmp.getNumber());
			Assert.assertEquals(emp.getCompany().getId(), basicEmp.getCompany().getId());
			Assert.assertEquals(emp.getCompany().getNumber(), basicEmp.getCompany().getNumber());
			
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
			
			Sentence sum = new Sentence(
					new ParamFunction(new Count(new ParamField(e, e.proxy().getNumber()))))
					.returnType(DBTypes.LONG).fromProperty(p.proxy().getCompany().getEmployeeCount());
			
			Query query = builder.select(p, c, e).add(sum)
			.from(p)
			
			.join(c)
			.on(c.proxy().getId())
			.eq(p.proxy().getCompany().getId())
			.and(c.proxy().getNumber())
			.eq(p.proxy().getCompany().getNumber())
			.eqProperty(p.proxy().getCompany())
			
			.join(e)
			.on(e.proxy().getNumber())
			.eq(p.proxy().getEmployee().getNumber())
			.and(e.proxy().getCompany().getId())
			.eq(p.proxy().getCompany().getId())
			.eqProperty(p.proxy().getEmployee())
			.and(e.proxy().getCompany().getNumber())
			.eq(p.proxy().getCompany().getNumber())
			
			.where()
			
			.clauseIf(true, e.proxy().getName())
				.condition(new Like(new ParamValue("%o"))).and()
			.clauseIf(false, new Lower(new ParamField(c, c.proxy().getName()))).condition(new Equals(new ParamValue(null))).and()
			.clauseIf(false, e.proxy().getNumber()).condition(
					new In(new ParamSubQuery(builder.subQuery().select(e2).from(e2)
							.where().clause(e.proxy().getNumber()).condition(
									new GreaterThan(new ParamValue(391)))))).and()
			.clause(new Sentence(builder.subQuery().select(e2).from(e2).limit(1))).condition(new GreaterThan(1))
			.groupBy()
			.having().clause(sum).condition(new GreaterThanEquals(1))
			
			.orderBy().asc(p, p.proxy().getDescription())
			.limit(10);
			
			List<Post> list = query.executeQuery();
			
			for (Post reg : list) {
				Assert.assertNotNull(reg.getCompany());
				Assert.assertNotNull(reg.getCompany().getName());
				Assert.assertNotNull(reg.getEmployee().getName());
			}
			
		}catch (Exception e) {
			
			throw new BeanException(e);
			
		}finally {
			
			SQLUtils.close(conn);
		}
	}
	
}
