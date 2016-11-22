package org.mentabean.jdbc.one_to_many;

import static org.junit.Assert.*;

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
import org.mentabean.util.OrderBy;
import org.mentabean.util.PropertiesProxy;
import org.mentabean.util.SQLUtils;

/**
 * This test illustrates all particularities of how MentaBean handles one-to-many relationships.
 * 
 * Engine can have zero or more parts (one-to-many relationship)
 * 
 * @author Sergio Oliveira Jr.
 */
public class OneToManyTest extends AbstractBeanSessionTest {
	
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
		
		Engine engine = PropertiesProxy.create(Engine.class);
		
		BeanConfig engineConfig = new BeanConfig(Engine.class, "engines")
			.pk(engine.getId(), DBTypes.AUTOINCREMENT)
			.field(engine.getName(), DBTypes.STRING);
		
		beanManager.addBeanConfig(engineConfig);
		
		Part part = PropertiesProxy.create(Part.class);
		
		BeanConfig partConfig = new BeanConfig(Part.class, "parts")
			.pk(part.getId(), DBTypes.AUTOINCREMENT)
			.field(part.getName(), DBTypes.STRING)
			.field(part.getEngine().getId(), "engine_id", DBTypes.INTEGER); // <=============== RELATIONSHIP !!!
		
		beanManager.addBeanConfig(partConfig);
		
		return beanManager;
	}
	
	@Test
	public void allTests() {
		
		// first insert the engine to which the parts will belong
		Engine engine = new Engine();
		engine.setName("MyEngine");
		
		session.insert(engine);
		
		int engineId = engine.getId();
		
		// now insert the parts, remembering to add the engine to which it belongs
		Part p1 = new Part();
		p1.setName("blah");
		p1.setEngine(engine);
		
		session.insert(p1);
		
		Part p2 = new Part();
		p2.setName("atah");
		p2.setEngine(engine);
		
		session.insert(p2);
		
		Part p3 = new Part();
		p3.setName("foo");
		p3.setEngine(engine);
		
		session.insert(p3);
		
		// now when you load the engine, its list of parts will be null (remember lazy loading always!)
		Engine e = new Engine(engineId);
		
		assertEquals(true, session.load(e));
		assertEquals("MyEngine", e.getName());
		assertEquals(null /* <==== */, e.getParts()); // lazy loading
		
		// now if you want to load the list of parts for this engine, you have to manually do so, whenever you want/need to...
		Part p = new Part();
		p.setEngine(e); // setting the engine with the correct id... now load a list... will load everything by the engine_id
		
		e.setParts(session.loadList(p)); // <====== MANUAL lazy loading...
		
		assertEquals(3, e.getParts().size());
		
		Part partProxy = PropertiesProxy.create(Part.class);

		// beautiful, when you load you can also order by if you want...
		e.setParts(session.loadList(p, OrderBy.get().asc(partProxy.getId())));
		assertEquals(3, e.getParts().size());
		assertEquals("blah", e.getParts().get(0).getName());
		assertEquals("atah", e.getParts().get(1).getName());
		assertEquals("foo", e.getParts().get(2).getName());
		
		e.setParts(session.loadList(p, OrderBy.get().asc(partProxy.getName())));
		assertEquals(3, e.getParts().size());
		assertEquals("atah", e.getParts().get(0).getName());
		assertEquals("blah", e.getParts().get(1).getName());
		assertEquals("foo", e.getParts().get(2).getName());
		
		e.setParts(session.loadList(p, OrderBy.get().desc(partProxy.getName())));
		assertEquals(3, e.getParts().size());
		assertEquals("foo", e.getParts().get(0).getName());
		assertEquals("blah", e.getParts().get(1).getName());
		assertEquals("atah", e.getParts().get(2).getName());
		
		e.setParts(session.loadList(p, OrderBy.get().asc(partProxy.getName()).asc(partProxy.getEngine().getId())));
		assertEquals(3, e.getParts().size());
		assertEquals("atah", e.getParts().get(0).getName());
		assertEquals("blah", e.getParts().get(1).getName());
		assertEquals("foo", e.getParts().get(2).getName());
	}
}	
