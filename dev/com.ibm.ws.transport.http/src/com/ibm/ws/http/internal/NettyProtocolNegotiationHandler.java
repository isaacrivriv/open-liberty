/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.internal;

import static io.netty.handler.logging.LogLevel.TRACE;;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.DefaultHttp2Connection;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandler;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandlerBuilder;
import io.netty.handler.codec.http2.InboundHttp2ToHttpAdapter;
import io.netty.handler.codec.http2.InboundHttp2ToHttpAdapterBuilder;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;

/**
 *
 */
public class NettyProtocolNegotiationHandler extends ApplicationProtocolNegotiationHandler {
    private static final TraceComponent tc = Tr.register(NettyProtocolNegotiationHandler.class);

    protected static final Http2FrameLogger LOGGER = new Http2FrameLogger(TRACE, HttpToHttp2ConnectionHandler.class);

    // Temp max content length
    protected static final int MAX_CONTENT_LENGTH = 1024 * 100;

    /**
     * Default to HTTP 2.0 for now
     */
    public NettyProtocolNegotiationHandler() {
        super(ApplicationProtocolNames.HTTP_2);
    }

    @Override
    protected void configurePipeline(ChannelHandlerContext ctx, String protocol) throws Exception {
        if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
            // HTTP2 Detailed pipeline for pings, rst, etc
//            ctx.pipeline().addLast(new NettyHttp2HandlerBuilder().build());
            // HTTP2 to HTTP 1.1 and back pipeline
            DefaultHttp2Connection connection = new DefaultHttp2Connection(true);
            InboundHttp2ToHttpAdapter listener = new InboundHttp2ToHttpAdapterBuilder(connection).propagateSettings(false).validateHttpHeaders(false).maxContentLength(MAX_CONTENT_LENGTH).build();

            ctx.pipeline().addLast(new HttpToHttp2ConnectionHandlerBuilder().frameListener(listener).frameLogger(LOGGER).connection(connection).build());

            ctx.pipeline().addLast(new NettyHttpDispatcherHandler());

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Configured pipeline with " + ctx.pipeline().names());
            }

            return;
        }

        if (ApplicationProtocolNames.HTTP_1_1.equals(protocol)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Not yet configured with HTTP 1.1 pipeline for incoming connection " + ctx.channel());
            }
        }

        throw new IllegalStateException("unknown protocol: " + protocol);
    }

}
