/**
 * Copyright 2008 Ali Ibrahim
 * <p>
 * This file is part of Autofetch. Autofetch is free software: you can redistribute it and/or modify it under the terms of the Lesser GNU General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. Autofetch is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the Lesser GNU General Public License for more details. You should have received a copy of the Lesser GNU General Public
 * License along with Autofetch.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.autofetch.hibernate;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.integrator.spi.ServiceContributingIntegrator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceContributor;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

import com.google.auto.service.AutoService;

@SuppressWarnings("unused")
@AutoService(Integrator.class)
public class AutofetchIntegrator implements ServiceContributingIntegrator {

	@Override
	public void integrate(
			Metadata metadata,
			SessionFactoryImplementor sessionFactory,
			SessionFactoryServiceRegistry serviceRegistry) {
		integrateEventListeners( serviceRegistry );
	}

	@Override
	public void disintegrate(
			SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
	}

	@Override
	public void prepareServices(StandardServiceRegistryBuilder serviceRegistryBuilder) {
		serviceRegistryBuilder.addInitiator( AutofetchServiceInitiator.INSTANCE );
	}

	private static void integrateEventListeners(ServiceRegistry serviceRegistry) {
		final ExtentManager extentManager = serviceRegistry.getService( AutofetchService.class ).getExtentManager();

		EventListenerRegistry eventListenerRegistry = serviceRegistry.getService( EventListenerRegistry.class );
		eventListenerRegistry.setListeners( EventType.LOAD, new AutofetchLoadListener( extentManager ) );

		eventListenerRegistry.setListeners(
				EventType.INIT_COLLECTION,
				new AutofetchInitializeCollectionListener( extentManager )
		);
	}
}
