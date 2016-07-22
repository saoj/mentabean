package org.mentabean.util;

import java.lang.reflect.Method;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;

import org.mentabean.BeanException;

public class DefaultProxy extends PropertiesProxy {

	public DefaultProxy(String chainProp) {
		super(chainProp);
	}
	
	public DefaultProxy() {
		super(null);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected <E> E createInternal(final Class<E> klass) {
		
		try {
		
    		ProxyFactory factory = new ProxyFactory();
    		factory.setSuperclass(klass);
    		
    		factory.setFilter(new MethodFilter() {
    
    			@Override
                public boolean isHandled(Method m) {
    				return getPropName(m) != null;
    			}
    		});
    		
    		MethodHandler handler = new MethodHandler() {
    	        
    			@Override
    	        public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
    				
    				return DefaultProxy.this.invoke(self, thisMethod, args);
    	        }
    	    };
    	    
    	    return (E) factory.create(new Class[0], new Object[0], handler);
    	    
		} catch(Exception e) {
			throw new BeanException(e);
		}
	}

}
