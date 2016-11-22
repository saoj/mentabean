package org.mentabean.jdbc.one_to_one_plus_one_to_many;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

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

/**
 * This test illustrates all particularities of how MentaBean handles one-to-one relationship in one direction plus one-to-many relationship
 * in the other direction.
 * 
 * Ex: Post has one-and-only-one User, but User of course can have many different Posts.
 * 
 * @author Sergio Oliveira Jr.
 */
public class OneToOnePlusOneToManyTest extends AbstractBeanSessionTest {
	
	private BeanSession session;
	
	@Before
	public void setUp() {
		
		AnsiSQLBeanSession.debugSql(false);
		AnsiSQLBeanSession.debugNativeSql(false);
		
		session = new H2BeanSession(configure(), getConnection());
		session.createTables();
		
		populate();
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
	
	private void populate() {
		
		{
			User user1 = new User();
			user1.setName("Sergio");
			
			session.insert(user1);
			
			Post post1 = new Post();
			post1.setTitle("MyPost1");
			post1.setUser(user1);
			
			session.insert(post1);
			
			Post post2 = new Post();
			post2.setTitle("MyPost2");
			post2.setUser(user1);
			
			session.insert(post2);
			
			Post post3 = new Post();
			post3.setTitle("MyPost3");
			post3.setUser(user1);
			
			session.insert(post3);
		}
		
		{
			User user1 = new User();
			user1.setName("Julia");
			
			session.insert(user1);
			
			Post post1 = new Post();
			post1.setTitle("SuperPost1");
			post1.setUser(user1);
			
			session.insert(post1);
			
			Post post2 = new Post();
			post2.setTitle("SuperPost2");
			post2.setUser(user1);
			
			session.insert(post2);
			
			Post post3 = new Post();
			post3.setTitle("SuperPost3");
			post3.setUser(user1);
			
			session.insert(post3);
		}
	}
	
	@Test
	public void testOneToOneRelationship() {
		
		// get a post by title
		Post post = new Post();
		post.setTitle("SuperPost2");
		
		post = session.loadUnique(post);
		
		// post has one user, check it:
		assertNotNull(post.getUser());
		assertTrue(post.getUser().getId() > 0);
		assertNull(post.getUser().getName());
		
		// now load it (manual lazy loading)
		assertEquals(true, session.load(post.getUser()));
		assertEquals("Julia", post.getUser().getName());
	}
	
	@Test
	public void testOneToManyRelationship() {
		
		User user = new User();
		user.setName("Sergio");
		
		user = session.loadUnique(user);
		
		// user has MANY posts... load them
		
		// right now it has nothing of course
		assertEquals(null, user.getPosts());
		
		Post post = new Post();
		post.setUser(user);
		
		List<Post> posts = session.loadList(post);
		
		assertEquals(3, posts.size());
		
		// add it to our User object:
		user.setPosts(posts);
		
		assertEquals(3, user.getPosts().size()); // just to illustrate
	}
}