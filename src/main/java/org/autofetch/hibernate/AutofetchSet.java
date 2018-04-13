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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.autofetch.hibernate.CollectionTracker;
import org.autofetch.hibernate.Statistics;
import org.autofetch.hibernate.Trackable;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.collection.internal.PersistentSet;

/**
 * Based on org.hibernate.collection.PersistentSet
 * @author Ali Ibrahim <aibrahim@cs.utexas.edu>
 *
 */
@SuppressWarnings("unchecked")
public class AutofetchSet extends PersistentSet implements Trackable {

    private CollectionTracker collectionTracker = new CollectionTracker();

    public AutofetchSet() {
        super();
    }

    public AutofetchSet(SessionImplementor si, Set s) {
        super(si, s);
    }

    public AutofetchSet(SessionImplementor si) {
        super(si);
    }
    
    public void addTracker(Statistics tracker) {
        collectionTracker.addTracker(tracker);
    }

    public void addTrackers(Set<Statistics> trackers) {
        collectionTracker.addTrackers(trackers);
    }

    public boolean disableTracking() {
        boolean oldValue = collectionTracker.isTracking();
        collectionTracker.setTracking(false);
        return oldValue;
    }

    public boolean enableTracking() {
        boolean oldValue = collectionTracker.isTracking();
        collectionTracker.setTracking(true);
        return oldValue;
    }

    public void removeTracker(Statistics stats) {
        collectionTracker.removeTracker(stats);
    }

    private void accessed() {
        if (wasInitialized()) {
            collectionTracker.trackAccess(set);
        }
    }

    /**
     * @see java.util.Set#size()
     */
    @Override
    public int size() {
        int ret = super.size();
        if (wasInitialized()) {
            accessed();
        }
        return ret;
    }

    /**
     * @see java.util.Set#isEmpty()
     */
    @Override
    public boolean isEmpty() {
        boolean ret = super.isEmpty();
        if (wasInitialized()) {
            accessed();
        }
        return ret;
    }

    /**
     * @see java.util.Set#contains(Object)
     */
    @Override
    public boolean contains(Object object) {
        boolean ret = super.contains(object);
        if (wasInitialized()) {
            accessed();
        }
        return ret;
    }

    /**
     * @see java.util.Set#iterator()
     */
    @Override
    public Iterator iterator() {
        Iterator iter = super.iterator();
        if (wasInitialized()) {
            accessed();
        }
        return iter;
    }

    /**
     * @see java.util.Set#toArray()
     */
    @Override
    public Object[] toArray() {
        Object[] arr = super.toArray();
        if (wasInitialized()) {
            accessed();
        }
        return arr;
    }

    /**
     * @see java.util.Set#toArray(Object[])
     */
    @Override
    public Object[] toArray(Object[] array) {
        Object[] arr = super.toArray(array);
        if (wasInitialized()) {
            accessed();
        }
        return arr;
    }

    /**
     * @see java.util.Set#add(Object)
     */
    @Override
    public boolean add(Object value) {
        Boolean exists = isOperationQueueEnabled()
                ? readElementExistence(value) : null;
        if (exists == null) {
            initialize(true);
            accessed();
            if (set.add(value)) {
                dirty();
                return true;
            } else {
                return false;
            }
        } else {
            return super.add(value);
        }
    }

    /**
     * @see java.util.Set#remove(Object)
     */
    @Override
    public boolean remove(Object value) {
        boolean ret = super.remove(value);
        if (wasInitialized()) {
            accessed();
        }
        return ret;
    }

    /**
     * @see java.util.Set#containsAll(Collection)
     */
    @Override
    public boolean containsAll(Collection coll) {
        boolean ret = super.containsAll(coll);
        if (wasInitialized()) {
            accessed();
        }
        return ret;
    }

    /**
     * @see java.util.Set#addAll(Collection)
     */
    @Override
    public boolean addAll(Collection coll) {
        if (coll.size() > 0) {
            initialize(true);
            accessed();
            if (set.addAll(coll)) {
                dirty();
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * @see java.util.Set#retainAll(Collection)
     */
    @Override
    public boolean retainAll(Collection coll) {
        boolean val = super.retainAll(coll);
        if (wasInitialized()) {
            accessed();
        }
        return val;
    }

    /**
     * @see java.util.Set#removeAll(Collection)
     */
    @Override
    public boolean removeAll(Collection coll) {
        boolean val = super.removeAll(coll);
        if (wasInitialized()) {
            accessed();
        }
        return val;
    }

    @Override
    public void clear() {
        super.clear();
        if (wasInitialized()) {
            accessed();
        }
    }

    @Override
    public String toString() {
        //if (needLoading) return "asleep";
        String ret = super.toString();
        if (wasInitialized()) {
            accessed();
        }
        return ret;
    }

    @Override
    public boolean equals(Object other) {
        boolean ret = super.equals(other);
        if (wasInitialized()) {
            accessed();
        }
        return ret;
    }

    @Override
    public int hashCode() {
        int ret = super.hashCode();
        if (wasInitialized()) {
            accessed();
        }
        return ret;
    }
    
    public boolean isAccessed() {
        return collectionTracker.isAccessed();
    }
}


