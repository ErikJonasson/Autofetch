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

import java.util.Set;

public interface Trackable {

    /**
     * @return old tracking status
     */
    boolean enableTracking();

    /**
     * @return old tracking status
     */
    boolean disableTracking();
    
    void removeTracker(Statistics tracker);
    
    void addTracker(Statistics tracker);
    
    void addTrackers(Set<Statistics> tracker);
    
    boolean isAccessed();
}
