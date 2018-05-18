/*
 * Created on Jun 6, 2005
 */
package org.autofetch.test;

import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.LazyInitializationException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.jpa.AvailableSettings;

import org.junit.Assert;
import org.junit.Test;

import org.autofetch.hibernate.AutofetchCriteria;
import org.autofetch.hibernate.AutofetchService;
import org.autofetch.hibernate.ExtentManager;
import org.autofetch.hibernate.Statistics;
import org.autofetch.hibernate.TraversalProfile;

/**
 * Test the gathering of extent statistics and use of fetch profile. The tests here assume a fetch depth greater than or equal to 3.
 *
 * @author aibrahim
 */
public class ExtentTest extends BaseEntityManagerFunctionalTestCase {

	// Number of subordinates for root object in createNObjectGraph
	private static final int NUM_SUBORDINATES = 50;

	private static final int NUM_FRIENDS = 2;

	private ExtentManager em;

	private SessionFactory sf;

	@Test
	public void testSimpleLoad() {
		em.clearExtentInformation();

		Long daveId = createObjectGraph( true );
		try (Session sess = openSession()) {
			Transaction tx = sess.beginTransaction();
			Employee dave = sess.load( Employee.class, daveId );
			Assert.assertTrue( "object should not be intialized", !Hibernate.isInitialized( dave ) );
			Assert.assertNotNull( dave.getName() );
			Assert.assertTrue( "object should be intialized now", Hibernate.isInitialized( dave ) );
			tx.commit();
		}
	}

	@Test
	public void test() {
		em.clearExtentInformation();

		Long daveId = createObjectGraph( true );

		try (Session sess = openSession()) {
			Transaction tx = sess.beginTransaction();
			Employee dave = sess.load( Employee.class, daveId );
			tx.commit();
			Assert.assertNotNull( dave.getName() );
		}
	}

	/**
	 * Test to show correct fetch profile is generated and executed.
	 */
	@Test
	public void testLoadFetchProfile() {
		em.clearExtentInformation();

		// Execute query multiple times
//        someAccess(true);
		Employee dave = someAccess( true );

		// These all should not throw lazy instantiation exception, because
		// they should have been fetch eagerly
		Assert.assertNotNull( dave.getSupervisor().getName() );
		Assert.assertNotNull( dave.getSupervisor().getSupervisor().getName() );
		Assert.assertNotEquals( 0, dave.getSupervisor().getSupervisor().getSubordinates().size() );
		Assert.assertNotNull( dave.getMentor().getName() );

		// This should throw a lazy instantiation exception
		try {
			Assert.assertNotNull( dave.getSupervisor().getSupervisor().getSupervisor().getName() );
			// Shouldn't get here
			Assert.fail( "Lazy instantion exception not thrown for a property which shouldn't have been fetched" );
		}
		catch (LazyInitializationException e) {
			// Good
		}
	}

	/**
	 * Test to show correct fetch profile is generated and executed when fetching multiple collections.
	 */
	@Test
	public void testMultipleCollectionFetch() {
		em.clearExtentInformation();

		// Execute query multiple times to build up extent statistics
		Employee dave = null;
		for ( int i = 0; i < 2; i++ ) {
			dave = multipleCollectionAccess( i == 0 );
		}

		// These all should not throw lazy instantiation exception, because
		// they should have been fetch eagerly.

		Assert.assertNotNull( dave.getSupervisor().getName() );
		Assert.assertNotEquals( 0, dave.getSupervisor().getSubordinates().size() );
		Assert.assertNotEquals( 0, dave.getSubordinates().iterator().next().getSubordinates().size() );
	}

	/**
	 * Test to show correct fetch profile is generated and executed when a collection is initialized
	 */
	@Test
	public void testInitializeCollectionFetch() {
		em.clearExtentInformation();

		// Execute query multiple times to build up extent statistics
		Employee dave = null;
		for ( int i = 0; i < 2; i++ ) {
			dave = initializeCollectionAccess( i == 0 );
		}

		// These all should not throw lazy instantiation exception, because
		// they should have been fetch eagerly.
		for ( Employee employee : dave.getSubordinates() ) {
			Assert.assertNotEquals( 0, employee.getSubordinates().size() );
		}
	}

