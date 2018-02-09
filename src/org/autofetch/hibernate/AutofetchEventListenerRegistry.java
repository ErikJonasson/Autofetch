package org.autofetch.hibernate;

import java.util.EventListener;

import org.hibernate.event.service.spi.DuplicationStrategy;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;

public class AutofetchEventListenerRegistry implements EventListenerRegistry, EventListener {

	public <T> EventListenerGroup<T> getEventListenerGroup(EventType<T> eventType) {
		// TODO Auto-generated method stub
		return null;
	}

	public void addDuplicationStrategy(DuplicationStrategy strategy) {
		// TODO Auto-generated method stub

	}

	public <T> void setListeners(EventType<T> type, Class<? extends T>... listeners) {
		// TODO Auto-generated method stub

	}

	public <T> void setListeners(EventType<T> type, T... listeners) {
		// TODO Auto-generated method stub

	}

	public <T> void appendListeners(EventType<T> type, Class<? extends T>... listeners) {
		// TODO Auto-generated method stub

	}

	public <T> void appendListeners(EventType<T> type, T... listeners) {
		// TODO Auto-generated method stub

	}

	public <T> void prependListeners(EventType<T> type, Class<? extends T>... listeners) {
		// TODO Auto-generated method stub

	}

	public <T> void prependListeners(EventType<T> type, T... listeners) {
		// TODO Auto-generated method stub

	}

}
