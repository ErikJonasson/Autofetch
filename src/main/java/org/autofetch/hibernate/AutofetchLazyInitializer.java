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

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.Proxy;
import javassist.util.proxy.ProxyFactory;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.pojo.BasicLazyInitializer;
import org.hibernate.proxy.pojo.javassist.JavassistLazyInitializer;
import org.hibernate.type.CompositeType;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

/**
 * This class is based on {@link JavassistLazyInitializer}.
 *
 * @author Ali Ibrahim <aibrahim@cs.utexas.edu>
 */
public class AutofetchLazyInitializer extends BasicLazyInitializer implements MethodHandler {

    private static final CoreMessageLogger LOG = CoreLogging.messageLogger(AutofetchLazyInitializer.class);

    private EntityTracker entityTracker;

    private boolean entityTrackersSet;

    private Class[] interfaces;

    private boolean constructed;

    // Check whether the last paremeter, set to false for now
    private AutofetchLazyInitializer(String entityName,
                                     Class persistentClass,
                                     Class[] interfaces,
                                     Serializable id,
                                     Method getIdentifierMethod,
                                     Method setIdentifierMethod,
                                     CompositeType componentIdType,
                                     SessionImplementor session,
                                     Set<Property> persistentProperties,
                                     boolean classOverridesEquals) {

        super(entityName, persistentClass, id, getIdentifierMethod, setIdentifierMethod,
                componentIdType, session, classOverridesEquals);

        this.interfaces = interfaces;

        AutofetchService autofetchService = session.getFactory().getServiceRegistry().getService(AutofetchService.class);
        this.entityTracker = new EntityTracker(persistentProperties, autofetchService.getExtentManager());
        this.entityTrackersSet = false;
    }

    @Override
    public Object invoke(final Object proxy, final Method thisMethod, final Method proceed, final Object[] args) throws Throwable {
        if (this.constructed) {
            Object result;
            try {
                result = this.invoke(thisMethod, args, proxy);
            } catch (Throwable t) {
                throw new Exception(t.getCause());
            }

            if (result == INVOKE_IMPLEMENTATION) {
                // Handle methods for tracking
                if (args.length == 0) {
                    switch (thisMethod.getName()) {
                        case "enableTracking":
                            return handleEnableTracking();
                        case "disableTracking":
                            return handleDisableTracking();
                        case "isAccessed":
                            return entityTracker.isAccessed();
                    }
                } else if (args.length == 1) {
                    if (thisMethod.getName().equals("addTracker") && thisMethod.getParameterTypes()[0].equals(Statistics.class)) {
                        return handleAddTracked(args[0]);
                    } else if (thisMethod.getName().equals("addTrackers") && thisMethod.getParameterTypes()[0].equals(Set.class)) {
                        return handleAddTrackers(args[0]);
                    } else if (thisMethod.getName().equals("removeTracker") && thisMethod.getParameterTypes()[0].equals(Statistics.class)) {
                        entityTracker.removeTracker((Statistics) args[0]);
                        return handleRemoveTracker(args);
                    } else if (thisMethod.getName().equals("extendProfile") && thisMethod.getParameterTypes()[0].equals(Statistics.class)) {
                        return extendProfile(args);
                    }
                }

                final Object target = getImplementation();
                final Object returnValue;

                try {
                    if (ReflectHelper.isPublic(persistentClass, thisMethod)) {
                        if (!thisMethod.getDeclaringClass().isInstance(target)) {
                            throw new ClassCastException(
                                    target.getClass().getName() + " incompatible with " + thisMethod.getDeclaringClass().getName()
                            );
                        }
                    } else {
                        thisMethod.setAccessible(true);
                    }

                    returnValue = thisMethod.invoke(target, args);
                    if (returnValue == target) {
                        if (returnValue.getClass().isInstance(proxy)) {
                            return proxy;
                        } else {
                            LOG.narrowingProxy(returnValue.getClass());
                        }
                    }

                    return returnValue;
                } catch (InvocationTargetException ite) {
                    throw ite.getTargetException();
                } finally {
                    if (!entityTrackersSet && target instanceof Trackable) {
                        entityTrackersSet = true;
                        Trackable entity = (Trackable) target;
                        entity.addTrackers(entityTracker.getTrackers());
                        if (entityTracker.isTracking()) {
                            entity.enableTracking();
                        } else {
                            entity.disableTracking();
                        }
                    }
                }
            } else {
                return result;
            }
        } else {
            // while constructor is running
            if (thisMethod.getName().equals("getHibernateLazyInitializer")) {
                return this;
            } else {
                return proceed.invoke(proxy, args);
            }
        }
    }

