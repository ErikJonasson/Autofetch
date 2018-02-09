/**
 * This file is part of Autofetch.
 * Autofetch is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License as published 
 * by the Free Software Foundation, either version 3 of the License, 
 * or (at your option) any later version. Autofetch is distributed in the 
 * hope that it will be useful, but WITHOUT ANY WARRANTY; without even 
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR 
 * PURPOSE.  See the Lesser GNU General Public License for more details. You 
 * should have received a copy of the Lesser GNU General Public License along 
 * with Autofetch.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.autofetch.hibernate;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.autofetch.hibernate.ExtentManager;
import org.hibernate.EmptyInterceptor;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.InvalidMappingException;
import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AutofetchHbmBinder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.ExtendsQueueEntry;
import org.hibernate.cfg.HbmBinder;
import org.hibernate.cfg.MetadataSourceType;
import org.hibernate.cfg.SettingsFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.internal.EventListenerRegistryImpl;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.InitializeCollectionEventListener;
import org.hibernate.event.spi.LoadEventListener;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.internal.util.xml.XmlDocument;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * Based on org.hibernate.cfg.Configuration.
 * 
 * Changes behavior of hibernate configuration to use our own HbmBinder and to
 * wrap session factories.
 * 
 * @author Ali Ibrahim <aibrahim@cs.utexas.edu>
 * 
 */
public class AutofetchConfiguration extends Configuration {
	private ExtentManager extentManager;
	private EventListenerRegistry eventListenerRegistry;
	private SessionFactory sessionFactory;

	public AutofetchConfiguration() {
		super();
		initialize();
	}

	public AutofetchConfiguration(SettingsFactory arg0) {
		super(arg0);
		initialize();
	}
	
	@Override
	public SessionFactory buildSessionFactory(ServiceRegistry serviceRegistry) throws HibernateException {
		sessionFactory= super.buildSessionFactory(serviceRegistry);
		eventListenerRegistry = ((SessionFactoryImpl)sessionFactory).getServiceRegistry().getService(AutofetchEventListenerRegistryImpl.class);
		return sessionFactory;
	}
	@Override
	public void add(XmlDocument metadataXml) throws MappingException {
		HbmBinder.bindRoot(metadataXml, createMappings(), CollectionHelper.EMPTY_MAP);
	}

	public void processHbmXml(XmlDocument metadataXml, Set<String> entityNames) {
		try {
			HbmBinder.bindRoot(metadataXml, createMappings(), CollectionHelper.EMPTY_MAP, entityNames);
		} catch (MappingException me) {
			throw new InvalidMappingException(metadataXml.getOrigin().getType(), metadataXml.getOrigin().getName(), me);
		}

		for (String entityName : entityNames) {
			if (annotatedClassesByEntityNameMap.containsKey(entityName)) {
				annotatedClasses.remove(annotatedClassesByEntityNameMap.get(entityName));
				annotatedClassesByEntityNameMap.remove(entityName);
			}
		}
	}

	// @Override
	// protected org.dom4j.Document findPossibleExtends() {
	// // Iterator iter = extendsQueue.iterator();
	// Iterator iter = extendsQueue.keySet().iterator();
	// while (iter.hasNext()) {
	// final ExtendsQueueEntry entry = (ExtendsQueueEntry) iter.next();
	// if (getClassMapping(entry.getExplicitName()) != null) {
	// // found
	// iter.remove();
	// return entry.getDocument();
	// } else if (getClassMapping(AutofetchHbmBinder.getClassName(entry
	// .getExplicitName(), entry.getMappingPackage())) != null) {
	// // found
	// iter.remove();
	// return entry.getDocument();
	// }
	// }
	// return null;
	// }
	@Override
	protected ExtendsQueueEntry findPossibleExtends() {
		Iterator<ExtendsQueueEntry> itr = extendsQueue.keySet().iterator();
		while (itr.hasNext()) {
			final ExtendsQueueEntry entry = itr.next();
			boolean found = getClassMapping(entry.getExplicitName()) != null || getClassMapping(
					AutofetchHbmBinder.getClassName(entry.getExplicitName(), entry.getMappingPackage())) != null;
			if (found) {
				itr.remove();
				return entry;
			}
		}
		return null;
	}

	@Override
	protected void reset() {
		super.reset();
		initialize();
	}

	private void initialize() {
		extentManager = new ExtentManager();
		eventListenerRegistry.setListeners(EventType.LOAD, new AutofetchLoadListener());
		eventListenerRegistry.setListeners(EventType.INIT_COLLECTION, new AutofetchInitializeCollectionListener(extentManager));
		setInterceptor(new AutofetchInterceptor(EmptyInterceptor.INSTANCE, extentManager));
//		getEventListeners().setLoadEventListeners(new LoadEventListener[] { new AutofetchLoadListener(extentManager) });
//		getEventListeners().setInitializeCollectionEventListeners(
//				new InitializeCollectionEventListener[] { new AutofetchInitializeCollectionListener(extentManager) });
		
	}

	/**
	 * Ensures that any interceptor is wrapped with the AutofetchInterceptor.
	 */
	@Override
	public Configuration setInterceptor(Interceptor i) {
		if (i instanceof AutofetchInterceptor) {
			return super.setInterceptor(i);
		} else {
			AutofetchInterceptor ai = (AutofetchInterceptor) getInterceptor();
			return super.setInterceptor(ai.copy(i));
		}
	}

	/**
	 * Ensures that the extent manager is set for any Autofetch listeners.
	 */
//	@Override
//	public void setListeners(String type, Object[] listeners) {
//		setExtentManager(listeners, extentManager);
//		super.setListeners(type, listeners);
//	}

	public ExtentManager getExtentManager() {
		return extentManager;
	}

	public void setExtentManager(ExtentManager em) {
		this.extentManager = em;
		// Propagate changes to listeners and interceptor
		AutofetchInterceptor ai = (AutofetchInterceptor) getInterceptor();
		ai.setExtentManager(em);
		((AutofetchConfiguration) eventListenerRegistry).
		setExtentManager(eventListenerRegistry.getEventListenerGroup(EventType.INIT_COLLECTION).listeners(), em);
		setExtentManager(eventListenerRegistry.getEventListenerGroup(EventType.LOAD).listeners(), em);
	}

	private <T> void setExtentManager(Iterable<T> listeners, ExtentManager em) {
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