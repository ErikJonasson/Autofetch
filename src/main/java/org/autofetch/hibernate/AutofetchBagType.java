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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.collection.internal.PersistentBag;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.usertype.UserCollectionType;

/**
 * Based on {@link org.hibernate.type.BagType}.
 *
 * @author Ali Ibrahim <aibrahim@cs.utexas.edu>
 */
public class AutofetchBagType implements UserCollectionType {

	public PersistentCollection instantiate(
			SessionImplementor session,
			CollectionPersister persister,
			Serializable key) {
		return new AutofetchBag( session );
	}

	@Override
	public PersistentCollection wrap(SessionImplementor session, Object collection) {
		return new AutofetchBag( session, cast( collection ) );
	}

	@Override
	public Object instantiate(int anticipatedSize) {
		return anticipatedSize <= 0 ? new ArrayList() : new ArrayList( anticipatedSize + 1 );
	}

	@Override
	public PersistentCollection instantiate(SessionImplementor session, CollectionPersister persister)
			throws HibernateException {
		return new AutofetchBag( session );
	}

	@Override
	public Iterator getElementsIterator(Object collection) {
		return ( (Set) collection ).iterator();
	}

	@Override
	public boolean contains(Object collection, Object entity) {
		return ( (Set) collection ).contains( entity );
	}

	@Override
	public Object indexOf(Object collection, Object entity) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object replaceElements(
			Object original, Object target, CollectionPersister persister, Object owner,
			Map copyCache, SessionImplementor session) throws HibernateException {
		( (PersistentBag) target ).clear();
		( (PersistentBag) target ).addAll( (Collection<?>) original );
		return target;
	}

	protected Collection cast(Object collection) {
		return (Collection) collection;
	}
}
