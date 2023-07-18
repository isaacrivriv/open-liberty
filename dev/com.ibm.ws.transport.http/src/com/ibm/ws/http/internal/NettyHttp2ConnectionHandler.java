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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2Flags;
import io.netty.handler.codec.http2.Http2FrameListener;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.util.CharsetUtil;

/**
 *
 */
public class NettyHttp2ConnectionHandler extends Http2ConnectionHandler implements Http2FrameListener {

    private static final TraceComponent tc = Tr.register(NettyHttp2ConnectionHandler.class);

    NettyHttp2ConnectionHandler(Http2ConnectionDecoder decoder, Http2ConnectionEncoder encoder,
                                Http2Settings initialSettings) {
        super(decoder, encoder, initialSettings);
    }

    static final ByteBuf RESPONSE_BYTES = Unpooled.unreleasableBuffer(
                                                                      Unpooled.copiedBuffer("Hello Liberty!!", CharsetUtil.UTF_8)).asReadOnly();

    @Override
    public int onDataRead(ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding, boolean endOfStream) throws Http2Exception {
        // TODO Auto-generated method stub
        Tr.debug(this, tc, "DataRead!! " + ctx.channel(), new Object[] { ctx, streamId, data, padding, endOfStream });
        int processedBytes = data.readableBytes() + padding;
        if (endOfStream) {
            Http2Headers respHeaders = new DefaultHttp2Headers().status(HttpResponseStatus.OK.codeAsText());
            encoder().writeHeaders(ctx, streamId, respHeaders, 0, false, ctx.newPromise());
            encoder().writeData(ctx, streamId, data, 0, true, ctx.newPromise());
        }
        return processedBytes;
    }

    @Override
    public void onGoAwayRead(ChannelHandlerContext ctx, int lastStreamId, long errorCode, ByteBuf debugData) throws Http2Exception {
        // TODO Auto-generated method stub
        Tr.debug(this, tc, "GoAwayRead!! " + ctx.channel(), new Object[] { ctx, lastStreamId, errorCode, debugData });
    }

    @Override
    public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int padding, boolean endOfStream) throws Http2Exception {
        // TODO Auto-generated method stub
        Tr.debug(this, tc, "HeadersRead!! " + ctx.channel(), new Object[] { ctx, streamId, headers, padding, endOfStream });
        if (endOfStream) {
            ByteBuf content = ctx.alloc().buffer();
            content.writeBytes(RESPONSE_BYTES.duplicate());
            ByteBufUtil.writeAscii(content, " - via HTTP/2");
            Http2Headers respHeaders = new DefaultHttp2Headers().status(HttpResponseStatus.OK.codeAsText());
            encoder().writeHeaders(ctx, streamId, respHeaders, 0, false, ctx.newPromise());
            encoder().writeData(ctx, streamId, content, 0, true, ctx.newPromise());
        }
    }

    @Override
    public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int streamDependency, short weight, boolean exclusive, int padding,
                              boolean endOfStream) throws Http2Exception {
        // TODO Auto-generated method stub
        Tr.debug(this, tc, "HeadersRead!! " + ctx.channel(), new Object[] { ctx, streamId, headers, streamDependency, weight, exclusive, padding, endOfStream });
        onHeadersRead(ctx, streamId, headers, padding, endOfStream);
    }

    @Override
    public void onPingAckRead(ChannelHandlerContext ctx, long data) throws Http2Exception {
        // TODO Auto-generated method stub
        Tr.debug(this, tc, "PingAckRead!! " + ctx.channel(), new Object[] { ctx, data });
    }

    @Override
    public void onPingRead(ChannelHandlerContext ctx, long data) throws Http2Exception {
        // TODO Auto-generated method stub
        Tr.debug(this, tc, "PingRead!! " + ctx.channel(), new Object[] { ctx, data });
    }

    @Override
    public void onPriorityRead(ChannelHandlerContext ctx, int streamId, int streamDependency, short weight, boolean exclusive) throws Http2Exception {
        // TODO Auto-generated method stub
        Tr.debug(this, tc, "PriorityRead!! " + ctx.channel(), new Object[] { ctx, streamId, streamDependency, weight, exclusive });
    }

    @Override
    public void onPushPromiseRead(ChannelHandlerContext ctx, int streamId, int promisedStreamId, Http2Headers headers, int padding) throws Http2Exception {
        // TODO Auto-generated method stub
        Tr.debug(this, tc, "PushPromiseRead!! " + ctx.channel(), new Object[] { ctx, streamId, promisedStreamId, headers, padding });
    }

    @Override
    public void onRstStreamRead(ChannelHandlerContext ctx, int streamId, long errorCode) throws Http2Exception {
        // TODO Auto-generated method stub
        Tr.debug(this, tc, "RstStreamRead!! " + ctx.channel(), new Object[] { ctx, streamId, errorCode });
    }

    @Override
    public void onSettingsAckRead(ChannelHandlerContext ctx) throws Http2Exception {
        // TODO Auto-generated method stub
        Tr.debug(this, tc, "SettingsAckRead!! " + ctx.channel(), new Object[] { ctx });
    }

    @Override
    public void onSettingsRead(ChannelHandlerContext ctx, Http2Settings settings) throws Http2Exception {
        // TODO Auto-generated method stub
        Tr.debug(this, tc, "SettingsRead!! " + ctx.channel(), new Object[] { ctx, settings });
    }

    @Override
    public void onUnknownFrame(ChannelHandlerContext ctx, byte frameType, int streamId, Http2Flags flags, ByteBuf payload) throws Http2Exception {
        // TODO Auto-generated method stub
        Tr.debug(this, tc, "UnknownFrame!! " + ctx.channel(), new Object[] { ctx, frameType, streamId, flags, payload });
    }

    @Override
    public void onWindowUpdateRead(ChannelHandlerContext ctx, int streamId, int windowSizeIncrement) throws Http2Exception {
        // TODO Auto-generated method stub
        Tr.debug(this, tc, "WindowUpdateRead!! " + ctx.channel(), new Object[] { ctx, streamId, windowSizeIncrement });
    }

}
