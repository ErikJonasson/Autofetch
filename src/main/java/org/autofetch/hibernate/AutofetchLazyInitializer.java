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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.internal.bytebuddy.PassThroughInterceptor;
import org.hibernate.bytecode.spi.BasicProxyFactory;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.ProxyConfiguration;
import org.hibernate.proxy.pojo.BasicLazyInitializer;
import org.hibernate.type.CompositeType;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;

/**
 * This class is based on {@link org.hibernate.proxy.pojo.javassist.JavassistLazyInitializer}.
 *
 * @author Ali Ibrahim <aibrahim@cs.utexas.edu>
 */
public class AutofetchLazyInitializer extends BasicLazyInitializer {

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( AutofetchLazyInitializer.class );

	private final EntityTracker entityTracker;

	private final Class[] interfaces;

	private boolean entityTrackersSet;

	private boolean constructed;

	private AutofetchLazyInitializer(
			String entityName,
			Class persistentClass,
			Class[] interfaces,
			Serializable id,
			Method getIdentifierMethod,
			Method setIdentifierMethod,
			CompositeType componentIdType,
			SharedSessionContractImplementor session,
			Set<Property> persistentProperties,
			boolean classOverridesEquals) {

		super( entityName, persistentClass, id, getIdentifierMethod, setIdentifierMethod,
			   componentIdType, session, classOverridesEquals
		);

		this.interfaces = interfaces;

		AutofetchService autofetchService = session.getFactory()
				.getServiceRegistry()
				.getService( AutofetchService.class );
		this.entityTracker = new EntityTracker( persistentProperties, autofetchService.getExtentManager() );
		this.entityTrackersSet = false;
	}

	@Override
	protected Object serializableProxy() {
		return new AutofetchSerializableProxy(
				getEntityName(),
				this.persistentClass,
				this.interfaces,
				getIdentifier(),
				( isReadOnlySettingAvailable() ?
						Boolean.valueOf( isReadOnly() ) :
						isReadOnlyBeforeAttachedToSession() ),
				this.getIdentifierMethod,
				this.setIdentifierMethod,
				this.componentIdType,
				this.entityTracker.getPersistentProperties()
		);
	}

	public static HibernateProxy getProxy(
			final String entityName,
			final Class persistentClass,
			final Class[] interfaces,
			final Method getIdentifierMethod,
			final Method setIdentifierMethod,
			final CompositeType componentIdType,
			final Serializable id,
			final SessionImplementor session,
			final Set<Property> persistentProperties) throws HibernateException {

		// note: interface is assumed to already contain HibernateProxy.class
		try {
			final AutofetchLazyInitializer lazyInitializer = new AutofetchLazyInitializer(
					entityName,
					persistentClass,
					interfaces,
					id,
					getIdentifierMethod,
					setIdentifierMethod,
					componentIdType,
					session,
					persistentProperties,
					ReflectHelper.overridesEquals( persistentClass )
			);
			final HibernateProxy proxy = (HibernateProxy) Environment.getBytecodeProvider().getProxyFactoryFactory()
					.buildBasicProxyFactory( interfaces.length == 1 ? persistentClass : null, interfaces )
					.getProxy();
			final Interceptor interceptor = new Interceptor( proxy, entityName, lazyInitializer );
			( (ProxyConfiguration) proxy ).$$_hibernate_set_interceptor( interceptor );
			lazyInitializer.constructed = true;
			return proxy;
		}
		catch (Throwable t) {
			final String message = LOG.bytecodeEnhancementFailed( entityName );
			LOG.error( message, t );
			throw new HibernateException( message, t );
		}
	}

	public static HibernateProxy getProxy(
			final BasicProxyFactory factory,
			final String entityName,
			final Class persistentClass,
			final Class[] interfaces,
			final Method getIdentifierMethod,
			final Method setIdentifierMethod,
			final CompositeType componentIdType,
			final Serializable id,
			final SharedSessionContractImplementor session,
			final Set<Property> persistentProperties) throws HibernateException {

		// note: interfaces is assumed to already contain HibernateProxy.class
		final AutofetchLazyInitializer lazyInitializer = new AutofetchLazyInitializer(
				entityName,
				persistentClass,
				interfaces,
				id,
				getIdentifierMethod,
				setIdentifierMethod,
				componentIdType,
				session,
				persistentProperties,
				ReflectHelper.overridesEquals( persistentClass )
		);

		final HibernateProxy proxy;
		try {
			proxy = (HibernateProxy) factory.getProxy();
		}
		catch (Exception e) {
			throw new HibernateException( "Bytecode Enhancement failed: " + persistentClass.getName(), e );
		}

		final Interceptor interceptor = new Interceptor( proxy, entityName, lazyInitializer );
		( (ProxyConfiguration) proxy ).$$_hibernate_set_interceptor( interceptor );
		lazyInitializer.constructed = true;

		return proxy;
	}

	private static class Interceptor extends PassThroughInterceptor {

		private final AutofetchLazyInitializer lazyInitializer;

		Interceptor(
				Object proxiedObject,
				String proxiedClassName,
				AutofetchLazyInitializer lazyInitializer) {
			super( proxiedObject, proxiedClassName );

			this.lazyInitializer = lazyInitializer;
		}

