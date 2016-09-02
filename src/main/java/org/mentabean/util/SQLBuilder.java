package org.mentabean.util;

import org.mentabean.sql.TableAlias;

public class SQLBuilder implements CharSequence {
	
	private final StringBuilder sb;
	private final TableAlias<?>[] aliases;
	
	public SQLBuilder() {
		this.sb = new StringBuilder();
		this.aliases = null;
	}
	
	public SQLBuilder(int initialCapacity) {
		this.sb = new StringBuilder(initialCapacity);
		this.aliases = null;
	}
	
	public SQLBuilder(TableAlias<?> ...aliases) {
		this.sb = new StringBuilder();
		this.aliases = aliases;
	}
	
	public SQLBuilder(int initialCapacity, TableAlias<?> ...aliases) {
		this.sb = new StringBuilder(initialCapacity);
		this.aliases = aliases;
	}
	
	private TableAlias<?> findAlias(Object instance) {
		
		if (aliases == null) return null;
		
		for(TableAlias<?> alias : aliases) {
			if (alias.proxy() == instance) {
				return alias;
			}
		}
		
		return null;
	}
	
	public SQLBuilder column(Object obj) {
		
		if (obj instanceof String) {
			// you are really adding a column name:
			append((String) obj);
		} else {
			Object[] instances = PropertiesProxy.getBeanInstances();
			if (instances == null || instances.length == 0) throw new IllegalStateException("Cannot find bean instance!");
			
			// here we assume the very first klass:
			Object instance = instances[0];
			TableAlias<?> alias = findAlias(instance);
			if (alias == null) throw new IllegalStateException("Cannot find alias: " + instance);
			append(alias.column(null)); // null because it will get from proxy
		}
		
		return this;
	}
	
	//////////////////////////////////////////////////
	/////// CharSequence implementation:
	//////////////////////////////////////////////////

	@Override
    public int length() {
	    return sb.length();
    }

	@Override
    public char charAt(int index) {
	    return sb.charAt(index);
    }

	@Override
    public CharSequence subSequence(int start, int end) {
	    return sb.subSequence(start, end);
    }

	//////////////////////////////////////////////////
	/////// Delegate methods to StringBuilder:
	//////////////////////////////////////////////////
	
	public int capacity() {
	    return sb.capacity();
    }

	@Override
	public int hashCode() {
	    return sb.hashCode();
    }

	public void ensureCapacity(int minimumCapacity) {
	    sb.ensureCapacity(minimumCapacity);
    }

	public void trimToSize() {
	    sb.trimToSize();
    }

	public void setLength(int newLength) {
	    sb.setLength(newLength);
    }

	@Override
	public boolean equals(Object obj) {
	    return sb.equals(obj);
    }

	public SQLBuilder append(Object obj) {
	    sb.append(obj);
	    return this;
    }

	public SQLBuilder append(String str) {
	    sb.append(str);
	    return this;
    }

	public SQLBuilder append(StringBuffer sb) {
	    this.sb.append(sb);
	    return this;
    }

	public SQLBuilder append(CharSequence s) {
	    sb.append(s);
	    return this;
    }

	public int codePointAt(int index) {
	    return sb.codePointAt(index);
    }

	public SQLBuilder append(CharSequence s, int start, int end) {
	    sb.append(s, start, end);
	    return this;
    }

	public SQLBuilder append(char[] str) {
	    sb.append(str);
	    return this;
    }

	public SQLBuilder append(char[] str, int offset, int len) {
	    sb.append(str, offset, len);
	    return this;
    }

	public SQLBuilder append(boolean b) {
	    sb.append(b);
	    return this;
    }

	public SQLBuilder append(char c) {
	    sb.append(c);
	    return this;
    }

	public SQLBuilder append(int i) {
	    sb.append(i);
	    return this;
    }

	public int codePointBefore(int index) {
	    return sb.codePointBefore(index);
    }

	public SQLBuilder append(long lng) {
	    sb.append(lng);
	    return this;
    }

	public SQLBuilder append(float f) {
	    sb.append(f);
	    return this;
    }

	public SQLBuilder append(double d) {
	    sb.append(d);
	    return this;
    }

	public SQLBuilder appendCodePoint(int codePoint) {
	    sb.appendCodePoint(codePoint);
	    return this;
    }

	public SQLBuilder delete(int start, int end) {
	    sb.delete(start, end);
	    return this;
    }

	public SQLBuilder deleteCharAt(int index) {
	    sb.deleteCharAt(index);
	    return this;
    }

	public SQLBuilder replace(int start, int end, String str) {
	    sb.replace(start, end, str);
	    return this;
    }

	public int codePointCount(int beginIndex, int endIndex) {
	    return sb.codePointCount(beginIndex, endIndex);
    }

	public SQLBuilder insert(int index, char[] str, int offset, int len) {
	    sb.insert(index, str, offset, len);
	    return this;
    }

	public SQLBuilder insert(int offset, Object obj) {
	    sb.insert(offset, obj);
	    return this;
    }

	public SQLBuilder insert(int offset, String str) {
	    sb.insert(offset, str);
	    return this;
    }

	public SQLBuilder insert(int offset, char[] str) {
	    sb.insert(offset, str);
	    return this;
    }

	public SQLBuilder insert(int dstOffset, CharSequence s) {
	    sb.insert(dstOffset, s);
	    return this;
    }

	public int offsetByCodePoints(int index, int codePointOffset) {
	    return sb.offsetByCodePoints(index, codePointOffset);
    }

	public SQLBuilder insert(int dstOffset, CharSequence s, int start, int end) {
	    sb.insert(dstOffset, s, start, end);
	    return this;
    }

	public SQLBuilder insert(int offset, boolean b) {
	    sb.insert(offset, b);
	    return this;
    }

	public SQLBuilder insert(int offset, char c) {
	    sb.insert(offset, c);
	    return this;
    }

	public SQLBuilder insert(int offset, int i) {
	    sb.insert(offset, i);
	    return this;
    }

	public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
	    sb.getChars(srcBegin, srcEnd, dst, dstBegin);
    }

	public SQLBuilder insert(int offset, long l) {
	    sb.insert(offset, l);
	    return this;
    }

	public SQLBuilder insert(int offset, float f) {
	    sb.insert(offset, f);
	    return this;
    }

	public SQLBuilder insert(int offset, double d) {
	    sb.insert(offset, d);
	    return this;
    }

	public int indexOf(String str) {
	    return sb.indexOf(str);
    }

	public int indexOf(String str, int fromIndex) {
	    return sb.indexOf(str, fromIndex);
    }

	public int lastIndexOf(String str) {
	    return sb.lastIndexOf(str);
    }

	public int lastIndexOf(String str, int fromIndex) {
	    return sb.lastIndexOf(str, fromIndex);
    }

	public SQLBuilder reverse() {
	    sb.reverse();
	    return this;
    }

	@Override
	public String toString() {
	    return sb.toString();
    }

	public void setCharAt(int index, char ch) {
	    sb.setCharAt(index, ch);
    }

	public String substring(int start) {
	    return sb.substring(start);
    }

	public String substring(int start, int end) {
	    return sb.substring(start, end);
    }
}