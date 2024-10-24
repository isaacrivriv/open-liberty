/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package com.ibm.ws.cdi.misplaced.spi.test.bundle.getclass.beaninjection;

import javax.enterprise.context.RequestScoped;

@RequestScoped
public abstract class AbstractString {

    public String getMsgFromAbstract() {
        return "This string comes from an abstract class where the subclass was registered via getbeans";
    }

    public String getOverriddenMsgFromSubclass() {
        return "TEST FAILED";
    }

    public abstract String getAbstractMethodString();

}
