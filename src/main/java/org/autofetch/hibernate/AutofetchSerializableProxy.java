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
import org.hibernate.proxy.AbstractSerializableProxy;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.pojo.javassist.JavassistLazyInitializer;
import org.hibernate.type.CompositeType;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Set;

public final class AutofetchSerializableProxy extends AbstractSerializableProxy {

    private Class persistentClass;
    private Class[] interfaces;
    private Class getIdentifierMethodClass;
    private Class setIdentifierMethodClass;
    private String getIdentifierMethodName;
    private String setIdentifierMethodName;
    private Class[] setIdentifierMethodParams;
    private CompositeType componentIdType;
    private Set<Property> persistentProperties;

    public AutofetchSerializableProxy() {
    }

    public AutofetchSerializableProxy(
            final String entityName,
            final Class persistentClass,
            final Class[] interfaces,
            final Serializable id,
            final Boolean readOnly,
            final Method getIdentifierMethod,
            final Method setIdentifierMethod,
            final CompositeType componentIdType,
            final Set<Property> persistentProperties) {

        super(entityName, id, readOnly);

        this.persistentClass = persistentClass;
        this.interfaces = interfaces;

        if (getIdentifierMethod != null) {
            getIdentifierMethodClass = getIdentifierMethod.getDeclaringClass();
            getIdentifierMethodName = getIdentifierMethod.getName();
        }

        if (setIdentifierMethod != null) {
            setIdentifierMethodClass = setIdentifierMethod.getDeclaringClass();
            setIdentifierMethodName = setIdentifierMethod.getName();
            setIdentifierMethodParams = setIdentifierMethod.getParameterTypes();
        }

        this.componentIdType = componentIdType;
        this.persistentProperties = persistentProperties;
    }

    private Object readResolve() {
        try {
            HibernateProxy proxy = AutofetchLazyInitializer.getProxy(
                    getEntityName(),
                    persistentClass,
                    interfaces,
                    getIdentifierMethodName == null ? null
                            : getIdentifierMethodClass.getDeclaredMethod(getIdentifierMethodName, (Class[]) null),
                    setIdentifierMethodName == null ? null
                            : setIdentifierMethodClass.getDeclaredMethod(setIdentifierMethodName, setIdentifierMethodParams),
                    componentIdType,
                    getId(),
                    null,
                    persistentProperties
            );
            setReadOnlyBeforeAttachedToSession((JavassistLazyInitializer) proxy.getHibernateLazyInitializer());
            return proxy;
        } catch (NoSuchMethodException nsme) {
            throw new HibernateException("could not create autofetch serializable proxy for entity: " + getEntityName(), nsme);
        }
    }
}
