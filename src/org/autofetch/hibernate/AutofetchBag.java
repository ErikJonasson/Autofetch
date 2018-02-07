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
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.autofetch.hibernate.CollectionTracker;
import org.autofetch.hibernate.Statistics;
import org.autofetch.hibernate.Trackable;
import org.hibernate.engine.spi.SessionImplementor;

/**
 * Based on org.hibernate.collection.PersistentBag.
 * Usually delegates to super class except when operation
 * might add an element. In that case, it re-implements
 * the method so that it can record an access before the
 * element is added.
 * 
 * @author Ali Ibrahim <aibrahim@cs.utexas.edu>
 *
 */
@SuppressWarnings("unchecked")
public class AutofetchBag extends org.hibernate.collection.internal.PersistentBag implements Trackable {
    private CollectionTracker collectionTracker =
        new CollectionTracker();

    public AutofetchBag() {
        super();
    }

    public AutofetchBag(SessionImplementor si, Collection s) {
        super(si, s);
    }

    public AutofetchBag(SessionImplementor si) {
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
            collectionTracker.trackAccess(bag);
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
        if ( !isOperationQueueEnabled() ) {
            write();
            accessed();
            return bag.add(value);
        }
        else {
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
    public boolean addAll(Collection values) {
        if ( values.size()==0 ) return false;
        if ( !isOperationQueueEnabled() ) {
            write();
            accessed();
            return bag.addAll(values);
        }
        else {
            return super.addAll(values);
        }
    }

    @Override
    public void clear() {
        super.clear();
        if (wasInitialized()) {
            accessed();
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
    
    /**
     * @see java.util.List#add(int, Object)
     */
    @Override
    public void add(int i, Object o) {
        write();
        accessed();
        bag.add(i, o);
    }

    /**
     * @see java.util.List#addAll(int, Collection)
     */
    @Override
    public boolean addAll(int i, Collection c) {
        if ( c.size()>0 ) {
            write();
            accessed();
            return bag.addAll(i, c);
        }
        else {
            return false;
        }
    }

    /**
     * @see java.util.List#get(int)
     */
    @Override
    public Object get(int i) {
        Object val = super.get(i);
        if (wasInitialized()) {
            accessed();
        }
        return val;
    }

    /**
     * @see java.util.List#indexOf(Object)
     */
    @Override
    public int indexOf(Object o) {
        int val = super.indexOf(o);
        if (wasInitialized()) {
            accessed();
        }
        return val;
    }

    /**
     * @see java.util.List#lastIndexOf(Object)
     */
    @Override
    public int lastIndexOf(Object o) {
        int val = super.lastIndexOf(o);
        if (wasInitialized()) {
            accessed();
        }
        return val;
    }

    /**
     * @see java.util.List#listIterator()
     */
    @Override
    public ListIterator listIterator() {
        ListIterator val = super.listIterator();
        if (wasInitialized()) {
            accessed();
        }
        return val;
    }

    /**
     * @see java.util.List#listIterator(int)
     */
    @Override
    public ListIterator listIterator(int i) {
        ListIterator val = super.listIterator(i);
        if (wasInitialized()) {
            accessed();
        }
        return val;
    }

    /**
     * @see java.util.List#remove(int)
     */
    @Override
    public Object remove(int i) {
        Object val = super.remove(i);
        if (wasInitialized()) {
            accessed();
        }
        return val;
    }

    /**
     * @see java.util.List#set(int, Object)
     */
    @Override
    public Object set(int i, Object o) {
        write();
        accessed();
        return bag.set(i, o);
    }

    /**
     * @see java.util.List#subList(int, int)
     */
    @Override
    public List subList(int start, int end) {
        List val = super.subList(start, end);
        if (wasInitialized()) {
            accessed();
        }
        return val;
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
