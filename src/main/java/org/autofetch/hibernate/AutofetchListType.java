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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.collection.internal.PersistentList;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.usertype.UserCollectionType;

/**
 * This class is based on {@link org.hibernate.type.ListType}.
 *
 * @author Ali Ibrahim <aibrahim@cs.utexas.edu>
 */
public class AutofetchListType implements UserCollectionType {

	@Override
	public PersistentCollection wrap(SharedSessionContractImplementor session, Object collection) {
		return new AutofetchList( session, cast( collection ) );
	}

	@Override
	public Object indexOf(Object collection, Object entity) {
		return cast( collection ).indexOf( entity );
	}

	@Override
	public Object instantiate(int anticipatedSize) {
		return anticipatedSize <= 0 ? new ArrayList() : new ArrayList( anticipatedSize + 1 );
	}

	@Override
	public PersistentCollection instantiate(SharedSessionContractImplementor session, CollectionPersister persister)
			throws HibernateException {
		return new AutofetchList( session );
	}

	@Override
	public Iterator getElementsIterator(Object collection) {
		return ( (PersistentList) collection ).iterator();
	}

	@Override
	public boolean contains(Object collection, Object entity) {
		return ( (PersistentList) collection ).contains( entity );
	}

	@Override
	public Object replaceElements(
			Object original, Object target, CollectionPersister persister, Object owner,
			Map copyCache, SharedSessionContractImplementor session) throws HibernateException {
		( (PersistentList) target ).clear();
		( (PersistentList) target ).addAll( (Collection<?>) original );
		return target;
	}

	protected List cast(Object collection) {
		return (List) collection;
	}
}