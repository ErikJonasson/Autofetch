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

import javassist.util.proxy.MethodHandler;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Set;

public class EntityProxyMethodHandler implements MethodHandler, Serializable {

    private final EntityTracker entityTracker;

    public EntityProxyMethodHandler(Set<Property> persistentProperties, ExtentManager extentManager) {
        this.entityTracker = new EntityTracker(persistentProperties, extentManager);
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