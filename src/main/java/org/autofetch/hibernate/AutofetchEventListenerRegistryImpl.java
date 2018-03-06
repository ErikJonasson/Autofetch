package org.autofetch.hibernate;

import org.hibernate.event.service.internal.EventListenerRegistryImpl;
import org.hibernate.event.spi.EventType;

public class AutofetchEventListenerRegistryImpl extends EventListenerRegistryImpl {

	private ExtentManager em;
	public AutofetchEventListenerRegistryImpl(ExtentManager em) {
		super();
		this.em = em;
	}
	@Override
	public <T> void setListeners(EventType<T> type, T... listeners) {
		setExtentManager(listeners, em);
		super.setListeners(type, listeners);
	}
	
	private <T> void setExtentManager(T[] listeners, ExtentManager em) {
		for (Object listener : listeners) {
			if (listener instanceof AutofetchInitializeCollectionListener) {
				((AutofetchInitializeCollectionListener) listener).setExtentManager(em);
			}
			if (listener instanceof AutofetchLoadListener) {
				((AutofetchLoadListener) listener).setExtentManager(em);
			}
		}
	}
}
