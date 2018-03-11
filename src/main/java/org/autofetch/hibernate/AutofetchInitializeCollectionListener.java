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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.cache.spi.CacheKey;
import org.hibernate.cache.spi.entry.CollectionCacheEntry;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.internal.CacheHelper;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.internal.DefaultInitializeCollectionEventListener;
import org.hibernate.event.spi.InitializeCollectionEvent;
import org.hibernate.event.spi.InitializeCollectionEventListener;
import org.hibernate.persister.collection.CollectionPersister;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class is based in part on
 * org.hibernate.event.def.DefaultInitializeCollectionEventListener.
 * It adds logic to determine prefetch directives and to setup the
 * tracking of elements in the loaded collection.
 *
 * @author Ali Ibrahim <aibrahim@cs.utexas.edu>
 */
public class AutofetchInitializeCollectionListener extends
        DefaultInitializeCollectionEventListener implements InitializeCollectionEventListener {

    private static final Log log = LogFactory.getLog(AutofetchInitializeCollectionListener.class);

    private final ExtentManager extentManager;

    public AutofetchInitializeCollectionListener(ExtentManager extentManager) {
        this.extentManager = extentManager;
    }

    @Override
    public void onInitializeCollection(InitializeCollectionEvent event) throws HibernateException {
        PersistentCollection collection = event.getCollection();
        SessionImplementor source = event.getSession();

        CollectionEntry ce = source.getPersistenceContext().getCollectionEntry(collection);
        if (ce == null) {
            throw new HibernateException("collection was evicted");
        }

        if (!collection.wasInitialized()) {

            final boolean foundInCache = initializeCollectionFromCache(
                    ce.getLoadedKey(), ce.getLoadedPersister(), collection, source
            );

            if (foundInCache) {
                log.trace("collection initialized from cache");
            } else {
                log.trace("collection not cached");

                CollectionPersister cp = ce.getLoadedPersister();
                String classname = cp.getOwnerEntityPersister().getEntityName();
                String tpKey = cp.getRole();

                List<Path> prefetchPaths = extentManager.getPrefetchPaths(tpKey);
                if (!prefetchPaths.isEmpty()) {
                    String assoc = tpKey.substring(tpKey.lastIndexOf('.') + 1);
                    List<Path> augmentedPaths = new ArrayList<>();
                    augmentedPaths.add(new Path().addTraversal(assoc));

                    for (Path p : prefetchPaths) {
                        augmentedPaths.add(p.prependTraversal(assoc));
                    }

                    log.debug("Prefetch paths: " + augmentedPaths);
                    AutofetchLoadListener.getResult(augmentedPaths,
                            classname,
                            ce.getKey(),
                            LockMode.NONE,
                            event.getSession());

                    if (!collection.wasInitialized()) {
                        throw new IllegalStateException("Collection not initialized");
                    }
                } else {
                    ce.getLoadedPersister().initialize(ce.getLoadedKey(), source);
                    if (source.getFactory().getStatistics().isStatisticsEnabled()) {
                        source.getFactory().getStatisticsImplementor().fetchCollection(ce.getLoadedPersister().getRole());
                    }
                }

                boolean oldTracking = false;
                if (collection instanceof Trackable) {
                    Trackable trackable = (Trackable) collection;
                    oldTracking = trackable.disableTracking();
                }

                Iterator elementsIter = collection.entries(cp);
                while (elementsIter.hasNext()) {
                    extentManager.markAsRoot(elementsIter.next(), tpKey);
                }

                if (collection instanceof Trackable) {
                    Trackable trackable = (Trackable) collection;
                    if (oldTracking) {
                        trackable.enableTracking();
                    }
                }

                log.trace("collection initialized");
            }
        }
    }

    /**
     * Try to initialize a collection from the cache
     */
    private boolean initializeCollectionFromCache(Serializable id,
                                                  CollectionPersister persister,
                                                  PersistentCollection collection,
                                                  SessionImplementor source) throws HibernateException {

        if (source.getLoadQueryInfluencers().hasEnabledFilters()
                && persister.isAffectedByEnabledFilters(source)) {
            return false;
        }

        final boolean useCache = persister.hasCache() && source.getCacheMode().isGetEnabled();

        if (useCache) {
            final SessionFactoryImplementor factory = source.getFactory();
            final CacheKey ck = source.generateCacheKey(id, persister.getKeyType(), persister.getRole());
            final Object ce = CacheHelper.fromSharedCache(source, ck, persister.getCacheAccessStrategy());
            if (factory.getStatistics().isStatisticsEnabled()) {
                if (ce == null) {
                    factory.getStatisticsImplementor().secondLevelCacheMiss(
                            persister.getCacheAccessStrategy().getRegion().getName());
                } else {
                    factory.getStatisticsImplementor().secondLevelCacheHit(
                            persister.getCacheAccessStrategy().getRegion().getName());
                }
            }

            if (ce != null) {
                CollectionCacheEntry cacheEntry = (CollectionCacheEntry) persister.getCacheEntryStructure()
                        .destructure(ce, factory);

                final PersistenceContext persistenceContext = source.getPersistenceContext();
                cacheEntry.assemble(collection, persister, persistenceContext.getCollectionOwner(id, persister));
                persistenceContext.getCollectionEntry(collection).postInitialize(collection);
                return true;
            }

            return false;
        }

        return false;
    }
}