	/**
	 * Test to show correct fetch profile is generated and executed.
	 */
	@Test
	public void testLoadFetchProfileCriteria() {
		em.clearExtentInformation();

		// Execute query multiple times
		Employee dave = null;
		for ( int i = 0; i < 2; i++ ) {
			dave = someAccessCriteria( i == 0 );
		}

		// These all should not throw lazy instantiation exception, because
		// they should have been fetch eagerly
		Assert.assertNotNull( dave.getSupervisor().getName() );
		Assert.assertNotNull( dave.getSupervisor().getSupervisor().getName() );
		Assert.assertNotEquals( 0, dave.getSupervisor().getSupervisor().getSubordinates().size() );
		Assert.assertNotNull( dave.getMentor().getName() );

		// This should throw a lazy instantiation exception
		try {
			Assert.assertNotNull( dave.getSupervisor().getSupervisor().getSupervisor().getName() );
			// Shouldn't get here
			Assert.fail( "Lazy instantion exception not thrown for a property which shouldn't have been fetched" );
		}
		catch (LazyInitializationException e) {
			// Good
		}
	}

	/**
	 * Test to show correct fetch profile is generated and executed when fetching multiple collections.
	 */
	@Test
	public void testMultipleCollectionFetchCriteria() {
		em.clearExtentInformation();

		// Execute query multiple times to build up extent statistics
		Employee dave = null;
		for ( int i = 0; i < 2; i++ ) {
			dave = multipleCollectionAccessCriteria( i == 0 );
		}

		// These all should not throw lazy instantiation exception, because
		// they should have been fetch eagerly.

		Assert.assertNotNull( dave.getSupervisor().getName() );
		Assert.assertNotEquals( 0, dave.getSupervisor().getSubordinates().size() );
		Assert.assertNotEquals( 0, dave.getSubordinates().iterator().next().getSubordinates().size() );
	}

	/**
	 * Test accessing address and checking extent statistics. Since address is a component, there should be not sub-extent for it.
	 */
	@Test
	public void testAddressAccess() {
		em.clearExtentInformation();

		addressAccess();

		TraversalProfile tp = em.getFirstProfile();
		checkStatistics( tp, 0, "m_mentor" );
		checkStatistics( tp, 0, "m_supervisor" );
		checkStatistics( tp, 0, "m_subordinates" );
		checkStatistics( tp, 0, "m_friends" );

		Assert.assertEquals( "The number of sub-extents is not 5", 4, tp.numSubProfiles() );
		Assert.assertTrue( "Supervisor extent should be empty", tp.getSubProfile( "m_supervisor" ).isEmpty() );
		Assert.assertTrue( "Subordinates extent should be empty", tp.getSubProfile( "m_subordinates" ).isEmpty() );
		Assert.assertTrue( "Friends extent should be empty", tp.getSubProfile( "m_friends" ).isEmpty() );
	}

	/**
	 * Load employee and traverse supervisor association and check extent statistics.
	 */
	@Test
	public void testFriendAccess() {
		em.clearExtentInformation();

		friendAccess();

		checkFriendAccess();
	}

	private void checkFriendAccess() {
		TraversalProfile tp = em.getFirstProfile();
		checkStatistics( tp, 0, "m_mentor" );
		checkStatistics( tp, 0, "m_supervisor" );
		checkStatistics( tp, 0, "m_subordinates" );
		checkStatistics( tp, 1, "m_friends" );

		checkStatisticsNotAccessed( tp.getSubProfile( "m_friends" ), "m_mentor", "m_supervisor", "m_subordinates" );

		Assert.assertTrue( "Supervisor extent should be empty", tp.getSubProfile( "m_supervisor" ).isEmpty() );
		Assert.assertTrue( "Subordinate extent should be empty", tp.getSubProfile( "m_subordinates" ).isEmpty() );
	}

	/**
	 * Load employee and traverse supervisor association two levels deep and check extent statistics.
	 */
	@Test
	public void testSecondLevelSupervisorAccess() {
		em.clearExtentInformation();

		secondLevelSupervisorAccess();

		TraversalProfile tp = em.getFirstProfile();
		checkStatistics( tp, 0, "m_mentor" );
		checkStatistics( tp, 1, "m_supervisor" );
		checkStatistics( tp, 0, "m_subordinates" );
		checkStatistics( tp, 0, "m_friends" );

		checkStatistics( tp.getSubProfile( "supervisor" ), 0, "subordinates" );
		checkStatistics( tp.getSubProfile( "supervisor" ), 1, "supervisor" );
		checkStatistics(
				tp.getSubProfile( "supervisor" ).getSubProfile( "supervisor" ),
				0,
				"supervisor",
				"mentor",
				"subordinates"
		);

		Assert.assertTrue( "Subordinate extent should be empty", tp.getSubProfile( "subordinates" ).isEmpty() );
	}

