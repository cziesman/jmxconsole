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
package io.github.cziesman.jmxconsole.util.editor;

import java.beans.PropertyEditorSupport;

/**
 * A property editor for {@link java.lang.Character}.
 *
 * @author adrian@jboss.org
 * @version $Revision$
 * @todo REVIEW: look at possibly parsing escape sequences?
 */
public class CharacterEditor extends PropertyEditorSupport {

    public void setAsText(final String text) {

        if (PropertyEditors.isNull(text)) {
            setValue(null);
            return;
        }
        if (text.length() != 1) {
            throw new IllegalArgumentException("Too many (" + text.length() + ") characters: '" + text + "'");
        }
        Object newValue = text.charAt(0);
        setValue(newValue);
    }

}