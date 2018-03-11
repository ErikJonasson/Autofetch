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
import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projection;
import org.hibernate.internal.CriteriaImpl;
import org.hibernate.sql.JoinType;
import org.hibernate.transform.ResultTransformer;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Wrapper around Hibernate criteria queries which performs prefetch.
 *
 * @author Ali Ibrahim <aibrahim@cs.utexas.edu>
 *
 */
public class AutofetchCriteria implements Criteria {

    private static final Log log = LogFactory.getLog(AutofetchLoadListener.class);

    private CriteriaImpl delegate;

    public AutofetchCriteria(CriteriaImpl ci) {
        delegate = ci;
    }

    /**
     * Convenience method which casts argument to CriteriaImpl
     * @param ci must be a CriteriaImpl
     */
    public AutofetchCriteria(Criteria ci) {
        delegate = (CriteriaImpl) ci;
    }

    @Override
    public List list() throws HibernateException {
        String classname = delegate.getEntityOrClassName();

        /// Add prefetch directives as needed
        addPrefetch(delegate, classname);
        Iterator subCriteria = delegate.iterateSubcriteria();
        while (subCriteria.hasNext()) {
            Criteria sc = (Criteria) subCriteria.next();
            addPrefetch(sc, makeTpKey(classname, sc.getAlias()));
        }

        List l = delegate.list();
        // Mark results of query as root entities
        ResultTransformer rt = delegate.getResultTransformer();
        // We can only handle transformers we know.
        if (rt.equals(Criteria.DISTINCT_ROOT_ENTITY)
                || rt.equals(Criteria.ROOT_ENTITY)
                || rt.equals(Criteria.ALIAS_TO_ENTITY_MAP)) {
            for (Object o : l) {
                if (rt.equals(Criteria.ALIAS_TO_ENTITY_MAP)) {
                    Map m = (Map) o;
                    Set aliasKeys = m.keySet();
                    for (Object aliasKey : aliasKeys) {
                        String tpKey = makeTpKey(classname, aliasKey);
                        getExtentManager().markAsRoot(m.get(aliasKey), tpKey);
                    }
                } else {
                    getExtentManager().markAsRoot(o, classname);
                }

            }
        }
        return l;
    }

    private void addPrefetch(Criteria crit, String tpKey) {
        List<Path> prefetchPaths = getExtentManager().getPrefetchPaths(tpKey);
        if (log.isDebugEnabled()) {
            log.debug("Prefetch paths for " + tpKey + ": " + prefetchPaths);
        }
        for (Path p : prefetchPaths) {
            crit.setFetchMode(p.toString(), FetchMode.JOIN);
        }
    }

    private String makeTpKey(String classname, Object alias) {
        return classname + ":" + alias;
    }

    @Override
    public Object uniqueResult() throws HibernateException {
        String classname = delegate.getEntityOrClassName();
        addPrefetch(delegate, classname);
        Object o = delegate.uniqueResult();
        getExtentManager().markAsRoot(o, classname);
        return o;
    }

    private ExtentManager getExtentManager() {
        return delegate.getSession().getFactory().getServiceRegistry().getService(AutofetchService.class).getExtentManager();
    }

    @Override
    public Criteria add(Criterion arg0) {
        return delegate.add(arg0);
    }

    @Override
    public Criteria addOrder(Order arg0) {
        return delegate.addOrder(arg0);
    }

    @Override
    public Criteria createAlias(String arg0, String arg1, int arg2) {
        return delegate.createAlias(arg0, arg1, arg2);
    }

    @Override
    public Criteria createAlias(String arg0, String arg1) {
        return delegate.createAlias(arg0, arg1);
    }

    @Override
    public Criteria createCriteria(String arg0, int arg1) {
        return delegate.createCriteria(arg0, arg1);
    }

    @Override
    public Criteria createCriteria(String arg0, String arg1, int arg2) {
        return delegate.createCriteria(arg0, arg1, arg2);
    }

    @Override
    public Criteria createCriteria(String arg0, String arg1) {
        return delegate.createCriteria(arg0, arg1);
    }

    @Override
    public Criteria createCriteria(String arg0) {
        return delegate.createCriteria(arg0);
    }

    @Override
    public ScrollableResults scroll() {
        return delegate.scroll();
    }

    @Override
    public ScrollableResults scroll(ScrollMode arg0) {
        return delegate.scroll(arg0);
    }

    @Override
    public Criteria setCacheable(boolean arg0) {
        return delegate.setCacheable(arg0);
    }

    @Override
    public Criteria setCacheMode(CacheMode arg0) {
        return delegate.setCacheMode(arg0);
    }

    @Override
    public Criteria setCacheRegion(String arg0) {
        return delegate.setCacheRegion(arg0);
    }

    @Override
    public Criteria setComment(String arg0) {
        return delegate.setComment(arg0);
    }

    @Override
    public Criteria setFetchMode(String arg0, FetchMode arg1) {
        return delegate.setFetchMode(arg0, arg1);
    }

    @Override
    public Criteria setFetchSize(int arg0) {
        return delegate.setFetchSize(arg0);
    }

    @Override
    public Criteria setFirstResult(int arg0) {
        return delegate.setFirstResult(arg0);
    }

    @Override
    public Criteria setFlushMode(FlushMode arg0) {
        return delegate.setFlushMode(arg0);
    }

    @Override
    public Criteria setLockMode(LockMode arg0) {
        return delegate.setLockMode(arg0);
    }

    @Override
    public Criteria setLockMode(String arg0, LockMode arg1) {
        return delegate.setLockMode(arg0, arg1);
    }

    @Override
    public Criteria setMaxResults(int arg0) {
        return delegate.setMaxResults(arg0);
    }

    @Override
    public Criteria setProjection(Projection arg0) {
        return delegate.setProjection(arg0);
    }

    @Override
    public Criteria setResultTransformer(ResultTransformer arg0) {
        return delegate.setResultTransformer(arg0);
    }

    @Override
    public Criteria setTimeout(int arg0) {
        return delegate.setTimeout(arg0);
    }

    @Override
    public String getAlias() {
        return delegate.getAlias();
    }

    @Override
    public Criteria createAlias(String associationPath, String alias, int joinType, Criterion withClause)
            throws HibernateException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Criteria createCriteria(String associationPath, String alias, int joinType, Criterion withClause)
            throws HibernateException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isReadOnlyInitialized() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isReadOnly() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Criteria setReadOnly(boolean readOnly) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Criteria createAlias(String associationPath, String alias, JoinType joinType) throws HibernateException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Criteria createAlias(String associationPath, String alias, JoinType joinType, Criterion withClause)
            throws HibernateException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Criteria createCriteria(String associationPath, JoinType joinType) throws HibernateException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Criteria createCriteria(String associationPath, String alias, JoinType joinType) throws HibernateException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Criteria createCriteria(String associationPath, String alias, JoinType joinType, Criterion withClause)
            throws HibernateException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Criteria addQueryHint(String hint) {
        // TODO Auto-generated method stub
        return null;
    }

//	@Override
//	public Criteria createAlias(String arg0, String arg1, int arg2, Criterion arg3) throws HibernateException {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public Criteria createCriteria(String arg0, String arg1, int arg2, Criterion arg3) throws HibernateException {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public boolean isReadOnly() {
//		// TODO Auto-generated method stub
//		return false;
//	}
//
//	@Override
//	public boolean isReadOnlyInitialized() {
//		// TODO Auto-generated method stub
//		return false;
//	}
//
//	@Override
//	public Criteria setReadOnly(boolean arg0) {
//		// TODO Auto-generated method stub
//		return null;
//	}
}