	/**
	 * Load employee and traverse supervisor + mentor associations and check extent statistics.
	 */
	@Test
	public void testSupervisorAndMentorAccess() {
		em.clearExtentInformation();

		supervisorAndMentorAccess();

		TraversalProfile tp = em.getFirstProfile();
		checkStatistics( tp, 1, "m_mentor" );
		checkStatistics( tp, 1, "m_supervisor" );
		checkStatistics( tp, 0, "m_subordinates" );
		checkStatistics( tp, 0, "m_friends" );

		checkStatistics( tp.getSubProfile( "supervisor" ), 0, "supervisor", "subordinates" );

		Assert.assertTrue( "Subordinate extent should be empty", tp.getSubProfile( "subordinates" ).isEmpty() );
	}

	/**
	 * Load employee and traverse mentor associations and check extent statistics.
	 */
	@Test
	public void testMentorAccess() {
		em.clearExtentInformation();

		mentorAccess();

		TraversalProfile tp = em.getFirstProfile();
		checkStatistics( tp, 1, "m_mentor" );
		checkStatistics( tp, 0, "m_supervisor" );
		checkStatistics( tp, 0, "m_subordinates" );
		checkStatistics( tp, 0, "m_friends" );

		checkStatistics( tp.getSubProfile( "m_mentor" ), 0, "m_mentor", "m_supervisor", "m_subordinates" );

		Assert.assertTrue( "Supervisor extent should be empty", tp.getSubProfile( "m_supervisor" ).isEmpty() );
		Assert.assertTrue( "Subordinate extent should be empty", tp.getSubProfile( "m_subordinates" ).isEmpty() );
	}

	/**
	 * Load mentor association with same query and program state several times and check extent statisitics.
	 */
	@Test
	public void testMentorAccessAggregate1() {
		em.clearExtentInformation();

		mentorAccess();

		TraversalProfile tp = em.getFirstProfile();
		checkStatistics( tp, 1, "m_mentor" );
		checkStatistics( tp, 0, "m_supervisor" );
		checkStatistics( tp, 0, "m_subordinates" );
		checkStatistics( tp, 0, "m_friends" );

		checkStatistics( tp.getSubProfile( "m_mentor" ), 0, "m_mentor", "m_supervisor", "m_subordinates" );

		Assert.assertTrue( "Supervisor extent should be empty", tp.getSubProfile( "m_supervisor" ).isEmpty() );
		Assert.assertTrue( "Subordinate extent should be empty", tp.getSubProfile( "m_subordinates" ).isEmpty() );
	}

	/**
	 * Load mentor association with same query and different program state and check extent statisitics.
	 */
	@Test
	public void testMentorAccessAggregate2() {
		em.clearExtentInformation();

		mentorAccess();
		mentorAccessProxy();

		TraversalProfile tp = em.getFirstProfile();
		checkStatistics( tp, 1, "m_mentor" );
		checkStatistics( tp, 0, "m_supervisor" );
		checkStatistics( tp, 0, "m_subordinates" );
		checkStatistics( tp, 0, "m_friends" );

		checkStatistics( tp.getSubProfile( "m_mentor" ), 0, "m_mentor", "m_supervisor", "m_subordinates" );

		Assert.assertTrue( "Supervisor extent should be empty", tp.getSubProfile( "m_supervisor" ).isEmpty() );
		Assert.assertTrue( "Subordinate extent should be empty", tp.getSubProfile( "m_subordinates" ).isEmpty() );
	}

	/**
	 * Test accessing the subordinates collection association and checking statistics.
	 */
	@Test
	public void testCollectionAccess() {
		em.clearExtentInformation();

		collectionAccess1();

		TraversalProfile tp = em.getFirstProfile();
		checkStatistics( tp, 1, "m_subordinates" );
		checkStatistics( tp, 0, "m_friends" );

		checkStatisticsNotAccessed( tp.getSubProfile( "m_subordinates" ), "m_mentor", "m_subordinates" );
	}

	/**
	 * Access mentor for each element in the subordinates collection and check extent statistics.
	 */
	@Test
	public void testCollectionAccess2ndLevel1() {
		em.clearExtentInformation();

		collectionAccess2();

		TraversalProfile tp = em.getFirstProfile();
		checkStatistics( tp, 1, "m_subordinates" );
		checkStatistics( tp, 0, "m_friends" );

		checkStatisticsNotAccessed( tp.getSubProfile( "m_subordinates" ), "m_subordinates" );

		checkStatisticsAccessed( tp.getSubProfile( "m_subordinates" ), "m_mentor" );
	}

