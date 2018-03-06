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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A node in a traversal profile.
 * 
 * @author aibrahim
 */
public class TraversalProfile implements Serializable {

    public static class TraversalProfileLink implements Serializable {

        protected TraversalProfile profile;

        protected Statistics stats;

        protected boolean collection;

        public TraversalProfileLink(TraversalProfile profile, boolean collection) {
            this.profile = profile;
            this.stats = new Statistics(profile);
            this.collection = collection;
        }
    }

    // Map from field name to traversal profile link
    private Map<String, TraversalProfileLink> subProfiles;

    // Parent extent, null if top level
    private TraversalProfile parent;

    private int level;

    /**
     * Default constructor. Constructs a top level traversal profile node.
     */
    public TraversalProfile() {
        this(null);
    }

    /**
     * Constructor.
     * 
     * @param parent
     *            parent extent, should be null if this is the top level extent
     */
    public TraversalProfile(TraversalProfile parent) {
        this.parent = parent;
        this.subProfiles = new HashMap<String, TraversalProfileLink>();
        if (parent == null) {
            level = 0;
        } else {
            level = parent.getLevel() + 1;
        }
    }

    public boolean hasSubProfile(String assoc) {
        return subProfiles.containsKey(assoc);
    }

    public Set<String> getAssociations() {
        return subProfiles.keySet();
    }
    
    /**
     * @param assoc
     * @return traversal profile
     */
    public TraversalProfile getSubProfile(String assoc) {
        return subProfiles.get(assoc).profile;
    }

    /**
     * @param assoc
     * @return traversal profile
     */
    public Statistics getSubProfileStats(String assoc) {
        return subProfiles.get(assoc).stats;
    }

    /**
     * @param assoc
     * @return traversal profile
     */
    public boolean isCollectionAssociation(String assoc) {
        return subProfiles.get(assoc).collection;
    }

    /**
     * Add a sub-profile
     * 
     * @param assoc
     *            association name
     * @param collection
     *            whether the association is a collection association
     * @return The newly created sub-profile
     * @throws MaxDepthExceededException
     *             sub-profile cannot be created because it would be too deep.
     */
	 
	 //Adds a new entry to the map with the assoc as key and a TraversalProfileLink as value
    protected TraversalProfile addSubProfile(String assoc, boolean collection) {
        TraversalProfile subProfile = new TraversalProfile(this);
        subProfiles
                .put(assoc, new TraversalProfileLink(subProfile, collection));
        return subProfile;
    }

    /**
     * @return whether there are any sub-extents
     */
    public boolean isEmpty() {
        return subProfiles.isEmpty();
    }

    /**
     * @return number of sub-extents
     */
    public int numSubProfiles() {
        return subProfiles.size();
    }

    /**
     * @return parent extent, null if this is a top-level extent
     */
    public TraversalProfile getParent() {
        return parent;
    }

    protected TraversalProfile getRoot() {
        if (getParent() != null) {
            return getParent().getRoot();
        } else {
            return this;
        }
    }

    /**
     * Returns whether this traversal profile is a top level 
     * traversal profile.
     * 
     * @return whether this traversal profile has no parent.
     */
    public boolean isTopLevel() {
        return parent == null;
    }

    /**
     * @return Return the level of the extent.
     */
    public int getLevel() {
        return level;
    }

    public String toString() {
        return toString(0);
    }

    /**
     * @return String representation of the aggregate extent.
     */
    public String toString(int indent) {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, TraversalProfileLink> entry : subProfiles
                .entrySet()) {
            pad(result, indent);
            result.append("- ");
            result.append(entry.getKey());
            if (entry.getValue().collection) {
                result.append("(C)");
            }
            result.append(" : ");
            result.append(entry.getValue().stats);
            result.append('\n');
            result.append(entry.getValue().profile.toString(indent + 2));
        }
        return result.toString();
    }

    private static void pad(StringBuilder sb, int numChars) {
        for (int i = 0; i < numChars; i++) {
            sb.append(' ');
        }
    }
}
