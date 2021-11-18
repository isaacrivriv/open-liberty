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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.jfapchannel.server.impl.JFapInboundConnLink;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

public class JmsChannelHandler extends SimpleChannelInboundHandler<JmsMessageEvent> {

	private static final TraceComponent tc = Tr.register(JmsChannelHandler.class);

    final private AttributeKey<JFapInboundConnLink> attrKey = AttributeKey.valueOf("JFapInboundConnLink");

    /**
     * Called when a new connection is established
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "channelActive", ctx.channel().remoteAddress() + " connected");
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, JmsMessageEvent msg) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "channelRead0",
                    ctx.channel() + ". [" + msg.getJmsMsg() + "] bytes received");
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "channelInactive", ctx.channel().remoteAddress() + " has been disonnected");
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "exceptionCaught. " + cause.getClass());
        }
        ctx.close();
    }
	
}
