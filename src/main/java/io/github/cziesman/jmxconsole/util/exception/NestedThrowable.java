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

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interface which is implemented by all the nested throwable flavors.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @version <tt>$Revision$</tt>
 */
public interface NestedThrowable extends Serializable {

    /**
     * A system wide flag to enable or disable printing of the
     * parent throwable traces.
     *
     * <p>
     * This value is set from the system property
     * <tt>org.jboss.util.NestedThrowable.parentTraceEnabled</tt>
     * or if that is not set defaults to <tt>true</tt>.
     */
    boolean PARENT_TRACE_ENABLED = Util.getBoolean("parentTraceEnabled", true);

    /**
     * A system-wide flag to enable or disable printing of the
     * nested detail throwable traces.
     *
     * <p>
     * This value is set from the system property
     * <tt>org.jboss.util.NestedThrowable.nestedTraceEnabled</tt>
     * or if that is not set defaults to <tt>true</tt> unless
     * using JDK 1.4 with {@link #PARENT_TRACE_ENABLED} set to false,
     * then <tt>false</tt> since there is a native mechansim for this there.
     *
     * <p>
     * Note then when running under 1.4 is is not possible to disable
     * the nested trace output, since that is handled by java.lang.Throwable
     * which we delegate the parent printing to.
     */
    boolean NESTED_TRACE_ENABLED = Util.getBoolean("nestedTraceEnabled", true);

    /**
     * A system wide flag to enable or disable checking of parent and child
     * types to detect uneeded nesting
     *
     * <p>
     * This value is set from the system property
     * <tt>org.jboss.util.NestedThrowable.detectDuplicateNesting</tt>
     * or if that is not set defaults to <tt>true</tt>.
     */
    boolean DETECT_DUPLICATE_NESTING = Util.getBoolean("detectDuplicateNesting", true);

    /**
     * Return the nested throwable.
     *
     * @return Nested throwable.
     */
    Throwable getNested();

    /**
     * Return the nested <tt>Throwable</tt>.
     *
     * <p>For JDK 1.4 compatibility.
     *
     * @return Nested <tt>Throwable</tt>.
     */
    Throwable getCause();

    /////////////////////////////////////////////////////////////////////////
    //                      Nested Throwable Utilities                     //
    /////////////////////////////////////////////////////////////////////////

    /**
     * Utilitiy methods for the various flavors of
     * <code>NestedThrowable</code>.
     */
    final class Util {

        // Can not be final due to init bug, see getLogger() for details
        // This variable should not be accessed directly so the getLogger method
        // will be able to check it is not null.
        private static Logger pvtLog = null;

        /**
         * Something is very broken with class nesting, which can sometimes
         * leave log uninitialized durring one of the following method calls.
         *
         * <p>
         * This is a HACK to keep those methods from NPE until this problem
         * can be resolved.
         */
        private static Logger getLogger() {

            if (pvtLog == null) {
                pvtLog = LoggerFactory.getLogger(NestedThrowable.class);
            }

            return pvtLog;
        }

        /**
         * A helper to get a boolean property.
         */
        private static boolean getBoolean(String name, boolean defaultValue) {

            name = NestedThrowable.class.getName() + "." + name;
            String value = System.getProperty(name, String.valueOf(defaultValue));

            // HACK see getLogger() for details
            Logger log = getLogger();

            log.debug(name + "=" + value);

            return Boolean.parseBoolean(value);
        }

        /**
         * Check and possibly warn if the nested exception type is the same
         * as the parent type (duplicate nesting).
         */
        public static void checkNested(final NestedThrowable parent,
                                       final Throwable child) {

            if (!DETECT_DUPLICATE_NESTING || parent == null || child == null) {
                return;
            }

            Class<?> parentType = parent.getClass();
            Class<?> childType = child.getClass();

            //
            // This might be backwards... I always get this confused
            //

            if (parentType.isAssignableFrom(childType)) {
                // HACK see getLogger() for details
                Logger log = getLogger();

                log.warn("Duplicate throwable nesting of same base type: " +
                        parentType + " is assignable from: " + childType);
            }
        }

        /**
         * Returns a formated message for the given detail message
         * and nested <code>Throwable</code>.
         *
         * @param msg    Detail message.
         * @param nested Nested <code>Throwable</code>.
         * @return Formatted message.
         */
        public static String getMessage(final String msg,
                                        final Throwable nested) {

            StringBuilder buff = new StringBuilder(msg == null ? "" : msg);

            if (nested != null) {
                buff.append(msg == null ? "- " : "; - ")
                        .append("nested throwable: (")
                        .append(nested)
                        .append(")");
            }

            return buff.toString();
        }

        /**
         * Prints the nested <code>Throwable</code> to the given stream.
         *
         * @param nested Nested <code>Throwable</code>.
         * @param stream Stream to print to.
         */
        public static void print(final Throwable nested,
                                 final PrintStream stream) {

            if (stream == null) {
                throw new NullArgumentException("stream");
            }

            if (NestedThrowable.NESTED_TRACE_ENABLED && nested != null) {
                synchronized (stream) {
                    if (NestedThrowable.PARENT_TRACE_ENABLED) {
                        stream.print(" + nested throwable: ");
                    } else {
                        stream.print("[ parent trace omitted ]: ");
                    }

                    nested.printStackTrace(stream);
                }
            }
        }

        /**
         * Prints the nested <code>Throwable</code> to the given writer.
         *
         * @param nested Nested <code>Throwable</code>.
         * @param writer Writer to print to.
         */
        public static void print(final Throwable nested,
                                 final PrintWriter writer) {

            if (writer == null) {
                throw new NullArgumentException("writer");
            }

            if (NestedThrowable.NESTED_TRACE_ENABLED && nested != null) {
                synchronized (writer) {
                    if (NestedThrowable.PARENT_TRACE_ENABLED) {
                        writer.print(" + nested throwable: ");
                    } else {
                        writer.print("[ parent trace omitted ]: ");
                    }

                    nested.printStackTrace(writer);
                }
            }
        }

    }

}
