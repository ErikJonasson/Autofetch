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
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.proxy.AbstractSerializableProxy;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.pojo.javassist.JavassistLazyInitializer;
import org.hibernate.type.CompositeType;

public final class AutofetchSerializableProxy extends AbstractSerializableProxy {

	private Class persistentClass;
	private Class[] interfaces;
	private final String identifierGetterMethodName;
	private final Class identifierGetterMethodClass;
	private final String identifierSetterMethodName;
	private final Class identifierSetterMethodClass;
	private final Class[] identifierSetterMethodParams;
	private final CompositeType componentIdType;
	private Set<Property> persistentProperties;

	public AutofetchSerializableProxy(
			final String entityName,
			final Class persistentClass,
			final Class[] interfaces,
			final Serializable id,
			final Boolean readOnly,
			final Method getIdentifierMethod,
			final Method setIdentifierMethod,
			final CompositeType componentIdType,
			final Set<Property> persistentProperties) {

		super( entityName, id, readOnly );

		this.persistentClass = persistentClass;
		this.interfaces = interfaces;
		if ( getIdentifierMethod != null ) {
			identifierGetterMethodName = getIdentifierMethod.getName();
			identifierGetterMethodClass = getIdentifierMethod.getDeclaringClass();
		}
		else {
			identifierGetterMethodName = null;
			identifierGetterMethodClass = null;
		}

		if ( setIdentifierMethod != null ) {
			identifierSetterMethodName = setIdentifierMethod.getName();
			identifierSetterMethodClass = setIdentifierMethod.getDeclaringClass();
			identifierSetterMethodParams = setIdentifierMethod.getParameterTypes();
		}
		else {
			identifierSetterMethodName = null;
			identifierSetterMethodClass = null;
			identifierSetterMethodParams = null;
		}

		this.componentIdType = componentIdType;
		this.persistentProperties = persistentProperties;
	}

	/**
	 * Deserialization hook.  This method is called by JDK deserialization.  We use this hook
	 * to replace the serial form with a live form.
	 *
	 * @return The live form.
	 */
	private Object readResolve() {
		try {
			HibernateProxy proxy = AutofetchLazyInitializer.getProxy(
					getEntityName(),
					persistentClass,
					interfaces,
					identifierGetterMethodClass == null ?
							null
							:
							identifierGetterMethodClass.getDeclaredMethod( identifierGetterMethodName, (Class[]) null ),
					identifierSetterMethodName == null ?
							null
							:
							identifierSetterMethodClass.getDeclaredMethod(
									identifierSetterMethodName,
									identifierSetterMethodParams
							),
					componentIdType,
					getId(),
					null,
					persistentProperties
			);
			setReadOnlyBeforeAttachedToSession( (JavassistLazyInitializer) proxy.getHibernateLazyInitializer() );
			return proxy;
		}
		catch (NoSuchMethodException nsme) {
			throw new HibernateException(
					"could not create autofetch serializable proxy for entity: " + getEntityName(),
					nsme
			);
		}
	}
}