	/**
	 * Access subordinates for each element in the subordinates collections and check extent statistics.
	 */
	@Test
	public void testCollectionAccess2ndLevel2() {
		em.clearExtentInformation();

		collectionAccess3();

		TraversalProfile tp = em.getFirstProfile();
		checkStatistics( tp, 1, "m_subordinates" );
		checkStatistics( tp, 0, "m_friends" );

		checkStatisticsNotAccessed( tp.getSubProfile( "m_subordinates" ), "m_mentor" );

		checkStatisticsAccessed( tp.getSubProfile( "m_subordinates" ), "m_subordinates" );
	}

	private void checkStatistics(TraversalProfile tp, int accesses, String... propNames) {
		for ( String propName : propNames ) {
			Assert.assertTrue( "subExtent for " + propName + " does not exist", tp.hasSubProfile( propName ) );
			Statistics stat = tp.getSubProfileStats( propName );
			Assert.assertEquals( "Check total for " + propName + " association is " + 1, 1, stat.getTotal() );
			Assert.assertEquals(
					"Check access for " + propName + " association is " + accesses,
					accesses,
					stat.getAccessed()
			);
		}
	}

	private void checkStatisticsAccessed(TraversalProfile tp, String... propNames) {
		for ( String propName : propNames ) {
			Assert.assertTrue( "subExtent for " + propName + " does not exist", tp.hasSubProfile( propName ) );
			Statistics stat = tp.getSubProfileStats( propName );
			Assert.assertTrue( "Check that toal is not zero for " + propName, stat.getTotal() > 0 );
			Assert.assertEquals(
					"Check access is same as total for  " + propName,
					stat.getAccessed(),
					stat.getTotal()
			);
		}
	}

	private void checkStatisticsNotAccessed(TraversalProfile tp, String... propNames) {
		for ( String propName : propNames ) {
			Assert.assertTrue( "subExtent for " + propName + " does not exist", tp.hasSubProfile( propName ) );
			Statistics stat = tp.getSubProfileStats( propName );
			Assert.assertTrue( "Check that toal is not zero for " + propName, stat.getTotal() > 0 );
			Assert.assertEquals( "Check access is zero for  " + propName, 0, stat.getAccessed() );
		}
	}

	private void collectionAccess1() {
		Long grandfatherId = createNObjectGraph();
		try (Session sess = openSession()) {
			Transaction tx = sess.beginTransaction();
			Employee grandfather = sess.load( Employee.class, grandfatherId );
			Assert.assertEquals(
					"Checking size of subordinates collection",
					NUM_SUBORDINATES,
					grandfather.getSubordinates().size()
			);
			for ( Employee child : grandfather.getSubordinates() ) {
				Assert.assertNotNull( child.getName() );
			}
			tx.commit();
		}
	}

	private void collectionAccess2() {
		Long grandfatherId = createNObjectGraph();
		try (Session sess = openSession()) {

			Transaction tx = sess.beginTransaction();
			Employee grandfather = sess.load( Employee.class, grandfatherId );
			Assert.assertEquals(
					"Checking size of subordinates collection",
					NUM_SUBORDINATES,
					grandfather.getSubordinates().size()
			);
			for ( Employee child : grandfather.getSubordinates() ) {
				Assert.assertNotNull( child.getName() );
				Assert.assertNotNull( child.getMentor().getName() );
			}
			tx.commit();
		}
	}

	private void collectionAccess3() {
		Long grandfatherId = createNObjectGraph();
		try (Session sess = openSession()) {
			Transaction tx = sess.beginTransaction();
			Employee grandfather = sess.load( Employee.class, grandfatherId );
			Assert.assertEquals(
					"Checking size of subordinates collection",
					NUM_SUBORDINATES,
					grandfather.getSubordinates().size()
			);
			for ( Employee child : grandfather.getSubordinates() ) {
				Assert.assertNotNull( child.getName() );
				child.getSubordinates().size();
			}
			tx.commit();
		}
	}

	private void addressAccess() {
		Long daveId = createObjectGraph( true );
		try (Session sess = openSession()) {
			Transaction tx = sess.beginTransaction();
			Employee dave = sess.load( Employee.class, daveId );
			Assert.assertNotNull( dave.getAddress().getStreet() );
			Assert.assertNotNull( dave.getAddress().getCity() ); // Second access shouldn't make a difference
			tx.commit();
		}
	}

