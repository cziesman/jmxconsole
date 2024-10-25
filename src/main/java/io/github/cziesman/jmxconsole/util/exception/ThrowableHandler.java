
/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015, Red Hat, Inc., and individual contributors as indicated
 * by the @authors tag.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.cziesman.jmxconsole.util.exception;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A static singleton that handles processing throwables that otherwise would
 * be ignored or dumped to System.err.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @version <tt>$Revision$</tt>
 */
public final class ThrowableHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ThrowableHandler.class);

    /**
     * The list of listeners
     */
    private static final List<ThrowableListener> listeners = Collections.synchronizedList(new ArrayList<ThrowableListener>());

    /////////////////////////////////////////////////////////////////////////
    //                            Listener Methods                         //
    /////////////////////////////////////////////////////////////////////////

    /**
     * Add a ThrowableListener to the listener list.  Listener is added only
     * if if it is not already in the list.
     *
     * @param listener ThrowableListener to add to the list.
     */
    public static void addThrowableListener(ThrowableListener listener) {
        // only add the listener if it isn't already in the list
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Remove a ThrowableListener from the listener list.
     *
     * @param listener ThrowableListener to remove from the list.
     */
    public static void removeThrowableListener(ThrowableListener listener) {

        listeners.remove(listener);
    }

    /**
     * Fire onThrowable to all registered listeners.
     *
     * @param type The type off the throwable.
     * @param t    Throwable
     */
    private static void fireOnThrowable(int type, Throwable t) {

        Object[] list = listeners.toArray();

        for (int i = 0; i < list.length; i++) {
            ((ThrowableListener) list[i]).onThrowable(type, t);
        }
    }

    /**
     * Add a throwable that is to be handled.
     *
     * @param type The type off the throwable.
     * @param t    Throwable to be handled.
     */
    public static void add(int type, Throwable t) {
        // don't add null throwables
        if (t == null) {
            return;
        }

        try {
            fireOnThrowable(type, t);
        } catch (Throwable bad) {
            // don't let these propagate, that could introduce unwanted side-effects
            LOG.error("Unable to handle throwable: " + t + " because of:", bad);
        }
    }

    /////////////////////////////////////////////////////////////////////////
    //                          Throwable Processing                       //
    /////////////////////////////////////////////////////////////////////////

    /**
     * Add a throwable that is to be handled with unknown type.
     *
     * @param t Throwable to be handled.
     */
    public static void add(Throwable t) {

        add(Type.UNKNOWN, t);
    }

    /**
     * Add a throwable that is to be handled with error type.
     *
     * @param t Throwable to be handled.
     */
    public static void addError(Throwable t) {

        add(Type.ERROR, t);
    }

    /**
     * Add a throwable that is to be handled with warning type.
     *
     * @param t Throwable to be handled.
     */
    public static void addWarning(Throwable t) {

        add(Type.ERROR, t);
    }

    /**
     * Container for throwable types.
     */
    public interface Type {

        /**
         * Unknown throwable.
         */
        int UNKNOWN = 0;

        /**
         * Error throwable.
         */
        int ERROR = 1;

        /**
         * Warning throwable.
         */
        int WARNING = 2;

    }

}
