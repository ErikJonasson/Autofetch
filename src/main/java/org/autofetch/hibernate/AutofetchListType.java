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

import org.hibernate.HibernateException;
import org.hibernate.collection.internal.PersistentList;
import org.hibernate.collection.internal.PersistentSet;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.ListType;
import org.hibernate.type.TypeFactory.TypeScope;
import org.hibernate.usertype.UserCollectionType;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * This class is based on org.hibernate.type.ListType.
 *
 * @author Ali Ibrahim <aibrahim@cs.utexas.edu>
 */
public class AutofetchListType implements UserCollectionType {

	public AutofetchListType() {
		
	}
	
    public PersistentCollection instantiate(SessionImplementor session,
                                            CollectionPersister persister, Serializable key) {
        return new AutofetchList(session);
    }

    @Override
    public PersistentCollection wrap(SessionImplementor session, Object collection) {
        return new AutofetchList(session, (java.util.List) collection);
    }

	@Override
	public PersistentCollection instantiate(SessionImplementor session, CollectionPersister persister)
			throws HibernateException {
		return new AutofetchList(session);
	}

	@Override
	public Iterator getElementsIterator(Object collection) {
		return ((PersistentList) collection).iterator();
	}

	@Override
	public boolean contains(Object collection, Object entity) {
		return ((PersistentList) collection).contains(entity);
	}

	@Override
	public Object indexOf(Object collection, Object entity) {
		return ((PersistentList) collection).indexOf(entity);
	}

	@Override
	public Object replaceElements(Object original, Object target, CollectionPersister persister, Object owner,
			Map copyCache, SessionImplementor session) throws HibernateException {
		((PersistentList) target).clear();
		((PersistentList) target).addAll((Collection<?>) original);
		return target;
	}

	@Override
	public Object instantiate(int anticipatedSize) {
		return new AutofetchList();
	}
}