	private void mentorAccessProxy() {
		mentorAccess();
	}

	private void mentorAccess() {
		Long daveId = createObjectGraph( true );
		try (Session sess = openSession();) {
			Transaction tx = sess.beginTransaction();
			Employee dave = sess.load( Employee.class, daveId );
			Assert.assertNotNull( dave.getMentor().getName() );
			Assert.assertNotNull( dave.getMentor().getName() ); // Second access shouldn't make a difference
			tx.commit();
		}
	}

	private void friendAccess() {
		Long daveId = createObjectGraph( true );
		try (Session sess = openSession();) {
			Transaction tx = sess.beginTransaction();
			Employee dave = sess.load( Employee.class, daveId );
			Assert.assertNotEquals( 0, , dave.getFriends().size() );
			tx.commit();
		}
	}

	private void secondLevelSupervisorAccess() {
		Long daveId = createObjectGraph( true );
		try (Session sess = openSession();) {
			Transaction tx = sess.beginTransaction();
			Employee dave = sess.load( Employee.class, daveId );
			Assert.assertNotNull( dave.getSupervisor() );
			Assert.assertNotNull( dave.getSupervisor().getSupervisor().getName() );
			tx.commit();
		}
	}

	private void supervisorAndMentorAccess() {
		Long daveId = createObjectGraph( true );
		try (Session sess = openSession();) {
			Transaction tx = sess.beginTransaction();
			Employee dave = sess.load( Employee.class, daveId );
			Assert.assertNotNull( dave.getSupervisor().getName() );
			Assert.assertNotNull( dave.getMentor().getName() );
			tx.commit();
		}
	}


	private Employee someAccess(boolean traverse) {
		Long daveId = createObjectGraph( true );
		try (Session sess = openSession();) {
			Transaction tx = sess.beginTransaction();
			Employee dave = sess.load( Employee.class, daveId );
			Assert.assertNotNull( dave.getName() );
			if ( traverse ) {
				Assert.assertNotNull( dave.getSupervisor().getName() );
				Assert.assertNotNull( dave.getMentor().getName() );
				Assert.assertNotNull( dave.getSupervisor().getSupervisor().getName() );
				Assert.assertNotEquals( 0, dave.getSupervisor().getSupervisor().getSubordinates().size() );
			}
			tx.commit();
			return dave;
		}
	}

	private Employee someAccessCriteria(boolean traverse) {
		Long daveId = createObjectGraph( true );
		try (Session sess = openSession();) {
			Transaction tx = sess.beginTransaction();
			Criteria crit = new AutofetchCriteria( sess.createCriteria( Employee.class ) );
			crit.add( Restrictions.eq( "id", daveId ) );
			Employee dave = (Employee) crit.uniqueResult();
			Assert.assertNotNull( dave.getName() );
			if ( traverse ) {
				Assert.assertNotNull( dave.getSupervisor().getName() );
				Assert.assertNotNull( dave.getMentor().getName() );
				Assert.assertNotEquals( 0, dave.getSupervisor().getSupervisor().getName() );
				Assert.assertNotEquals( 0, dave.getSupervisor().getSupervisor().getSubordinates().size() );
			}
			tx.commit();
			return dave;
		}
	}

	private Employee initializeCollectionAccess(boolean traverse) {
		Long daveId = createObjectGraph( false );
		try (Session sess = openSession();) {
			Transaction tx = sess.beginTransaction();
			Query q = sess.createQuery( "from Employee e where e.id = :id" );
			q.setParameter( "id", daveId );
			Employee dave = (Employee) q.uniqueResult();
			Assert.assertNotNull( dave.getName() );
			Assert.assertNotEquals( 0, dave.getSubordinates().size() );
			if ( traverse ) {
				for ( Employee employee : dave.getSubordinates() ) {
					Assert.assertNotEquals( 0, employee.getSubordinates().size() );
				}
			}
			tx.commit();
			return dave;
		}
	}

	private Employee multipleCollectionAccess(boolean traverse) {
		Long daveId = createObjectGraph( false );
		try (Session sess = openSession();) {
			Transaction tx = sess.beginTransaction();
			Employee dave = sess.load( Employee.class, daveId );
			Assert.assertNotNull( dave.getName() );
			if ( traverse ) {
				Assert.assertNotNull( dave.getSupervisor().getName() );
				Assert.assertNotEquals( 0, dave.getSupervisor().getSubordinates().size() );
				for ( Employee employee : dave.getSubordinates() ) {
					Assert.assertNotEquals( 0, employee.getSubordinates().size() );
				}
			}
			tx.commit();
			return dave;
		}
	}

