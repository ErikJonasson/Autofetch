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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper around Hibernate criteria queries which performs prefetch.
 *
 * @author Ali Ibrahim <aibrahim@cs.utexas.edu>
 */
public class AutofetchCriteria implements Criteria {

	private static final Logger log = LoggerFactory.getLogger( AutofetchLoadListener.class );

	private final CriteriaImpl delegate;

	public AutofetchCriteria(CriteriaImpl criteria) {
		this.delegate = criteria;
	}

	/**
	 * Convenience method which casts argument to CriteriaImpl
	 *
	 * @param criteria must be a CriteriaImpl
	 */
	public AutofetchCriteria(Criteria criteria) {
		this.delegate = (CriteriaImpl) criteria;
	}

	@Override
	public List list() throws HibernateException {
		String classname = this.delegate.getEntityOrClassName();

		/// Add prefetch directives as needed
		addPrefetch( this.delegate, classname );
		Iterator subCriteria = this.delegate.iterateSubcriteria();
		while ( subCriteria.hasNext() ) {
			Criteria sc = (Criteria) subCriteria.next();
			addPrefetch( sc, makeTpKey( classname, sc.getAlias() ) );
		}

		List results = this.delegate.list();

		// Mark results of query as root entities
		ResultTransformer rt = delegate.getResultTransformer();

		// We can only handle transformers we know.
		if ( rt.equals( Criteria.DISTINCT_ROOT_ENTITY )
				|| rt.equals( Criteria.ROOT_ENTITY )
				|| rt.equals( Criteria.ALIAS_TO_ENTITY_MAP ) ) {
			for ( Object o : results ) {
				if ( rt.equals( Criteria.ALIAS_TO_ENTITY_MAP ) ) {
					Map m = (Map) o;
					Set aliasKeys = m.keySet();
					for ( Object aliasKey : aliasKeys ) {
						String tpKey = makeTpKey( classname, aliasKey );
						getExtentManager().markAsRoot( m.get( aliasKey ), tpKey );
					}
				}
				else {
					getExtentManager().markAsRoot( o, classname );
				}
			}
		}

		return results;
	}

	@Override
	public Object uniqueResult() throws HibernateException {
		String classname = this.delegate.getEntityOrClassName();
		addPrefetch( this.delegate, classname );
		Object o = this.delegate.uniqueResult();
		getExtentManager().markAsRoot( o, classname );
		return o;
	}

	@Override
	public Criteria add(Criterion criterion) {
		return this.delegate.add( criterion );
	}

	@Override
	public Criteria addOrder(Order ordering) {
		return delegate.addOrder( ordering );
	}

	@Override
	@Deprecated
	public Criteria createAlias(String associationPath, String alias, int joinType) {
		return delegate.createAlias( associationPath, alias, joinType );
	}

	@Override
	public Criteria createAlias(String associationPath, String alias) {
		return delegate.createAlias( associationPath, alias );
	}

	@Override
	@Deprecated
	public Criteria createCriteria(String associationPath, int joinType) {
		return delegate.createCriteria( associationPath, joinType );
	}

	@Override
	@Deprecated
	public Criteria createCriteria(String associationPath, String alias, int joinType) {
		return delegate.createCriteria( associationPath, alias, joinType );
	}

	@Override
	public Criteria createCriteria(String associationPath, String alias) {
		return delegate.createCriteria( associationPath, alias );
	}

	@Override
	public Criteria createCriteria(String associationPath) {
		return delegate.createCriteria( associationPath );
	}

	@Override
	public ScrollableResults scroll() {
		return delegate.scroll();
	}

	@Override
	public ScrollableResults scroll(ScrollMode scrollMode) {
		return delegate.scroll( scrollMode );
	}

	@Override
	public Criteria setCacheable(boolean cacheable) {
		return delegate.setCacheable( cacheable );
	}

	@Override
	public Criteria setCacheMode(CacheMode cacheMode) {
		return delegate.setCacheMode( cacheMode );
	}

	@Override
	public Criteria setCacheRegion(String cacheRegion) {
		return delegate.setCacheRegion( cacheRegion );
	}

