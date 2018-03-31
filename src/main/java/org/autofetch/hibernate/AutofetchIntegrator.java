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

import com.google.auto.service.AutoService;
import org.hibernate.EntityMode;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.integrator.spi.ServiceContributingIntegrator;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

import java.util.Iterator;

@SuppressWarnings("unused")
@AutoService(Integrator.class)
public class AutofetchIntegrator implements ServiceContributingIntegrator {

    @Override
    public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
    }

    @Override
    public void prepareServices(StandardServiceRegistryBuilder serviceRegistryBuilder) {
        serviceRegistryBuilder.addInitiator(AutofetchServiceInitiator.INSTANCE);
    }

    @Override
    public void integrate(Configuration configuration, SessionFactoryImplementor sessionFactory,
                          SessionFactoryServiceRegistry serviceRegistry) {
        doIntegrate(serviceRegistry);

        final Iterator<PersistentClass> classMappings = configuration.getClassMappings();
        while (classMappings.hasNext()) {
            PersistentClass persistentClass = classMappings.next();
            persistentClass.addTuplizer(EntityMode.POJO, AutofetchTuplizer.class.getName());

            final Iterator propertyIterator = persistentClass.getPropertyIterator();
            while (propertyIterator.hasNext()) {
                org.hibernate.mapping.Property property = (org.hibernate.mapping.Property) propertyIterator.next();
                String name = property.getName();
                if (property.getValue() instanceof org.hibernate.mapping.Collection) {
                    replaceCollection(property, persistentClass);
                }
            }
        }
    }

    @Override
    public void integrate(MetadataImplementor metadata,
                          SessionFactoryImplementor sessionFactory,
                          SessionFactoryServiceRegistry serviceRegistry) {
    }

    private void doIntegrate(ServiceRegistry serviceRegistry) {
        final ExtentManager extentManager = serviceRegistry.getService(AutofetchService.class).getExtentManager();

        EventListenerRegistry eventListenerRegistry = serviceRegistry.getService(EventListenerRegistry.class);
        eventListenerRegistry.setListeners(EventType.LOAD, new AutofetchLoadListener(extentManager));

        eventListenerRegistry.setListeners(EventType.INIT_COLLECTION,
                new AutofetchInitializeCollectionListener(extentManager));
    }

    private static void replaceCollection(org.hibernate.mapping.Property collectionProperty, PersistentClass owner) {
        if (!(collectionProperty.getValue() instanceof org.hibernate.mapping.Collection)) {
            return;
        }

        org.hibernate.mapping.Collection value = (org.hibernate.mapping.Collection) collectionProperty.getValue();

        if (value instanceof org.hibernate.mapping.Bag) {
            value.setTypeName(AutofetchBagType.class.getName());
        } else if (value instanceof org.hibernate.mapping.IdentifierBag) {
            value.setTypeName(AutofetchIdBagType.class.getName());
        } else if (value instanceof org.hibernate.mapping.List) {
            value.setTypeName(AutofetchListType.class.getName());
        } else if (value instanceof org.hibernate.mapping.Set) {
            value.setTypeName(AutofetchSetType.class.getName());
        } else {
            throw new UnsupportedOperationException("Collection type not supported: " + value.getClass());
        }
    }
}
