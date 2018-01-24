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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class manages extents for queries.
 * 
 * @author aibrahim
 */
public class ExtentManager implements Serializable {

    private static final Log log = LogFactory.getLog(ExtentManager.class);
    
    // Map from QueryProgramStatePair to QueryExtent
    private Map<QueryProgramStatePair, TraversalProfile> tpMap =
        new LinkedHashMap<QueryProgramStatePair, TraversalProfile>();

    // Enables prefetching
    private boolean prefetch = true;
    
    // Threshold frequency for prefetching associations
    private static final double DEFAULT_FETCH_PARAM = 0.4;
    
    private double fetchParam = DEFAULT_FETCH_PARAM;   
    
    private static final int DEFAULT_MAX_PREFETCH_DEPTH = 5;
    
    private int maxPrefetchDepth = DEFAULT_MAX_PREFETCH_DEPTH; 
    
	// Check if maxPrefetch depth is being exceeded, if not create a subprofile for that parentnode. (call to TP)
    public boolean addSubProfile(TraversalProfile parentNode,
            String assoc,
            boolean collection) {
        if (parentNode.getLevel() >= maxPrefetchDepth) {
            return false;
        } else {
            parentNode.addSubProfile(assoc, collection);
            return true;
        }
    }
    //Mark as root based on queryId, if the entity is trackable, and if it was already accessed call extendProfile, if not just add the tracker to that entity.
    public void markAsRoot(Object o, String queryId) {
        TraversalProfile tp = getTraversalProfile(queryId);
        if (o instanceof TrackableEntity) {
            Statistics stats = new Statistics(tp);
            TrackableEntity te = (TrackableEntity) o;
            stats.incrementTotal(1);
            // Root element might have been accessed already for example
            // if it was in the session cache or an element of a collection.
            // If so, then propagate the tracking information to its fields.
            if (te.isAccessed()) {
                stats.loadedAssociation();
                te.extendProfile(stats);
            } else {
                te.addTracker(stats);
            }
        }
    }
    
    public void setPrefetch(boolean fetch) {
        prefetch = fetch;
    }

    public int getMaxPrefetchDepth() {
        return maxPrefetchDepth;
    }

    public void setMaxPrefetchDepth(int maxPrefetchDepth) {
        this.maxPrefetchDepth = maxPrefetchDepth;
    }

    public double getFetchParam() {
        return fetchParam;
    }

    public void setFetchParam(double fetchParam) {
        this.fetchParam = fetchParam;
    }

    /**
     * This method purges all the extent information
     */
    public void clearExtentInformation() {
        if (log.isDebugEnabled()) {
            log.debug("Clearing extent information.");
        }
        tpMap.clear();
    }

    /**
     * Returns query extent for a given query and program state. If none exists,
     * it creates a new one and returns it.
     * 
     * @param queryId
     * @return The traversal profiel for the given queryId and this program point.
     */
    protected synchronized TraversalProfile getTraversalProfile(
            String queryId) {
        ProgramStack state = new ProgramStack();
        QueryProgramStatePair key = new QueryProgramStatePair(queryId,
                state);
        TraversalProfile tp = tpMap.get(key);
        if (tp == null) {
            tp = new TraversalProfile();
            tpMap.put(key, tp);
        }
        return tp;
    }

    /**
     * Returns list of prefetch paths. List may be empty.
     * 
     * @param queryId
     * @return List of paths to prefetch for a given query identifier
     * and program point.
     */
    public List<Path> getPrefetchPaths(String queryId) {
        TraversalProfile tp = getTraversalProfile(queryId);
        List<Path> paths = new ArrayList<Path>();
        if ((tp == null) || !prefetch) {
            return paths;
        }

       getPrefetchPaths(tp, new Path(), paths, 1.0, false);
       return paths;
    }
    
    private void getPrefetchPaths(TraversalProfile tp,
            Path prefix,
            List<Path> paths,
            double parentProbability,
            boolean collAssocBanned) {
		//What is prefix in this case?
        if (prefix.size() > maxPrefetchDepth) {
            return;
        }
        
        for (String assoc : tp.getAssociations()) {

            boolean collection = tp.isCollectionAssociation(assoc);
            if (collAssocBanned && collection) {
                continue;
            }
            
            long accessed = tp.getSubProfileStats(assoc).getAccessed();
            long total = tp.getSubProfileStats(assoc).getTotal();
            
            double localAccessPercentage =
                (double)accessed / (double)total;
            double accessPercentage = localAccessPercentage * parentProbability; 
            if (accessPercentage > fetchParam) {					//This part is vague, what happens if it is an collection? When is it "banned"?
                boolean wasCollAssocBanned = collAssocBanned;
                collAssocBanned = collAssocBanned || collection;
                Path newPath = prefix.addTraversal(assoc);
                paths.add(newPath);
                TraversalProfile subProfile = tp.getSubProfile(assoc);
                getPrefetchPaths(subProfile, newPath, paths, accessPercentage,
                        wasCollAssocBanned);
            }
        }
    }
    
    /**
     * Returns string representation of data in ExtentManager.
     * 
     * @return representation of data in ExtentManager
     */
    public String printable() {
        StringBuilder result = new StringBuilder();
        Iterator extentsIter = tpMap.entrySet().iterator();
        while (extentsIter.hasNext()) {
            Map.Entry entry = (Map.Entry) extentsIter.next();
            result.append(entry.getKey() + "\r\n--> " + entry.getValue()
                    + "\r\n\r\n");
        }

        return result.toString();
    }

    /**
     * @return a set of Map.Entry of QueryProgramState and extents
     */
    public Set<Map.Entry<QueryProgramStatePair, TraversalProfile>> getExtentEntries() {
        return tpMap.entrySet();
    }
    
    /**
     * This is a utility class used as the key to retrieve query extents.
     * Immutable class.
     * 
     * @author aibrahim
     */
    public static class QueryProgramStatePair implements Serializable {

        private String queryRootClasses;

        private ProgramStack state;

        /**
         * Constructor
         * 
         * @param qrc query identifier
         * @param state program state
         */
        public QueryProgramStatePair(String qrc, ProgramStack state) {
            this.queryRootClasses = qrc;
            this.state = state;
        }

        /**
         * @see java.lang.Object#hashCode()
         */
        public int hashCode() {
            return 37 * queryRootClasses.hashCode() + state.hashCode();
        }

        /**
         * @see java.lang.Object#equals(java.lang.Object)
         */
        public boolean equals(Object o) {
            if (!(o instanceof QueryProgramStatePair)) {
                return false;
            }
            QueryProgramStatePair other = (QueryProgramStatePair) o;
            return queryRootClasses.equals(other.queryRootClasses)
                    && state.equals(other.state);
        }

        /**
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return "(" + queryRootClasses + " , " + state + ")";
        }

        public String getQueryRootClasses() {
            return queryRootClasses;
        }

        public ProgramStack getState() {
            return state;
        }
    }
}