/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.demo;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.transaction.TransactionManager;

import junit.framework.TestCase;
import org.apache.lucene.search.Query;
import org.infinispan.Cache;
import org.infinispan.config.Configuration;
import org.infinispan.config.ConfigurationValidatingVisitor;
import org.infinispan.config.GlobalConfiguration;
import org.infinispan.config.InfinispanConfiguration;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.cfg.Environment;
import org.hibernate.ogm.datastore.infinispan.impl.TransactionManagerLookupDelegator;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.DatabaseRetrievalMethod;
import org.hibernate.search.query.ObjectLookupMethod;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.transaction.JBossTSStandaloneTransactionManagerLookup;

public class InsertAndQueryTest extends TestCase {
	private static final Logger log = LoggerFactory.getLogger( InsertAndQueryTest.class );

	private FullTextEntityManager fullTextEntityManager;
	private CacheContainer cacheContainer;

	public void testInsertQueryDelete() {
		createTestData();

		fullTextEntityManager.getTransaction().begin();

		FullTextQuery fullTextQuery = createMatchAllFulltextQuery();
		assertTrue( fullTextQuery.getResultSize() == 10 );

		fullTextEntityManager.getTransaction().commit();
		fullTextEntityManager.clear();

		deleteTestData();
	}

	@Override
	protected void setUp() throws Exception {
		cacheContainer = createCustomCacheManager( "infinispan-config.xml", new Properties() );

		TransactionManager transactionManager = new JBossTSStandaloneTransactionManagerLookup().getTransactionManager(
				null
		);
		transactionManager.begin();
		EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory( "ogm-demo" );
		fullTextEntityManager = Search.getFullTextEntityManager( entityManagerFactory.createEntityManager() );
	}

	@Override
	protected void tearDown() throws Exception {
		fullTextEntityManager.close();
	}

	private FullTextQuery createMatchAllFulltextQuery() {
		QueryBuilder queryBuilder = fullTextEntityManager.getSearchFactory()
				.buildQueryBuilder()
				.forEntity( Event.class )
				.get();
		Query query = queryBuilder.all().createQuery();
		FullTextQuery fulltextQuery = fullTextEntityManager.createFullTextQuery( query );
		fulltextQuery.initializeObjectsWith( ObjectLookupMethod.SKIP, DatabaseRetrievalMethod.FIND_BY_ID );
		return fulltextQuery;
	}

	private void checkInfinispanCache() {
		Cache<String, Object> cache = cacheContainer.getCache( "ENTITIES" );
		for ( Map.Entry entry : cache.entrySet() ) {
			log.info( entry.getKey().toString() + "" + entry.getValue().toString() );
		}

		cache = cacheContainer.getCache( "ASSOCIATIONS" );
		for ( Map.Entry entry : cache.entrySet() ) {
			log.info( entry.getKey().toString() + "" + entry.getValue().toString() );
		}
	}

	private void createTestData() {
		// create a couple of events...
		fullTextEntityManager.getTransaction().begin();
		for ( int i = 0; i < 10; i++ ) {
			Event event = new Event( "Event " + i, new Date() );
			event.addLogEntry( "Initial Creation of event " + i );
			fullTextEntityManager.persist( event );
		}
		fullTextEntityManager.getTransaction().commit();
		fullTextEntityManager.clear();
	}

	private void deleteTestData() {
		// delete all events...
		if ( !fullTextEntityManager.getTransaction().isActive() ) {
			fullTextEntityManager.getTransaction().begin();
		}
		List<Event> result = createMatchAllFulltextQuery().getResultList();
		for ( Event event : result ) {
			fullTextEntityManager.remove( event );
		}
		fullTextEntityManager.getTransaction().commit();
	}

	static EmbeddedCacheManager createCustomCacheManager(String cfgName, Properties properties) {
		try {
			InfinispanConfiguration configuration = InfinispanConfiguration.newInfinispanConfiguration(
					cfgName, InfinispanConfiguration.resolveSchemaPath(),
					new ConfigurationValidatingVisitor()
			);
			GlobalConfiguration globalConfiguration = configuration.parseGlobalConfiguration();
			Configuration defaultConfiguration = configuration.parseDefaultConfiguration();
			properties.setProperty(
					Environment.TRANSACTION_MANAGER_STRATEGY,
					JBossTSStandaloneTransactionManagerLookup.class.getName()
			);
			TransactionManagerLookupDelegator transactionManagerLookupDelegator = new TransactionManagerLookupDelegator(
					properties
			);
			final DefaultCacheManager cacheManager = new DefaultCacheManager(
					globalConfiguration,
					defaultConfiguration,
					true
			);
			for ( Map.Entry<String, Configuration> entry : configuration.parseNamedConfigurations().entrySet() ) {
				Configuration cfg = entry.getValue();
				cfg.setTransactionManagerLookup( transactionManagerLookupDelegator );
				cacheManager.defineConfiguration( entry.getKey(), cfg );
			}
			return cacheManager;
		}
		catch ( Exception e ) {
			e.printStackTrace();
			System.exit( -1 );
		}
		return null; //actually this line is unreachable
	}
}
