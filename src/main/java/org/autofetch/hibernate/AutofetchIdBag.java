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

import org.hibernate.collection.internal.PersistentIdentifierBag;
import org.hibernate.engine.spi.SessionImplementor;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

/**
 * Based on {@link PersistentIdentifierBag}.
 * Usually delegates to super class except when operation
 * might add an element. In that case, it re-implements
 * the method so that it can record an access before the
 * element is added.
 *
 * @author Ali Ibrahim <aibrahim@cs.utexas.edu>
 */
@SuppressWarnings("unchecked")
public class AutofetchIdBag extends PersistentIdentifierBag implements Trackable {

    private final CollectionTracker collectionTracker = new CollectionTracker();

    @SuppressWarnings("unused")
    public AutofetchIdBag() {
        // Available only for serialization.
    }

    public AutofetchIdBag(SessionImplementor session) {
        super(session);
    }

    public AutofetchIdBag(SessionImplementor session, Collection coll) {
        super(session, coll);
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
        this.write();
        this.accessed();
        this.values.add(value);
        return true;
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
    public boolean addAll(Collection values) {
        if (values.size() > 0) {
            this.write();
            this.accessed();
            return values.addAll(values);
        }

        return false;
    }

    @Override
    public void clear() {
        super.clear();
        this.accessed();
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

    /**
     * @see java.util.List#add(int, Object)
     */
    @Override
    public void add(int index, Object element) {
        this.write();
        this.accessed();
        this.beforeAdd(index);
        this.values.add(index, element);
    }

    /**
     * @see java.util.List#get(int)
     */
    @Override
    public Object get(int i) {
        Object val = super.get(i);
        this.accessed();
        return val;
    }

    /**
     * @see java.util.List#indexOf(Object)
     */
    @Override
    public int indexOf(Object o) {
        int val = super.indexOf(o);
        this.accessed();
        return val;
    }

    /**
     * @see java.util.List#lastIndexOf(Object)
     */
    @Override
    public int lastIndexOf(Object o) {
        int val = super.lastIndexOf(o);
        this.accessed();
        return val;
    }

    /**
     * @see java.util.List#listIterator()
     */
    @Override
    public ListIterator listIterator() {
        ListIterator val = super.listIterator();
        this.accessed();
        return val;
    }

    /**
     * @see java.util.List#listIterator(int)
     */
    @Override
    public ListIterator listIterator(int i) {
        ListIterator val = super.listIterator(i);
        this.accessed();
        return val;
    }

    /**
     * @see java.util.List#remove(int)
     */
    @Override
    public Object remove(int i) {
        Object val = super.remove(i);
        this.accessed();
        return val;
    }

    /**
     * @see java.util.List#set(int, Object)
     */
    @Override
    public Object set(int index, Object element) {
        this.write();
        this.accessed();
        return values.set(index, element);
    }

    /**
     * @see java.util.List#subList(int, int)
     */
    @Override
    public List subList(int start, int end) {
        List val = super.subList(start, end);
        this.accessed();
        return val;
    }

    @Override
    public String toString() {
        //if (needLoading) return "asleep";
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
        return collectionTracker.isAccessed();
    }

    private void beforeAdd(int index) {
        for (int i = index; i < this.values.size(); i++) {
            this.identifiers.put(i + 1, this.identifiers.get(i));
        }

        this.identifiers.remove(index);
    }

    private void accessed() {
        if (this.wasInitialized()) {
            this.collectionTracker.trackAccess(this.values);
        }
    }
}
