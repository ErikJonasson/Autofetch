/*
 * Created on Jun 6, 2005
 */
package org.autofetch.test;

import org.autofetch.hibernate.AutofetchTuplizer;
import org.hibernate.annotations.Tuplizer;

import javax.persistence.Basic;
import javax.persistence.Embeddable;

/**
 * Represents an address for an employee.
 *
 * @author aibrahim
 */
@Tuplizer(impl = AutofetchTuplizer.class)
@Embeddable
public class Address {

    @Basic
    private String m_street;

    @Basic
    private String m_city;

    @Basic
    private String m_state;

    /**
     * Default constructor. Needed for Hibernate.
     */
    public Address() {
    }

    /**
     * Creates new address
     */
    public Address(String street, String city, String state) {
        m_street = street;
        m_city = city;
        m_state = state;
    }

    public String getCity() {
        return m_city;
    }

    public void setCity(String city) {
        m_city = city;
    }

    public String getState() {
        return m_state;
    }

    public void setState(String state) {
        m_state = state;
    }

    public String getStreet() {
        return m_street;
    }

    public void setStreet(String street) {
        m_street = street;
    }
}
