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

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpUtil.setContentLength;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http2.HttpConversionUtil;
import io.netty.util.CharsetUtil;

/**
 *
 */
public class NettyHttpDispatcherHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final TraceComponent tc = Tr.register(NettyHttpDispatcherHandler.class);

//    static final ByteBuf RESPONSE_BYTES = Unpooled.unreleasableBuffer(
//                                                                      Unpooled.copiedBuffer("Hello Liberty Dispatcher!!", CharsetUtil.UTF_8)).asReadOnly();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "ChannelRead fired for: " + ctx.channel());
        }
        // Get stream ID for message
        String streamId = request.headers().get(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text());
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "DataRead!! " + ctx.channel(), new Object[] { ctx, request, streamId });
        }
        // Prepare HTTP response
        ByteBuf RESPONSE_BYTES = Unpooled.copiedBuffer("Hello Liberty Dispatcher!!", CharsetUtil.UTF_8).asReadOnly().retain();
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, RESPONSE_BYTES);
        response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");
        setContentLength(response, response.content().readableBytes());
        // Set stream ID for response and write back
        response.headers().set(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text(), streamId);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "ChannelRead responding with: " + response + " with data: " + RESPONSE_BYTES);
        }
        ctx.writeAndFlush(response);
    }

}
