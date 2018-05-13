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

import java.io.Serializable;

/**
 * Immutable.
 *
 * @author Ali Ibrahim <aibrahim@cs.utexas.edu>
 */
public class Property implements Serializable {

    private final String name;

    private final boolean collection;

    public Property(org.hibernate.mapping.Property property) {
        if (property == null) {
            throw new NullPointerException("property name cannot be null");
        }

        this.name = property.getName();
        this.collection = property.getType().isCollectionType();
    }

    public boolean isCollection() {
        return collection;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append(this.name);
        if (this.collection) {
            str.append("<C>");
        }

        return str.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Property property = (Property) o;
        if (isCollection() != property.isCollection()) {
            return false;
        }

        return getName().equals(property.getName());
    }

    @Override
    public int hashCode() {
        int result = getName().hashCode();
        result = 31 * result + (isCollection() ? 1 : 0);
        return result;
    }
}