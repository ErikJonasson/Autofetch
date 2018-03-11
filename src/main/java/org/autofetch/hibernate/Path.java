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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable.
 *
 * @author Ali Ibrahim <aibrahim@cs.utexas.edu>
 */
//Used in extentmanager to
public class Path implements Serializable {

    private List<String> traversals;

    @SuppressWarnings("unchecked")
    public Path() {
        traversals = Collections.EMPTY_LIST; // Use Collections.emptyList() instead
    }

    public Path(List<String> traversals) {
        super(); //NOT NEEDED?
        this.traversals = traversals;
    }

    public Path addTraversal(String traversal) {
        List<String> newTraversals = new ArrayList<>(traversals);
        newTraversals.add(traversal);
        return new Path(newTraversals);
    }

    public Path prependTraversal(String traversal) {
        List<String> newTraversals = new ArrayList<>(traversals.size() + 1);
        newTraversals.add(traversal);
        newTraversals.addAll(traversals);
        return new Path(newTraversals);
    }

    public boolean isEmpty() {
        return traversals.isEmpty();
    }

    public int size() {
        return traversals.size();
    }

    public List<String> traversals() {
        return Collections.unmodifiableList(traversals); // Does this exist anymore?
    }

    public Path removeLastTraversal() {
        if (isEmpty()) {
            throw new IllegalStateException("Empty path.");
        }

        return new Path(traversals.subList(0, traversals.size() - 1));
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(traversals.get(0));
        for (String traversal : traversals.subList(1, traversals.size())) {
            sb.append('.');
            sb.append(traversal);
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Path) {
            Path p = (Path) o;
            return p.traversals.equals(this.traversals);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return traversals.hashCode();
    }
}
