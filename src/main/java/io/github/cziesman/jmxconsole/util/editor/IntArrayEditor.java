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
import java.util.StringTokenizer;

/**
 * A property editor for int[].
 *
 * @version <tt>$Revision$</tt>
 */
public class IntArrayEditor extends PropertyEditorSupport {

    /**
     * @return a comma separated string of the array elements
     */
    public String getAsText() {

        int[] theValue = (int[]) getValue();
        StringBuilder text = new StringBuilder();
        int length = theValue == null ? 0 : theValue.length;
        for (int n = 0; n < length; n++) {
            if (n > 0) {
                text.append(',');
            }
            text.append(theValue[n]);
        }
        return text.toString();
    }

    /**
     * Build a int[] from comma or eol separated elements
     */
    public void setAsText(final String text) {

        StringTokenizer stok = new StringTokenizer(text, ",\r\n");
        int[] theValue = new int[stok.countTokens()];
        int i = 0;
        while (stok.hasMoreTokens()) {
            theValue[i++] = Integer.decode(stok.nextToken());
        }
        setValue(theValue);
    }

}
