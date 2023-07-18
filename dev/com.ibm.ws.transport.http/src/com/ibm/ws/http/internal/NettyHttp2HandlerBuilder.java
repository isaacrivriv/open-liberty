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

import static io.netty.handler.logging.LogLevel.INFO;

import io.netty.handler.codec.http2.AbstractHttp2ConnectionHandlerBuilder;
import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.Http2Settings;

/**
 *
 */
public class NettyHttp2HandlerBuilder extends AbstractHttp2ConnectionHandlerBuilder<NettyHttp2ConnectionHandler, NettyHttp2HandlerBuilder> {

    private static final Http2FrameLogger logger = new Http2FrameLogger(INFO, NettyHttp2ConnectionHandler.class);

    public NettyHttp2HandlerBuilder() {
        frameLogger(logger);
    }

    @Override
    public NettyHttp2ConnectionHandler build() {
        return super.build();
    }

    @Override
    protected NettyHttp2ConnectionHandler build(Http2ConnectionDecoder decoder, Http2ConnectionEncoder encoder,
                                                Http2Settings initialSettings) {
        NettyHttp2ConnectionHandler handler = new NettyHttp2ConnectionHandler(decoder, encoder, initialSettings);
        frameListener(handler);
        return handler;
    }

}
