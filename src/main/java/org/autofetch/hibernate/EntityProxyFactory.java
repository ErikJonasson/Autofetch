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

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.Proxy;
import javassist.util.proxy.ProxyFactory;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class EntityProxyFactory {

    private static final CoreMessageLogger LOG = CoreLogging.messageLogger(AutofetchLazyInitializer.class);

    private static final MethodFilter FINALIZE_FILTER = new MethodFilter() {
        @Override
        public boolean isHandled(Method m) {
            // skip finalize methods
            return !(m.getParameterTypes().length == 0 && m.getName().equals("finalize"));
        }
    };

    private static final ConcurrentMap<Class<?>, Class<?>> entityFactoryMap = new ConcurrentHashMap<>();

    private static final ConcurrentMap<Class<?>, Constructor<?>> entityConstructorMap = new ConcurrentHashMap<>();

    private static Class<?> getProxyFactory(Class<?> persistentClass, String idMethodName) {
        if (!entityFactoryMap.containsKey(persistentClass)) {
            ProxyFactory factory = new ProxyFactory();
            factory.setSuperclass(persistentClass);
            factory.setInterfaces(new Class[]{TrackableEntity.class});
            factory.setFilter(FINALIZE_FILTER);
            entityFactoryMap.putIfAbsent(persistentClass, factory.createClass());
        }

        return entityFactoryMap.get(persistentClass);
    }

    private static <T> Constructor<T> getDefaultConstructor(Class<T> clazz) throws NoSuchMethodException {
        Constructor<T> constructor = clazz.getDeclaredConstructor();
        if (!constructor.isAccessible()) {
            constructor.setAccessible(true);
        }

        return constructor;
    }

    public static Object getProxyInstance(Class persistentClass, String idMethodName, Set<Property> persistentProperties,
                                          ExtentManager extentManager)
            throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {

        if (Modifier.isFinal(persistentClass.getModifiers())) {
            // Use the default constructor, because final classes cannot be inherited.
            return useDefaultConstructor(persistentClass);
        }

        Class<?> factory = getProxyFactory(persistentClass, idMethodName);
        try {
            final Object proxy = factory.newInstance();
            ((Proxy) proxy).setHandler(new EntityProxyMethodHandler(persistentProperties, extentManager));
            return proxy;
        } catch (IllegalAccessException | InstantiationException e) {
            return useDefaultConstructor(persistentClass);
        }
    }

    private static Object useDefaultConstructor(Class<?> clazz) throws NoSuchMethodException, InstantiationException,
            InvocationTargetException, IllegalAccessException {

        if (!entityConstructorMap.containsKey(clazz)) {
            entityConstructorMap.put(clazz, getDefaultConstructor(clazz));
        }

        final Constructor<?> c = entityConstructorMap.get(clazz);

        return c.newInstance((Object[]) null);
    }
}
