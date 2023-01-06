/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

//
// Generated By:JAX-WS RI IBM 2.1.1 in JDK 6 (JAXB RI IBM JAXB 2.1.3 in JDK 1.6)
//

package com.ibm.was.wssample.sei.ping;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlSeeAlso;

@WebService(name = "PingServicePortType", targetNamespace = "http://com/ibm/was/wssample/sei/ping/")
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
@XmlSeeAlso({
              ObjectFactory.class
})
public interface PingServicePortType {

    /**
     *
     * @param parameter
     */
    @WebMethod(action = "pingOperation")
    @Oneway
    public void pingOperation(
                              @WebParam(name = "pingStringInput", targetNamespace = "http://com/ibm/was/wssample/sei/ping/", partName = "parameter") PingStringInput parameter);

}