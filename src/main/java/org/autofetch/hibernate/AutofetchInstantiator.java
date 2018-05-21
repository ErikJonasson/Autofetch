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
 */
package org.autofetch.hibernate;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.tuple.entity.PojoEntityInstantiator;

/**
 * This code is based on the instantiator implementations in the Hibernate source code.
 * It instantiates entities using proxies so that their accesses may be tracked.
 *
 * @author Ali Ibrahim <aibrahim@cs.utexas.edu>
 */
public class AutofetchInstantiator extends PojoEntityInstantiator {

	private final ExtentManager extentManager;

	private final Class<?> mappedClass;

	private Set<org.autofetch.hibernate.Property> persistentProperties;

	AutofetchInstantiator(
			EntityMetamodel entityMetamodel,
			PersistentClass persistentClass,
			ReflectionOptimizer.InstantiationOptimizer optimizer,
			ExtentManager extentManager) {
		super( entityMetamodel, persistentClass, optimizer );

		this.extentManager = extentManager;
		this.mappedClass = persistentClass.getMappedClass();

		this.persistentProperties = new HashSet<>();

		@SuppressWarnings("unchecked")
		Iterator<Property> propIter = persistentClass.getPropertyClosureIterator();
		while ( propIter.hasNext() ) {
			this.persistentProperties.add( new org.autofetch.hibernate.Property( propIter.next() ) );
		}
	}

	@Override
	public Object instantiate() {
		try {
			return applyInterception( EntityProxyFactory.getProxyInstance(
					mappedClass,
					persistentProperties,
					extentManager
			) );
		}
		catch (IllegalAccessException | NoSuchMethodException |
				InvocationTargetException | java.lang.InstantiationException e) {
			throw new HibernateException( "Unable to instantiate class", e );
		}
	}
}
