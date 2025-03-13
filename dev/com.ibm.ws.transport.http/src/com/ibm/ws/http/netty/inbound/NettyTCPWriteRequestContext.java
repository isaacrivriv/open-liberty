/*******************************************************************************
 * Copyright (c) 2023, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty.inbound;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.ws.http.netty.NettyHttpConstants;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.bytebuffer.WsByteBufferUtils;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;
import com.ibm.wsspi.tcpchannel.TCPWriteCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPWriteRequestContext;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.stream.ChunkedInput;
import io.openliberty.http.netty.stream.WsByteBufferChunkedInput;

/**
 *
 */
public class NettyTCPWriteRequestContext implements TCPWriteRequestContext {

    private static final TraceComponent tc = Tr.register(NettyTCPWriteRequestContext.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    private final NettyTCPConnectionContext connectionContext;
    private final Channel nettyChannel;

    private WsByteBuffer[] buffers;
    private final WsByteBuffer[] defaultBuffers = new WsByteBuffer[1];
    private ByteBuffer byteBufferArray[] = null;
    private ByteBuffer byteBufferArrayDirect[] = null;
    // define reusable arrrays of most common sizes
    private ByteBuffer byteBufferArrayOf1[] = null;
    private final ByteBuffer byteBufferArrayOf2[] = null;
    private final ByteBuffer byteBufferArrayOf3[] = null;
    private final ByteBuffer byteBufferArrayOf4[] = null;

    private VirtualConnection vc;
    private String streamID = "-1";

    public NettyTCPWriteRequestContext(NettyTCPConnectionContext connectionContext, Channel nettyChannel) {

        this.connectionContext = connectionContext;
        this.nettyChannel = nettyChannel;
    }

    @Override
    public TCPConnectionContext getInterface() {
        return connectionContext;
    }

    @Override
    public void clearBuffers() {
        if (Objects.nonNull(this.buffers)) {
            for (int i = 0; i < this.buffers.length; i++) {
                this.buffers[i].clear();
            }
        }

    }

    public void setVC(VirtualConnection vc) {
        this.vc = vc;
    }

    public void setStreamId(String streamId) {
        this.streamID = streamId;
    }

    @Override
    public WsByteBuffer[] getBuffers() {
        return this.buffers;
    }

    @Override
    public WsByteBuffer getBuffer() {
        if (this.buffers == null) {
            return null;
        }
        return this.buffers[0];
    }

    @Override
    public void setBuffers(WsByteBuffer[] bufs) {

        if (Objects.isNull(bufs)) {
            clearBuffers();
            return;
        }
        // Assign the new buffers
        this.buffers = bufs;

        // If buffers are not null, ensure they're compacted to remove any trailing nulls
        if (bufs != null) {
            // Determine the actual number of non-null buffers
            int numBufs = 0;
            for (WsByteBuffer buf : bufs) {
                if (buf == null) {
                    break;
                }
                numBufs++;
            }

            // If there are trailing nulls, create a new array without them
            if (numBufs != bufs.length) {
                this.buffers = new WsByteBuffer[numBufs];
                System.arraycopy(bufs, 0, this.buffers, 0, numBufs);
            }
        }

        // Reset arrays to free memory quicker.
        if (this.byteBufferArray != null) {
            Arrays.fill(this.byteBufferArray, null); // Efficiently set all elements to null
            this.byteBufferArray = null;
        }

        if (this.byteBufferArrayDirect != null) {
            Arrays.fill(this.byteBufferArrayDirect, null); // Efficiently set all elements to null
            this.byteBufferArrayDirect = null;
        }

        // Update byteBufferArray based on the new buffers
        if (this.buffers != null && this.buffers.length > 0) {
            this.byteBufferArray = new ByteBuffer[this.buffers.length];
            for (int i = 0; i < this.buffers.length; i++) {
                this.byteBufferArray[i] = this.buffers[i].getWrappedByteBufferNonSafe();
            }
        } else {
            // If there are no buffers, set byteBufferArray to null
            this.byteBufferArray = null;
        }

    }

    @Override
    public void setBuffer(WsByteBuffer buf) {

        // reset arrays to free memory quicker. defect 457362
        if (this.byteBufferArray != null) {
            // reset references
            for (int i = 0; i < this.byteBufferArray.length; i++) {
                this.byteBufferArray[i] = null;
            }
        }
        if (this.byteBufferArrayDirect != null) {
            // reset references
            for (int i = 0; i < this.byteBufferArrayDirect.length; i++) {
                this.byteBufferArrayDirect[i] = null;
            }
            this.byteBufferArrayDirect = null;
        }
        this.defaultBuffers[0] = null; // reset reference

        if (buf != null) {
            this.buffers = this.defaultBuffers;
            this.buffers[0] = buf;

            if (this.byteBufferArrayOf1 == null) {
                this.byteBufferArrayOf1 = new ByteBuffer[1];
            }
            this.byteBufferArray = this.byteBufferArrayOf1;
            this.byteBufferArray[0] = buf.getWrappedByteBufferNonSafe();

        } else {
            this.buffers = null;
            this.byteBufferArray = null;
        }

    }

    private void awaitChannelFuture(ChannelFuture future, int timeout, String timeoutMsg, String failureMsg)
        throws IOException, InterruptedException {

        CountDownLatch latch = new CountDownLatch(1);
        future.addListener(f -> latch.countDown());
        if (!latch.await(timeout, TimeUnit.MILLISECONDS)) {
            throw new IOException(timeoutMsg);
        }
        if (!future.isSuccess()) {
            throw new IOException(failureMsg, future.cause());
        }
    }

    @Override
    public long write(long numBytes, int timeout) throws IOException {
        
        if (nettyChannel.eventLoop().inEventLoop()) {

            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            System.out.println(" MSP -> Current Stack:");
            for (int i = 2; i < stack.length; i++) {
                System.out.println(stack[i]);
            }

            throw new IllegalStateException("Cannot invoke a blocking write on the Netty event loop thread.");
        }

        long writtenBytes = 0L;
        // If using HTTP2 chunk logic or something else, keep the relevant parts.
        final String protocol = nettyChannel.attr(NettyHttpConstants.PROTOCOL).get();
        final boolean isWsoc = "WebSocket".equals(protocol);
        final boolean isH2 = "HTTP2".equals(protocol);
        final boolean hasContentLength = nettyChannel.hasAttr(NettyHttpConstants.CONTENT_LENGTH)
                                         && nettyChannel.attr(NettyHttpConstants.CONTENT_LENGTH).get() != null;

        try {
            for (WsByteBuffer buffer : buffers) {
                if (buffer == null || buffer.remaining() <= 0) {
                    continue;
                }

                

                if (isH2) {
                    
                    writtenBytes += buffer.remaining();
                    AbstractMap.SimpleEntry<Integer, WsByteBuffer> entry = new AbstractMap.SimpleEntry<>(Integer.valueOf(this.streamID), HttpDispatcher.getBufferManager().wrap(WsByteBufferUtils.asByteArray(buffer)));
                    ChannelFuture future = nettyChannel.write(entry);
                    //awaitChannelFuture(future, timeout, "Write operation timed out (HTTP2 chunk).",
                    //    "Write operation failed (HTTP2 chunk).");
                    // if (!awaitFuture(future, timeout)) {
                    //     throw new IOException("Write operation timed out (HTTP2 chunk).");
                    // }
                    // if (!future.isSuccess()) {
                    //     throw new IOException("Write operation failed (HTTP2 chunk).", future.cause());
                    // }

                } else if (hasContentLength || isWsoc) {
                    ByteBuf nettyBuf = Unpooled.wrappedBuffer(WsByteBufferUtils.asByteArray(buffer));
                    int bytes = nettyBuf.readableBytes();
                    //ChannelFuture future = nettyChannel.writeAndFlush(nettyBuf);
                    ChannelFuture future = nettyChannel.write(nettyBuf);
                   // awaitChannelFuture(future, timeout, "Write operation timed out (hasContentLength/WebSocket).",
                    //    "Write operation failed (hasContentLength/WebSocket).");

                    // if (!awaitFuture(future, timeout)) {
                    //     throw new IOException("Write operation timed out (hasContentLength/WebSocket).");
                    // }
                    // if (!future.isSuccess()) {
                    //     throw new IOException("Write operation failed (hasContentLength/WebSocket).", future.cause());
                    // }
                    writtenBytes += bytes;

                } else {
                   // ChunkedInput<ByteBuf> chunkedInput = new WsByteBufferChunkedInput(buffer);
                   // ChannelFuture future = nettyChannel.writeAndFlush(chunkedInput);

                    ByteBuf nettyBuf = Unpooled.wrappedBuffer(WsByteBufferUtils.asByteArray(buffer));
                    DefaultHttpContent httpContent = new DefaultHttpContent(nettyBuf);
                
                    ChannelFuture future = nettyChannel.write(httpContent);
                   // awaitChannelFuture(future, timeout, "Write operation timed out (Chunked).",
                    //    "Write operation failed (Chunked).");

                    // if (!awaitFuture(future, timeout)) {
                    //     throw new IOException("Write operation timed out (Chunked).");
                    // }
                    // if (!future.isSuccess()) {
                    //     throw new IOException("Write operation failed (Chunked).", future.cause());
                    // }
                   // writtenBytes += chunkedInput.length();
                    writtenBytes += nettyBuf.readableBytes();
                }
            }

        //     if (!hasContentLength && !isWsoc) {
        //     ChannelFuture lastFuture = nettyChannel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        //     if (!awaitFuture(lastFuture, timeout)) {
        //         throw new IOException("Write operation timed out (LastHttpContent).");
        //     }
        //     if (!lastFuture.isSuccess()) {
        //         throw new IOException("Write operation failed (LastHttpContent).", lastFuture.cause());
        //     }
        // }

            //ChannelFuture flushFuture = nettyChannel.writeAndFlush(Unpooled.EMPTY_BUFFER);

            ChannelFuture flushFuture = nettyChannel.writeAndFlush(Unpooled.EMPTY_BUFFER);
            awaitChannelFuture(flushFuture, timeout, "Flush operation timed out.", "Flush operation failed.");
            // if (!awaitFuture(flushFuture, timeout)) {
            //     throw new IOException("Flush operation timed out.");
            // }
            // if (!flushFuture.isSuccess()) {
            //     throw new IOException("Flush operation failed.", flushFuture.cause());
            // }

        } catch (InterruptedException e) {
            // Restore interrupt status
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for write to complete.", e);
        }

        return writtenBytes;
    }


    private boolean awaitFuture(ChannelFuture future, int timeout) throws InterruptedException {
        if (timeout == USE_CHANNEL_TIMEOUT) {
            timeout = 60000;
        }

        if (timeout == IMMED_TIMEOUT) {
            return future.isDone();
        } else if (timeout == NO_TIMEOUT) {
            future.await();
            return true;
        } else {
            return future.await(timeout, TimeUnit.MILLISECONDS);
        }
    }


    @Override
    public VirtualConnection write(long numBytes, TCPWriteCompletedCallback callback, boolean forceQueue, int timeout) {
        boolean wasWritable = nettyChannel.isWritable();
        long totalWrittenBytes = 0;
        ChannelFuture lastWriteFuture = null;
        boolean hasContentLength = nettyChannel.hasAttr(NettyHttpConstants.CONTENT_LENGTH) && Objects.nonNull(nettyChannel.attr(NettyHttpConstants.CONTENT_LENGTH).get());
        //check if wsoc
        final String protocol = nettyChannel.attr(NettyHttpConstants.PROTOCOL).get();

        final boolean isHttp10 = "HTTP10".equals(protocol);

        final boolean isWsoc = "WebSocket".equals(protocol);

        final boolean isH2 = "HTTP2".equals(protocol);

        if (Objects.isNull(buffers)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Ignoring write, null buffers passed for channel: " + nettyChannel);
            }
            // TODO If there is nothing to write, should this be null or vc?
            return null;
        }

        try {
            for (WsByteBuffer buffer : buffers) {
                if (buffer != null && buffer.hasRemaining()) { // Check if buffer is not null and has data
                    byte[] byteArray = WsByteBufferUtils.asByteArray(buffer);
                    if (byteArray != null) {

                        if (isH2) {
                            totalWrittenBytes += buffer.remaining();
                            AbstractMap.SimpleEntry<Integer, WsByteBuffer> entry = new AbstractMap.SimpleEntry<Integer, WsByteBuffer>(Integer.valueOf(this.streamID), HttpDispatcher.getBufferManager().wrap(WsByteBufferUtils.asByteArray(buffer)));
                            lastWriteFuture = this.nettyChannel.writeAndFlush(entry);

                        }

                        else if (hasContentLength || isWsoc || isHttp10) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(this, tc, "Writing sync on channel: " + nettyChannel + " which is wsoc? " + isWsoc);
                            }
                            ByteBuf nettyBuf = Unpooled.wrappedBuffer(WsByteBufferUtils.asByteArray(buffer));
                            lastWriteFuture = this.nettyChannel.writeAndFlush(nettyBuf); // Write data to the channel
                            totalWrittenBytes += nettyBuf.readableBytes();
                        }

                        else {
                            //ChunkedInput<ByteBuf> chunkedInput = new WsByteBufferChunkedInput(buffer);
                            //lastWriteFuture = nettyChannel.writeAndFlush(chunkedInput);
                            //totalWrittenBytes += chunkedInput.length();

                            ByteBuf nettyBuf = Unpooled.wrappedBuffer(WsByteBufferUtils.asByteArray(buffer));
                            DefaultHttpContent httpContent = new DefaultHttpContent(nettyBuf);

                            ChannelFuture future = nettyChannel.writeAndFlush(httpContent);
                            totalWrittenBytes += nettyBuf.readableBytes();
                        }

//                        ByteBuf nettyBuf = Unpooled.wrappedBuffer(WsByteBufferUtils.asByteArray(buffer));
//                        lastWriteFuture = nettyChannel.write(nettyBuf);
//                        totalWrittenBytes += nettyBuf.readableBytes();
                    }
                }
            }

