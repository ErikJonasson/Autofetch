/**
 * Copyright 2008 Ali Ibrahim
 * <p>
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
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.property.Getter;
import org.hibernate.property.Setter;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.tuple.Instantiator;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.tuple.entity.PojoEntityTuplizer;
import org.hibernate.type.ComponentType;

import java.io.Serializable;
import java.util.Map;

/**
 * This class modifies the default hibernate POJO tuplizer to
 * instantiate entities that implement the TrackableEntity interface.
 *
 * @author Ali Ibrahim <aibrahim@cs.utexas.edu>
 */
public class AutofetchTuplizer extends PojoEntityTuplizer {

    public AutofetchTuplizer(EntityMetamodel entityMetamodel, PersistentClass mappedEntity) {
        super(entityMetamodel, mappedEntity);
    }

    public AutofetchTuplizer(EntityMetamodel entityMetamodel, EntityBinding mappedEntity) {
        super(entityMetamodel, mappedEntity);
    }

    @Override
    protected Instantiator buildInstantiator(PersistentClass pc) {
        final AutofetchService autofetchService = getFactory().getServiceRegistry().getService(AutofetchService.class);
        final ExtentManager extentManager = autofetchService.getExtentManager();

        final ReflectionOptimizer optimizer = this.getOptimizer(pc.getMappedClass());

        return new AutofetchInstantiator(
                pc,
                optimizer != null ? optimizer.getInstantiationOptimizer() : null,
                extentManager
        );
    }

    @Override
    protected ProxyFactory buildProxyFactoryInternal(PersistentClass pc, Getter idGetter, Setter idSetter) {
        return new AutofetchProxyFactory(pc);
    }

    @Override
    public Serializable getIdentifier(Object entity) throws HibernateException {
        boolean wasTracking = disableTracking(entity);
        Serializable val = super.getIdentifier(entity);
        if (wasTracking) {
            enableTracking(entity);
        }
        return val;
    }

    @Override
    public Serializable getIdentifier(Object entity, SessionImplementor session) {
        boolean wasTracking = disableTracking(entity);
        Serializable val = super.getIdentifier(entity, session);
        if (wasTracking) {
            enableTracking(entity);
        }
        return val;
    }

    @Override
    public void setIdentifier(Object entity, Serializable id) throws HibernateException {
        boolean wasTracking = disableTracking(entity);
        super.setIdentifier(entity, id);
        if (wasTracking) {
            enableTracking(entity);
        }
    }

    @Override
    public void setIdentifier(Object entity, Serializable id, SessionImplementor session) {
        boolean wasTracking = disableTracking(entity);
        super.setIdentifier(entity, id, session);
        if (wasTracking) {
            enableTracking(entity);
        }
    }

    @Override
    public Object[] getPropertyValues(Object entity) throws HibernateException {
        boolean wasTracking = disableTracking(entity);
        Object[] val = super.getPropertyValues(entity);
        if (wasTracking) {
            enableTracking(entity);
        }
        return val;
    }

    @Override
    public Object[] getPropertyValuesToInsert(Object entity, Map mergeMap, SessionImplementor session) throws HibernateException {
        boolean wasTracking = disableTracking(entity);
        Object[] val = super.getPropertyValuesToInsert(entity, mergeMap, session);
        if (wasTracking) {
            enableTracking(entity);
        }
        return val;
    }

    @Override
    protected Object[] getPropertyValuesWithOptimizer(Object object) {
        boolean wasTracking = disableTracking(object);
        Object[] val = super.getPropertyValuesWithOptimizer(object);
        if (wasTracking) {
            enableTracking(object);
        }
        return val;
    }

    @Override
    public void setPropertyValues(Object entity, Object[] values) throws HibernateException {
        boolean wasTracking = disableTracking(entity);
        super.setPropertyValues(entity, values);
        if (wasTracking) {
            enableTracking(entity);
        }
    }

