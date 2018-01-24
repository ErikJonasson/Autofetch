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

import java.util.Iterator;

import org.autofetch.hibernate.ExtentManager;
import org.hibernate.EmptyInterceptor;
import org.hibernate.Interceptor;
import org.hibernate.MappingException;
import org.hibernate.cfg.AutofetchHbmBinder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.ExtendsQueueEntry;
import org.hibernate.cfg.SettingsFactory;
import org.hibernate.event.InitializeCollectionEventListener;
import org.hibernate.event.LoadEventListener;
import org.hibernate.util.CollectionHelper;

/**
 * Based on org.hibernate.cfg.Configuration.
 * 
 * Changes behavior of hibernate configuration to use our own HbmBinder and to
 * wrap session factories.
 * 
 * @author Ali Ibrahim <aibrahim@cs.utexas.edu>
 * 
 */
public class AutofetchConfiguration extends Configuration {

    private ExtentManager extentManager;

    public AutofetchConfiguration() {
        super();
        initialize();
    }

    public AutofetchConfiguration(SettingsFactory arg0) {
        super(arg0);
        initialize();
    }

    @Override
    protected void add(org.dom4j.Document doc) throws MappingException {
        AutofetchHbmBinder.bindRoot(doc, createMappings(),
                CollectionHelper.EMPTY_MAP);
    }

    @Override
    protected org.dom4j.Document findPossibleExtends() {
        //      Iterator iter = extendsQueue.iterator();
        Iterator iter = extendsQueue.keySet().iterator();
        while (iter.hasNext()) {
            final ExtendsQueueEntry entry = (ExtendsQueueEntry) iter.next();
            if (getClassMapping(entry.getExplicitName()) != null) {
                // found
                iter.remove();
                return entry.getDocument();
            } else if (getClassMapping(AutofetchHbmBinder.getClassName(entry
                    .getExplicitName(), entry.getMappingPackage())) != null) {
                // found
                iter.remove();
                return entry.getDocument();
            }
        }
        return null;
    }
    
    @Override
    protected void reset() {
        super.reset();
        initialize();
    }
    
    private void initialize() {
        extentManager = new ExtentManager();
        getEventListeners().setLoadEventListeners(new LoadEventListener[] {
            new AutofetchLoadListener(extentManager) });
        getEventListeners().setInitializeCollectionEventListeners(
                new InitializeCollectionEventListener[] { 
                        new AutofetchInitializeCollectionListener(
                                extentManager) });
        setInterceptor(new AutofetchInterceptor(EmptyInterceptor.INSTANCE,
                extentManager));
    }

    /**
     * Ensures that any interceptor is wrapped with the AutofetchInterceptor.
     */
    @Override
    public Configuration setInterceptor(Interceptor i) {
        if (i instanceof AutofetchInterceptor) {
            return super.setInterceptor(i);
        } else {
            AutofetchInterceptor ai = (AutofetchInterceptor) getInterceptor();
            return super.setInterceptor(ai.copy(i));
        }
    }
    
    /**
     * Ensures that the extent manager is set for any Autofetch
     * listeners.
     */
    @Override
    public void setListeners(String type, Object[] listeners) {
        setExtentManager(listeners, extentManager);
        super.setListeners(type, listeners);
    }
    
    public ExtentManager getExtentManager() {
        return extentManager;
    }

    public void setExtentManager(ExtentManager em) {
        this.extentManager = em;
        // Propagate changes to listeners and interceptor
        AutofetchInterceptor ai = (AutofetchInterceptor) getInterceptor();
        ai.setExtentManager(em);
        setExtentManager(
                getEventListeners().getInitializeCollectionEventListeners(),
                em);
        setExtentManager(
                getEventListeners().getLoadEventListeners(),
                em);
    }
    
    private void setExtentManager(Object[] listeners, ExtentManager em) {
        for (Object listener : listeners) {
            if (listener instanceof AutofetchInitializeCollectionListener) {
                ((AutofetchInitializeCollectionListener)listener).setExtentManager(em);
            }
            if (listener instanceof AutofetchLoadListener) {
                ((AutofetchLoadListener)listener).setExtentManager(em);
            }
        }
    }
}