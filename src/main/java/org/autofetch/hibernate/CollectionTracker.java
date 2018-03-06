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
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class CollectionTracker implements Serializable {

    private Set<Statistics> trackers = new HashSet<Statistics>();

    private boolean accessed = false;

    private boolean tracking = true;
	
	
	/* 
	If this gets called when tracking is activated, set accessed and increment accessed in each of the trackers. For each trackable entity, call extendProfile
	Used in AutofetchSet, AutofetchBag, AutofetchIdBag, AutofetchList
	*/
    public void trackAccess(Collection collection) {
        if (!accessed && tracking) {
            accessed = true;
            for (Statistics stats : trackers) {
                stats.loadedAssociation();
            }
            for (Object elem : collection) {
                if (elem instanceof TrackableEntity) {
                    TrackableEntity entity = (TrackableEntity) elem;
                    for (Statistics stats : trackers) {
                        entity.extendProfile(stats);
                    }
                }
            }
        }
    }

    public Set<Statistics> getTrackers() {
        return trackers;
    }

    public void addTracker(Statistics newTracker) {
        trackers.add(newTracker);
    }

    public void addTrackers(Set<Statistics> newTrackers) {
        trackers.addAll(newTrackers);
    }

    public boolean isTracking() {
        return tracking;
    }

    public void setTracking(boolean tracking) {
        this.tracking = tracking;
    }

    public void removeTracker(Statistics tracker) {
        trackers.remove(tracker);
    }

    public boolean isAccessed() {
        return accessed;
    }
}
