/**
 * Copyright 2008 Ali Ibrahim
 * 
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
import java.lang.reflect.Method;
import java.util.Set;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

public class EntityProxyCallback implements MethodInterceptor, Serializable {
    
    private EntityTracker entityTracker;
    
	
	//Class used in mainly EntityProxyFactory, also used in the hibernate package
    public EntityProxyCallback(
            Set<Property> persistentProperties,
            ExtentManager extentManager) {
        entityTracker = new EntityTracker(persistentProperties, extentManager);
    }

    public Object intercept(Object obj, Method m, Object[] params,
            MethodProxy mp) throws Throwable {
        
        // Handle methods for toggling tracking and setting usage statistics
		// Handle different input for tracking settings
        if (params.length == 0) {
            if (m.getName().equals("disableTracking")) {
                boolean oldValue = entityTracker.isTracking();
                entityTracker.setTracking(false);
                return oldValue;
            } else if (m.getName().equals("enableTracking")) {
                boolean oldValue = entityTracker.isTracking();
                entityTracker.setTracking(true);
                return oldValue;
            } else if (m.getName().equals("isAccessed")) {
                return entityTracker.isAccessed();
            }
        } else if (params.length == 1) {
            if (m.getName().equals("addTracker")
                    && m.getParameterTypes()[0].equals(Statistics.class)) {
                entityTracker.addTracker((Statistics) params[0]);
                return null;
            } else if (m.getName().equals("addTrackers")
                    && m.getParameterTypes()[0].equals(Set.class)) {
                @SuppressWarnings("unchecked")
                Set<Statistics> newTrackers = (Set) params[0];
                entityTracker.addTrackers(newTrackers);
                return null;
            } else if (m.getName().equals("extendProfile")
                    && m.getParameterTypes()[0].equals(Statistics.class)) {
                entityTracker.extendProfile(
                        (Statistics) params[0],
                        obj);
                return null;
            } else if (m.getName().equals("removeTracker")
                    && m.getParameterTypes()[0].equals(Statistics.class)) {
                entityTracker.removeTracker((Statistics) params[0]);
                return null;
            }
        }

        entityTracker.trackAccess(obj);
        
        return mp.invokeSuper(obj, params);
    }
}