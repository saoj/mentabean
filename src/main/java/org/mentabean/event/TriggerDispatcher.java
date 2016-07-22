package org.mentabean.event;

import java.util.ArrayList;
import java.util.List;

public class TriggerDispatcher {

	private final List<TriggerListener> triggers = new ArrayList<TriggerListener>();
	
	public void addTrigger(TriggerListener trigger) {
		
		synchronized (triggers) {
			
			if (!triggers.contains(trigger)) {
				
				triggers.add(trigger);
			}
		}
	}
	
	public void removeTrigger(TriggerListener trigger) {
		
		synchronized (triggers) {
			
			triggers.remove(trigger);
		}
	}
	
	public void dispatch(Type type, TriggerEvent evt) {
		type.dispatchAll(triggers, evt);
	}
	
	public enum Type {
		
		BEFORE_INSERT {

			@Override
			void fire(TriggerListener l, TriggerEvent evt) {
				l.beforeInsert(evt);
			}
			
		},
		
		AFTER_INSERT {

			@Override
			void fire(TriggerListener l, TriggerEvent evt) {
				l.afterInsert(evt);
			}
			
		},
		
		BEFORE_UPDATE {

			@Override
			void fire(TriggerListener l, TriggerEvent evt) {
				l.beforeUpdate(evt);
			}
			
		},
		
		AFTER_UPDATE {

			@Override
			void fire(TriggerListener l, TriggerEvent evt) {
				l.afterUpdate(evt);
			}
			
		},
		
		BEFORE_DELETE {

			@Override
			void fire(TriggerListener l, TriggerEvent evt) {
				l.beforeDelete(evt);
			}
			
		},
		
		AFTER_DELETE {

			@Override
			void fire(TriggerListener l, TriggerEvent evt) {
				l.afterDelete(evt);
			}
			
		};
		
		public void dispatchAll(List<TriggerListener> list, TriggerEvent evt) {
			for (TriggerListener l : list) {
				fire(l, evt);
			}
		}
		
		abstract void fire(TriggerListener l, TriggerEvent evt);
	}
	
}
