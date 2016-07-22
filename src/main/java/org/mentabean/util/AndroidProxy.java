package org.mentabean.util;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import org.mentabean.BeanException;

import com.google.dexmaker.stock.ProxyBuilder;

public class AndroidProxy extends PropertiesProxy {

	private static File dexCache;
	
	public AndroidProxy(File dexCache) {
		super(null);
		AndroidProxy.dexCache = dexCache;
	}
	
	public AndroidProxy(String chainProp) {
		super(chainProp);
	}
	
	@Override
	protected <E> E createInternal(Class<E> klass) {
		
		try {
			
			return ProxyBuilder.forClass(klass)
			         .dexCache(dexCache)
			         .handler(new Handler())
			         .build();
			
		} catch (Exception e) {
			throw new BeanException(e);
		}
	}
	
	private class Handler implements InvocationHandler {

		@Override
		public Object invoke(Object self, Method thisMethod, Object[] args) throws Throwable {
			
			return AndroidProxy.this.invoke(self, thisMethod, args);
		}
		
	}

}
