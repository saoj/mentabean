package  org.mentabean.util;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.mentabean.BeanException;

public abstract class PropertiesProxy {
	
	public static PropertiesProxy INSTANCE;
	private static final ThreadLocal<List<String>> propertyNames = new ThreadLocal<List<String>>();
	private static final ThreadLocal<List<Object>> beanInstances = new ThreadLocal<List<Object>>();
	private String chainProp;	
	
	public PropertiesProxy(String chainProp) {
		this.chainProp = chainProp;
	}	
	
	private PropertiesProxy createInstance(String chainProp) {
		try {
			return getClass().getConstructor(String.class).newInstance(chainProp);
		} catch (Exception e) {
			throw new BeanException("Was not able to get PropertiesProxy instance!", e);
		}
	}
	
	private static Class<?>[] ignored = new Class<?>[] {
			BigDecimal.class, Date.class, Timestamp.class, Time.class
	}; 
	
	public static <E> E create(Class<E> klass) {
		PropertiesProxy pp = INSTANCE.createInstance(null);
		return pp.createInternal(klass);
	}
	
	public static String getPropertyName() {
		
		List<String> list = propertyNames.get();
		
		if (list == null || list.size() != 1) {
			throw new BeanException("Was not able to get property name through the proxy!");
		}
		
		String propName = list.get(0);
		list.clear();
		
		// take this time to also clear the classes:
		List<Object> beans = beanInstances.get();
		if (beans != null) beans.clear();
		
		return propName;
	}
	
	public static Object[] getBeanInstances() {
		
		List<Object> list = beanInstances.get();
		
		if (list == null || list.size() == 0) {
			throw new BeanException("Was not able to get bean instances through the proxy!");
		}
		
		Object[] array = new Object[list.size()];
		
		array = list.toArray(array);
		
		list.clear();
		
		return array;
	}
	
	public static void addBeanInstance(Object proxy) {
		
		List<Object> list = beanInstances.get();
		if (list == null) {
			list = new LinkedList<Object>();
			beanInstances.set(list);
		}
		list.add(proxy);
	}
	
	public static void addPropertyName(String name) {
		List<String> list = propertyNames.get();
		if (list == null) {
			list = new LinkedList<String>();
			propertyNames.set(list);
		}
		list.add(name);
	}
	
	public static boolean hasBeanInstance() {
		return hasElements(beanInstances.get());
	}
	
	public static boolean hasProperties() {
		return hasElements(propertyNames.get());
	}
	
	private static boolean hasElements(Collection<?> coll) {
		return coll != null && coll.size() > 0; 
	}
	
	public static String[] getPropertyNames() {
		
		List<String> list = propertyNames.get();
		
		if (list == null || list.size() == 0) {
			throw new BeanException("Was not able to get property names through the proxy!");
		}
		
		String[] array = new String[list.size()];
		
		array = list.toArray(array);
		
		list.clear();
		
		// take this time to also clear the instances:
		List<Object> beans = beanInstances.get();
		if (beans != null) beans.clear();
		
		return array;
	}
	
	/**
	 * Return the property name, if the method is a valid JavaBean getter
	 * 
	 * @param method the method
	 * @return the property name of null if not a valid getter
	 */
	protected static String getPropName(Method method) {
		
		String methodName = method.getName();
		Class<?> propType = method.getReturnType();
		
		if (propType.equals(Void.class)) return null; // not a getter
		
		Class<?>[] params = method.getParameterTypes();
		if (params != null && params.length > 0) return null; // not a getter
		
		String propName;
		
		if (methodName.startsWith("get") && methodName.length() > 3) {
		
			propName = methodName.substring(3);
			
		} else if (methodName.startsWith("is") && methodName.length() > 2 && (propType.equals(boolean.class) || propType.equals(Boolean.class))) {
			
			propName = methodName.substring(2);
			
		} else {
			
			return null; // not a getter...
		}
		
		propName = propName.substring(0, 1).toLowerCase() + propName.substring(1); // first letter is lower-case
		
		if (propName.equals("class")) return null; // not a property...
		
		return propName;
	}
	
	protected static boolean isIgnored(Class<?> clazz) {
		
		for (Class<?> ign : ignored) {
			if (ign.isAssignableFrom(clazz)) {
				return true;
			}
		}
		
		return false;
	}
	
	protected abstract <E> E createInternal(final Class<E> klass);
	
	protected Object invoke(Object self, Method thisMethod, Object[] args) {
		
		List<String> list = propertyNames.get();
		
		if (list == null) {
			list = new LinkedList<String>();
			propertyNames.set(list);
		}
		
		String propName = getPropName(thisMethod);
		
		if (chainProp != null) {
			list.add(chainProp + "." + propName);
			
			// remember to remove original one because it is not really a property
			// like "user" in "user.id"
			
			list.remove(chainProp);
			
		} else {
			list.add(propName);
		}
		
		// add the object type for SQLBuilder column( ... ) method
		List<Object> beans = beanInstances.get();
		
		if (beans == null) {
			beans = new LinkedList<Object>();
			beanInstances.set(beans);
		}
		
		beans.add(self);
		
		Class<?> propType = thisMethod.getReturnType();
		
		// take care of primitives that cannot be null...
		
		if (propType.equals(boolean.class)) {
			return false;
		} else if (propType.equals(char.class)) {
			return (char) 0;
		} else if (propType.equals(byte.class)) {
			return (byte) 0;
		} else if (propType.equals(long.class)) {
			return (long) 0;
		} else if (propType.equals(int.class)) {
			return (int) 0;
		} else if (propType.equals(short.class)) {
			return (short) 0;
		} else if (propType.equals(float.class)) {
			return (float) 0;
		} else if (propType.equals(double.class)) {
			return (double) 0;
		} else {
			
			// Class must NOT be final otherwise javassist does not work!
			if (Modifier.isFinal(propType.getModifiers()) || propType.isInterface()) {
				return null;
			}
			
			// Javassist doesn't work with overwritten enums either...
			if (propType.isEnum()) {
				return null;
			}
			
			// Must be ignored from Javassist
			if (isIgnored(propType)) {
				return null;
			}
			
			// return a new one
			PropertiesProxy proxy = INSTANCE.createInstance(chainProp != null ? chainProp + "." + propName : propName);
			return proxy.createInternal(propType);
		}
	}
	
}