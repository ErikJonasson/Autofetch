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
package org.hibernate.cfg;

import java.util.Map;
import java.util.ArrayList;
import java.util.Iterator;

import org.hibernate.MappingException;
import org.hibernate.util.StringHelper;
import org.hibernate.engine.NamedSQLQueryDefinition;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.dom4j.Attribute;
import org.dom4j.Element;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * AHI: This class is copied here so that calls to HbmBinder can be changed to calls
 * to AutofetchHbmBinder.
 * 
 * @author Emmanuel Bernard
 */
@SuppressWarnings("unchecked")
public class AutofetchNamedSQLQuerySecondPass extends AutofetchResultSetMappingBinder implements QuerySecondPass {
	private static Log log = LogFactory.getLog( AutofetchNamedSQLQuerySecondPass.class);
	private Element queryElem;
	private String path;
	private Mappings mappings;

	public AutofetchNamedSQLQuerySecondPass(Element queryElem, String path, Mappings mappings) {
		this.queryElem = queryElem;
		this.path = path;
		this.mappings = mappings;
	}

	public void doSecondPass(Map persistentClasses) throws MappingException {
		String queryName = queryElem.attribute( "name" ).getValue();
		if (path!=null) queryName = path + '.' + queryName;

		boolean cacheable = "true".equals( queryElem.attributeValue( "cacheable" ) );
		String region = queryElem.attributeValue( "cache-region" );
		Attribute tAtt = queryElem.attribute( "timeout" );
		Integer timeout = tAtt == null ? null : new Integer( tAtt.getValue() );
		Attribute fsAtt = queryElem.attribute( "fetch-size" );
		Integer fetchSize = fsAtt == null ? null : new Integer( fsAtt.getValue() );
		Attribute roAttr = queryElem.attribute( "read-only" );
		boolean readOnly = roAttr != null && "true".equals( roAttr.getValue() );
		Attribute cacheModeAtt = queryElem.attribute( "cache-mode" );
		String cacheMode = cacheModeAtt == null ? null : cacheModeAtt.getValue();
		Attribute cmAtt = queryElem.attribute( "comment" );
		String comment = cmAtt == null ? null : cmAtt.getValue();

		java.util.List synchronizedTables = new ArrayList();
		Iterator tables = queryElem.elementIterator( "synchronize" );
		while ( tables.hasNext() ) {
			synchronizedTables.add( ( (Element) tables.next() ).attributeValue( "table" ) );
		}
		boolean callable = "true".equals( queryElem.attributeValue( "callable" ) );

		NamedSQLQueryDefinition namedQuery;
		Attribute ref = queryElem.attribute( "resultset-ref" );
		String resultSetRef = ref == null ? null : ref.getValue();
		if ( StringHelper.isNotEmpty( resultSetRef ) ) {
			namedQuery = new NamedSQLQueryDefinition(
					queryElem.getText(),
					resultSetRef,
					synchronizedTables,
					cacheable,
					region,
					timeout,
					fetchSize,
					AutofetchHbmBinder.getFlushMode( queryElem.attributeValue( "flush-mode" ) ),
					AutofetchHbmBinder.getCacheMode( cacheMode ),
					readOnly,
					comment,
					AutofetchHbmBinder.getParameterTypes( queryElem ),
					callable
			);
			//TODO check there is no actual definition elemnents when a ref is defined
		}
		else {
			ResultSetMappingDefinition definition = buildResultSetMappingDefinition( queryElem, path, mappings );
			namedQuery = new NamedSQLQueryDefinition(
					queryElem.getText(),
					definition.getQueryReturns(),
					synchronizedTables,
					cacheable,
					region,
					timeout,
					fetchSize,
					AutofetchHbmBinder.getFlushMode( queryElem.attributeValue( "flush-mode" ) ),
					AutofetchHbmBinder.getCacheMode( cacheMode ),
					readOnly,
					comment,
					AutofetchHbmBinder.getParameterTypes( queryElem ),
					callable
			);
		}

		log.debug( "Named SQL query: " + queryName + " -> " + namedQuery.getQueryString() );
		mappings.addSQLQuery( queryName, namedQuery );
	}
}
