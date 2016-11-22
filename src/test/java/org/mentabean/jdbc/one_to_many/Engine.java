package org.mentabean.jdbc.one_to_many;

import java.util.List;

public class Engine {
	
	private int id;
	private String name;
	private List<Part> parts;
	
	public Engine() {
		
	}
	
	public Engine(int id) {
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

	public List<Part> getParts() {
		return parts;
	}

	public void setParts(List<Part> parts) {
		this.parts = parts;
	}
}