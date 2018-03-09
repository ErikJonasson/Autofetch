/**
 * Copyright 2008 Ali Ibrahim
 * 
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
import java.util.Map;

import org.autofetch.hibernate.Trackable;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.property.Getter;
import org.hibernate.property.Setter;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.tuple.Instantiator;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.tuple.entity.PojoEntityTuplizer;

/**
 * This class modifies the default hibernate POJO tuplizer to
 * instantiate entities that implement the TrackableEntity interface.
 * @author Ali Ibrahim <aibrahim@cs.utexas.edu>
 *
 */
public class AutofetchTuplizer extends PojoEntityTuplizer {

    public AutofetchTuplizer(EntityMetamodel entityMetamodel,
            PersistentClass mappedEntity) {
        super(entityMetamodel, mappedEntity);
    }

    @Override
    protected Instantiator buildInstantiator(PersistentClass pc) {
        AutofetchInterceptor ai =
            (AutofetchInterceptor) getFactory().getInterceptor();
        return new AutofetchInstantiator(pc, ai.getExtentManager());
    }

    @Override
    protected ProxyFactory buildProxyFactoryInternal(PersistentClass pc,
            Getter idGetter, Setter idSetter) {
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
    public void setIdentifier(Object entity, Serializable id) throws HibernateException {
        boolean wasTracking = disableTracking(entity);
        super.setIdentifier(entity, id);
        if (wasTracking) {
            enableTracking(entity);
        }
    }

    @Override
    public Object[] getPropertyValues(Object arg0) throws HibernateException {
        boolean wasTracking = disableTracking(arg0);
        Object[] val = super.getPropertyValues(arg0);
        if (wasTracking) {
            enableTracking(arg0);
        }
        return val;
    }

    @Override
    public Object[] getPropertyValuesToInsert(Object arg0, Map arg1,
            SessionImplementor arg2) throws HibernateException {
        boolean wasTracking = disableTracking(arg0);
        Object[] val = super.getPropertyValuesToInsert(arg0, arg1, arg2);
        if (wasTracking) {
            enableTracking(arg0);
        }
        return val;
    }

    @Override
    protected Object[] getPropertyValuesWithOptimizer(Object arg0) {
        boolean wasTracking = disableTracking(arg0);
        Object[] val = super.getPropertyValuesWithOptimizer(arg0);
        if (wasTracking) {
            enableTracking(arg0);
        }
        return val;
    }

    @Override
    public void setPropertyValues(Object arg0, Object[] arg1)
            throws HibernateException {
        boolean wasTracking = disableTracking(arg0);
        super.setPropertyValues(arg0, arg1);
        if (wasTracking) {
            enableTracking(arg0);
        }
    }

    @Override
    protected void setPropertyValuesWithOptimizer(Object arg0, Object[] arg1) {
        boolean wasTracking = disableTracking(arg0);
        super.setPropertyValuesWithOptimizer(arg0, arg1);
        if (wasTracking) {
            enableTracking(arg0);
        }
    }

    @Override
    public Object getPropertyValue(Object arg0, int arg1)
            throws HibernateException {
        boolean wasTracking = disableTracking(arg0);
        Object val = super.getPropertyValue(arg0, arg1);
        if (wasTracking) {
            enableTracking(arg0);
        }
        return val;
    }

    @Override
    public Object getPropertyValue(Object arg0, String arg1)
            throws HibernateException {
        boolean wasTracking = disableTracking(arg0);
        Object val = super.getPropertyValue(arg0, arg1);
        if (wasTracking) {
            enableTracking(arg0);
        }
        return val;
    }

    @Override
    public void setPropertyValue(Object arg0, int arg1, Object arg2)
            throws HibernateException {
        boolean wasTracking = disableTracking(arg0);
        super.setPropertyValue(arg0, arg1, arg2);
        if (wasTracking) {
            enableTracking(arg0);
        }
    }

    @Override
    public void setPropertyValue(Object arg0, String arg1, Object arg2)
            throws HibernateException {
        boolean wasTracking = disableTracking(arg0);
        super.setPropertyValue(arg0, arg1, arg2);
        if (wasTracking) {
            enableTracking(arg0);
        }
    }

    private boolean disableTracking(Object o) {
        if (o instanceof Trackable) {
            Trackable entity = (Trackable) o;
            boolean oldValue = entity.disableTracking();
            return oldValue;
        } else {
            return false;
        }
    }

    private boolean enableTracking(Object o) {
        if (o instanceof Trackable) {
            Trackable entity = (Trackable) o;
            boolean oldValue = entity.enableTracking();
            return oldValue;
        } else {
            return false;
        }
    }

}