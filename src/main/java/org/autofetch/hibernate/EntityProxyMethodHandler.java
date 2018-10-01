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
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import org.hibernate.proxy.ProxyConfiguration;
import org.hibernate.proxy.ProxyConfiguration.Interceptor;

import javassist.util.proxy.MethodHandler;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.BindingPriority;
import net.bytebuddy.implementation.bind.annotation.DefaultCall;
import net.bytebuddy.implementation.bind.annotation.Morph;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.Super;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.SuperMethod;
import net.bytebuddy.implementation.bind.annotation.This;

public class EntityProxyMethodHandler implements MethodHandler, Serializable {


	private static final long serialVersionUID = 1L;
	private final EntityTracker entityTracker;

	EntityProxyMethodHandler(
			Set<Property> persistentProperties,
			ExtentManager extentManager) {
		this.entityTracker = new EntityTracker(persistentProperties, extentManager );
	}


	@Override
	public Object invoke(Object obj, Method thisMethod, Method proceed, Object[] args) throws Throwable {
	    if (args.length == 0) {
            if (thisMethod.getName().equals("disableTracking")) {
                boolean oldValue = entityTracker.isTracking();
                entityTracker.setTracking(false);
                return oldValue;
            } else if (thisMethod.getName().equals("enableTracking")) {
                boolean oldValue = entityTracker.isTracking();
                entityTracker.setTracking(true);
                return oldValue;
            } else if (thisMethod.getName().equals("isAccessed")) {
                return entityTracker.isAccessed();
            }
            else if (thisMethod.getName().equals("hashCode")) {

            }

        } else if (args.length == 1) {
            if (thisMethod.getName().equals("addTracker") && thisMethod.getParameterTypes()[0].equals(Statistics.class)) {
                entityTracker.addTracker((Statistics) args[0]);
                return null;
            } else if (thisMethod.getName().equals("addTrackers") && thisMethod.getParameterTypes()[0].equals(Set.class)) {
                @SuppressWarnings("unchecked")
                Set<Statistics> newTrackers = (Set) args[0];
                entityTracker.addTrackers(newTrackers);
                return null;
            } else if (thisMethod.getName().equals("extendProfile") && thisMethod.getParameterTypes()[0].equals(Statistics.class)) {
                entityTracker.extendProfile((Statistics) args[0], obj);
                return null;
            } else if (thisMethod.getName().equals("removeTracker") && thisMethod.getParameterTypes()[0].equals(Statistics.class)) {
                entityTracker.removeTracker((Statistics) args[0]);
                return null;
            }
        }

        entityTracker.trackAccess(obj);

        return proceed.invoke(obj, args);
    }
}


//	@Override
//	@RuntimeType
//	public Object intercept(@This Object instance,  @Origin Method method, @AllArguments Object[] arguments)
//			throws Exception {
//		final String methodName = method.getName();
//		if ( "toString".equals( methodName ) ) {
//			return proxiedClassName + "@" + System.identityHashCode( instance );
//		}
//		else if ( "equals".equals( methodName ) ) {
//			return proxiedObject == instance;
//		}
//		else if ( "hashCode".equals( methodName ) ) {
//			return System.identityHashCode( instance );
//		}
//		else if ( arguments.length == 0 ) {
//			switch ( methodName ) {
//				case "disableTracking": {
//					boolean oldValue = entityTracker.isTracking();
//					entityTracker.setTracking( false );
//					return oldValue;
//				}
//				case "enableTracking": {
//					boolean oldValue = entityTracker.isTracking();
//					entityTracker.setTracking( true );
//					return oldValue;
//				}
//				case "isAccessed":
//					return entityTracker.isAccessed();
//
//				default:
//					break;
//			}
//		}
//		else if ( arguments.length == 1 ) {
//			if ( methodName.equals( "addTracker" ) && method.getParameterTypes()[0].equals( Statistics.class ) ) {
//				entityTracker.addTracker( (Statistics) arguments[0] );
//				return null;
//			}
//			else if ( methodName.equals( "addTrackers" ) && method.getParameterTypes()[0].equals( Set.class ) ) {
//				@SuppressWarnings("unchecked")
//				Set<Statistics> newTrackers = (Set) arguments[0];
//				entityTracker.addTrackers( newTrackers );
//				return null;
//			}
//			else if ( methodName
//					.equals( "extendProfile" ) && method.getParameterTypes()[0].equals( Statistics.class ) ) {
//				entityTracker.extendProfile( (Statistics) arguments[0], instance );
//				return null;
//			}
//			else if ( methodName
//					.equals( "removeTracker" ) && method.getParameterTypes()[0].equals( Statistics.class ) ) {
//				entityTracker.removeTracker( (Statistics) arguments[0] );
//				return null;
//			}
//		}
//
//		entityTracker.trackAccess( instance );
//		return method.invoke( instance, arguments );
//	}
//}