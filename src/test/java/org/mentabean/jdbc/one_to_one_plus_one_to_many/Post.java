package org.mentabean.jdbc.one_to_one_plus_one_to_many;

public class Post {
	
	private int id;
	private String title;
	private User user;
	
	public Post() {
		
	}
	
	public Post(int id) {
		this.id = id;
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