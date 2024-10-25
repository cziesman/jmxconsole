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

/**
 * A common superclass for <tt>RuntimeException</tt> classes that can
 * contain a nested <tt>Throwable</tt> detail object.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @version <tt>$Revision$</tt>
 */
public class NestedRuntimeException extends RuntimeException implements NestedThrowable {

    /**
     * The serialVersionUID
     */
    private static final long serialVersionUID = 9017248167673450852L;

    /**
     * The nested throwable
     */
    protected final Throwable nested;

    /**
     * Construct a <tt>NestedRuntimeException</tt> with the specified
     * detail message.
     *
     * @param msg Detail message.
     */
    public NestedRuntimeException(final String msg) {

        super(msg);
        this.nested = null;
    }

    /**
     * Construct a <tt>NestedRuntimeException</tt> with the specified
     * detail message and nested <tt>Throwable</tt>.
     *
     * @param msg    Detail message.
     * @param nested Nested <tt>Throwable</tt>.
     */
    public NestedRuntimeException(final String msg, final Throwable nested) {

        super(msg);
        this.nested = nested;
        NestedThrowable.Util.checkNested(this, nested);
    }

    /**
     * Construct a <tt>NestedRuntimeException</tt> with the specified
     * nested <tt>Throwable</tt>.
     *
     * @param nested Nested <tt>Throwable</tt>.
     */
    public NestedRuntimeException(final Throwable nested) {

        this(nested.getMessage(), nested);
    }

    /**
     * Construct a <tt>NestedRuntimeException</tt> with no detail.
     */
    public NestedRuntimeException() {

        super();
        this.nested = null;
    }

    /**
     * Return the nested <tt>Throwable</tt>.
     *
     * @return Nested <tt>Throwable</tt>.
     */
    public Throwable getNested() {

        return nested;
    }

    /**
     * Return the nested <tt>Throwable</tt>.
     *
     * <p>For JDK 1.4 compatibility.
     *
     * @return Nested <tt>Throwable</tt>.
     */
    public Throwable getCause() {

        return nested;
    }

    /**
     * Returns the composite throwable message.
     *
     * @return The composite throwable message.
     */
    public String getMessage() {

        return NestedThrowable.Util.getMessage(super.getMessage(), nested);
    }

    /**
     * Prints the composite message and the embedded stack trace to the
     * specified print stream.
     *
     * @param stream Stream to print to.
     */
    public void printStackTrace(final PrintStream stream) {

        if (nested == null || NestedThrowable.PARENT_TRACE_ENABLED) {
            super.printStackTrace(stream);
        }
        NestedThrowable.Util.print(nested, stream);
    }

    /**
     * Prints the composite message and the embedded stack trace to the
     * specified print writer.
     *
     * @param writer Writer to print to.
     */
    public void printStackTrace(final PrintWriter writer) {

        if (nested == null || NestedThrowable.PARENT_TRACE_ENABLED) {
            super.printStackTrace(writer);
        }
        NestedThrowable.Util.print(nested, writer);
    }

    /**
     * Prints the composite message and the embedded stack trace to
     * <tt>System.err</tt>.
     */
    public void printStackTrace() {

        printStackTrace(System.err);
    }

}
