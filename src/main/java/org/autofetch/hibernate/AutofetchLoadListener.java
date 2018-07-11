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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.event.internal.DefaultLoadEventListener;
import org.hibernate.event.spi.LoadEvent;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class extends the hibernate default event listener to add prefetch
 * directives where appropriate and setup the tracking of loaded objects.
 *
 * @author Ali Ibrahim <aibrahim@cs.utexas.edu>
 */
public class AutofetchLoadListener extends DefaultLoadEventListener {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(AutofetchLoadListener.class);

    private final ExtentManager extentManager;

    public AutofetchLoadListener(ExtentManager extentManager) {
        this.extentManager = extentManager;
    }

    @Override
    protected Object loadFromDatasource(LoadEvent event,
                                        EntityPersister entityPersister, EntityKey entityKey,
                                        LoadType loadType) throws HibernateException {

        String classname = entityPersister.getEntityName();
        if (log.isDebugEnabled()) {
            log.debug("Entity id: " + event.getEntityId());
        }

        List<Path> prefetchPaths = extentManager.getPrefetchPaths(classname);

        Object result;
        if (!prefetchPaths.isEmpty()) {
            result = getResult(prefetchPaths, classname, event.getEntityId(), event.getLockMode(), event.getSession());
            if (result instanceof HibernateProxy) {
                HibernateProxy proxy = (HibernateProxy) result;
                if (proxy.getHibernateLazyInitializer().isUninitialized()) {
                    throw new IllegalStateException("proxy uninitialized");
                }
                result = proxy.getHibernateLazyInitializer().getImplementation();
            }
        } else {
            result = super.loadFromDatasource(event, entityPersister, entityKey, loadType);
        }

        extentManager.markAsRoot(result, classname);

        return result;
    }

    public static Object getResult(List<Path> prefetchPaths, String classname,
                                   Serializable id, LockMode lm, Session sess) {
        StringBuilder queryStr = new StringBuilder();
        queryStr.append("from ").append(classname).append(" entity");
        Map<Path, String> pathAliases = new HashMap<>();
        int aliasCnt = 0;
        pathAliases.put(new Path(), "entity");

        // Assumes prefetchPaths is ordered such larger paths appear after smaller ones.
        // Also assumes all prefixes of a path are present except the empty prefix.
        for (Path p : prefetchPaths) {
            String oldAlias = pathAliases.get(p.removeLastTraversal());
            String newAlias = "af" + (aliasCnt++);
            String lastField = p.traversals().get(p.size() - 1);
            pathAliases.put(p, newAlias);
            queryStr.append(" left outer join fetch ");
            queryStr.append(oldAlias).append(".").append(lastField).append(" ").append(newAlias);
        }
        queryStr.append(" where entity.id = :id");

        if (log.isDebugEnabled()) {
            log.debug("Autofetched Query: " + queryStr);
        }

        Query q = sess.createQuery(queryStr.toString());
        q.setLockMode("entity", lm);
        q.setFlushMode(FlushMode.MANUAL);
        q.setParameter("id", id);

        long startTimeMillis = System.currentTimeMillis();
        Object o = q.uniqueResult();
        if (log.isDebugEnabled()) {
            log.debug("Query execution time: " +
                    (System.currentTimeMillis() - startTimeMillis));
        }
        return o;
    }
}
