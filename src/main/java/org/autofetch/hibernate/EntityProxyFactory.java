/**
 * Copyright 2008 Ali Ibrahim
 * <p>
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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.hibernate.cfg.Environment;
import org.hibernate.proxy.ProxyConfiguration;

//Class holding Factories for all classes, Constructors for all classes, sets a callbackfilter on the enhancer (which does what exactly?), Enhancer takes care of callbacks
public class EntityProxyFactory {

	private static final ConcurrentMap<Class<?>, Constructor<?>> entityConstructorMap = new ConcurrentHashMap<>();

	private static <T> Constructor<T> getDefaultConstructor(Class<T> clazz) throws NoSuchMethodException {
		Constructor<T> constructor = clazz.getDeclaredConstructor();
		if ( !constructor.isAccessible() ) {
			constructor.setAccessible( true );
		}

		return constructor;
	}

	static Object getProxyInstance(
			Class persistentClass,
			Set<Property> persistentProperties,
			ExtentManager extentManager)
			throws NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {

		if ( Modifier.isFinal( persistentClass.getModifiers() ) ) {
			// Use the default constructor, because final classes cannot be inherited.
			return useDefaultConstructor( persistentClass );
		}

		final ProxyConfiguration proxy = (ProxyConfiguration) Environment.getBytecodeProvider()
				.getProxyFactoryFactory()
				.buildBasicProxyFactory( persistentClass, new Class[] { TrackableEntity.class } )
				.getProxy();
		proxy.$$_hibernate_set_interceptor( new EntityProxyMethodHandler(
				proxy,
				persistentClass.getName(),
				persistentProperties,
				extentManager
		) );
		return proxy;
	}

	private static Object useDefaultConstructor(Class<?> clazz)
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
		if ( !entityConstructorMap.containsKey( clazz ) ) {
			entityConstructorMap.put( clazz, getDefaultConstructor( clazz ) );
		}

		final Constructor<?> c = entityConstructorMap.get( clazz );
		return c.newInstance();
	}
}
