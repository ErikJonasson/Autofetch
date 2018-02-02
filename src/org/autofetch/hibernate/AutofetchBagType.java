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

import java.io.Serializable;

import org.dom4j.Element;
import org.hibernate.EntityMode;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.collection.PersistentElementHolder;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.BagType;
import org.hibernate.type.TypeFactory.TypeScope;

/**
 * Based on org.hibernate.type.BagType.
 * @author Ali Ibrahim <aibrahim@cs.utexas.edu>
 *
 */
public class AutofetchBagType extends BagType {

    public AutofetchBagType(TypeScope typeScope, String role, String propertyRef,
            boolean isEmbeddedInXML) {
        super(typeScope, role, propertyRef, isEmbeddedInXML);
    }

    @Override
    public PersistentCollection instantiate(SessionImplementor session,
            CollectionPersister persister, Serializable key) {
        if (session.getEntityMode() == EntityMode.DOM4J) {
            return new PersistentElementHolder(session, persister, key);
        } else {
            return new AutofetchBag(session);
        }
    }

    @Override
    public PersistentCollection wrap(SessionImplementor session,
            Object collection) {
        if (session.getEntityMode() == EntityMode.DOM4J) {
            return new PersistentElementHolder(session, (Element) collection);
        } else {
            return new AutofetchBag(session, (java.util.Collection) collection);
        }
    }
}
