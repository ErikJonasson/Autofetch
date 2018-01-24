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
 *
 */
package org.autofetch.hibernate;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

import net.sf.cglib.proxy.InvocationHandler;

import org.autofetch.hibernate.EntityTracker;
import org.autofetch.hibernate.Property;
import org.autofetch.hibernate.Statistics;
import org.autofetch.hibernate.Trackable;
import org.autofetch.hibernate.TrackableEntity;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.proxy.pojo.BasicLazyInitializer;
import org.hibernate.type.AbstractComponentType;
import org.hibernate.util.ReflectHelper;

/**
 * This class is based on org.hibernate.proxy.pjo.cglib.CGLIBLazyInitializer.
 * @author Ali Ibrahim <aibrahim@cs.utexas.edu>
 *
 */
public class AutofetchProxyCallback extends BasicLazyInitializer implements
        InvocationHandler {
    
    private EntityTracker entityTracker;
    
    private boolean entityTrackersSet;

    public AutofetchProxyCallback(String entityName, Class persistentClass,
            Serializable id, Method getIdentifierMethod,
            Method setIdentifierMethod, AbstractComponentType componentIdType,
            SessionImplementor session, Set<Property> persistentProperties) {
        super(entityName, persistentClass, id, getIdentifierMethod,
                setIdentifierMethod, componentIdType, session);
        AutofetchInterceptor ai =
            (AutofetchInterceptor) session.getInterceptor();
        this.entityTracker = new EntityTracker(
                persistentProperties, ai.getExtentManager());
        this.entityTrackersSet = false;
    }

    public Object invoke(final Object proxy, final Method m,
            final Object[] params) throws Throwable {
        // Handle methods for tracking
        if (params.length == 0) {
            if (m.getName().equals("disableTracking")) {
                boolean oldValue = entityTracker.isTracking();
                entityTracker.setTracking(false);
                if (!isUninitialized()) {
                    Object o = getImplementation();
                    if (o instanceof Trackable) {
                        Trackable entity = (Trackable) o;
                        entity.disableTracking();
                    }
                }
                return oldValue;
            } else if (m.getName().equals("enableTracking")) {
                boolean oldValue = entityTracker.isTracking();
                entityTracker.setTracking(true);
                if (!isUninitialized()) {
                    Object o = getImplementation();
                    if (o instanceof Trackable) {
                        Trackable entity = (Trackable) o;
                        entity.enableTracking();
                    }
                }
                return oldValue;
            } else if (m.getName().equals("isAccessed")) {
                return entityTracker.isAccessed();
            }
        } else if (params.length == 1) {
            if (m.getName().equals("addTracker")
                    && m.getParameterTypes()[0].equals(Statistics.class)) {
                entityTracker.addTracker((Statistics) params[0]);
                if (!isUninitialized()) {
                    Object o = getImplementation();
                    if (o instanceof Trackable) {
                        Trackable entity = (Trackable) o;
                        entity.addTracker((Statistics) params[0]);
                    }
                }
                return null;
            } else if (m.getName().equals("addTrackers")
                    && m.getParameterTypes()[0].equals(Set.class)) {
                @SuppressWarnings("unchecked")
                Set<Statistics> newTrackers = (Set) params[0];
                entityTracker.addTrackers(newTrackers);
                if (!isUninitialized()) {
                    Object o = getImplementation();
                    if (o instanceof Trackable) {
                        Trackable entity = (Trackable) o;
                        entity.addTrackers(newTrackers);
                    }
                }
                return null;
            } else if (m.getName().equals("removeTracker")
                    && m.getParameterTypes()[0].equals(Statistics.class)) {
                entityTracker.removeTracker((Statistics) params[0]);
                if (!isUninitialized()) {
                    Object o = getImplementation();
                    if (o instanceof Trackable) {
                        Trackable entity = (Trackable) o;
                        entity.removeTracker((Statistics) params[0]);
                    }
                }
                return null;
            }  else if (m.getName().equals("extendProfile")
                    && m.getParameterTypes()[0].equals(Statistics.class)) {
                if (!isUninitialized()) {
                    Object o = getImplementation();
                    if (o instanceof TrackableEntity) {
                        TrackableEntity entity = (TrackableEntity) o;
                        entity.extendProfile((Statistics) params[0]);
                    }
                } else {
                    throw new IllegalStateException(
                            "Can't call extendProfile on unloaded proxy.");
                }
                return null;
            }
        }

        // Actually invoke method
        Object result = invoke(m, params, proxy);
        if (result == INVOKE_IMPLEMENTATION) {
            Object target = getImplementation();
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
            try {
                final Object returnValue;
                if (ReflectHelper.isPublic(persistentClass, m)) {
                    if (!m.getDeclaringClass().isInstance(target)) {
                        throw new ClassCastException(target.getClass()
                                .getName());
                    }
                    returnValue = m.invoke(target, params);
                } else {
                    if (!m.isAccessible()) {
                        m.setAccessible(true);
                    }
                    returnValue = m.invoke(target, params);
                }
                return returnValue == target ? proxy : returnValue;
            } catch (InvocationTargetException ite) {
                throw ite.getTargetException();
            }
        } else {
            return result;
        }
    }

    @Override
    protected Object serializableProxy() {
        throw new UnsupportedOperationException(
                "Cannot serialize autofetch proxies.");
    }
}
