package org.mentabean.event;

public interface TriggerListener {

	public void beforeInsert(TriggerEvent evt);
	
	public void afterInsert(TriggerEvent evt);
	
	public void beforeUpdate(TriggerEvent evt);
	
	public void afterUpdate(TriggerEvent evt);
	
	public void beforeDelete(TriggerEvent evt);
	
	public void afterDelete(TriggerEvent evt);
	
}
