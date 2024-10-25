/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package io.github.cziesman.jmxconsole.html;

import java.io.IOException;
import java.net.URLDecoder;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;

import javax.management.AttributeList;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.cziesman.jmxconsole.control.OpResultInfo;
import io.github.cziesman.jmxconsole.control.Server;
import io.github.cziesman.jmxconsole.model.DomainData;
import io.github.cziesman.jmxconsole.model.MBeanData;

/**
 * The HTML jmxconsole controller servlet.
 *
 * @author Scott.Stark@jboss.org
 * @author Dimitris.Andreadis@jboss.org
 */
public class HtmlAdaptorServlet extends HttpServlet {

    private static final String ACTION_PARAM = "action";

    private static final String FILTER_PARAM = "filter";

    private static final String DISPLAY_MBEANS_ACTION = "displayMBeans";

    private static final String INSPECT_MBEAN_ACTION = "inspectMBean";

    private static final String UPDATE_ATTRIBUTES_ACTION = "updateAttributes";

    private static final String INVOKE_OP_ACTION = "invokeOp";

    private static final String INVOKE_OP_BY_NAME_ACTION = "invokeOpByName";

    private static final Logger LOG = LoggerFactory.getLogger(HtmlAdaptorServlet.class);

    /**
     * Creates a new instance of HtmlAdaptor
     */
    public HtmlAdaptorServlet() {

    }

    public void init(ServletConfig config) throws ServletException {

        super.init(config);
    }

