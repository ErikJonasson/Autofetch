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
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.CallbackFilter;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.InvocationHandler;
import net.sf.cglib.proxy.NoOp;

import org.autofetch.hibernate.TrackableEntity;
import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.type.AbstractComponentType;

/**
 * Based on org.hibernate.proxy.pojo.cglib.CGLIBProxyFactory
 * @author Ali Ibrahim <aibrahim@cs.utexas.edu>
 *
 */
public class AutofetchProxyFactory implements ProxyFactory {

    protected static final Class[] NO_CLASSES = new Class[0];

    private Class persistentClass;
    private String entityName;
    private Class[] interfaces;
    private Method getIdentifierMethod;
    private Method setIdentifierMethod;
    private AbstractComponentType componentIdType;
    private Class factory;
    private Set<org.autofetch.hibernate.Property> persistentProperties;

    private static final CallbackFilter FINALIZE_FILTER = new CallbackFilter() {
        public int accept(Method method) {
            if ( method.getParameterTypes().length == 0 && method.getName().equals("finalize") ){
                return 1;
            }
            else {
                return 0;
            }
        }
    };
    
    public AutofetchProxyFactory(PersistentClass pc) {
        persistentProperties = new HashSet<org.autofetch.hibernate.Property>();
        @SuppressWarnings("unchecked")
        Iterator<Property> propIter = pc.getPropertyClosureIterator();
        while (propIter.hasNext()) {
            Property prop = propIter.next();
            org.autofetch.hibernate.Property p = new org.autofetch.hibernate.Property(
                    prop.getName(),
                    prop.getType().isCollectionType());
            persistentProperties.add(p);
        }
    }
    
    @SuppressWarnings("unchecked")
    public void postInstantiate(
        final String entityName,
        final Class persistentClass,
        final Set interfaces,
        final Method getIdentifierMethod,
        final Method setIdentifierMethod,
        AbstractComponentType componentIdType)
    throws HibernateException {
        this.entityName = entityName;
        this.persistentClass = persistentClass;
        interfaces.add(TrackableEntity.class); // add our own interface
        this.interfaces = (Class[]) interfaces.toArray(NO_CLASSES);
        this.getIdentifierMethod = getIdentifierMethod;
        this.setIdentifierMethod = setIdentifierMethod;
        this.componentIdType = componentIdType;
        
        // Instead of calling CGLIBLazyInitializer to create the factory
        // we do it ourselves so that we can set useFactory to true.
        Enhancer e = new Enhancer();
        e.setSuperclass(this.interfaces.length > 1 ? persistentClass : null);
        e.setInterfaces(this.interfaces);
        e.setCallbackTypes(new Class[]{
            InvocationHandler.class,
            NoOp.class,
            });
        e.setCallbackFilter(FINALIZE_FILTER);
        e.setUseFactory(false);
        e.setInterceptDuringConstruction(false );
        factory = e.createClass();
    }

    public HibernateProxy getProxy(Serializable id, SessionImplementor session)
        throws HibernateException {

        final AutofetchProxyCallback instance = new AutofetchProxyCallback(
                entityName,
                persistentClass,
                id,
                getIdentifierMethod,
                setIdentifierMethod,
                componentIdType,
                session,
                persistentProperties
            );
        
        final HibernateProxy proxy;
        try {
            Enhancer.registerCallbacks(factory,
                    new Callback[] { instance, null });
            proxy = (HibernateProxy) factory.newInstance();
        } catch (Exception e) {
            throw new HibernateException("CGLIB Enhancement failed: "
                    + persistentClass.getName(), e);
        } finally {
            // HHH-2481 make sure the callback gets cleared, otherwise the
            // instance stays in a static thread local.
            Enhancer.registerCallbacks(factory, null);
        }
        
        return proxy;
    }
}