    private Object handleDisableTracking() {
        boolean oldValue = entityTracker.isTracking();
        this.entityTracker.setTracking(false);
        if (!isUninitialized()) {
            Object o = getImplementation();
            if (o instanceof Trackable) {
                Trackable entity = (Trackable) o;
                entity.disableTracking();
            }
        }

        return oldValue;
    }

    private Object handleEnableTracking() {
        boolean oldValue = this.entityTracker.isTracking();
        this.entityTracker.setTracking(true);

        if (!isUninitialized()) {
            Object o = getImplementation();
            if (o instanceof Trackable) {
                Trackable entity = (Trackable) o;
                entity.enableTracking();
            }
        }

        return oldValue;
    }

    private Object extendProfile(Object[] params) {
        if (!isUninitialized()) {
            Object o = getImplementation();
            if (o instanceof TrackableEntity) {
                TrackableEntity entity = (TrackableEntity) o;
                entity.extendProfile((Statistics) params[0]);
            }
        } else {
            throw new IllegalStateException("Can't call extendProfile on unloaded self.");
        }

        return null;
    }

    private Object handleRemoveTracker(Object[] params) {
        if (!isUninitialized()) {
            Object o = getImplementation();
            if (o instanceof Trackable) {
                Trackable entity = (Trackable) o;
                entity.removeTracker((Statistics) params[0]);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Object handleAddTrackers(Object param) {
        Set<Statistics> newTrackers = (Set<Statistics>) param;
        this.entityTracker.addTrackers(newTrackers);
        if (!isUninitialized()) {
            Object o = getImplementation();
            if (o instanceof Trackable) {
                Trackable entity = (Trackable) o;
                entity.addTrackers(newTrackers);
            }
        }

        return null;
    }

    private Object handleAddTracked(Object param) {
        this.entityTracker.addTracker((Statistics) param);
        if (!isUninitialized()) {
            Object o = getImplementation();
            if (o instanceof Trackable) {
                Trackable entity = (Trackable) o;
                entity.addTracker((Statistics) param);
            }
        }

        return null;
    }

    @Override
    protected Object serializableProxy() {
        return new AutofetchSerializableProxy(
                getEntityName(),
                this.persistentClass,
                this.interfaces,
                getIdentifier(),
                (isReadOnlySettingAvailable() ? Boolean.valueOf(isReadOnly()) : isReadOnlyBeforeAttachedToSession()),
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
            final AutofetchLazyInitializer instance = new AutofetchLazyInitializer(
                    entityName,
                    persistentClass,
                    interfaces,
                    id,
                    getIdentifierMethod,
                    setIdentifierMethod,
                    componentIdType,
                    session,
                    persistentProperties,
                    ReflectHelper.overridesEquals(persistentClass)
            );

            final ProxyFactory factory = new ProxyFactory();
            factory.setSuperclass(interfaces.length == 1 ? persistentClass : null);
            factory.setInterfaces(interfaces);
            factory.setFilter(FINALIZE_FILTER);
            Class cl = factory.createClass();
            final HibernateProxy proxy = (HibernateProxy) cl.newInstance();
            ((Proxy) proxy).setHandler(instance);
            instance.constructed = true;
            return proxy;
        } catch (Throwable t) {
            LOG.error(LOG.javassistEnhancementFailed(entityName), t);
            throw new HibernateException(LOG.javassistEnhancementFailed(entityName), t);
        }
    }

    public static HibernateProxy getProxy(
            final Class factory,
            final String entityName,
            final Class persistentClass,
            final Class[] interfaces,
            final Method getIdentifierMethod,
            final Method setIdentifierMethod,
            final CompositeType componentIdType,
            final Serializable id,
            final SessionImplementor session,
            final Set<Property> persistentProperties) throws HibernateException {

        // note: interfaces is assumed to already contain HibernateProxy.class
        final AutofetchLazyInitializer instance = new AutofetchLazyInitializer(
                entityName,
                persistentClass,
                interfaces,
                id,
                getIdentifierMethod,
                setIdentifierMethod,
                componentIdType,
                session,
                persistentProperties,
                ReflectHelper.overridesEquals(persistentClass)
        );

        final HibernateProxy proxy;
        try {
            proxy = (HibernateProxy) factory.newInstance();
        } catch (Exception e) {
            throw new HibernateException("Javassist Enhancement failed: " + persistentClass.getName(), e);
        }

        ((Proxy) proxy).setHandler(instance);
        instance.constructed = true;

        return proxy;
    }

    private static final MethodFilter FINALIZE_FILTER = new MethodFilter() {

        @Override
        public boolean isHandled(Method m) {
            // skip finalize methods
            return !(m.getParameterTypes().length == 0 && m.getName().equals("finalize"));
        }
    };
}
