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

import org.autofetch.hibernate.ExtentManager;
import org.autofetch.hibernate.TrackableEntity;
import org.hibernate.CallbackException;
import org.hibernate.EmptyInterceptor;
import org.hibernate.Interceptor;

public class AutofetchInterceptor extends DelegateInterceptor {

    // This is not used by this class, but this is a useful place
    // to store it so that is accessible by any object with access
    // to the session.
    private ExtentManager extentManager;
    
    public AutofetchInterceptor(ExtentManager extentManager) {
        this(EmptyInterceptor.INSTANCE, extentManager);
    }

    public AutofetchInterceptor(Interceptor i, ExtentManager extentManager) {
        super(i);
        this.extentManager = extentManager;
    }
    
    /**
     * Useful method for creating new instance of this interceptor
     * and changing delegate interceptor in one swoop.
     * @param newInterceptor
     * @return new AutofetchInterceptor
     */
    public AutofetchInterceptor copy(Interceptor newInterceptor) {
        return new AutofetchInterceptor(newInterceptor, extentManager);
    }

    @Override
    public String getEntityName(Object o) throws CallbackException {
        if (o instanceof TrackableEntity) {
            return o.getClass().getSuperclass().getName();
        } else {
            return super.getEntityName(o);
        }
    }
    
    public ExtentManager getExtentManager() {
        return extentManager;
    }

    public void setExtentManager(ExtentManager extentManager) {
        this.extentManager = extentManager;
    }
}