		@Override
		@RuntimeType
		public Object intercept(@This Object instance, @Origin Method method, @AllArguments Object[] arguments)
				throws Exception {
			final String methodName = method.getName();

			if ( lazyInitializer.constructed ) {
				Object result;
				try {
					result = lazyInitializer.invoke( method, arguments, instance );
				}
				catch (Throwable t) {
					throw new Exception( t.getCause() );
				}

				if ( result == INVOKE_IMPLEMENTATION ) {
					if ( arguments.length == 0 ) {
						switch ( methodName ) {
							case "enableTracking":
								return handleEnableTracking();
							case "disableTracking":
								return handleDisableTracking();
							case "isAccessed":
								return lazyInitializer.entityTracker.isAccessed();
						}
					}
					else if ( arguments.length == 1 ) {
						if ( methodName.equals( "addTracker" ) && method.getParameterTypes()[0].equals(
								Statistics.class ) ) {
							return handleAddTracked( arguments[0] );
						}
						else if ( methodName.equals( "addTrackers" ) && method.getParameterTypes()[0].equals(
								Set.class ) ) {
							return handleAddTrackers( arguments[0] );
						}
						else if ( methodName
								.equals( "removeTracker" ) && method.getParameterTypes()[0].equals( Statistics.class ) ) {
							lazyInitializer.entityTracker.removeTracker( (Statistics) arguments[0] );
							return handleRemoveTracker( arguments );
						}
						else if ( methodName
								.equals( "extendProfile" ) && method.getParameterTypes()[0].equals( Statistics.class ) ) {
							return extendProfile( arguments );
						}
					}

					final Object target = lazyInitializer.getImplementation();
					final Object returnValue;

					try {
						if ( ReflectHelper.isPublic( lazyInitializer.persistentClass, method ) ) {
							if ( !method.getDeclaringClass().isInstance( target ) ) {
								throw new ClassCastException(
										target.getClass().getName() + " incompatible with " + method.getDeclaringClass()
												.getName()
								);
							}
						}
						else {
							method.setAccessible( true );
						}

						returnValue = method.invoke( target, arguments );
						if ( returnValue == target ) {
							if ( returnValue.getClass().isInstance( instance ) ) {
								return instance;
							}
							else {
								LOG.narrowingProxy( returnValue.getClass() );
							}
						}

						return returnValue;
					}
					catch (InvocationTargetException ite) {
						throw new RuntimeException( ite.getTargetException() );
					}
					finally {
						if ( !lazyInitializer.entityTrackersSet && target instanceof Trackable ) {
							lazyInitializer.entityTrackersSet = true;
							Trackable entity = (Trackable) target;
							entity.addTrackers( lazyInitializer.entityTracker.getTrackers() );
							if ( lazyInitializer.entityTracker.isTracking() ) {
								entity.enableTracking();
							}
							else {
								entity.disableTracking();
							}
						}
					}
				}
				else {
					return result;
				}
			}
			else {
				// while constructor is running
				if ( methodName.equals( "getHibernateLazyInitializer" ) ) {
					return this;
				}
				else {
					return super.intercept( instance, method, arguments );
				}
			}
		}

		private Object handleDisableTracking() {
			boolean oldValue = lazyInitializer.entityTracker.isTracking();
			this.lazyInitializer.entityTracker.setTracking( false );
			if ( !lazyInitializer.isUninitialized() ) {
				Object o = lazyInitializer.getImplementation();
				if ( o instanceof Trackable ) {
					Trackable entity = (Trackable) o;
					entity.disableTracking();
				}
			}

			return oldValue;
		}

		private Object handleEnableTracking() {
			boolean oldValue = this.lazyInitializer.entityTracker.isTracking();
			this.lazyInitializer.entityTracker.setTracking( true );

			if ( !lazyInitializer.isUninitialized() ) {
				Object o = lazyInitializer.getImplementation();
				if ( o instanceof Trackable ) {
					Trackable entity = (Trackable) o;
					entity.enableTracking();
				}
			}

			return oldValue;
		}

		private Object extendProfile(Object[] params) {
			if ( !lazyInitializer.isUninitialized() ) {
				Object o = lazyInitializer.getImplementation();
				if ( o instanceof TrackableEntity ) {
					TrackableEntity entity = (TrackableEntity) o;
					entity.extendProfile( (Statistics) params[0] );
				}
			}
			else {
				throw new IllegalStateException( "Can't call extendProfile on unloaded self." );
			}

			return null;
		}

		private Object handleRemoveTracker(Object[] params) {
			if ( !lazyInitializer.isUninitialized() ) {
				Object o = lazyInitializer.getImplementation();
				if ( o instanceof Trackable ) {
					Trackable entity = (Trackable) o;
					entity.removeTracker( (Statistics) params[0] );
				}
			}
			return null;
		}

		@SuppressWarnings("unchecked")
		private Object handleAddTrackers(Object param) {
			Set<Statistics> newTrackers = (Set<Statistics>) param;
			this.lazyInitializer.entityTracker.addTrackers( newTrackers );
			if ( !lazyInitializer.isUninitialized() ) {
				Object o = lazyInitializer.getImplementation();
				if ( o instanceof Trackable ) {
					Trackable entity = (Trackable) o;
					entity.addTrackers( newTrackers );
				}
			}

			return null;
		}

		private Object handleAddTracked(Object param) {
			this.lazyInitializer.entityTracker.addTracker( (Statistics) param );
			if ( !lazyInitializer.isUninitialized() ) {
				Object o = lazyInitializer.getImplementation();
				if ( o instanceof Trackable ) {
					Trackable entity = (Trackable) o;
					entity.addTracker( (Statistics) param );
				}
			}

			return null;
		}
	}
}
