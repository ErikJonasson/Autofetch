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
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.tuple.PojoInstantiator;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * This code is based on the instantior implementations in the Hibernate
 * source code.
 * It instantiates entities using proxies so that their accesses may be
 * tracked.
 *
 * @author Ali Ibrahim <aibrahim@cs.utexas.edu>
 */
public class AutofetchInstantiator extends PojoInstantiator {

    private Class mappedClass;

    private String idMethodName = "***none***";

    private final boolean embeddedIdentifier;

    private final Class proxyInterface;

    private Set<org.autofetch.hibernate.Property> persistentProperties;

    private final ExtentManager extentManager;

    public AutofetchInstantiator(PersistentClass persistentClass, ReflectionOptimizer.InstantiationOptimizer optimizer, ExtentManager extentManager) {
        super(persistentClass, optimizer);
        this.extentManager = extentManager;

        this.mappedClass = persistentClass.getMappedClass();
        if (persistentClass.getIdentifierProperty() != null && persistentClass.getIdentifierProperty().getGetter(mappedClass) != null) {
            this.idMethodName = persistentClass.getIdentifierProperty().getGetter(mappedClass).getMethodName();
        }

        this.proxyInterface = persistentClass.getProxyInterface();
        this.embeddedIdentifier = persistentClass.hasEmbeddedIdentifier();
        this.persistentProperties = new HashSet<>();

        @SuppressWarnings("unchecked")
        Iterator<Property> propIter = persistentClass.getPropertyClosureIterator();
        while (propIter.hasNext()) {
            Property prop = propIter.next();
            org.autofetch.hibernate.Property p = new org.autofetch.hibernate.Property(prop.getName(), prop.getType().isCollectionType());
            this.persistentProperties.add(p);
        }
    }

    @Override
    public Object instantiate() {
        // instantiate our own proxy instead of using hibernate's class
        try {
            return EntityProxyFactory.getProxyInstance(mappedClass, idMethodName, persistentProperties, extentManager);
        } catch (Exception ie) {
            throw new HibernateException("Unable to instantiate class", ie);
        }
    }

    @Override
    public Object instantiate(Serializable id) {
        // replicate logic in Hibernate PojoInstantiator
        final boolean useEmbeddedIdentifierInstanceAsEntity = embeddedIdentifier && id != null && id.getClass().equals(mappedClass);
        return useEmbeddedIdentifierInstanceAsEntity ? id : instantiate();
    }

    @Override
    public boolean isInstance(Object o) {
        return mappedClass.isInstance(o) || (proxyInterface != null && proxyInterface.isInstance(o));
    }
}
