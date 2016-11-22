# MentaBean
Simple ORM and Query Builder

## Quick Tutorial

For the subsequent recipes, we'll be using the object below:
```Java
package org.mentabean.jdbc.one_to_one;

public class User {
	
	private int id;
	private String name;
	
	public User() { /* mandatory */ }
	
	public User(int id) { 
      this.id = id;
	}

	public int getId() { return id; }

	public void setId(int id) { this.id = id; }

	public String getName() { return name; }

	public void setName(String name) { this.name = name; }
}
```

#### 1. Mapping an object to a database table
```Java
BeanConfig userConfig = new BeanConfig(User.class, "users") // table name is "users"
    .pk(user.getId(), "user_id", DBTypes.AUTOINCREMENT) // id maps to user_id column
    .field(user.getName(), "username", DBTypes.STRING); // name maps to username column
```
```Java
BeanConfig userConfig = new BeanConfig(User.class, "users") // table name is "users"
    .pk(user.getId(), DBTypes.AUTOINCREMENT) // id maps to id column
    .field(user.getName(), DBTypes.STRING); // name maps to name column
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