            boolean stillWritable = nettyChannel.isWritable();
            //nettyChannel.flush();

            if (lastWriteFuture == null && wasWritable && stillWritable && totalWrittenBytes >= numBytes) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Found lastWriteFuture to be null or unable to keep writing on channel: " + nettyChannel);
                    Tr.debug(this, tc, "lastWriteFuture: " + lastWriteFuture + " wasWritable: " + wasWritable + " stillWritable: " + stillWritable + " totalWrittenBytes: "
                                       + totalWrittenBytes + " numBytes: " + numBytes);
                }
                // Every thing was written here. Do callback in another thread
                if (forceQueue) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "Forcing callback on channel: " + nettyChannel);
                    }
                    HttpDispatcher.getExecutorService().submit(() -> {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(this, tc, "Calling callback in asynchronous thread for channel: " + nettyChannel);
                        }
                        callback.complete(vc, this);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(this, tc, "Finished callback in asynchronous thread for channel: " + nettyChannel);
                        }
                    });
                    return null;
                }
                return vc;

            } else {

                if (lastWriteFuture != null) {
                    // We don't have to do the callback if everything wrote properly
                    if (lastWriteFuture.isDone()) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(this, tc, "Found lastWriteFuture to be finished on channel: " + nettyChannel);
                        }
                        // Everything was written, if forceQueue need to do callback on another thread
                        if (forceQueue) {
                            HttpDispatcher.getExecutorService().submit(() -> {
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                    Tr.debug(this, tc, "Calling callback in asynchronous thread for channel: " + nettyChannel);
                                }
                                callback.complete(vc, this);
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                    Tr.debug(this, tc, "Finished callback in asynchronous thread for channel: " + nettyChannel);
                                }
                            });
                            return null;
                        }
                        return vc;
                    }
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "Went async, found lastWriteFuture to be running on channel: " + nettyChannel);
                    }
                    lastWriteFuture.addListener((ChannelFutureListener) future -> {
                        boolean succeeded = future.isSuccess();
                        HttpDispatcher.getExecutorService().submit(() -> {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(this, tc, "Listener called with success? " + succeeded +" for channel: " + nettyChannel);
                            }
                            if(succeeded){
                                callback.complete(vc, this);
                            } else {
                                callback.error(vc, this, new IOException(future.cause()));
                            }
                        });
                    });
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "In else block with lastWriteFuture being null for channel: " + nettyChannel);
                    }
                }
            }

        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Caught exception on channel: " + nettyChannel + " , " + e);
            }
            callback.error(vc, null, new IOException(e));
        }
        return null; // Return null as the write operation is queued or forced to queue
    }
}