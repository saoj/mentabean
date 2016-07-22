package org.mentabean.util;

public class Limit {
	
	private final int x;
	
	public Limit(int x) {
		this.x = x;
	}
	
	public int intValue() {
		return x;
	}
	
	@Override
	public String toString() {
		return String.valueOf(x);
	}
}