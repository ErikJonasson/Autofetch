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
import java.util.Iterator;

import org.hibernate.CallbackException;
import org.hibernate.EntityMode;
import org.hibernate.Interceptor;
import org.hibernate.Transaction;
import org.hibernate.type.Type;

/**
 * Abstract class for interceptors which act as delegates for other
 * interceptors.
 * @author Ali Ibrahim <aibrahim@cs.utexas.edu>
 *
 */
public abstract class DelegateInterceptor implements Interceptor, Serializable {

    protected Interceptor delegate;
    
    public DelegateInterceptor(Interceptor delegate) {
        this.delegate = delegate;
    }

    public void afterTransactionBegin(Transaction arg0) {
        delegate.afterTransactionBegin(arg0);
    }

    public void afterTransactionCompletion(Transaction arg0) {
        delegate.afterTransactionCompletion(arg0);
    }

    public void beforeTransactionCompletion(Transaction arg0) {
        delegate.beforeTransactionCompletion(arg0);
    }

    public int[] findDirty(Object arg0, Serializable arg1, Object[] arg2, Object[] arg3, String[] arg4, Type[] arg5) {
        return delegate.findDirty(arg0, arg1, arg2, arg3, arg4, arg5);
    }

    public Object getEntity(String arg0, Serializable arg1) throws CallbackException {
        return delegate.getEntity(arg0, arg1);
    }

    public String getEntityName(Object arg0) throws CallbackException {
        return delegate.getEntityName(arg0);
    }

    public Object instantiate(String arg0, EntityMode arg1, Serializable arg2) throws CallbackException {
        return delegate.instantiate(arg0, arg1, arg2);
    }

    public Boolean isTransient(Object arg0) {
        return delegate.isTransient(arg0);
    }

    public void onCollectionRecreate(Object arg0, Serializable arg1) throws CallbackException {
        delegate.onCollectionRecreate(arg0, arg1);
    }

    public void onCollectionRemove(Object arg0, Serializable arg1) throws CallbackException {
        delegate.onCollectionRemove(arg0, arg1);
    }

    public void onCollectionUpdate(Object arg0, Serializable arg1) throws CallbackException {
        delegate.onCollectionUpdate(arg0, arg1);
    }

    public void onDelete(Object arg0, Serializable arg1, Object[] arg2, String[] arg3, Type[] arg4) throws CallbackException {
        delegate.onDelete(arg0, arg1, arg2, arg3, arg4);
    }

    public boolean onFlushDirty(Object arg0, Serializable arg1, Object[] arg2, Object[] arg3, String[] arg4, Type[] arg5) throws CallbackException {
        return delegate.onFlushDirty(arg0, arg1, arg2, arg3, arg4, arg5);
    }

    public boolean onLoad(Object arg0, Serializable arg1, Object[] arg2, String[] arg3, Type[] arg4) throws CallbackException {
        return delegate.onLoad(arg0, arg1, arg2, arg3, arg4);
    }

    public String onPrepareStatement(String arg0) {
        return delegate.onPrepareStatement(arg0);
    }

    public boolean onSave(Object arg0, Serializable arg1, Object[] arg2, String[] arg3, Type[] arg4) throws CallbackException {
        return delegate.onSave(arg0, arg1, arg2, arg3, arg4);
    }

    public void postFlush(Iterator arg0) throws CallbackException {
        delegate.postFlush(arg0);
    }

    public void preFlush(Iterator arg0) throws CallbackException {
        delegate.preFlush(arg0);
    }

    public Interceptor getDelegate() {
        return delegate;
    }

    public void setDelegate(Interceptor delegate) {
        this.delegate = delegate;
    }
}
