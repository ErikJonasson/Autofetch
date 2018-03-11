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
import java.util.HashSet;
import java.util.Set;

//Used as an attribute in EntityProxyMethodHandler get access to the accessed valies
public class EntityTracker implements Serializable {

    private boolean accessed = false;

    private boolean tracking = true;

    private Set<Statistics> trackers = new HashSet<>();

    private Set<Property> persistentProperties;

    private ExtentManager extentManager;

    public EntityTracker(
            Set<Property> persistentProperties,
            ExtentManager extentManager) {
        this.persistentProperties = persistentProperties;
        this.extentManager = extentManager;
    }

    //Set flag and count how many times the entity has been accessed.  Calls extendProfile which adds it to another TP
    public void trackAccess(Object entity) throws Throwable {
        if (tracking && !accessed) {
            this.accessed = true;
            for (Statistics tracker : trackers) {
                tracker.loadedAssociation();
                extendProfile(tracker, entity);
            }
        }
    }

    public void removeTracker(Statistics tracker) {
        trackers.remove(tracker);
    }

    // Get the property value, if its trackable call private method extendTracker (which returns subgraph-tracker) and add that tracker to this entity
    public void extendProfile(Statistics tracker, Object entity) {
        for (Property prop : persistentProperties) {
            Object propVal = null;
            try {
                propVal = getPropertyValue(prop.getName(), entity);
            } catch (IllegalStateException e) {
                if (e.getCause() != null
                        && e.getCause() instanceof NoSuchMethodException) {
                    continue; // Weird property, just ignore it.
                }
            }
            if (propVal instanceof Trackable) {
                Trackable propEntity = (Trackable) propVal;
                Statistics propTracker = extendTracker(tracker,
                        prop.getName(), prop.isCollection());
                if (propTracker != null) {
                    propEntity.addTracker(propTracker);
                }
            }
        }
    }

    private static final Class[] NO_CLASSSES = new Class[0];

    private static final Object[] NO_OBJECTS = new Object[0];

    private Object getPropertyValue(String propName, Object o) { // This is vague
        // Construct method name using the property name and
        // convention for getters
        // Uppercase first letter of prop
        StringBuilder propCapitalized = new StringBuilder(propName);
        char firstLetter = Character.toUpperCase(propCapitalized.charAt(0));
        propCapitalized.setCharAt(0, firstLetter);
        String methodName = "get" + propCapitalized;
        try {
            Method m = o.getClass().getMethod(methodName, NO_CLASSSES);
            if (!m.isAccessible()) {
                m.setAccessible(true);
            }
            return m.invoke(o, NO_OBJECTS);
        } catch (Exception e) {
            throw new IllegalStateException("Could not access property: "
                    + propName + ":" + methodName, e);
        }
    }

    /**
     * @param tracker
     * @param assoc
     * @param collection
     * @return null if traversal profile could not be extended
     */
    //Get parentnode, if it doesnt have any subprofiles and cant add subprofile, return null. Otherwise return new, extended tracker
    private Statistics extendTracker(Statistics tracker, String assoc, boolean collection) {
        TraversalProfile parentNode = tracker.getProfileNode();
        if (!parentNode.hasSubProfile(assoc)) {
            if (!extentManager.addSubProfile(parentNode, assoc, collection)) {
                return null;
            }
        }

        Statistics newTracker = parentNode.getSubProfileStats(assoc);
        newTracker.incrementTotal(1);
        return newTracker;
    }

    public Set<Statistics> getTrackers() {
        return trackers;
    }

    public void addTracker(Statistics newTracker) {
        trackers.add(newTracker);
    }

    public void addTrackers(Set<Statistics> newTrackers) {
        trackers.addAll(newTrackers);
    }

    public Set<Property> getPersistentProperties() {
        return persistentProperties;
    }

    public void setPersistentProperties(Set<Property> persistentProperties) {
        this.persistentProperties = persistentProperties;
    }

    public boolean isTracking() {
        return tracking;
    }

    public void setTracking(boolean tracking) {
        this.tracking = tracking;
    }

    public boolean isAccessed() {
        return accessed;
    }
}
