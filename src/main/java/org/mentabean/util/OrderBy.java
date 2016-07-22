package org.mentabean.util;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class OrderBy {
	
	public static enum SortOrder { ASC, DESC };
	
	private final Map<String, SortOrder> fields = new LinkedHashMap<String, SortOrder>();
	
	public OrderBy orderByAsc(String field) {
		
		if (field == null) {
			return orderByAsc((Object) null);
		}
		
		fields.put(field, SortOrder.ASC);
		
		return this;
	}
	
	public OrderBy orderByAsc(Object field) {
		return orderByAsc(PropertiesProxy.getPropertyName());
	}
	
	public OrderBy orderByDesc(String field) {
		
		if (field == null) {
			return orderByDesc((Object) null);
		}
		
		fields.put(field, SortOrder.DESC);
		
		return this;
	}
	
	public OrderBy orderByDesc(Object field) {
		return orderByDesc(PropertiesProxy.getPropertyName());
	}
	
	@Override
	public String toString() {
		if (fields.size() == 0) return "";
		StringBuilder sb = new StringBuilder(128);
		Iterator<String> iter = fields.keySet().iterator();
		while(iter.hasNext()) {
			String field = iter.next();
			SortOrder so = fields.get(field);
			if (sb.length() > 0) sb.append(", ");
			sb.append(field).append(" ").append(so.toString().toLowerCase());
		}
		return sb.toString();
	}
	
	public boolean isEmpty() {
		return fields.isEmpty();
	}
	
}