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
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataContributor;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

import org.jboss.jandex.IndexView;

import com.google.auto.service.AutoService;

@SuppressWarnings("unused")
@AutoService(MetadataContributor.class)
public class AutofetchMetadataContributor implements MetadataContributor {

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
