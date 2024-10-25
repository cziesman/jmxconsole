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
package io.github.cziesman.jmxconsole.control;

import java.beans.IntrospectionException;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.cziesman.jmxconsole.model.DomainData;
import io.github.cziesman.jmxconsole.model.MBeanData;
import io.github.cziesman.jmxconsole.util.Classes;
import io.github.cziesman.jmxconsole.util.editor.PropertyEditors;

/**
 * Utility methods related to the MBeanServer interface
 *
 * @author Scott.Stark@jboss.org
 * @author Dimitris.Andreadis@jboss.org
 */
public class Server {

    private static final Logger LOG = LoggerFactory.getLogger(Server.class);

    private static final Collection<String> omittedDomains = Collections.singletonList("jboss.jsr77");

    public static MBeanServer getMBeanServer() {
        // TODO - could be considered JBoss API?
        // return org.jboss.mx.util.MBeanServerLocator.locateJBoss();
        return ManagementFactory.getPlatformMBeanServer();
    }

    public static Iterator<DomainData> getDomainData(String filter) throws JMException {

        MBeanServer server = getMBeanServer();
        TreeMap<String, DomainData> domainData = new TreeMap<>();
        if (server != null) {
            ObjectName filterName = null;
            if (filter != null) {
                filterName = new ObjectName(filter);
            }
            Set<ObjectName> objectNames = server.queryNames(filterName, null);
            Iterator<ObjectName> objectNamesIter = objectNames.iterator();
            while (objectNamesIter.hasNext()) {
                ObjectName name = objectNamesIter.next();
                if (omittedDomains.contains(name.getDomain())) {
                    continue;
                }
                try {
                    MBeanInfo info = server.getMBeanInfo(name);
                    String domainName = name.getDomain();
                    MBeanData mbeanData = new MBeanData(name, info);
                    DomainData data = domainData.get(domainName);
                    if (data == null) {
                        data = new DomainData(domainName);
                        domainData.put(domainName, data);
                    }
                    data.addData(mbeanData);
                } catch (InstanceNotFoundException ex) {
                    LOG.error(ex.getMessage());
                }
            }
        }

        return domainData.values().iterator();
    }

    public static MBeanData getMBeanData(String name) throws JMException {

        MBeanServer server = getMBeanServer();
        ObjectName objName = new ObjectName(name);
        MBeanInfo info = server.getMBeanInfo(objName);

        return new MBeanData(objName, info);
    }

    public static Object getMBeanAttributeObject(String name, String attrName) throws JMException {

        MBeanServer server = getMBeanServer();
        ObjectName objName = new ObjectName(name);

        return server.getAttribute(objName, attrName);
    }

    public static String getMBeanAttribute(String name, String attrName) throws JMException {

        MBeanServer server = getMBeanServer();
        ObjectName objName = new ObjectName(name);
        String value = null;
        try {
            Object attr = server.getAttribute(objName, attrName);
            if (attr != null) {
                value = attr.toString();
            }
        } catch (JMException e) {
            value = e.getMessage();
        }
        return value;
    }

    public static AttrResultInfo getMBeanAttributeResultInfo(String name, MBeanAttributeInfo attrInfo) throws JMException {

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        MBeanServer server = getMBeanServer();
        ObjectName objName = new ObjectName(name);
        String attrName = attrInfo.getName();
        String attrType = attrInfo.getType();
        Object value = null;
        Throwable throwable = null;
        if (attrInfo.isReadable()) {
            try {
                value = server.getAttribute(objName, attrName);
            } catch (Throwable t) {
                throwable = t;
            }
        }
        Class<?> typeClass = null;
        try {
            typeClass = Classes.getPrimitiveTypeForName(attrType);
            if (typeClass == null) {
                typeClass = loader.loadClass(attrType);
            }
        } catch (ClassNotFoundException ignore) {
        }
        PropertyEditor editor = null;
        if (typeClass != null) {
            editor = PropertyEditorManager.findEditor(typeClass);
        }

        return new AttrResultInfo(attrName, editor, value, throwable);
    }

    public static AttributeList setAttributes(String name, HashMap<String, String> attributes) throws JMException {

        MBeanServer server = getMBeanServer();
        ObjectName objName = new ObjectName(name);
        MBeanInfo info = server.getMBeanInfo(objName);
        MBeanAttributeInfo[] attributesInfo = info.getAttributes();
        AttributeList newAttributes = new AttributeList();
        for (int a = 0; a < attributesInfo.length; a++) {
            MBeanAttributeInfo attrInfo = attributesInfo[a];
            String attrName = attrInfo.getName();
            if (!attributes.containsKey(attrName)) {
                continue;
            }
            String value = attributes.get(attrName);
            if (value.equals("null") && server.getAttribute(objName, attrName) == null) {
                LOG.trace("ignoring 'null' for " + attrName);
                continue;
            }
            String attrType = attrInfo.getType();
            Attribute attr = null;
            try {
                Object realValue = PropertyEditors.convertValue(value, attrType);
                attr = new Attribute(attrName, realValue);
            } catch (ClassNotFoundException e) {
                String s = (attr != null) ? attr.getName() : attrType;
                LOG.trace("Failed to load class for attribute: " + s, e);
                throw new ReflectionException(e, "Failed to load class for attribute: " + s);
            } catch (IntrospectionException e) {
                LOG.trace("Skipped setting attribute: {}, cannot find PropertyEditor for type: {}", attrName, attrType);
                continue;
            }

            server.setAttribute(objName, attr);
            newAttributes.add(attr);
        }
        return newAttributes;
    }

    public static OpResultInfo invokeOp(String name, int index, String[] args) throws JMException {

        MBeanServer server = getMBeanServer();
        ObjectName objName = new ObjectName(name);
        MBeanInfo info = server.getMBeanInfo(objName);
        MBeanOperationInfo[] opInfo = info.getOperations();
        MBeanOperationInfo op = opInfo[index];
        MBeanParameterInfo[] paramInfo = op.getSignature();
        String[] argTypes = new String[paramInfo.length];
        for (int p = 0; p < paramInfo.length; p++) {
            argTypes[p] = paramInfo[p].getType();
        }
        return invokeOpByName(name, op.getName(), argTypes, args);
    }

    public static OpResultInfo invokeOpByName(String name, String opName, String[] argTypes, String[] args) throws JMException {

        MBeanServer server = getMBeanServer();
        ObjectName objName = new ObjectName(name);
        int length = argTypes != null ? argTypes.length : 0;
        Object[] typedArgs = new Object[length];
        for (int p = 0; p < typedArgs.length; p++) {
            String arg = args[p];
            try {
                Object argValue = PropertyEditors.convertValue(arg, argTypes[p]);
                typedArgs[p] = argValue;
            } catch (ClassNotFoundException e) {
                LOG.trace("Failed to load class for arg" + p, e);
                throw new ReflectionException(e, "Failed to load class for arg" + p);
            } catch (java.beans.IntrospectionException e) {
                // If the type is not java.lang.Object throw an exception
                if (!argTypes[p].equals("java.lang.Object")) {
                    throw new javax.management.IntrospectionException(
                            "Failed to find PropertyEditor for type: " + argTypes[p]);
                }
                // Just use the String arg
                typedArgs[p] = arg;
                continue;
            }
        }
        Object opReturn = server.invoke(objName, opName, typedArgs, argTypes);
        return new OpResultInfo(opName, argTypes, args, opReturn);
    }

}
