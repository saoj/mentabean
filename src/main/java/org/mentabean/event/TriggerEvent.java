package org.mentabean.event;

import org.mentabean.BeanSession;

public class TriggerEvent {

	private final BeanSession session;
	private final Object bean;
	
	public TriggerEvent(final BeanSession session, final Object bean) {
		this.session = session;
		this.bean = bean;
	}

	public BeanSession getSession() {
		return session;
	}

	@SuppressWarnings("unchecked")
	public <E> E getBean() {
		return (E) bean;
	}
	
}
