/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package mpGraphQL10.defaultvalue;

public class VariablesAsString implements Variables {

    public static VariablesAsString newVars(String widgetString) {
        return new VariablesAsString(widgetString);
    }

    private String widgetString;

    private VariablesAsString(String widgetString) {
        this.widgetString = widgetString;
    }

    public String getWidgetString() {
        return widgetString;
    }

    public void setWidgetString(String widgetString) {
        this.widgetString = widgetString;
    }
}
