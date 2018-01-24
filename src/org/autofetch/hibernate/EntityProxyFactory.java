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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.CallbackFilter;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.NoOp;



//Class holding Factories for all classes, Constructors for all classes, sets a callbackfilter on the enhancer (which does what exactly?), Enhancer takes care of callbacks
public class EntityProxyFactory {

    private static Map<Class, Class> entityFactoryMap = new HashMap<Class, Class>();

    private static Map<Class, Constructor> entityConstructorMap = new HashMap<Class, Constructor>();

    private static class EntityCallbackFilter implements CallbackFilter {

        private String idMethodName;

        public EntityCallbackFilter(String idMethodName) {
            this.idMethodName = idMethodName;
        }

        public int accept(Method method) {
            if (method.getParameterTypes().length == 0
                    && method.getName().equals("finalize")) {
                return 1;
            } else if (method.getParameterTypes().length == 0
                    && method.getName().equals(idMethodName)) {
                return 1;
            } else {
                return 0;
            }
        }
    };

	//if entityFactoryMap doesnt contain that specific class, add it
    public static Class getProxyFactory(Class persistentClass,
            String idMethodName) {
		//Not sure how enhancer work, but it seems like you tell enhancer what type of subclasses that can be created, and all the method calls will be intercepted by entitycallbackfilter
        if (!entityFactoryMap.containsKey(persistentClass)) {
            Enhancer e = new Enhancer();
            e.setSuperclass(persistentClass);
            e.setInterfaces(new Class[] { TrackableEntity.class }); 
            e.setCallbackTypes(new Class[] { MethodInterceptor.class,
                    NoOp.class, });
            e.setCallbackFilter(new EntityCallbackFilter(idMethodName));
            e.setUseFactory(false);
            e.setInterceptDuringConstruction(false);
            entityFactoryMap.put(persistentClass, e.createClass());
        }

        return entityFactoryMap.get(persistentClass);
    }
    
    public static Constructor getDefaultConstructor(Class clazz)
        throws NoSuchMethodException {
        @SuppressWarnings("unchecked")
        Constructor constructor =
            clazz.getDeclaredConstructor(new Class[0]); //Why new Class[0]?
        if ( !constructor.isAccessible()) {
            constructor.setAccessible(true);
        }
        return constructor;
    }

    public static Object getProxyInstance(Class persistentClass, 
            String idMethodName, Set<Property> persistentProperties,
            ExtentManager extentManager)
            throws InstantiationException, IllegalAccessException,
            NoSuchMethodException, InvocationTargetException {

        if (Modifier.isFinal(persistentClass.getModifiers())) { // Why if final?
            return useDefaultConstructor(persistentClass);
        }
        
        Class factory = getProxyFactory(persistentClass, idMethodName);
        try {

            Enhancer.registerCallbacks(factory, new Callback[] {	//Need some explanation of this
                    new EntityProxyCallback(persistentProperties,
                            extentManager), null });
            Object o = factory.newInstance();
            return o;
        } catch (Exception ie) { // can't create proxy
            return useDefaultConstructor(persistentClass);
        } finally { // Avoid memory leak
            Enhancer.registerCallbacks(factory, null);
        }
    }
    
    private static Object useDefaultConstructor(Class clazz)
        throws NoSuchMethodException, InstantiationException,
        InvocationTargetException, IllegalAccessException {
        if (!entityConstructorMap.containsKey(clazz)) {
            entityConstructorMap.put(clazz,
                    getDefaultConstructor(clazz));
        }
        Constructor c = entityConstructorMap.get(clazz);
        return c.newInstance((Object[])null);
    }
}