    public void destroy() {

    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        processRequest(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        processRequest(request, response);
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String action = request.getParameter(ACTION_PARAM);

        if (action == null) {
            action = DISPLAY_MBEANS_ACTION;
        }

        switch (action) {
        case DISPLAY_MBEANS_ACTION:
            displayMBeans(request, response);
            break;
        case INSPECT_MBEAN_ACTION:
            inspectMBean(request, response);
            break;
        case UPDATE_ATTRIBUTES_ACTION:
            updateAttributes(request, response);
            break;
        case INVOKE_OP_ACTION:
            invokeOp(request, response);
            break;
        case INVOKE_OP_BY_NAME_ACTION:
            invokeOpByName(request, response);
            break;
        }
    }

    /**
     * Display all mbeans categorized by domain
     */
    private void displayMBeans(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // get ObjectName filter from request or session context
        HttpSession session = request.getSession(false);
        String filter = request.getParameter(FILTER_PARAM);

        if (filter == null && session != null) {
            // try using previously provided filter from session context
            filter = (String) session.getAttribute(FILTER_PARAM);
        }

        if (filter != null && !filter.isEmpty()) {
            // Strip any enclosing quotes
            if (filter.charAt(0) == '"') {
                filter = filter.substring(1);
            }
            if (filter.charAt(filter.length() - 1) == '"') {
                filter = filter.substring(0, filter.length() - 2);
            }

            // be a litte it tolerant to user input
            String domain = "*";
            String props = "*,*";

            int separator = filter.indexOf(':');
            int assignment = filter.indexOf('=');

            if (separator == -1 && assignment != -1) {
                // assume properties only
                props = filter.trim();
            } else if (separator == -1 && assignment == -1) {
                // assume domain name only
                domain = filter.trim();
            } else {
                // domain and properties
                domain = filter.substring(0, separator).trim();
                props = filter.substring(separator + 1).trim();
            }

            if (domain.isEmpty()) {
                domain = "*";
            }

            if (props.isEmpty()) {
                props = "*,*";
            }
            if (props.endsWith(",")) {
                props += "*";
            }
            if (!props.endsWith(",*")) {
                props += ",*";
            }
            if (props.equals("*,*")) {
                props = "*";
            }

            filter = domain + ":" + props;

            if (filter.equals("*:*")) {
                filter = "";
            }
        } else {
            filter = "";
        }

        // Change "<" and ">" to "&lt;" and "&gt;" in filter string
        filter = translateMetaCharacters(filter);

        // update request filter and store filter in session context,
        // so it can be used when no filter has been submitted in
        // current request
        request.setAttribute(FILTER_PARAM, filter);

        if (session != null) {
            session.setAttribute(FILTER_PARAM, filter);
        }

        Iterator<DomainData> mbeans;
        try {
            mbeans = getDomainData(filter);
        } catch (Exception e) {
            request.setAttribute("filterError", e.getMessage());
            try {
                mbeans = getDomainData("");
            } catch (Exception e1) {
                throw new ServletException("Failed to get MBeans", e);
            }
        }
        request.setAttribute("mbeans", mbeans);
        RequestDispatcher rd = this.getServletContext().getRequestDispatcher("/displayMBeans.jsp");
        rd.forward(request, response);
    }

    /**
     * Display an mbeans attributes and operations
     */
    private void inspectMBean(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String name = request.getParameter("name");
        LOG.trace("inspectMBean, name=" + name);
        try {
            MBeanData data = getMBeanData(name);
            request.setAttribute("mbeanData", data);
            RequestDispatcher rd = this.getServletContext().getRequestDispatcher("/inspectMBean.jsp");
            rd.forward(request, response);
        } catch (Exception e) {
            throw new ServletException("Failed to get MBean data", e);
        }
    }

    /**
     * Update the writable attributes of an mbean
     */
    private void updateAttributes(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        final String name = request.getParameter("name");
        LOG.trace("updateAttributes, name={}", name);
        Enumeration<String> paramNames = request.getParameterNames();
        final HashMap<String, String> attributes = new HashMap<>();
        while (paramNames.hasMoreElements()) {
            String param = paramNames.nextElement();
            if (param.equals("name") || param.equals("action")) {
                continue;
            }
            String value = request.getParameter(param);
            LOG.trace("name=" + param + ", value='" + value + "'");
            // Ignore null values, these are empty write-only fields
            if (value == null || value.isEmpty()) {
                continue;
            }
            attributes.put(param, value);
        }

        try {
            AttributeList newAttributes = setAttributes(name, attributes);
            MBeanData data = getMBeanData(name);
            request.setAttribute("mbeanData", data);
            RequestDispatcher rd = this.getServletContext().getRequestDispatcher("/inspectMBean.jsp");
            rd.forward(request, response);
        } catch (Exception e) {
            throw new ServletException("Failed to update attributes", e);
        }
    }

    /**
     * Invoke an mbean operation given the index into the MBeanOperationInfo{} array of the mbean.
     */
    private void invokeOp(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String reqname = request.getParameter("name");
        final String name = URLDecoder.decode(reqname, "UTF-8");
        LOG.trace("invokeOp, name=" + name);
        final String[] args = getArgs(request);
        String methodIndex = request.getParameter("methodIndex");
        if (methodIndex == null || methodIndex.isEmpty()) {
            throw new ServletException("No methodIndex given in invokeOp form");
        }
        final int index = Integer.parseInt(methodIndex);

        try {
            OpResultInfo opResult = invokeOp(name, index, args);
            request.setAttribute("opResultInfo", opResult);
            RequestDispatcher rd = this.getServletContext().getRequestDispatcher("/displayOpResult.jsp");
            rd.forward(request, response);
        } catch (Exception e) {
            throw new ServletException("Failed to invoke operation", e);
        }
    }

    /**
     * Invoke an mbean operation given the method name and its signature.
     */
    private void invokeOpByName(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        final String name = request.getParameter("name");
        LOG.trace("invokeOpByName, name=" + name);
        final String[] argTypes = request.getParameterValues("argType");
        final String[] args = getArgs(request);
        final String methodName = request.getParameter("methodName");
        if (methodName == null) {
            throw new ServletException("No methodName given in invokeOpByName form");
        }
        try {
            OpResultInfo opResult = invokeOpByName(name, methodName, argTypes, args);
            request.setAttribute("opResultInfo", opResult);
            RequestDispatcher rd = this.getServletContext().getRequestDispatcher("/displayOpResult.jsp");
            rd.forward(request, response);
        } catch (Exception e) {
            throw new ServletException("Failed to invoke operation", e);
        }
    }

    /**
     * Extract the argN values from the request into a String[]
     */
    private String[] getArgs(HttpServletRequest request) {

        ArrayList<String> argList = new ArrayList<>();
        for (int i = 0; true; i++) {
            String name = "arg" + i;
            String value = request.getParameter(name);
            if (value == null) {
                break;
            }
            argList.add(value);
            LOG.trace(name + "=" + value);
        }
        String[] args = new String[argList.size()];
        argList.toArray(args);
        return args;
    }

    /**
     * Translate html metacharacters in filter string only '<' and '>'
     */
    private String translateMetaCharacters(String s) {

        s = s.replaceAll("<", "&lt;");
        s = s.replaceAll(">", "&gt;");
        return s;
    }

    private MBeanData getMBeanData(final String name) throws PrivilegedActionException {

        return AccessController.doPrivileged(new PrivilegedExceptionAction<MBeanData>() {

            public MBeanData run() throws Exception {

                return Server.getMBeanData(name);
            }
        });
    }

    private Iterator<DomainData> getDomainData(final String filter) throws PrivilegedActionException {

        return AccessController.doPrivileged((PrivilegedExceptionAction<Iterator<DomainData>>) () -> Server.getDomainData(filter));
    }

    private OpResultInfo invokeOp(final String name, final int index, final String[] args) throws PrivilegedActionException {

        return AccessController.doPrivileged((PrivilegedExceptionAction<OpResultInfo>) () -> Server.invokeOp(name, index, args));
    }

    private OpResultInfo invokeOpByName(final String name, final String methodName, final String[] argTypes, final String[] args) throws PrivilegedActionException {

        return AccessController.doPrivileged((PrivilegedExceptionAction<OpResultInfo>) () -> Server.invokeOpByName(name, methodName, argTypes, args));
    }

    private AttributeList setAttributes(final String name, final HashMap<String, String> attributes) throws PrivilegedActionException {

        return AccessController.doPrivileged((PrivilegedExceptionAction<AttributeList>) () -> Server.setAttributes(name, attributes));
    }

}