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

import java.util.Iterator;

import org.hibernate.EntityMode;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataContributor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceContributor;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

import org.jboss.jandex.IndexView;

import com.google.auto.service.AutoService;

@SuppressWarnings("unused")
@AutoService(Integrator.class)
public class AutofetchIntegrator implements ServiceContributor, MetadataContributor, Integrator {

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
	public void contribute(InFlightMetadataCollector metadataCollector, IndexView jandexIndex) {
		for ( PersistentClass persistentClass : metadataCollector.getEntityBindingMap().values() ) {
			persistentClass.addTuplizer( EntityMode.POJO, AutofetchTuplizer.class.getName() );

			final Iterator propertyIterator = persistentClass.getPropertyIterator();
			while ( propertyIterator.hasNext() ) {
				Property property = (Property) propertyIterator.next();
				String name = property.getName();
				if ( property.getValue() instanceof Collection ) {
					replaceCollection( property, persistentClass );
				}
			}
		}
	}

	@Override
	public void contribute(StandardServiceRegistryBuilder serviceRegistryBuilder) {
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

	private static void replaceCollection(org.hibernate.mapping.Property collectionProperty, PersistentClass owner) {
		if ( !( collectionProperty.getValue() instanceof org.hibernate.mapping.Collection ) ) {
			return;
		}

		org.hibernate.mapping.Collection value = (org.hibernate.mapping.Collection) collectionProperty.getValue();

		if ( value instanceof org.hibernate.mapping.Bag ) {
			value.setTypeName( AutofetchBagType.class.getName() );
		}
		else if ( value instanceof org.hibernate.mapping.IdentifierBag ) {
			value.setTypeName( AutofetchIdBagType.class.getName() );
		}
		else if ( value instanceof org.hibernate.mapping.List ) {
			value.setTypeName( AutofetchListType.class.getName() );
		}
		else if ( value instanceof org.hibernate.mapping.Set ) {
			value.setTypeName( AutofetchSetType.class.getName() );
		}
		else {
			throw new UnsupportedOperationException( "Collection type not supported: " + value.getClass() );
		}
	}
}
