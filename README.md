# MentaBean
Simple ORM and Query Builder

## Quick Tutorial

For the subsequent recipes, we'll be using the object below:
```Java
public class User {

	private int id;
	private String name;

	public User() { /* mandatory */ }

	public User(int id) {
	  this.id = id;
	}

	// getters and setters here...
}
```

#### 1. Mapping an object to a database table
```Java
User user = PropertiesProxy.create(User.class);
BeanConfig userConfig = new BeanConfig(User.class, "users") // table name is "users"
  .pk(user.getId(), "user_id", DBTypes.AUTOINCREMENT) // "id" property maps to "user_id" column
  .field(user.getName(), "username", DBTypes.STRING); // "name" property maps to "username" column
```

#### 2. Loading an object by its PK
```Java
User user = new User(12); // PK = 12
if (beanSession.load(user)) {
	System.out.println("Loaded user with name: " + user.getName());
} else {
	System.out.println("Could not find an user with id: " + user.getId());
}
```

#### 3. Inserting an object
```Java
User user = new User(); // no PK (it will be created upon insertion)
user.setName("Sergio");

beanSession.insert(user); // throws exception if cannot insert for any reason

System.out.println("Inserted new user in the database with id: " + user.getId());
```

#### 4. Updating an object
```Java
User user = new User(123);
if (beanSession.load(user)) { // always a good practice to update a loaded object
	user.setName("Julia"); // changing the name to something else...
	if (beanSession.update(user)) {
		System.out.println("Rows updated: " + rows); // should be 1 unless the name was already "Julia"
	}
}
```

#### 5. Deleting an object
```Java
User user = new User(234); // you can only delete with a PK
if (beanSession.delete(user)) { // no need to load when deleting by PK
	System.out.println("User was deleted: " + user.getId());
}
```
#### 6. Loading list of objects
```Java
User user = new User(); // no PK
user.setName("Bob");
List<User> users = beanSession.loadList(user); // load all users with name "Bob"
```

#### 7. Loading a sorted list of objects
```Java
User user = new User(); // no PK
user.setName("Bob");
User userProxy = PropertiesProxy.create(User.class);
List<User> users = beanSession.loadList(user, OrderBy.get().desc(userProxy.getId()));
```

#### 8. Loading a list of objects with limit
```Java
User user = new user(); // no PK
user.setName("Bob");
List<User> users = beanSession.loadList(user, Limit.get(10));
```

#### 9. Write any SQL select query
```Java
PreparedStatement stmt = null;
ResultSet rset = null;

try {

    TableAlias<User> userAlias = beanSession.createTableAlias(User.class);
    User user = userAlias.proxy();

    SQLBuilder sql = new SQLBuilder(userAlias);
    sql.append("select ");
    sql.append(userAlias.columns());
    sql.append(" from ").append(userAlias.tableName());
    sql.append(" where ").column(user.getName()).append(" like ? and ").column(user.getId())
		  .append(" > ?").append(" order by ").column(user.getName()).append(" desc");

    stmt = SQLUtils.prepare(conn, sql.toString(), "M%", 11); // varargs for params

    rset = stmt.executeQuery();

    List<User> users = new LinkedList<User>();

    while(rset.next()) {
        User u = new User();
        beanSession.populateBean(rset, u);
        users.add(u);
    }

    System.out.println("Number of users loaded: " + users.size());

} finally {
    SQLUtils.close(rset, stmt);
}
```

#### 10. Write same query above but now using QueryBuilder
```Java
QueryBuilder builder = beanSession.buildQuery();
Alias<User> userAlias = builder.aliasTo(User.class);
User user = userAlias.proxy();

// Note that in this case you don't have to worry about opening Statements/ResultSets.
// You just create and execute your query and it gives you the list of objects you want.

List<User> users = builder
	.selectFrom(userAlias)
	.where()
	.clause(user.getName())
	.condition(Like.get("M%"))
	.and()
	.clause(user.getId())
	.condition(GreaterThan.get(11))
	.orderBy()
	.desc(userAlias, user.getName())
	.executeQuery();
```

***
For the subsequent recipes, we'll be using the objects below:
```Java
public class User {

	private int id;
	private String name;
	private List<Post> posts;

	public User() { /* mandatory */ }

	public User(int id) {
		this.id = id;
	}

	// getters and setters
}

public class Post {

	private int id;
	private String title;
	private User user;

	public Post() { /* mandatory */ }

	public Post(int id) {
		this.id = id;
	}

	// getters and setters...
}
```

#### 11. Configuring a one-to-one relationship
```Java
User user = PropertiesProxy.create(User.class);
BeanConfig userConfig = new BeanConfig(User.class, "users")
  .pk(user.getId(), DBTypes.AUTOINCREMENT) // "id" property maps to "id" column
  .field(user.getName(), DBTypes.STRING); // "name" property maps to "name" column

Post post = PropertiesProxy.create(Post.class);
BeanConfig postConfig = new BeanConfig(Post.class, "posts")
  .pk(post.getId(), DBTypes.AUTOINCREMENT) // "id" property maps to "id" column
  .field(post.getTitle(), DBTypes.STRING) // "title" property maps to "title" column
  .field(post.getUser().getId(), "user_id", DBTypes.INTEGER); // <===== user_id is the FK column linked to the User PK
```

#### 12. Loading a one-to-one relationship
```Java
Post post = new Post(23);
if (beanSession.load(post)) {
	System.out.println("User ID from post: " + post.getUser().getId()); // works
	System.out.println("User NAME from post: " + post.getUser().getName()); // prints null

	// MentaBean always uses lazy-loading for dependencies
	// When you need, you load the dependency yourself
	// note that manual lazy loading of dependencies is a conscious implementation decision
	beanSession.load(post.getUser());

	System.out.println("User NAME from post: " + post.getUser().getName()); // now good!
}
```

#### 13. Loading a one-to-many relationship
```Java
User user = new user(345); // PK
if (beanSession.load(user)) {
	System.out.println("Posts: " + user.getPosts()); // prints null (remember lazy-loading)

	// MentaBean always uses lazy-loading for dependencies
	// When you need, you load the dependency yourself
	// note that manual lazy loading of dependencies is a conscious implementation decision
	Post post = new Post();
	post.setUser(user);
	List<Post> posts = beanSession.loadList(post); // load all posts for this user
	user.setPosts(posts);

	System.out.println("Posts: " + user.getPosts()); // now good!
}
```

More recipes coming soon. Feel free to suggest new ones!

For now, please refer to http://mentabean.soliveirajr.com to see the complete MentaBean documentation.