    @Override
    protected void setPropertyValuesWithOptimizer(Object object, Object[] values) {
        boolean wasTracking = disableTracking(object);
        super.setPropertyValuesWithOptimizer(object, values);
        if (wasTracking) {
            enableTracking(object);
        }
    }

    @Override
    public Object getPropertyValue(Object entity, int i) throws HibernateException {
        boolean wasTracking = disableTracking(entity);
        Object val = super.getPropertyValue(entity, i);
        if (wasTracking) {
            enableTracking(entity);
        }
        return val;
    }

    @Override
    public Object getPropertyValue(Object entity, String propertyPath) throws HibernateException {
        boolean wasTracking = disableTracking(entity);
        Object val = super.getPropertyValue(entity, propertyPath);
        if (wasTracking) {
            enableTracking(entity);
        }
        return val;
    }

    @Override
    public void setPropertyValue(Object entity, int i, Object value) throws HibernateException {
        boolean wasTracking = disableTracking(entity);
        super.setPropertyValue(entity, i, value);
        if (wasTracking) {
            enableTracking(entity);
        }
    }

    @Override
    public void setPropertyValue(Object entity, String propertyName, Object value) throws HibernateException {
        boolean wasTracking = disableTracking(entity);
        super.setPropertyValue(entity, propertyName, value);
        if (wasTracking) {
            enableTracking(entity);
        }
    }

    @Override
    public void resetIdentifier(Object entity, Serializable currentId, Object currentVersion) {
        boolean wasTracking = disableTracking(entity);
        super.resetIdentifier(entity, currentId, currentVersion);
        if (wasTracking) {
            enableTracking(entity);
        }
    }

    @Override
    public void resetIdentifier(Object entity, Serializable currentId, Object currentVersion, SessionImplementor session) {
        boolean wasTracking = disableTracking(entity);
        super.resetIdentifier(entity, currentId, currentVersion, session);
        if (wasTracking) {
            enableTracking(entity);
        }
    }

    @Override
    public Object getVersion(Object entity) throws HibernateException {
        boolean wasTracking = disableTracking(entity);
        Object version = super.getVersion(entity);
        if (wasTracking) {
            enableTracking(entity);
        }
        return version;
    }

    @Override
    protected Object getComponentValue(ComponentType type, Object component, String propertyPath) {
        return super.getComponentValue(type, component, propertyPath);
    }

    @Override
    public Getter getGetter(int i) {
        return super.getGetter(i);
    }

    @Override
    public void afterInitialize(Object entity, boolean lazyPropertiesAreUnfetched, SessionImplementor session) {
        super.afterInitialize(entity, lazyPropertiesAreUnfetched, session);
    }

    @Override
    protected boolean shouldGetAllProperties(Object entity) {
        return super.shouldGetAllProperties(entity);
    }

    private boolean disableTracking(Object o) {
        if (o instanceof Trackable) {
            Trackable entity = (Trackable) o;
            return entity.disableTracking();
        } else {
            return false;
        }
    }

    private boolean enableTracking(Object o) {
        if (o instanceof Trackable) {
            Trackable entity = (Trackable) o;
            return entity.enableTracking();
        } else {
            return false;
        }
    }

    private ReflectionOptimizer getOptimizer(Class<?> mappedClass) {
        final String[] getterNames = new String[super.propertySpan];
        final String[] setterNames = new String[super.propertySpan];
        final Class[] propTypes = new Class[super.propertySpan];
        for (int i = 0; i < super.propertySpan; i++) {
            getterNames[i] = super.getters[i].getMethodName();
            setterNames[i] = super.setters[i].getMethodName();
            propTypes[i] = super.getters[i].getReturnType();
        }

        if (super.hasCustomAccessors || !Environment.useReflectionOptimizer()) {
            return null;
        }

        return Environment.getBytecodeProvider().getReflectionOptimizer(
                mappedClass, getterNames, setterNames, propTypes
        );
    }
}
