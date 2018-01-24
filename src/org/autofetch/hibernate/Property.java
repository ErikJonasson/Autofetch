/**
 * Copyright 2008 Ali Ibrahim
 * 
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
package org.autofetch.hibernate;

import java.io.Serializable;

/**
 * Immutable.
 * 
 * @author Ali Ibrahim <aibrahim@cs.utexas.edu>
 * 
 */
 
 //Used in EntityTracker, EntityProxy-classes, describes the persistent entities.
public class Property implements Serializable {

    private String name;

    private boolean collection;

    public Property(String name, boolean collection) {
        super();
        if (name == null) {
            throw new NullPointerException("property name cannot be null");
        }
        this.name = name;
        this.collection = collection;
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
        str.append(name);
        if (collection) {
            str.append("<C>");
        }
        return str.toString();
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (collection ? 1231 : 1237);
        result = PRIME * result + name.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj instanceof Property) {
            final Property other = (Property) obj;
            return collection == other.collection
                    && name.equals(other.name);
        } else {
            return false;
        }
    }
}
