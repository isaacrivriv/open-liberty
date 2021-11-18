/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jfap.inbound.netty;

import java.net.InetSocketAddress;
import com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer;

public class JmsMessageEvent {
	
	private final WsByteBuffer jmsMsg;
	private final InetSocketAddress remoteAddr;

	public JmsMessageEvent(final WsByteBuffer data, final InetSocketAddress addr) {
		this.jmsMsg = data;
		this.remoteAddr = addr;
	}

	public InetSocketAddress getRemoteAddress() {
		return remoteAddr;
	}

	public WsByteBuffer getJmsMsg() {
		return jmsMsg;
	}

}
