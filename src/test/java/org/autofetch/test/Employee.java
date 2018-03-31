/*
 * Created on Jun 6, 2005
 */
package org.autofetch.test;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

/**
 * @author aibrahim
 */
@Entity
public class Employee {

	@Id
	@Column(name = "employee_id")
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long m_id;

	@Column(name = "name")
	private String m_name;

	@JoinColumn(name = "supervisor_id")
	@ManyToOne(cascade = { CascadeType.ALL })
	private Employee m_supervisor;

	//@CollectionType(type = "org.autofetch.hibernate.AutofetchSetType")
	@JoinColumn(name = "supervisor_id")
	@OneToMany(cascade = { CascadeType.ALL }, fetch = FetchType.LAZY)
	private Set<Employee> m_subordinates;

	@Embedded
	@Basic(fetch = FetchType.LAZY)
	private Address m_address;

	@JoinColumn(name = "mentor_id")
	@ManyToOne(cascade = { CascadeType.ALL })
	private Employee m_mentor;

	//@CollectionType(type = "org.autofetch.hibernate.AutofetchSetType")
	@ManyToMany(cascade = { CascadeType.ALL })
	@JoinTable(name = "friends", joinColumns = { @JoinColumn(name = "friend_id") }, inverseJoinColumns = { @JoinColumn(name = "befriended_id") })
	private Set<Employee> m_friends;

	/**
	 * Default constructor. Needed by Hibernate
	 */
	protected Employee() {
		// Nothing
	}

	/**
	 * Creates employee with name and supervisor and no subordinates
	 *
	 * @param name should be unique and not null
	 * @param supervisor employee supervisor, null indicates no supervisor.
	 */
	public Employee(String name, Employee supervisor, Employee mentor, Address addr) {
		m_name = name;
		m_supervisor = supervisor;
		m_subordinates = new HashSet<>();
		m_friends = new HashSet<>();
		m_mentor = mentor;
		m_address = addr;
	}

	public String getName() {
		return m_name;
	}

	public void setName(String name) {
		m_name = name;
	}

	public Set<Employee> getSubordinates() {
		return m_subordinates;
	}

	public void setSubordinates(Set<Employee> subordinates) {
		m_subordinates = subordinates;
	}

	public void addSubordinate(Employee e) {
		m_subordinates.add( e );
	}

	public Employee getSupervisor() {
		return m_supervisor;
	}

	public void setSupervisor(Employee supervisor) {
		m_supervisor = supervisor;
	}

	public Long getId() {
		return m_id;
	}

	public void setId(Long id) {
		m_id = id;
	}

	public Address getAddress() {
		return m_address;
	}

	public void setAddress(Address address) {
		m_address = address;
	}

	public Employee getMentor() {
		return m_mentor;
	}

	public void setMentor(Employee mentor) {
		m_mentor = mentor;
	}

	public Set<Employee> getFriends() {
		return m_friends;
	}

	public void setFriends(Set<Employee> friends) {
		m_friends = friends;
	}

	public void addFriend(Employee friend) {
		m_friends.add( friend );
	}

	@Override
	public int hashCode() {
		return m_name.hashCode();
	}

	@Override
	public boolean equals(Object other) {
		return ( other instanceof Employee )
				&& ( (Employee) other ).m_name.equals( m_name );
	}

	@Override
	public String toString() {
		return m_name;
	}
}
