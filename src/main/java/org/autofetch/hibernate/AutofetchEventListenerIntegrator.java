package org.autofetch.hibernate;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.beanvalidation.DuplicationStrategyImpl;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.DuplicationStrategy;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.metamodel.Metadata;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

public class AutofetchEventListenerIntegrator implements Integrator {
	private ExtentManager extentManager;
	
	public AutofetchEventListenerIntegrator(ExtentManager extentManager) {
		this.extentManager = extentManager;
	}

	public void integrate(Metadata metadata, SessionFactoryImplementor sessionFactory,
			SessionFactoryServiceRegistry serviceRegistry) {
//		EventListenerRegistry eventListenerRegistry = serviceRegistry.getService(AutofetchEventListenerRegistryImpl.class);
//		eventListenerRegistry.getEventListenerGroup(EventType.LOAD).appendListener(new AutofetchLoadListener(extentManager));
		EventListenerRegistry eventListenerRegistry = ((SessionFactoryImpl) sessionFactory).getServiceRegistry()
                .getService(EventListenerRegistry.class);
            eventListenerRegistry.setListeners(EventType.LOAD, new AutofetchLoadListener(extentManager));
            eventListenerRegistry.setListeners(EventType.INIT_COLLECTION,
                new AutofetchInitializeCollectionListener(extentManager));
	}

	@Override
	public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {

	}

	public class CustomDuplicationStrategy implements DuplicationStrategy {

		@Override
		public boolean areMatch(Object listener, Object original) {
			return false;
		}

		@Override
		public Action getAction() {
			return Action.ERROR;
		}
	}


	@Override
	public void integrate(Configuration configuration, SessionFactoryImplementor sessionFactory,
			SessionFactoryServiceRegistry serviceRegistry) {
//		EventListenerRegistry eventListenerRegistry = serviceRegistry.getService(AutofetchEventListenerRegistryImpl.class);
		//eventListenerRegistry.getEventListenerGroup(EventType.LOAD).appendListener(new AutofetchLoadListener(extentManager));
        EventListenerRegistry eventListenerRegistry = ((SessionFactoryImpl) sessionFactory).getServiceRegistry()
                .getService(EventListenerRegistry.class);
            eventListenerRegistry.setListeners(EventType.LOAD, new AutofetchLoadListener(extentManager));
             eventListenerRegistry.setListeners(EventType.INIT_COLLECTION,
                new AutofetchInitializeCollectionListener(extentManager));
            eventListenerRegistry.addDuplicationStrategy(new CustomDuplicationStrategy());
            eventListenerRegistry.getEventListenerGroup(EventType.LOAD).addDuplicationStrategy(new CustomDuplicationStrategy());
	}

	@Override
	public void integrate(MetadataImplementor metadata, SessionFactoryImplementor sessionFactory,
			SessionFactoryServiceRegistry serviceRegistry) {
		EventListenerRegistry eventListenerRegistry = serviceRegistry.getService(AutofetchEventListenerRegistryImpl.class);
		//eventListenerRegistry.getEventListenerGroup(EventType.LOAD).appendListener(new AutofetchLoadListener(extentManager));
        eventListenerRegistry = ((SessionFactoryImpl) sessionFactory).getServiceRegistry()
                .getService(AutofetchEventListenerRegistryImpl.class);
            eventListenerRegistry.setListeners(EventType.LOAD, new AutofetchLoadListener(extentManager));
            eventListenerRegistry.setListeners(EventType.INIT_COLLECTION,
                new AutofetchInitializeCollectionListener(extentManager));
		
	}

}
