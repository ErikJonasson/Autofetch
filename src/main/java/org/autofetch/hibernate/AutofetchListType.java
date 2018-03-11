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

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.ListType;
import org.hibernate.type.TypeFactory.TypeScope;

import java.io.Serializable;

/**
 * This class is based on org.hibernate.type.ListType.
 *
 * @author Ali Ibrahim <aibrahim@cs.utexas.edu>
 */
public class AutofetchListType extends ListType {

    public AutofetchListType(TypeScope typeScope, String role, String propertyRef) {
        super(typeScope, role, propertyRef);
    }

    @Override
    public PersistentCollection instantiate(SessionImplementor session,
                                            CollectionPersister persister, Serializable key) {
        return new AutofetchList(session);
    }

    @Override
    public PersistentCollection wrap(SessionImplementor session, Object collection) {
        return new AutofetchList(session, (java.util.List) collection);
    }
}