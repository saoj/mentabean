package org.mentabean.jdbc.one_to_one_plus_one_to_many;

import java.util.List;

public class User {
	
	private int id;
	private String name;
	private List<Post> posts;
	
	public User() {
		
	}
	
	public User(int id) {
		this.id = id;
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
	
	public void setPosts(List<Post> posts) {
		this.posts = posts;
	}
	
	public List<Post> getPosts() {
		return posts;
	}
}