package org.mentabean.jdbc.one_to_many_without_join_table;

public class Part {
	
	private int id;
	private String name;
	private Engine engine;
	
	public Part() {
		
	}
	
	public Part(int id) {
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
	
	public void setEngine(Engine engine) {
		this.engine = engine;
	}
	
	public Engine getEngine() {
		return engine;
	}
}