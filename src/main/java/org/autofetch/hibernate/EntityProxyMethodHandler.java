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

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Set;

import org.hibernate.bytecode.internal.bytebuddy.PassThroughInterceptor;
import org.hibernate.proxy.ProxyConfiguration;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;

public class EntityProxyMethodHandler extends PassThroughInterceptor
		implements ProxyConfiguration.Interceptor, Serializable {

	private final EntityTracker entityTracker;

	EntityProxyMethodHandler(
			Object proxiedObject,
			String proxiedClassName,
			Set<Property> persistentProperties,
			ExtentManager extentManager) {
		super( proxiedObject, proxiedClassName );
		this.entityTracker = new EntityTracker( persistentProperties, extentManager );
	}

	@Override
	@RuntimeType
	public Object intercept(@This Object instance, @Origin Method method, @AllArguments Object[] arguments)
			throws Exception {
		if ( arguments.length == 0 ) {
			switch ( method.getName() ) {
				case "disableTracking": {
					boolean oldValue = entityTracker.isTracking();
					entityTracker.setTracking( false );
					return oldValue;
				}
				case "enableTracking": {
					boolean oldValue = entityTracker.isTracking();
					entityTracker.setTracking( true );
					return oldValue;
				}
				case "isAccessed":
					return entityTracker.isAccessed();
			}
		}
		else if ( arguments.length == 1 ) {
			if ( method.getName().equals( "addTracker" ) && method.getParameterTypes()[0].equals( Statistics.class ) ) {
				entityTracker.addTracker( (Statistics) arguments[0] );
				return null;
			}
			else if ( method.getName().equals( "addTrackers" ) && method.getParameterTypes()[0].equals( Set.class ) ) {
				@SuppressWarnings("unchecked")
				Set<Statistics> newTrackers = (Set) arguments[0];
				entityTracker.addTrackers( newTrackers );
				return null;
			}
			else if ( method.getName()
					.equals( "extendProfile" ) && method.getParameterTypes()[0].equals( Statistics.class ) ) {
				entityTracker.extendProfile( (Statistics) arguments[0], instance );
				return null;
			}
			else if ( method.getName()
					.equals( "removeTracker" ) && method.getParameterTypes()[0].equals( Statistics.class ) ) {
				entityTracker.removeTracker( (Statistics) arguments[0] );
				return null;
			}
		}

		entityTracker.trackAccess( instance );
		return super.intercept( instance, method, arguments );
	}
}