	@Override
	public Criteria setComment(String comment) {
		return delegate.setComment( comment );
	}

	@Override
	public Criteria setFetchMode(String associationPath, FetchMode fetchMode) {
		return delegate.setFetchMode( associationPath, fetchMode );
	}

	@Override
	public Criteria setFetchSize(int fetchSize) {
		return delegate.setFetchSize( fetchSize );
	}

	@Override
	public Criteria setFirstResult(int firstResult) {
		return delegate.setFirstResult( firstResult );
	}

	@Override
	public Criteria setFlushMode(FlushMode flushMode) {
		return delegate.setFlushMode( flushMode );
	}

	@Override
	public Criteria setLockMode(LockMode lockMode) {
		return delegate.setLockMode( lockMode );
	}

	@Override
	public Criteria setLockMode(String alias, LockMode lockMode) {
		return delegate.setLockMode( alias, lockMode );
	}

	@Override
	public Criteria setMaxResults(int maxResults) {
		return delegate.setMaxResults( maxResults );
	}

	@Override
	public Criteria setProjection(Projection projection) {
		return delegate.setProjection( projection );
	}

	@Override
	public Criteria setResultTransformer(ResultTransformer tupleMapper) {
		return delegate.setResultTransformer( tupleMapper );
	}

	@Override
	public Criteria setTimeout(int setTimeout) {
		return delegate.setTimeout( setTimeout );
	}

	@Override
	public String getAlias() {
		return delegate.getAlias();
	}

	@Override
	@Deprecated
	public Criteria createAlias(String associationPath, String alias, int joinType, Criterion withClause)
			throws HibernateException {
		return this.delegate.createAlias( associationPath, alias, joinType, withClause );
	}

	@Override
	@Deprecated
	public Criteria createCriteria(String associationPath, String alias, int joinType, Criterion withClause)
			throws HibernateException {
		return this.delegate.createCriteria( associationPath, alias, joinType, withClause );
	}

	@Override
	public boolean isReadOnlyInitialized() {
		return this.delegate.isReadOnlyInitialized();
	}

	@Override
	public boolean isReadOnly() {
		return this.delegate.isReadOnly();
	}

	@Override
	public Criteria setReadOnly(boolean readOnly) {
		return this.delegate.setReadOnly( readOnly );
	}

	@Override
	public Criteria createAlias(String associationPath, String alias, JoinType joinType) throws HibernateException {
		return this.delegate.createCriteria( associationPath, alias, joinType );
	}

	@Override
	public Criteria createAlias(String associationPath, String alias, JoinType joinType, Criterion withClause)
			throws HibernateException {
		return this.delegate.createCriteria( associationPath, alias, joinType, withClause );
	}

	@Override
	public Criteria createCriteria(String associationPath, JoinType joinType) throws HibernateException {
		return this.delegate.createCriteria( associationPath, joinType );
	}

	@Override
	public Criteria createCriteria(String associationPath, String alias, JoinType joinType) throws HibernateException {
		return this.delegate.createCriteria( associationPath, alias, joinType );
	}

	@Override
	public Criteria createCriteria(String associationPath, String alias, JoinType joinType, Criterion withClause)
			throws HibernateException {
		return this.delegate.createCriteria( associationPath, alias, joinType, withClause );
	}

	@Override
	public Criteria addQueryHint(String hint) {
		return this.delegate.addQueryHint( hint );
	}

	private void addPrefetch(Criteria crit, String tpKey) {
		List<Path> prefetchPaths = getExtentManager().getPrefetchPaths( tpKey );
		if ( log.isDebugEnabled() ) {
			log.debug( "Prefetch paths for " + tpKey + ": " + prefetchPaths );
		}

		for ( Path p : prefetchPaths ) {
			crit.setFetchMode( p.toString(), FetchMode.JOIN );
		}
	}

	private ExtentManager getExtentManager() {
		return this.delegate.getSession()
				.getFactory()
				.getServiceRegistry()
				.getService( AutofetchService.class )
				.getExtentManager();
	}

	private static String makeTpKey(String classname, Object alias) {
		return classname + ":" + alias;
	}
}
