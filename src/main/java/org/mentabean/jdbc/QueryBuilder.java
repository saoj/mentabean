package org.mentabean.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.mentabean.BeanConfig;
import org.mentabean.BeanException;
import org.mentabean.DBField;
import org.mentabean.sql.Condition;
import org.mentabean.sql.HasParams;
import org.mentabean.sql.Sentence;
import org.mentabean.sql.param.DefaultParamHandler;
import org.mentabean.sql.param.Param;
import org.mentabean.sql.param.ParamHandler;
import org.mentabean.sql.param.ParamValue;
import org.mentabean.util.PropertiesProxy;
import org.mentabean.util.SQLUtils;

/**
 * Fluent QueryBuilder useful to create SQL queries
 * 
 * @author margel
 * @author erico
 *
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class QueryBuilder {

	private static final String REGEX = "(AND|OR|\\([\\s]*?\\)|WHERE|HAVING)[\\s]*?$";
	private static final String NO_CLAUSE_REGEX = ".*(WHERE|HAVING|\\()[\\s]*?$";
	private StringBuilder sb = new StringBuilder();
	private final AnsiSQLBeanSession session;
	private List<Object> paramValues = new ArrayList<Object>();
	private List<Alias<?>> selectAliases;
	private List<Alias<?>> createdAliases = new ArrayList<Alias<?>>();
	private Map<String, Sentence> sentences = new HashMap<String, Sentence>();
	private Alias aliasFrom;
	private int parenthesis = 0;
	private boolean clauseIf;
	private ParamHandler paramHandler;

	protected QueryBuilder(final AnsiSQLBeanSession session) {
		this.session = session;
		this.paramHandler = new DefaultParamHandler();
	}
	
	protected QueryBuilder(final AnsiSQLBeanSession session, ParamHandler paramHandler) {
		this.session = session;
		this.paramHandler = paramHandler;
	}
	
	public List<Alias<?>> getCreatedAliases() {
		return createdAliases;
	}
	
	/**
	 * Builds an initial <i>SELECT</i> statement with given aliases
	 * @param as - Alias(es) with properties that will be retrieved
	 * @return A new <code>Select</code> object
	 */
	public Select select(Alias<?>... as){

		return new Select(null, as);
	}
	
	public Select selectDistinct(Alias<?>... as){
		
		return new Select("DISTINCT", as);
	}
	
	/**
	 * Builds an initial <i>SELECT alias FROM alias</i> statement.<br>
	 * Same as <code>select(alias).from(alias)</code>
	 * @param as - Alias with properties that will be retrieved
	 * @return A new <code>From</code> object
	 * @see #select(Alias...)
	 * @see Select#from(Alias)
	 */
	public From selectFrom(Alias<?> as){
		
		return select(as).from(as);
	}
	
	public From selectDistinctFrom(Alias<?> as){
		
		return new Select("DISTINCT", as).from(as);
	}
	
	/**
	 * Builds an initial <i>SELECT</i> statement with given sentences
	 * @param sentences - Sentence(s) to insert in <i>SELECT</i> clause
	 * @return A new <code>Select</code> object
	 */
	public Select select(Sentence... sentences){
		
		return new Select(null, sentences);
	}
	
	public Select selectDistinct(Sentence... sentences){
		
		return new Select("DISTINCT", sentences);
	}

	/**
	 * Creates a new QueryBuilder with the same session. It's useful to build sub-queries from the main query
	 * @return The new QueryBuilder instance
	 */
	public QueryBuilder subQuery() {
		QueryBuilder qb = new QueryBuilder(session);
		//qb.selectAliases = selectAliases;
		qb.createdAliases = createdAliases;
		return qb;
	}

	/**
	 * Creates an alias to be used in this QueryBuilder
	 * 
	 * @param clazz - Bean class that will be mapped to a BeanConfig 
	 * @param alias - String indicating the alia's name
	 * @return - The alias object
	 */
	public <T> Alias<T> aliasTo(Class<? extends T> clazz, String alias){

		return new Alias<T>(clazz, alias);
	}

	/**
	 * Creates an alias to be used in this QueryBuilder. The alia's name is the simple name of the bean class
	 * 
	 * @param clazz - Bean class that will be mapped to a BeanConfig 
	 * @return - The alias object
	 */
	public <T> Alias<T> aliasTo(Class<? extends T> clazz){

		return new Alias<T>(clazz, clazz.getSimpleName().toLowerCase());
	}

	private void appendTable(Alias<?> a) {

		sb.append(a.config.getTableName()).append(' ').append(a.aliasStr);
	}

	private void append(Param param) {

		if (param != null)
			sb.append(param.paramInQuery());
		add(param);
	}
	
	private void add(Object param) {
		
		if (param instanceof HasParams) {
			HasParams hasParams = (HasParams) param;
			Param[] params = hasParams.getParams();
			if (params != null) {
				for (Param p : params) {
					add(p);
				}
			}
		}else {
			Param p = paramHandler.findBetter(this, param);
			if (p != null && p.values() != null && p.values().length > 0) {
				
				for (Object value : p.values()) {
					if (value != null) {
						paramValues.add(value);
					}
				}
			}
		}
	}
	
	private void applyRegex() {
		remove(REGEX);
	}
	
	private void remove(String regex) {
	
		sb = new StringBuilder(sb.toString().replaceAll(regex, ""));
		
	}
	
	private void addAnd() {
		if (clauseIf) {
			if (sb.toString().matches(NO_CLAUSE_REGEX)) {
				return;
			}
			applyRegex();
			sb.append(" AND ");
		}
	}
	
	private void addOr() {
		if (clauseIf) {
			if (sb.toString().matches(NO_CLAUSE_REGEX)) {
				return;
			}
			applyRegex();
			sb.append(" OR ");
		}
	}
	
	private void openPar() {
		parenthesis++;
		sb.append('(');
	}
	
	private void closePar() {
		applyRegex();
		parenthesis--;
		sb.append(')');
		applyRegex();
		clauseIf = true;
	}
	
	private Alias<?> findAlias() {
		
		Object[] instances = PropertiesProxy.getBeanInstances();
		if (instances == null || instances.length == 0) throw new IllegalStateException("Cannot find proxy instance!");
		
		Object instance = instances[0];
		
		//TODO PropertiesProxy.addBeanInstance(instance);
		
		for (Alias<?> a : createdAliases) {
			if (a.pxy() == instance)
				return a;
		}
		
		throw new IllegalStateException("Cannot find alias for " + instance);
	}
	
	public class Alias<T> {

		private BeanConfig config;
		private String aliasStr;
		private T pxy;
		private String[] returns;
		private String[] returnMinus;
		private Map<Key, Alias> joined = new HashMap<Key, Alias>();

		private Alias(Class<? extends T> clazz, String aliasStr) {

			this.aliasStr = aliasStr;
			this.config = session.getConfigFor(clazz);

			if (this.config == null)
				throw new BeanException("BeanConfig not found for "+clazz);
			
			this.pxy = (T) PropertiesProxy.create(config.getBeanClass());
			
			createdAliases.add(this);
		}

		/**
		 * Defines the properties that will return. In other words, <b>only</b> these properties will be populated
		 * @param returns
		 */
		public void setReturns(Object... returns){

			this.returns = AnsiSQLBeanSession.getProperties(returns);
		}

		/**
		 * Defines the properties that will <b>NOT</b> return. In other words, these properties will be not populated 
		 * @param returns
		 */
		public void setReturnMinus(Object... returns){

			this.returnMinus = AnsiSQLBeanSession.getProperties(returns);
		}

		/**
		 * Returns the proxy for bean class
		 * @return The proxy
		 */
		public T pxy(){

			return pxy;
		}

		/**
		 * Convert the given property to database column 
		 * @param property - The bean property (can be through proxy)
		 * @return The database column name
		 */
		public String toColumn(Object property){

			return session.propertyToColumn(config.getBeanClass(), property, aliasStr);
		}

		/**
		 * Populates the bean according to ResultSet
		 * @param rs
		 * @param bean
		 */
		public void populateBean(ResultSet rs, T bean){

			session.populateBeanImpl(rs, bean, aliasStr, returns, returnMinus, false);
		}
		
		public void populateAll(ResultSet rs, T bean) {

			populateBean(rs, bean);

			for (Map.Entry<Key, Alias> m : joined.entrySet()) {
				
				//only if alias is in SELECT clause
				if (selectAliases.contains(m.getValue())) {
					
					Object value = session.getPropertyBean(bean, m.getKey().property, m.getKey().forceInstance);
					
					if (value != null) {
						m.getValue().populateAll(rs, value);
					}
				}

			}
		}
		
		@Override
		public String toString() {
			return "Alias "+aliasStr+" of "+config.getBeanClass();
		}
		
		/**
		 * Configures a property name to receive data from alias. When <code>forceIntance</code> is <b>true</b> 
		 * it will force the creation of a new instance for the property, in other words, 
		 * the value will never be <code>null</code>
		 * @param property - Bean property to populate
		 * @param forceInstance
		 * @param alias - Alias
		 */
		private void put(Object property, boolean forceInstance, Alias<?> alias) {
			joined.put(new Key().property(property).forceInstance(forceInstance), alias);
		}
		
		private class Key {
			
			private String property;
			private boolean forceInstance;
			
			public Key property(Object property) {
				this.property = AnsiSQLBeanSession.getProperties(new Object[] {property})[0];
				//this.property = PropertiesProxy.getPropertyName();
				return this;
			}
			
			public Key forceInstance(boolean force) {
				this.forceInstance = force;
				return this;
			}
		}

	}

	public class Select implements Appendable<Select> {

		private boolean init = false;
		
		private Select(String str) {
			sb.append("SELECT "+(str == null || str.isEmpty() ? "" : str+" "));
			selectAliases = new ArrayList<Alias<?>>();
		}
		
		private Select(String str, Alias<?>... as) {
			this(str);
			this.add(as);
		}
		
		private Select(String str, Sentence... sentences) {
			this(str);
			this.add(sentences);
		}
		
		/**
		 * Add the sentences in <code>SELECT</code> clause
		 * @param sentences
		 * @return this
		 */
		public Select add(Sentence... sentences) {
			
			for (Sentence s : sentences) {
				
				if (s.getName() == null || s.getName().isEmpty())
					throw new BeanException("The sentence ("+s.build()+") in SELECT clause must have a name");
				
				if(init)
					sb.append(",");
				
				sb.append(s.build()).append(' ').append(s.getName());
				QueryBuilder.this.sentences.put(s.getName(), s);
				QueryBuilder.this.add(s);
				init = true;
			}
			
			return this;
		}
		
		/**
		 * Add the alias columns in <code>SELECT</code> clause
		 * @param as
		 * @return this
		 */
		public Select add(Alias<?>... as) {

			for (Alias<?> alias: as) {

				if(init)
					sb.append(",");

				selectAliases.add(alias);
				sb.append(session.buildSelectImpl(alias.config.getBeanClass(), alias.aliasStr, alias.returns, alias.returnMinus, false, true));
				init = true;
			}
			
			return this;
		}

		/**
		 * Creates the <b>FROM</b> keyword for given alias appending the table name in SQL query. 
		 * @param alias
		 * @return A new <code>From</code> object
		 */
		public From from(Alias<?> alias){

			return new From(alias);
		}

		@Override
		public Select append(Param p) {

			QueryBuilder.this.append(p);
			return this;
		}
	}

	public class From extends Query implements CanOrder, CanLimit, CanGroupBy, Appendable<From> {

		private From(Alias<?> alias) {
			
			aliasFrom = alias;
			sb.append(" FROM ").append(alias.config.getTableName()).append(" ").append(alias.aliasStr);
		}

		private From() {}

		/**
		 * Creates a <b>LEFT JOIN</b> sentence using the given alias
		 * <br><br>
		 * <b>E.g.:</b>
		 * <ul>
		 * <code>
		 * 	builder.select(...)<br><ul>
		 * 		.from(a)<br>
		 * 		.leftJoin(b)<br>
		 * 		...</ul>
		 * </code>
		 * </ul>
		 * 
		 * @param a - The alias to join
		 * @return a new {@link On} object
		 */
		public On leftJoin(Alias<?> a){
			return join(a, "LEFT JOIN");
		}

		/**
		 * Creates a <b>RIGHT JOIN</b> sentence using the given alias
		 * <br><br>
		 * <b>E.g.:</b>
		 * <ul>
		 * <code>
		 * 	builder.select(...)<br><ul>
		 * 		.from(a)<br>
		 * 		.rightJoin(b)<br>
		 * 		...</ul>
		 * </code>
		 * </ul>
		 * 
		 * @param a - The alias to join
		 * @return a new {@link On} object
		 */
		public On rightJoin(Alias<?> a){
			return join(a, "RIGHT JOIN");
		}

		/**
		 * Creates a <b>JOIN</b> sentence using the given alias
		 * <br><br>
		 * <b>E.g.:</b>
		 * <ul>
		 * <code>
		 * 	builder.select(...)<br><ul>
		 * 		.from(a)<br>
		 * 		.join(b)<br>
		 * 		...</ul>
		 * </code>
		 * </ul>
		 * 
		 * @param a - The alias to join
		 * @return a new {@link On} object
		 */
		public On join(Alias<?> a){
			return join(a, "JOIN");
		}
		
		/**
		 * Creates a join using the given join type
		 * @param a - Alias to join
		 * @param joinType - The join type (E.g: <code>"CROSS JOIN"</code> or <code>"LEFT OUTER JOIN"</code>)
		 * @return a new {@link On} object
		 */
		public On join(Alias<?> a, String joinType) {
			sb.append(" "+joinType+" ");
			appendTable(a);
			return new On(a, true);			
		}

		public Where where() {
			return new Where(true);
		}

		@Override
		public Order orderBy() {

			return new Order();
		}

		@Override
		public Limit limit(Object lim) {

			return new Limit(lim);
		}
		
		@Override
		public From append(Param p) {

			QueryBuilder.this.append(p);
			return this;
		}

		@Override
		public GroupBy groupBy(Alias<?>... aliases) {
			return new GroupBy(aliases);
		}

		@Override
		public GroupBy groupByProp(Alias<?> alias, Object... properties) {
			return new GroupBy(alias, properties);
		}

		@Override
		public GroupBy groupBy() {
			return new GroupBy();
		}

		@Override
		public GroupBy groupBy(Param... p) {
			return new GroupBy(p);
		}
	}

	/**
	 * Class representing a 'pos-join' operation
	 * @author erico
	 *
	 */
	public class On {

		private Alias<?> aliasPK;

		private On(Alias<?> aliasPK, boolean init) {

			sb.append(init ? " ON " : " AND ");
			this.aliasPK = aliasPK;
		}

		public OnEquals on(Object property) {
			sb.append(aliasPK.toColumn(property));
			return new OnEquals(aliasPK);
		}

		public UsingPK pkOf(Alias<?> alias) {
			return new UsingPK(alias);
		}

	}

	public class UsingPK {

		private Alias<?> aliasPK;
		
		private UsingPK(Alias<?> aliasPK) {
			this.aliasPK = aliasPK;
		}

		public PopulateUsing in(Alias<?> aliasFK) {
			buildOn(aliasPK, aliasFK);
			return new PopulateUsing(aliasPK, aliasFK);
		}
		
		private void buildOn(Alias<?> aPK, Alias<?> aFK){

			Iterator<DBField> pks = aPK.config.pks();
			DBField df = pks.next();

			sb.append(aPK.aliasStr).append(".").append(df.getDbName()).append(" = ")
			.append(aFK.aliasStr).append(".").append(df.getDbName());

			while (pks.hasNext()){

				df = pks.next();

				sb.append(" AND ").append(aPK.aliasStr).append(".").append(df.getDbName())
				.append(" = ").append(aFK.aliasStr).append(".").append(df.getDbName());

			}
		}
	}
	
	public class PopulateUsing extends From {
		
		private Alias<?> aliasPK, aliasFK;
		
		public PopulateUsing(Alias<?> aliasPK, Alias<?> aliasFK) {
			this.aliasFK = aliasFK;
			this.aliasPK = aliasPK;
		}
		
		/**
		 * Defines the property of foreign bean <b>(specified on {@link UsingPK#in(Alias)} method)</b> 
		 * that will receive the value from alias PK <b>(specified on {@link On#pkOf(Alias)} method)</b> and
		 * force the creation of a new instance if bean property is not set.
		 * <br><br>
		 * <b>E.g.:</b>
		 * <ul>
		 * <code>
		 * 	builder.select(user, city)<br><ul>
		 * 		.from(user)<br>
		 * 		.join(city)<br>
		 * 		.pkOf(city).in(user)<br>
		 * 		<b>.inPropertyForcingInstance(user.pxy().getCity())</b><br>
		 * 		...</ul>
		 * </code>
		 * </ul>
		 * @param propertyBean
		 * @return this
		 * @see Alias#put(Object, boolean, Alias)
		 */
		public From inPropertyForcingInstance(Object propertyBean) {
			aliasFK.put(propertyBean, true, aliasPK);
			return this;
		}
		
		/**
		 * Defines the property of foreign bean <b>(specified on {@link UsingPK#in(Alias)} method)</b> 
		 * that will receive the value from alias PK <b>(specified on {@link On#pkOf(Alias)} method)</b>
		 * <br><br>
		 * <b>E.g.:</b>
		 * <ul>
		 * <code>
		 * 	builder.select(user, city)<br><ul>
		 * 		.from(user)<br>
		 * 		.join(city)<br>
		 * 		.pkOf(city).in(user)<br>
		 * 		<b>.inProperty(user.pxy().getCity())</b><br>
		 * 		...</ul>
		 * </code>
		 * </ul>
		 * @param propertyBean
		 * @return this
		 * @see Alias#put(Object, boolean, Alias)
		 */
		public From inProperty(Object propertyBean) {
			aliasFK.put(propertyBean, false, aliasPK);
			return this;
		}
		
		/**
		 * Defines the property of primary bean <b>(specified on {@link On#pkOf(Alias)} method)</b> 
		 * that will receive the value from foreign alias (aliasFK) <b>(specified on {@link UsingPK#in(Alias)} method)</b> and
		 * force the creation of a new instance if bean property is not set.
		 * <br><br><b>Note: </b>The <b>pkProperty</b> is generally used in 1x1 relationship.
		 * <br><br>
		 * <b>E.g.:</b>
		 * <ul>
		 * <code>
		 * 	builder.select(person, identity)<br><ul>
		 * 		.from(person)<br>
		 * 		.join(identity)<br>
		 * 		.pkOf(person).in(identity)<br>
		 * 		<b>.pkPropertyForcingInstance(person.pxy().getIdentity())</b><br>
		 * 		...</ul>
		 * </code>
		 * </ul>
		 * @param propertyBean
		 * @return this
		 * @see Alias#put(Object, boolean, Alias)
		 */
		public From pkPropertyForcingInstance(Object propertyBean) {
			aliasPK.put(propertyBean, true, aliasFK);
			return this;
		}
		
		/**
		 * Defines the property of primary bean <b>(specified on {@link On#pkOf(Alias)} method)</b> 
		 * that will receive the value from foreign alias (aliasFK) <b>(specified on {@link UsingPK#in(Alias)} method)</b>
		 * <br><br><b>Note: </b>The <b>pkProperty</b> is generally used in 1x1 relationship.
		 * <br><br>
		 * <b>E.g.:</b>
		 * <ul>
		 * <code>
		 * 	builder.select(person, identity)<br><ul>
		 * 		.from(person)<br>
		 * 		.join(identity)<br>
		 * 		.pkOf(person).in(identity)<br>
		 * 		<b>.pkProperty(person.pxy().getIdentity())</b><br>
		 * 		...</ul>
		 * </code>
		 * </ul>
		 * @param propertyBean
		 * @return this
		 * @see Alias#put(Object, boolean, Alias)
		 */
		public From pkProperty(Object propertyBean) {
			aliasPK.put(propertyBean, false, aliasFK);
			return this;
		}
	}
	
	public class OnEquals {

		private Alias<?> aliasPK;

		private OnEquals(Alias<?> aliasPK) {
			sb.append('=');
			this.aliasPK = aliasPK;
		}

		/**
		 * Equals the given property to the property defined in <b>on</b> method
		 * <br><br>
		 * <b>E.g.:</b>
		 * <ul>
		 * <code>
		 * 	builder.select(a, b)<br><ul>
		 * 		.from(a)<br>
		 * 		.join(b)<br>
		 * 		.on(b.pxy().getSomething())<br>
		 * 		<b>.eq(a.pxy().getSomething())</b><br>
		 * 		...</ul>
		 * </code>
		 * </ul>
		 * @param property
		 * @return A new <code>Equals</code> object
		 */
		public Equals eq(Object property) {
			
			Alias<?> alias = findAlias();
			
			sb.append(alias.toColumn(property));
			return new Equals(this.aliasPK, alias);
		}
		
	}
	
	public class Equals extends From {

		private Alias<?> aliasFK, aliasPK;

		private Equals(Alias<?> aliasPK, Alias<?> aliasFK) {
			this.aliasPK = aliasPK;
			this.aliasFK = aliasFK;
		}
		
		/**
		 * Defines the property of bean <b>specified as alias on {@link OnEquals#eq(Alias, Object)} method</b> 
		 * that will receive the value from alias <b>specified on {@link From#join(Alias)} method</b> and
		 * force the creation of a new instance if bean property is not set.
		 * <br><br>
		 * <b>E.g.:</b>
		 * <ul>
		 * <code>
		 * 	builder.select(user, city)<br><ul>
		 * 		.from(user)<br>
		 * 		.join(city)<br>
		 * 		.on(city.pxy().getId())<br>
		 * 		.eq(user, user.pxy().getCity().getId())<br>
		 * 		<b>.inPropertyForcingInstance(user.pxy().getCity())</b><br>
		 * 		...</ul>
		 * </code>
		 * </ul>
		 * @param propertyBean
		 * @return this
		 * @see Alias#put(Object, boolean, Alias)
		 */
		public Equals eqPropertyForcingInstance(Object propertyBean) {
			aliasFK.put(propertyBean, true, aliasPK);
			return this;
		}
		
		/**
		 * Defines the property of bean <b>specified as alias on {@link OnEquals#eq(Alias, Object)} method</b> 
		 * that will receive the value from alias <b>specified on {@link From#join(Alias)} method</b>
		 * <br><br>
		 * <b>E.g.:</b>
		 * <ul>
		 * <code>
		 * 	builder.select(user, city)<br><ul>
		 * 		.from(user)<br>
		 * 		.join(city)<br>
		 * 		.on(city.pxy().getId())<br>
		 * 		.eq(user, user.pxy().getCity().getId())<br>
		 * 		<b>.inProperty(user.pxy().getCity())</b><br>
		 * 		...</ul>
		 * </code>
		 * </ul>
		 * @param propertyBean
		 * @return this
		 * @see Alias#put(Object, boolean, Alias)
		 */
		public Equals eqProperty(Object propertyBean) {
			aliasFK.put(propertyBean, false, aliasPK);
			return this;
		}

		public OnEquals and(Object property) {
			return new On(aliasPK, false).on(property);
		}
	}

	public class Where implements Appendable<Where>, HasInitClause<InitClauseWhere> {

		private Where (boolean init) {
			if (init) {
				sb.append(" WHERE ");
			}
		}
		
		@Override
		public InitClauseWhere clauseIf(boolean clauseIf, Object param) {
			QueryBuilder.this.clauseIf = clauseIf;
			return new InitClauseWhere(param);
		}

		@Override
		public Where append(Param p) {

			QueryBuilder.this.append(p);
			return this;
		}

		@Override
		public InitClauseWhere clause(Object param) {
			return clauseIf(true, param);
		}

		/**
		 * Insert a left parenthesis '(' in query
		 * @return this
		 */
		public Where openPar() {
			QueryBuilder.this.openPar();
			return this;
		}

	}

	public class InitClauseWhere extends InitClause implements Appendable<InitClauseWhere>, HasEndClause<EndClauseWhere> {

		private InitClauseWhere(Object param) {
			
			super(param);
		}
		
		@Override
		public InitClauseWhere append(Param p) {

			QueryBuilder.this.append(p);
			return this;
		}

		@Override
		public EndClauseWhere condition(String condition) {

			return new EndClauseWhere(condition);
		}

		@Override
		public EndClauseWhere condition(Condition condition) {

			return new EndClauseWhere(condition);
		}

	}

	public class InitClauseHaving extends InitClause implements Appendable<InitClauseHaving>, HasEndClause<EndClauseHaving> {
		
		private InitClauseHaving(Object param) {
			
			super(param);
		}

		@Override
		public InitClauseHaving append(Param p) {

			QueryBuilder.this.append(p);
			return this;
		}

		@Override
		public EndClauseHaving condition(String condition) {

			return new EndClauseHaving(condition);
		}

		@Override
		public EndClauseHaving condition(Condition condition) {

			return new EndClauseHaving(condition);
		}

	}

	public class InitClause {
		
		private InitClause (Object param) {
			
			Param p = paramHandler.findBetter(QueryBuilder.this, param);
			
			if (clauseIf) {
//				System.out.println(p.getClass()+" - "+p.paramInQuery());
//				if (p.values() != null)
//				for (Object o : p.values()) {
//					if (o != null)
//					System.out.println("type: "+o.getClass());
//				}
					
				add(p);
				sb.append(' ').append(p.paramInQuery());
			}
			
		}

	}

	public class EndClauseWhere extends EndClause implements Appendable<EndClauseWhere>, CanGroupBy {

		private EndClauseWhere(Condition condition) {

			if (clauseIf) {
				add(condition);
				init(condition.build());
			}
		}

		private EndClauseWhere(String condition) {

			if (clauseIf) {
				init(condition);
			}
		}
		
		/**
		 * Insert a left parenthesis '(' in query
		 * @return this
		 */
		public EndClauseWhere openPar() {
			QueryBuilder.this.openPar();
			return this;
		}
		
		/**
		 * Insert a right parenthesis ')' in query
		 * @return this
		 */
		public EndClauseWhere closePar() {
			QueryBuilder.this.closePar();
			return this;
		}

		public Where and() {
			
			addAnd();
			return new Where(false);
		}

		public Where or() {
			
			addOr();
			return new Where(false);
		}

		@Override
		public EndClauseWhere append(Param p) {

			QueryBuilder.this.append(p);
			return this;
		}

		@Override
		public GroupBy groupBy(Alias<?>... aliases) {
			return new GroupBy(aliases);
		}
		
		@Override
		public GroupBy groupBy(Param... params) {
			return new GroupBy(params);
		}

		@Override
		public GroupBy groupByProp(Alias<?> alias, Object... properties) {
			return new GroupBy(alias, properties);
		}

		@Override
		public GroupBy groupBy() {
			return new GroupBy();
		}

	}

	public class EndClauseHaving extends EndClause implements Appendable<EndClauseHaving> {

		private EndClauseHaving(Condition condition) {

			if (clauseIf) {
				add(condition);
				init(condition.build());
			}
		}

		private EndClauseHaving(String condition) {

			if (clauseIf) {
				init(condition);
			}
		}
		
		/**
		 * Insert a left parenthesis '(' in query
		 * @return this
		 */
		public EndClauseHaving openPar() {
			
			QueryBuilder.this.openPar();
			return this;
		}
		
		/**
		 * Insert a right parenthesis ')' in query
		 * @return this
		 */
		public EndClauseHaving closePar() {
			
			QueryBuilder.this.closePar();
			return this;
		}

		public Having and() {
			
			addAnd();
			return new Having(false);
		}

		public Having or() {
			
			addOr();
			return new Having(false);
		}

		@Override
		public EndClauseHaving append(Param p) {

			QueryBuilder.this.append(p);
			return this;
		}

	}

	public abstract class EndClause extends Query implements CanOrder, CanLimit {

		protected void init(String condition) {

			sb.append(' ').append(condition);
		}

		@Override
		public Order orderBy() {

			return new Order();
		}

		@Override
		public Limit limit(Object lim) {

			return new Limit(lim);
		}
		
	}

	public class Limit extends Query implements CanOrder, Appendable<Limit> {

		private Limit(Object lim) {

			Param param = paramHandler.findBetter(QueryBuilder.this, lim);
			
			if (param != null) {
				applyRegex();
				add(param);
				
				Object numberObj = paramValues.get(paramValues.size()-1);
				if (numberObj instanceof Number) {
					Number numberLimit = (Number) numberObj;
					if (numberLimit.longValue() <= 0) {
						paramValues.remove(paramValues.size()-1);
						return;
					}
				}
				
				sb.append(" LIMIT ").append(param.paramInQuery());
			}
		}
		
		@Override
		public Order orderBy() {

			return new Order();
		}

		public Offset offset(Integer offset) {

			return new Offset(offset);
		}
		
		public Offset offset(Param param) {
			
			return new Offset(param);
		}

		@Override
		public Limit append(Param p) {

			QueryBuilder.this.append(p);
			return this;
		}
	}

	public class Offset extends Query implements Appendable<Offset> {

		public Offset(Number offset) {
			this(offset != null && offset.longValue() > 0 ? new ParamValue(offset) : null);
		}
		
		public Offset(Param param) {
			
			if (param != null) {
				add(param);
				sb.append(" OFFSET ").append(param.paramInQuery());
			}
		}

		public Order orderBy() {

			return new Order();
		}

		@Override
		public Offset append(Param p) {

			QueryBuilder.this.append(p);
			return this;
		}

	}

	public class Order implements Appendable<Order> {

		private Order() {

			applyRegex();
			sb.append(" ORDER BY ");			
		}
		
		public Ordering asc(Param param) {
			
			return new Ordering().asc(param);
		}
		
		public Ordering desc(Param param) {
			
			return new Ordering().desc(param);
		}

		public Ordering asc(Alias<?> alias, Object... properties){

			return new Ordering().asc(alias, properties);
		}

		public Ordering desc(Alias<?> alias, Object... properties){
			
			return new Ordering().desc(alias, properties);
		}
		
		@Override
		public Order append(Param p) {

			QueryBuilder.this.append(p);
			return this;
		}
	}
	
	public class Ordering extends Query implements CanLimit, Appendable<Ordering> {
		
		private boolean alreadyOrder = false;
		
		private void initOrder() {
			
			if (alreadyOrder) {
				sb.append(',');
			}
			
			alreadyOrder = true;
		}
		
		public Ordering asc(Param param) {
			
			initOrder();
			add(param);
			sb.append(param.paramInQuery()).append(" ASC ");
			return this;
		}
		
		public Ordering desc(Param param) {
			
			initOrder();
			add(param);
			sb.append(param.paramInQuery()).append(" DESC ");
			return this;
		}

		public Ordering asc(Alias<?> alias, Object... properties){
			iterateOrderBy(" ASC ", alias, properties);
			return this;
		}

		public Ordering desc(Alias<?> alias, Object... properties){
			iterateOrderBy(" DESC ", alias, properties);
			return this;
		}
		
		@Override
		public Limit limit(Object lim) {

			return new Limit(lim);
		}
		
		private void iterateOrderBy(String orderType, Alias<?> alias, Object[] properties){

			String[] props = AnsiSQLBeanSession.getProperties(properties);

			initOrder();

			for(String prop : props){

				sb.append(alias.toColumn(prop)).append(orderType).append(",");
			}

			sb.setCharAt(sb.length()-1, ' ');
		}

		@Override
		public Ordering append(Param p) {
			QueryBuilder.this.append(p);
			return this;
		}
	}

	public class GroupBy extends Query implements Appendable<GroupBy>, CanLimit, CanOrder {

		private void init() {
			
			applyRegex();
			sb.append(!sb.toString().endsWith(" GROUP BY ") ? " GROUP BY " : ",");
		}

		private GroupBy(Alias<?> alias, Object... properties) {
			init();
			add(alias, properties);
		}

		private GroupBy(Alias<?>... alias) {
			init();
			add(alias);
		}
		
		private GroupBy(Param... params) {
			init();
			add(params);
		}
		
		private GroupBy() {
			init();
			add(selectAliases.toArray(new Alias<?>[0]));
		}

		public GroupBy add(Alias<?> alias, Object... properties) {
			
			if (!sb.toString().endsWith(" GROUP BY ")) {
				sb.append(',');
			}
			
			sb.append(session.buildSelectImpl(alias.config.getBeanClass(), alias.aliasStr, 
					AnsiSQLBeanSession.getProperties(properties), null, false, false));
			
			return this;
		}

		public GroupBy add(Alias<?>... aliases) {
			
			for (Alias<?> alias : aliases) {
				
				if (!sb.toString().endsWith(" GROUP BY "))
					sb.append(',');
				
				sb.append(session.buildSelectImpl(alias.config.getBeanClass(), alias.aliasStr, 
						alias.returns, alias.returnMinus, false, false));
			}
			
			return this;
		}
		
		public GroupBy add(Param... params) {
			
			for (Param p : params) {
				if (!sb.toString().endsWith(" GROUP BY "))
					sb.append(',');
				
				append(p);
			}
			
			return this;
		}

		public Having having() {
			return new Having(true);
		}

		@Override
		public GroupBy append(Param p) {
			QueryBuilder.this.append(p);
			return this;
		}

		@Override
		public Order orderBy() {
			return new Order();
		}

		@Override
		public Limit limit(Object lim) {
			return new Limit(lim);
		}

	}

	public class Having extends Query implements Appendable<Having>, HasInitClause<InitClauseHaving>{

		private Having (boolean init) {
			if (init) {
				sb.append(" HAVING ");
			}
		}

		@Override
		public InitClauseHaving clauseIf(boolean clauseIf, Object param){
			
			QueryBuilder.this.clauseIf = clauseIf;
			return new InitClauseHaving(param);
		}
		
		@Override
		public Having append(Param p) {
			QueryBuilder.this.append(p);
			return this;
		}

		@Override
		public InitClauseHaving clause(Object param) {
			return clauseIf(true, param);
		}

		/**
		 * Insert a left parenthesis '(' in query
		 * @return this
		 */
		public Having openPar() {
			QueryBuilder.this.openPar();
			return this;
		}

	}

	public interface HasInitClause<T extends InitClause> {

		/**
		 * Insert the param as a clause in query (same of <code>clauseIf(true, param)</code>).
		 * <p>
		 * <b>E.g.:</b>
		 * <ul>
		 * <code>
		 * 		...<br>
		 * 		.where()<br>
		 * 		.clause(new ParamNative("anything"))<br>
		 * 		.condition(...)<br>
		 * 		...<br>
		 * 
		 * </code>
		 * </ul>
		 * </p>
		 * @param param
		 */
		public T clause(Object param);
		
		/**
		 * Insert the param as a clause in query if and only if the flag <b>clauseIf</b> is <b>true</b>
		 * @param clauseIf - Flag indicating if this clause will be inserted in SQL query
		 * @param param
		 * @see #clause(Param)
		 */
		public T clauseIf(boolean clauseIf, Object param);
		
	}

	public interface HasEndClause<T extends EndClause> {

		public T condition(String condition);

		public T condition(Condition condition);

	}

	public interface CanOrder {

		public Order orderBy();
	}

	public interface CanLimit {

		public Limit limit(Object lim);
	}

	public interface CanGroupBy {

		public GroupBy groupBy(Alias<?>... aliases);
		
		public GroupBy groupBy(Param... p);

		public GroupBy groupByProp(Alias<?> alias, Object... properties);
		
		/**
		 * Group by all aliases fields used in <i>SELECT</i> clause
		 * @return this
		 */
		public GroupBy groupBy();
		
	}

	public interface Appendable<T> {

		/**
		 * Appends the parameter directly in query
		 * @param p - Param
		 * @return this
		 */
		public T append(Param p);
	}
	
	public void finish() {
		
		for (Alias<?> a : selectAliases) {
			a.joined.clear();
		}
		
		paramValues.clear();
		sb = new StringBuilder();			
	}
	
	/**
	 * Represents a query ready to execute
	 * @author erico
	 *
	 */
	public class Query {

		private Query() {}
		
		/**
		 * Returns a <code>PreparedStatement</code> setting all given parameters in order. 
		 * 
		 * @param params
		 * @return A <code>PreparedStatement</code> using this session connection
		 * @see #getSQL()
		 * @see SQLUtils#prepare(java.sql.Connection, String, Object...)
		 */
		private PreparedStatement prepare(Object... params) {

			try {

				PreparedStatement ppst = SQLUtils.prepare(session.getConnection(), getSQL(), params);
				
				if (AnsiSQLBeanSession.DEBUG_NATIVE) {
					System.out.println("CUSTOM QUERY (NATIVE): "+ppst);
				}
				
				return ppst;
				
			} catch (SQLException e) {

				throw new BeanException("Error preparing statement", e);
			}
		}
		
		/**
		 * Prepares a statement with paramValues to execute the query manually.
		 * @return A <code>PreparedStatement</code> using this session connection
		 */
		public PreparedStatement prepare() {

			return prepare(paramValues.toArray());
		}
		
		/**
		 * Executes the query returning a <code>List</code> of beans declared in <b>FROM</b> clause.
		 * 
		 * @return A list containing all beans retrieved by <code>ResultSet</code>
		 * @see Alias#populateAll(ResultSet, Object)
		 */
		public <T> List<T> executeQuery() {
			
			PreparedStatement ppst = null;
			
			try {
				ppst = prepare();
				
				ResultSet rs = ppst.executeQuery();
				
				List<T> list = new ArrayList<T>();
				T bean;
				
				while (rs.next()) {
					
					bean = (T) aliasFrom.config.getBeanClass().newInstance();
					aliasFrom.populateAll(rs, bean);
					
					for (Sentence s : sentences.values()) {
						session.injectValue(bean, s.getProperty(),
								s.getValue(rs), s.getReturnType().getTypeClass());
					}
					
					list.add(bean);
				}
				
				return list;
				
			} catch (Exception e) {
				
				throw new BeanException("Unable to execute query from QueryBuilder\n"+
						e.getMessage(), e);
			}finally {
				
				finish();
				
				SQLUtils.close(ppst);
			}
		}
		
		/**
		 * Executes the query returning a single value according returnType of sentence in query.
		 * @return The value returned by query
		 */
		public <T> T executeSentence() {

			if (sentences.values().size() != 1)
				throw new BeanException("This query must have exactly one sentence to execute");
			
			PreparedStatement ppst = null;

			try {
				ppst = prepare();

				ResultSet rs = ppst.executeQuery();

				if (rs.next()) {

					if (!rs.isLast()) {
						throw new BeanException("The query returns more than one result");
					}
					Sentence s = sentences.values().iterator().next();
					return (T) s.getValue(rs);

				}

				return null;

			} catch (Exception e) {

				throw new BeanException("Unable to execute sentence from QueryBuilder\n"+
						e.getMessage(), e);
			}finally {
				
				finish();

				SQLUtils.close(ppst);
			}
		}
		
		public <T> T getValueFromResultSet(ResultSet rs, String name) throws SQLException {
			
			Sentence s = sentences.get(name);
			
			if (s == null)
				throw new BeanException("The sentence name '"+name+"' is not included in query");
			
			return (T) s.getValue(rs);
		}
		
		/**
		 * Returns the SQL generated by QueryBuilder
		 * @return The String SQL
		 */
		public String getSQL() {

			if (parenthesis != 0) {
				throw new BeanException("Invalid parenthesis");
			}
			
			applyRegex();
			
			if (AnsiSQLBeanSession.DEBUG) {
				System.out.println("CUSTOM QUERY: "+sb.toString());
			}

			return sb.toString();
		}
		
		public List<Object> getParamValues() {
			return paramValues;
		}
	
	}
	
}
