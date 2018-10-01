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

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.type.CompositeType;

/**
 * Based on @{link JavassistProxyFactory}.
 *
 * @author Ali Ibrahim <aibrahim@cs.utexas.edu>
 */
public class AutofetchProxyFactory implements ProxyFactory {

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger(AutofetchProxyFactory.class);

	private static final Class<?>[] NO_CLASSES = new Class<?>[0];

	private Class<?> proxiedClass;
	private String entityName;
	private Class<?>[] interfaces;
	private Method getIdentifierMethod;
	private Method setIdentifierMethod;
	private CompositeType componentIdType;

	private Set<org.autofetch.hibernate.Property> persistentProperties;

	public AutofetchProxyFactory(PersistentClass persistentClass) {
		this.proxiedClass = persistentClass.getMappedClass();
		this.persistentProperties = new HashSet<>();

		@SuppressWarnings("unchecked")
		Iterator<Property> propIter = (Iterator<Property>) persistentClass.getPropertyClosureIterator();
		while (propIter.hasNext()) {
			this.persistentProperties.add(new org.autofetch.hibernate.Property(propIter.next()));
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void postInstantiate(String entityName, Class persistentClass, Set interfaces, Method getIdentifierMethod,
			Method setIdentifierMethod, CompositeType componentIdType) throws HibernateException {

		this.entityName = entityName;
		this.proxiedClass = persistentClass;
		this.interfaces = (Class<?>[]) interfaces.toArray(NO_CLASSES);
		this.getIdentifierMethod = getIdentifierMethod;
		this.setIdentifierMethod = setIdentifierMethod;
		this.componentIdType = componentIdType;
	}

	@Override
	public HibernateProxy getProxy(Serializable id, SharedSessionContractImplementor session) throws HibernateException {
		return AutofetchLazyInitializer.getProxy(entityName, proxiedClass, interfaces, getIdentifierMethod,
				setIdentifierMethod, componentIdType, id, session, persistentProperties);
	}
}
