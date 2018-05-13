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

import org.hibernate.collection.internal.PersistentSet;
import org.hibernate.engine.spi.SessionImplementor;

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
 * Based on {@link PersistentSet}.
 *
 * @author Ali Ibrahim <aibrahim@cs.utexas.edu>
 */
@SuppressWarnings("unchecked")
public class AutofetchSet extends PersistentSet implements Trackable {

    private final CollectionTracker collectionTracker = new CollectionTracker();

    @SuppressWarnings("unused")
    protected AutofetchSet() {
        // Available only for serialization.
    }

    /**
     * Instantiates a lazy set (the underlying set is uninitialized).
     *
     * @param session The session to which this set will belong.
     */
    public AutofetchSet(SessionImplementor session) {
        super(session);
    }

    /**
     * Instantiates a non-lazy set (the underlying set is constructed from the incoming set reference).
     *
     * @param session The session to which this set will belong.
     * @param set     The underlying set data.
     */
    public AutofetchSet(SessionImplementor session, Set set) {
        super(session, set);
    }

    @Override
    public void addTracker(Statistics tracker) {
        this.collectionTracker.addTracker(tracker);
    }

    @Override
    public void addTrackers(Set<Statistics> trackers) {
        this.collectionTracker.addTrackers(trackers);
    }

    @Override
    public boolean disableTracking() {
        boolean oldValue = this.collectionTracker.isTracking();
        this.collectionTracker.setTracking(false);
        return oldValue;
    }

    @Override
    public boolean enableTracking() {
        boolean oldValue = this.collectionTracker.isTracking();
        this.collectionTracker.setTracking(true);
        return oldValue;
    }

    @Override
    public void removeTracker(Statistics stats) {
        this.collectionTracker.removeTracker(stats);
    }

    /**
     * @see java.util.Set#size()
     */
    @Override
    public int size() {
        int ret = super.size();
        this.accessed();
        return ret;
    }

    /**
     * @see java.util.Set#isEmpty()
     */
    @Override
    public boolean isEmpty() {
        boolean ret = super.isEmpty();
        this.accessed();
        return ret;
    }

    /**
     * @see java.util.Set#contains(Object)
     */
    @Override
    public boolean contains(Object object) {
        boolean ret = super.contains(object);
        this.accessed();
        return ret;
    }

    /**
     * @see java.util.Set#iterator()
     */
    @Override
    public Iterator iterator() {
        Iterator iter = super.iterator();
        this.accessed();
        return iter;
    }

    /**
     * @see java.util.Set#toArray()
     */
    @Override
    public Object[] toArray() {
        Object[] arr = super.toArray();
        this.accessed();
        return arr;
    }

    /**
     * @see java.util.Set#toArray(Object[])
     */
    @Override
    public Object[] toArray(Object[] array) {
        Object[] arr = super.toArray(array);
        this.accessed();
        return arr;
    }

    /**
     * @see java.util.Set#add(Object)
     */
    @Override
    public boolean add(Object value) {
        Boolean exists = isOperationQueueEnabled() ? readElementExistence(value) : null;
        if (exists == null) {
            initialize(true);
            this.accessed();
            if (this.set.add(value)) {
                this.dirty();
                return true;
            }

            return false;
        }

        return super.add(value);
    }

    /**
     * @see java.util.Set#remove(Object)
     */
    @Override
    public boolean remove(Object value) {
        boolean ret = super.remove(value);
        this.accessed();
        return ret;
    }

    /**
     * @see java.util.Set#containsAll(Collection)
     */
    @Override
    public boolean containsAll(Collection coll) {
        boolean ret = super.containsAll(coll);
        this.accessed();
        return ret;
    }

    /**
     * @see java.util.Set#addAll(Collection)
     */
    @Override
    public boolean addAll(Collection coll) {
        if (coll.size() > 0) {
            initialize(true);
            this.accessed();
            if (this.set.addAll(coll)) {
                this.dirty();
                return true;
            }

            return false;
        }

        return false;
    }

    /**
     * @see java.util.Set#retainAll(Collection)
     */
    @Override
    public boolean retainAll(Collection coll) {
        boolean val = super.retainAll(coll);
        this.accessed();
        return val;
    }

    /**
     * @see java.util.Set#removeAll(Collection)
     */
    @Override
    public boolean removeAll(Collection coll) {
        boolean val = super.removeAll(coll);
        this.accessed();
        return val;
    }

    @Override
    public void clear() {
        super.clear();
        this.accessed();
    }

    @Override
    public String toString() {
        String ret = super.toString();
        this.accessed();
        return ret;
    }

    @Override
    public boolean equals(Object other) {
        boolean ret = super.equals(other);
        this.accessed();
        return ret;
    }

    @Override
    public int hashCode() {
        int ret = super.hashCode();
        this.accessed();
        return ret;
    }

    @Override
    public boolean isAccessed() {
        return this.collectionTracker.isAccessed();
    }

    private void accessed() {
        if (wasInitialized()) {
            this.collectionTracker.trackAccess(this.set);
        }
    }
}
