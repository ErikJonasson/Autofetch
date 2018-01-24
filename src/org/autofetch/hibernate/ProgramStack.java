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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Represents the program state based on which we make decisions about which
 * extent to use with a query. This class is immutable.
 * 
 * @author aibrahim
 */
public class ProgramStack implements Serializable {

    private static final int MAX_STACK_FRAMES = 20;

    private List<StackTraceElement> m_stacktrace;

    /**
     * Builds program state based on the stack trace it is given. The maximum
     * number of stack frames we will keep track of is given by
     * MAX_STACK_FRAMES. If there are more than the maximum number of stack
     * frames we take only the first MAX_STACK_FRAMES from the stack trace.
     * 
     * @param stacktrace
     */
    public ProgramStack(StackTraceElement[] stacktrace) {
        if (stacktrace == null) {
            throw new IllegalArgumentException("Stacktrace cannot be null");
        }
        if (stacktrace.length == 0) {
            throw new IllegalArgumentException(
                    "Stracktrace must have at least one element");
        }

        m_stacktrace = new ArrayList<StackTraceElement>(
                Arrays.asList(stacktrace));
        
        // Filter out hibernate, autofetch, and native stack frames
        Iterator<StackTraceElement> stIter = m_stacktrace.iterator();
        while (stIter.hasNext()) {
            StackTraceElement ste = stIter.next();
            boolean hibernateCoreMethod = 
                ste.getClassName().startsWith("org.hibernate.") &&
                    !ste.getClassName().startsWith("org.hibernate.test.");
            boolean autofetchCoreMethod = 
                ste.getClassName().startsWith("org.autofetch.") &&
                    !ste.getClassName().startsWith("org.autofetch.test.");
            if (hibernateCoreMethod || autofetchCoreMethod ||
                    ste.isNativeMethod()) {
                stIter.remove(); // Why delete native method?
            }
        }
        
        // Truncate stacktrace to max stack frames
        if (m_stacktrace.size() > MAX_STACK_FRAMES) {
            m_stacktrace = m_stacktrace.subList(0, MAX_STACK_FRAMES);
        }
    }

    /**
     * Default constructor. Builds program state based on where it is
     * instantiated.
     */
    public ProgramStack() {
        this(Thread.currentThread().getStackTrace());
    }

    /**
     * Creates string representation of program state.
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
       return m_stacktrace.toString();
    }

    /**
     * Returns whether this object is equal to another.
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        return (o instanceof ProgramStack)
                && (((ProgramStack)o).m_stacktrace.equals(this.m_stacktrace));
    }

    /**
     * Returns hashCode.
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return m_stacktrace.hashCode();
    }
}
