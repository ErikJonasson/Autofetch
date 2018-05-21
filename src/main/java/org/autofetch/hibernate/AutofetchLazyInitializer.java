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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.ProxyConfiguration;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.proxy.pojo.BasicLazyInitializer;
import org.hibernate.type.CompositeType;

/**
 * This class is based on {@link org.hibernate.proxy.pojo.javassist.JavassistLazyInitializer}.
 *
 * @author Ali Ibrahim <aibrahim@cs.utexas.edu>
 */
public class AutofetchLazyInitializer extends BasicLazyInitializer implements ProxyConfiguration.Interceptor {

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( AutofetchLazyInitializer.class );

	private final EntityTracker entityTracker;

	private final Class[] interfaces;

	private AutofetchLazyInitializer(
			String entityName,
			Class<?> persistentClass,
			Class<?>[] interfaces,
			Serializable id,
			Method getIdentifierMethod,
			Method setIdentifierMethod,
			CompositeType componentIdType,
			SharedSessionContractImplementor session,
			Set<Property> persistentProperties,
			boolean overridesEquals) {

		super(
				entityName,
				persistentClass,
				id,
				getIdentifierMethod,
				setIdentifierMethod,
				componentIdType,
				Objects.requireNonNull( session, "Hibernate session cannot be null" ),
				overridesEquals
		);

		this.interfaces = interfaces;

		AutofetchService autofetchService = session.getFactory()
				.getServiceRegistry()
				.getService( AutofetchService.class );
		this.entityTracker = new EntityTracker( persistentProperties, autofetchService.getExtentManager() );
	}

	@Override
	public Object intercept(Object proxy, Method thisMethod, Object[] args) throws Throwable {
		Object result = this.invoke( thisMethod, args, proxy );
		if ( result == INVOKE_IMPLEMENTATION ) {
			final String methodName = thisMethod.getName();
			if ( args.length == 0 ) {
				switch ( methodName ) {
					case "enableTracking":
						return handleEnableTracking();
					case "disableTracking":
						return handleDisableTracking();
					case "isAccessed":
						return entityTracker.isAccessed();
				}
			}
			else if ( args.length == 1 ) {
				if ( methodName.equals( "addTracker" ) && thisMethod.getParameterTypes()[0].equals(
						Statistics.class ) ) {
					return handleAddTracked( args[0] );
				}
				else if ( methodName.equals( "addTrackers" ) && thisMethod.getParameterTypes()[0].equals(
						Set.class ) ) {
					return handleAddTrackers( args[0] );
				}
				else if ( methodName
						.equals( "removeTracker" ) && thisMethod.getParameterTypes()[0].equals( Statistics.class ) ) {
					entityTracker.removeTracker( (Statistics) args[0] );
					return handleRemoveTracker( args );
				}
				else if ( methodName
						.equals( "extendProfile" ) && thisMethod.getParameterTypes()[0].equals( Statistics.class ) ) {
					return extendProfile( args );
				}
			}

			Object target = getImplementation();
			final Object returnValue;
			try {
				if ( ReflectHelper.isPublic( persistentClass, thisMethod ) ) {
					if ( !thisMethod.getDeclaringClass().isInstance( target ) ) {
						throw new ClassCastException(
								target.getClass().getName()
										+ " incompatible with "
										+ thisMethod.getDeclaringClass().getName()
						);
					}
					returnValue = thisMethod.invoke( target, args );
				}
				else {
					thisMethod.setAccessible( true );
					returnValue = thisMethod.invoke( target, args );
				}

				if ( returnValue == target ) {
					if ( returnValue.getClass().isInstance( proxy ) ) {
						return proxy;
					}
					else {
						LOG.narrowingProxy( returnValue.getClass() );
					}
				}
				return returnValue;
			}
			catch (InvocationTargetException ite) {
				throw ite.getTargetException();
			}
		}
		else {
			return result;
		}
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

	static HibernateProxy getProxy(
			final String entityName,
			final Class<?> persistentClass,
			final Class<?>[] interfaces,
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
			ProxyFactory proxyFactory = Environment.getBytecodeProvider().getProxyFactoryFactory()
					.buildProxyFactory( session.getFactory() );
			proxyFactory.postInstantiate(
					entityName,
					persistentClass,
					new HashSet<>( Arrays.asList( interfaces ) ),
					getIdentifierMethod,
					setIdentifierMethod,
					componentIdType
			);
			proxy = proxyFactory.getProxy( id, session );
			( (ProxyConfiguration) proxy ).$$_hibernate_set_interceptor( lazyInitializer );
		}
		catch (Exception e) {
			String msg = LOG.bytecodeEnhancementFailed( persistentClass.getName() );
			LOG.error( msg );
			throw new HibernateException( msg, e );
		}

		( (ProxyConfiguration) proxy ).$$_hibernate_set_interceptor( lazyInitializer );

		return proxy;
	}

	private Object handleDisableTracking() {
		boolean oldValue = entityTracker.isTracking();
		this.entityTracker.setTracking( false );
		if ( !isUninitialized() ) {
			Object o = getImplementation();
			if ( o instanceof Trackable ) {
				Trackable entity = (Trackable) o;
				entity.disableTracking();
			}
		}

		return oldValue;
	}

	private Object handleEnableTracking() {
		boolean oldValue = this.entityTracker.isTracking();
		this.entityTracker.setTracking( true );

		if ( !isUninitialized() ) {
			Object o = getImplementation();
			if ( o instanceof Trackable ) {
				Trackable entity = (Trackable) o;
				entity.enableTracking();
			}
		}

		return oldValue;
	}

	private Object extendProfile(Object[] params) {
		if ( !isUninitialized() ) {
			Object o = getImplementation();
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
		if ( !isUninitialized() ) {
			Object o = getImplementation();
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
		this.entityTracker.addTrackers( newTrackers );
		if ( !isUninitialized() ) {
			Object o = getImplementation();
			if ( o instanceof Trackable ) {
				Trackable entity = (Trackable) o;
				entity.addTrackers( newTrackers );
			}
		}

		return null;
	}

	private Object handleAddTracked(Object param) {
		this.entityTracker.addTracker( (Statistics) param );
		if ( !isUninitialized() ) {
			Object o = getImplementation();
			if ( o instanceof Trackable ) {
				Trackable entity = (Trackable) o;
				entity.addTracker( (Statistics) param );
			}
		}

		return null;
	}
}
