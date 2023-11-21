/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http2.upgrade;

import com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.CoalescingBufferQueue;
import io.netty.channel.VoidChannelPromise;

/**
 *
 */
public class ServletUpgradeHandler extends ChannelInboundHandlerAdapter {

    public enum UpgradeState {
        INIT, // Data read will not be passed on to next handlers but will be stored here
        ACTIVE,
        CLOSED
    }

    private final CoalescingBufferQueue queue;
    private final Channel channel;
    long totalBytesRead = 0;

    private class ReadNotifier {
        public synchronized void readReady() {
            this.notifyAll();
        }
    }

    private final ReadNotifier readNotifier = new ReadNotifier();

    // When added upgrade is in place so initialize as INIT
    private UpgradeState state = UpgradeState.INIT;

    TCPReadCompletedCallback callback;

    /**
     * Initialize the queue that will store the data
     */
    public ServletUpgradeHandler(Channel channel) {
        // TODO Auto-generated constructor stub
        this.queue = new CoalescingBufferQueue(channel);
        this.channel = channel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // TODO Auto-generated method stub
        if (msg instanceof ByteBuf) {
            System.out.println("Got content to store!!");
            ByteBuf buf = (ByteBuf) msg;
            System.out.println(ByteBufUtil.hexDump(buf));
            queue.add(buf);
            totalBytesRead += buf.readableBytes();
            if (callback != null) {
                callback.complete(null, null);
            }
            readNotifier.readReady();
        } else {
            System.out.println("Need to verify!! Message was not a ByteBuf object!! Passing on as normal");
            System.out.println(msg);
            super.channelRead(ctx, msg);
        }
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        // TODO Auto-generated method stub
        System.out.println("Channel writeable changed!!!");
        super.channelWritabilityChanged(ctx);
    }

    /**
     * Helper method to establish if there is data on the queue to be read
     */
    public boolean containsQueuedData() {
        return !queue.isEmpty();
    }

    /**
     * Helper method to establish if there is data on the queue to be read
     */
    public int queuedDataSize() {
        return queue.readableBytes();
    }

    public void waitForDataRead(long waitTime) throws InterruptedException {
        synchronized (readNotifier) {
            readNotifier.wait((waitTime < 0) ? 0 : waitTime);
        }
    }

//    public void notifyDataRead() {
//        synchronized (readNotifier) {
//            readNotifier.notifyAll();
//        }
//    }

    /**
     * Helper method to read from Queue
     */
    public synchronized ByteBuf read(int size, ChannelPromise promise) {
        if (!containsQueuedData()) {
            // No data to send, do we need to wait until we get some? Or do we call back? Or just send error or empty buffer?
            System.out.println("No data stored yet!!");
            return Unpooled.EMPTY_BUFFER;
        }
        if (promise == null)
            return queue.remove(size, new VoidChannelPromise(channel, true));
        return queue.remove(size, promise);
    }

    /**
     * Helper method to change state
     */
    public void updateState(UpgradeState state) {
        this.state = state;
    }

    /**
     * Helper method to set read listener
     */
    public void setReadListener(TCPReadCompletedCallback callback) {
        this.callback = callback;
        if (containsQueuedData()) {
            callback.complete(null, null);
        }
    }

}
