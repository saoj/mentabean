package org.mentabean.jdbc.one_to_one;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mentabean.BeanConfig;
import org.mentabean.BeanManager;
import org.mentabean.BeanSession;
import org.mentabean.DBTypes;
import org.mentabean.jdbc.AbstractBeanSessionTest;
import org.mentabean.jdbc.AnsiSQLBeanSession;
import org.mentabean.jdbc.H2BeanSession;
import org.mentabean.util.PropertiesProxy;
import org.mentabean.util.SQLUtils;

import static org.junit.Assert.*;

/**
 * This test illustrates all particularities of how MentaBean handles one-to-one relationships.
 * 
 * Post has one-and-only-one User
 * 
 * @author Sergio Oliveira Jr.
 */
public class OneToOneTest extends AbstractBeanSessionTest {
	
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
		
		Post post = PropertiesProxy.create(Post.class);
		
		BeanConfig postConfig = new BeanConfig(Post.class, "posts")
			.pk(post.getId(), DBTypes.AUTOINCREMENT)
			.field(post.getTitle(), DBTypes.STRING)
			.field(post.getUser().getId(), "user_id", DBTypes.INTEGER);
		
		beanManager.addBeanConfig(postConfig);
		
		return beanManager;
	}
	
	@Test
	public void testInsertOnlyAddOneBeanWithoutFKConstraint() {
		
		// when inserting a Post with a User, only the post is inserted. The User is never inserted.
		// a FK constraint can prevent that, in other words, a FK constraint will not allow a Post to be inserted when there is not an User for it.

		int userId = 23453;
		User user = new User(userId);
		user.setName("Sergio");
		
		Post post = new Post();
		post.setTitle("This is a test!");
		post.setUser(user);
		
		// if you had a FK configured, that would throw a database constraint exception!
		session.insert(post);
		
		int postId = post.getId();
		
		Post p = new Post(postId);
		
		assertEquals(true, session.load(p));
		assertEquals("This is a test!", p.getTitle());
		
		// user_id was inserted in the Post table:
		assertEquals(userId, p.getUser().getId());
		
		// but there is no User for this user_id
		userId = p.getUser().getId();
		User u = new User(userId);
		assertEquals(false, session.load(u));
		
		// you must manually insert the user first, then proceed to insert the post:
		u = new User();
		u.setName("Julia");
		session.insert(u);

		userId = u.getId(); // inserted id (PK)

		// now insert the Post
		p = new Post();
		p.setTitle("Another test!");
		p.setUser(u);
		session.insert(p);
		
		postId = p.getId(); // inserted id (PK)
		
		// now check the database:
		p = new Post(postId);
		assertEquals(true, session.load(p));
		assertEquals("Another test!", p.getTitle());
		assertEquals(userId, p.getUser().getId());
		
		// MentaBean always use lazy loading, so the user from the recently loaded post will not be populated!
		assertEquals(null, p.getUser().getName());
		
		// lazy load:
		assertEquals(true, session.load(p.getUser()));
		
		// now it is here!
		assertEquals("Julia", p.getUser().getName());
		
		// and of course the user is in the database!
		u = new User(userId);
		assertEquals(true, session.load(u));
		assertEquals(userId, u.getId());
		assertEquals("Julia", u.getName());
	}
	
	@Test
	public void testLazyLoading() {
		
		// one-to-one is always lazy loaded...
		
		User user = new User();
		user.setName("Patricia");
		
		session.insert(user);
		
		int userId = user.getId();
		
		Post post = new Post();
		post.setTitle("Hello there!");
		post.setUser(user);
		
		session.insert(post);
		
		int postId = post.getId();
		
		// now load the post:
		Post p = new Post(postId);
		
		assertEquals(true, session.load(p));
		assertEquals(postId, p.getId());
		assertEquals("Hello there!", p.getTitle());
		
		// now the user_id is there but nothing else !!!
		assertEquals(userId, p.getUser().getId());
		assertEquals(null /* <===== */, p.getUser().getName()); // IT WAS NOT LOADED
		
		// you have to manually load the dependencies (manual lazy loading)
		// NOTE: This is just one line of code
		assertEquals(true, session.load(p.getUser()));
		
		// and now of couse the name is there:
		assertEquals("Patricia", p.getUser().getName());
	}
}