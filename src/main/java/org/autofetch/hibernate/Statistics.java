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
 * Class which contains statisitics about the loading of a query association
 * 
 * @author aibrahim
 */
public class Statistics implements Serializable {

    private long total;

    private long accessed;
    
    private TraversalProfile profileNode;

    /**
     * Default constructor. Initializes total and loaded to 0.
     */
    public Statistics(TraversalProfile node) {
        total = 0;
        accessed = 0;
        this.profileNode = node;
    }

    private static final long MAX_TOTAL = Long.MAX_VALUE - Integer.MAX_VALUE;

    /**
     * Increments the total amount times this association
     * has been instantiated.
     * 
     * @param amt
     */
	 
    public synchronized void incrementTotal(int amt) { // this should be possible to do more elegantly
        // Protect against overflow, a little bit of a hack because
        // we are arbitrarily discounting statistics when
        // the total grows too large.
        if (total > MAX_TOTAL) {
            total = (long) Math.ceil(total / 2.0);
            amt /= 2;
        }
        total += amt;
    }

    /**
     * Call this method when an association is accessed for the first time.
     * Should not be called twice for a specific instance of an association.
     */
    public synchronized void loadedAssociation() {
        accessed++;

        // Sanity check: number of accesses should be <= total
        if (accessed > total) {
            throw new IllegalStateException("accesses " + accessed
                    + " greater than total " + total);
        }
    }

    /**
     * @return The percentage of time the association is accessed.
     */
	 
	 
	 
	 
	 
    public double accessPercentage() {
        return (double) accessed / (double) total;
    }

    /**
     * @return Return accessed
     */
    public long getAccessed() {
        return accessed;
    }

    /**
     * @return Returns the total.
     */
    public long getTotal() {
        return total;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return accessed + " / " + total;
    }

    public TraversalProfile getProfileNode() {
        return profileNode;
    }
}