	private Employee multipleCollectionAccessCriteria(boolean traverse) {
		Long daveId = createObjectGraph( false );
		try (Session sess = openSession();) {
			Transaction tx = sess.beginTransaction();
			Criteria crit = new AutofetchCriteria( sess.createCriteria( Employee.class ) );
			crit.add( Restrictions.eq( "id", daveId ) );
			Employee john = (Employee) crit.uniqueResult();
			Assert.assertNotNull( john.getName() );
			if ( traverse ) {
				Assert.assertNotNull( john.getSupervisor().getName() );
				Assert.assertNotEquals( 0, john.getSupervisor().getSubordinates().size() );
				for ( Employee employee : john.getSubordinates() ) {
					Assert.assertNotEquals( 0, employee.getSubordinates().size() );
				}
			}
			tx.commit();
			return john;
		}
	}

	/**
	 * @param returnLeaf whether to return the leaf node (Dave) or intermediate node (John)
	 *
	 * @return Id of either Dave or John
	 */
	private Long createObjectGraph(boolean returnLeaf) {
		Session sess;
		Transaction tx = null;
		try {
			sess = openSession();
			tx = sess.beginTransaction();
			Employee root = new Employee( "Root", null, null, new Address( "", "", "" ) );
			Employee e4 = new Employee( "Ali", null, null, new Address( "104 Main St.", "Austin", "Texas" ) );
			Employee e5 = new Employee( "Ben", null, null, new Address( "105 Main St.", "Austin", "Texas" ) );
			Employee e0 = new Employee( "John", root, e5, new Address( "100 Main St.", "Austin", "Texas" ) );
			Employee e1 = new Employee( "Mike", e0, null, new Address( "101 Main St.", "Austin", "Texas" ) );
			Employee e2 = new Employee( "Sam", e0, e4, new Address( "102 Main St.", "Austin", "Texas" ) );
			Employee e3 = new Employee( "Dave", e1, e2, new Address( "103 Main St.", "Austin", "Texas" ) );
			root.addSubordinate( e0 );
			e0.addSubordinate( e1 );
			e0.addSubordinate( e2 );
			e1.addSubordinate( e3 );

			// Make some friends for e3
			for ( int i = 0; i < NUM_FRIENDS; i++ ) {
				Employee friend = new org.autofetch.test.Employee( "Friend" + i, e2, e0,
																   new Address( "100 Friend St.", "Austin", "Texas" )
				);
				e3.addFriend( friend );
			}

			sess.save( root );
			tx.commit();
			tx = null;
			if ( returnLeaf ) {
				return e3.getId();
			}
			else {
				return e0.getId();
			}
		}
		finally {
			if ( tx != null ) {
				tx.rollback();
			}
		}
	}

	private Long createNObjectGraph() {
		Session sess;
		Transaction tx = null;
		try {
			sess = openSession();
			tx = sess.beginTransaction();
			Employee grandfather = new org.autofetch.test.Employee( "Grandfather", null, null,
																	new Address( "100 Main St.", "Austin", "Texas" )
			);
			for ( int i = 0; i < NUM_SUBORDINATES; i++ ) {
				Employee childMentor = new Employee( "Mentor" + i, null, null,
													 new Address( "101 Main St.", "Austin", "Texas" )
				);
				Employee child = new Employee( "Child" + i, grandfather, childMentor,
											   new Address( "100 Main St.", "Austin", "Texas" )
				);
				grandfather.addSubordinate( child );
			}
			sess.save( grandfather );
			tx.commit();
			tx = null;
			return grandfather.getId();
		}
		finally {
			if ( tx != null ) {
				tx.rollback();
			}
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Employee.class, Address.class };
	}

	@Override
	protected void afterEntityManagerFactoryBuilt() {
		this.em = serviceRegistry().getService( AutofetchService.class ).getExtentManager();
	}

	@Override
	protected void addConfigOptions(Map options) {
		options.put( AvailableSettings.ENHANCER_ENABLE_DIRTY_TRACKING, true );
		options.put( AvailableSettings.ENHANCER_ENABLE_LAZY_INITIALIZATION, true );
		options.put( AvailableSettings.ENHANCER_ENABLE_ASSOCIATION_MANAGEMENT, true );
	}

	private Session openSession() {
		return super.createEntityManager().unwrap( Session.class );
	}
}
