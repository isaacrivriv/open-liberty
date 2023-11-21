/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
package com.ibm.ws.webcontainer31.srt;

import java.io.IOException;
import java.net.SocketTimeoutException;

import javax.servlet.ReadListener;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.transport.access.TransportConstants;
import com.ibm.ws.webcontainer.srt.SRTInputStream;
import com.ibm.ws.webcontainer31.async.ThreadContextManager;
import com.ibm.ws.webcontainer31.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer31.upgrade.UpgradeReadCallback;
import com.ibm.ws.webcontainer31.util.UpgradeInputByteBufferUtil;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPReadRequestContext;
import com.ibm.wsspi.webcontainer31.WCCustomProperties31;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.CoalescingBufferQueue;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * @author anupag
 * 
 * This stream will be returned by WebConnectionImpl on getInputStream.
 * The upgraded connection requires new input stream which is not HTTP. So we will not use SRTInputStream31 and make this as separate.
 * This includes the APIs which the application can call and will be available for the Upgraded InputStream provided to the customer. 
 * 
 * The InputStream returned will be using bytebuffers as per Java Servlet spec 3.1 and will be implemented in UpgradeInputByteBufferUtil. 
 * 
 */
public class SRTUpgradeInputStream31 extends SRTInputStream {

    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(SRTUpgradeInputStream31.class, 
                                                         WebContainerConstants.TR_GROUP, 
                                                         WebContainerConstants.NLS_PROPS );
    private boolean closed = false;
    protected UpgradeInputByteBufferUtil _inBuffer;
    // this.in defined in SRTInputStream

    public void init(UpgradeInputByteBufferUtil input)
    {    
        this._inBuffer = input;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {  
            Tr.debug(tc, "init upgrade input");         
        } 
    } 

    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.srt.SRTInputStream#read()
     */
    @Override
    public int read() throws IOException
    {   
        synchronized(this){
            int value = 0;
            if(closed) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isErrorEnabled())
                    Tr.error(tc, "stream.is.closed.no.read.write");
                throw new IOException(Tr.formatMessage(tc, "stream.is.closed.no.read.write"));
            }

            if (this._inBuffer != null) {
                value = this._inBuffer.read();
            }
            else {
                value = this.in.read();
            }
            return value;
        }
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.srt.SRTInputStream#read(byte[])
     */
    @Override
    public int read(byte[] output) throws IOException {

        return this.read(output, 0, output.length);
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.srt.SRTInputStream#read(byte[], int, int)
     */
    @Override
    public int read(byte[] output, int offset, int length) throws IOException {
        synchronized(this){
            int value = 0;
            if(closed) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isErrorEnabled())
                    Tr.error(tc, "stream.is.closed.no.read.write");
                throw new IOException(Tr.formatMessage(tc, "stream.is.closed.no.read.write"));
            }
            if(output == null){
                if (TraceComponent.isAnyTracingEnabled() && tc.isErrorEnabled())
                    Tr.error(tc, "read.write.bytearray.null");
                throw new NullPointerException(Tr.formatMessage(tc, "read.write.bytearray.null"));
            }
            if((offset < 0) || (length < 0) || (length > (output.length - offset))){
                if (TraceComponent.isAnyTracingEnabled() && tc.isErrorEnabled())
                    Tr.error(tc, "read.write.offset.length.bytearraylength", new Object[] {offset, length, output.length});
                throw new IndexOutOfBoundsException(Tr.formatMessage(tc, "read.write.offset.length.bytearraylength", new Object[] {offset, length, output.length}));
            }

            if (this._inBuffer != null) {
                value = _inBuffer.read(output, offset, length);
            }
            else {
                value = this.in.read(output, offset, length);
            }

            return value;
        }
    }


    /* (non-Javadoc)
     * @see javax.servlet.ServletInputStream#isFinished()
     */
    @Override
    public boolean isFinished() {

        if(closed){
            return true;
        }
        
        return false;
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletInputStream#isReady()
     */
    @Override
    public boolean isReady() {
        //If we are closing it means we have read all the data
        if(closed){
            return true;
        }

        return _inBuffer.isReady();
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletInputStream#setReadListener(javax.servlet.ReadListener)
     */
    @Override
    public void setReadListener(ReadListener arg0) {
        if(closed) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isErrorEnabled())
                Tr.error(tc, "stream.is.closed.no.read.write");
            throw new IllegalStateException(Tr.formatMessage(tc, "stream.is.closed.no.read.write"));
        }
        if(_inBuffer != null) _inBuffer.setupReadListener(arg0, this);
    }

    /* (non-Javadoc)
     * @see java.io.InputStream#close()
     */
    @Override
    @FFDCIgnore(InterruptedException.class)
    public void close() throws IOException {
        synchronized(this) {
            if(closed){
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Input stream close previously called ...return ");
                }
                return;
            }
            else{
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Calling close on the util class");
                }
                closed = true;
                //Call close on the helper buffer class
                if(_inBuffer != null && _inBuffer.close()){
                    try {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Timeout has been called, waiting for it to complete, " + _inBuffer.isClosing());
                        }
                        this.wait();
                    } catch (InterruptedException e) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Completed waiting for timeout to be called");
                        }
                    }
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Calling close on parent");
                }
                //Call the super close
                super.close();
            }
        }
    }

    public UpgradeInputByteBufferUtil getInputBufferHelper(){
        return _inBuffer;        
    }
    
    /* (non-Javadoc)
     * @see javax.servlet.ServletInputStream#readLine(byte[], int, int)
     */
    @Override
    public int readLine(byte[] b, int off, int len) throws IOException {
        //Call into the UpgradeInputByteBufferUtil to indicate a readLine is going on and we need to block
        if(_inBuffer != null) _inBuffer.readLineCall();
        
        int readLineReturn = super.readLine(b, off, len);
        //Call into the UpgradeInputByteBufferUtil to indicate the readLine is complete
        if(_inBuffer != null) _inBuffer.readLineCall();
        
        return readLineReturn;
    }
    
    
//    public static class NettyUpgradeInputStream31 extends SRTUpgradeInputStream31{
//        
//        private Channel nettyChannel;
//        
//        // Need to keep track of buffers received here
//        CoalescingBufferQueue queue;
//        
//        
//        /**
//         * 
//         */
//        public NettyUpgradeInputStream31(Channel channel) {
//            // TODO Auto-generated constructor stub
//            this.nettyChannel = channel;
//            this.queue = new CoalescingBufferQueue(channel);
//            if(nettyChannel.pipeline().get("NettyUpgradeInputStream31Handler") != null) {
//                throw new UnsupportedOperationException("Tried adding the same NettyUpgradeInputStream31Handler to the channel for reading!!");
//            }
//            nettyChannel.pipeline().addLast("NettyUpgradeInputStream31Handler", new SimpleChannelInboundHandler<ByteBuf>() {
//
//                @Override
//                protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
//                    // TODO Auto-generated method stub
//                    System.out.println("Received byte buf: "+msg);
//                    queue.add(msg);
//                }
//                
//            });
//        }
//        
//        ReadListener _rl = null;
//        
//        @Override
//        public void setReadListener(ReadListener arg0) {
//            if(arg0 == null){
//                if (TraceComponent.isAnyTracingEnabled() && tc.isErrorEnabled())
//                    Tr.error(tc, "readlistener.is.null");
//                throw new NullPointerException(Tr.formatMessage(tc, "readlistener.is.null"));
//            }
//            if(_rl != null){
//                if (TraceComponent.isAnyTracingEnabled() && tc.isErrorEnabled())
//                    Tr.error(tc, "readlistener.already.started");
//                throw new IllegalStateException(Tr.formatMessage(tc, "readlistener.already.started"));
//                
//            }
//            //Save off the current Thread data by creating the ThreadContextManager. Then pass it into the callback       
//            ThreadContextManager tcm = new ThreadContextManager();
//            
//            NettyUpgradeReadCallBack _tcpChannelCallback = new NettyUpgradeReadCallBack(arg0, null, tcm, this);
//            _rl = arg0;
//            _tcpChannelCallback.complete();
//        }
//        
//        @Override
//        public boolean isReady() {
//            // TODO Auto-generated method stub
//            return true;
//        }
//        
//    }
//    
//    public class NettyUpgradeReadCallBack extends UpgradeReadCallback{
//        
//        //The users ReadListener so we can callback to them
//        ReadListener _rl;
//        private SRTUpgradeInputStream31 _srtUpgradeStream;
//        //ThreadContextManager to push and pop the thread's context data
//        private ThreadContextManager _contextManager;
//        private boolean onErrorCalled = false;
//
//        /**
//         * @param rl
//         * @param uIBBU
//         * @param tcm
//         * @param srtUpgradeStream
//         */
//        public NettyUpgradeReadCallBack(ReadListener rl, UpgradeInputByteBufferUtil uIBBU, ThreadContextManager tcm, SRTUpgradeInputStream31 srtUpgradeStream) {
//            super(rl, uIBBU, tcm, srtUpgradeStream);
//            _rl = rl;
//            _contextManager = tcm;
//            _srtUpgradeStream = srtUpgradeStream;
//        }
//        
//        public void complete() {
//            complete(null, null);
//        }
//        
//        /* (non-Javadoc)
//         * @see com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback#complete(com.ibm.wsspi.channelfw.VirtualConnection, com.ibm.wsspi.tcpchannel.TCPReadRequestContext)
//         */
//        @Override
//        @FFDCIgnore(IOException.class)
//        public void complete(VirtualConnection vc, TCPReadRequestContext rsc) {
//            synchronized(_srtUpgradeStream){
//                if(closed){
//                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                        Tr.debug(tc, "The upgradedStream is closing, won't notify user of data");
//                    }
//                    _srtUpgradeStream.notify();
//                    return;
//                }
//            }
//            try{
//                //Set the original Context Class Loader before calling the users onDataAvailable
//                //Push the original thread's context onto the current thread, also save off the current thread's context
//                _contextManager.pushContextData();
//
//                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                    Tr.debug(tc, "Calling user's ReadListener.onDataAvailable : " + _rl);
//                }
//                //Call into the user's ReadListener to indicate there is data available
//                _rl.onDataAvailable();
//
//                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                    Tr.debug(tc, "User's ReadListener.onDataAvailable complete, reading for more data : " + _rl);
//                }
//                _rl.onAllDataRead();
//            } catch(Throwable onDataAvailableException) {
//                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                    Tr.debug(tc, "ReadListener.onDataAvailable threw an exception : " + onDataAvailableException + ", " + _rl);
//                }
//                //Call directly into the customers ReadListener.onError
//                _rl.onError(onDataAvailableException);
//            } finally {
//                //Revert back to the thread's current context
//                _contextManager.popContextData();
//
//                synchronized(_srtUpgradeStream){
//                    if(!closed){
//                        System.out.println("DO WE NEED TO REQUEST SOMETHING HERE?!?!");
//                    } else {
//                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                            Tr.debug(tc, "The upgradedStream is closing, won't issue the initial read");
//                        }
//                        _srtUpgradeStream.notify();
//                    }
//                }
//            }
//        }
//        
//        /* (non-Javadoc)
//         * @see com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback#error(com.ibm.wsspi.channelfw.VirtualConnection, com.ibm.wsspi.tcpchannel.TCPReadRequestContext, java.io.IOException)
//         */
//        @Override
//        @FFDCIgnore(IOException.class)
//        public void error(VirtualConnection vc, TCPReadRequestContext rsc, IOException ioe) {
//            boolean closing = false;
//            boolean isFirstRead = false;
//            
//            synchronized(_srtUpgradeStream){
//                closing = closed;
//                isFirstRead = false;
//            }
//            if(!closing){
//                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                    Tr.debug(tc, "Encountered an error during the initial read for data : " + ioe + ", " + _rl);
//                }
//
//                try{
//                    //Set the original Context Class Loader before calling the users onDataAvailable
//                    //Push the original thread's context onto the current thread, also save off the current thread's context
//                    _contextManager.pushContextData();
//
//                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                        Tr.debug(tc, "Error encountered while we are not closing : " + _rl);
//                    }
//
//                    if(!isFirstRead){
//                        if(WCCustomProperties31.UPGRADE_READ_TIMEOUT != TCPReadRequestContext.NO_TIMEOUT && ioe instanceof SocketTimeoutException){
//                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                                Tr.debug(tc, "Other side did not send data within the timeout time, calling onError : " + _rl);
//                            }
//                            if(! onErrorCalled) {
//                                onErrorCalled  = true;
//                                _rl.onError(ioe);
//                            }
//                        } else {
//                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                                Tr.debug(tc, "Other side must be closed, check if not called before call onAllDataRead : " + _rl);
//                            }
//                        }
//                    } else {
//                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                            Tr.debug(tc, "Encountered an error during the first initialRead for data, calling the ReadListener.onError : " + _rl);
//                        }
//                        if (TraceComponent.isAnyTracingEnabled() && tc.isErrorEnabled())
//                            Tr.error(tc, "setReadListener.initialread.failed");
//
//                        if(!onErrorCalled) {
//                            onErrorCalled = true;
//                            _rl.onError(ioe);
//                        }
//                    }
//                } finally {
//                    try {
//                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                            Tr.debug(tc, "finally call close from ReadListener.onError : " + _rl);
//                        }
//                        close();
//                    } catch( Exception e ) {
//                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                            Tr.debug(tc, "Caught exception during WebConnection.close : " + e + "," + _rl);
//                        }
//                    }
//
//                    //Revert back to the thread's current context
//                    _contextManager.popContextData();
//                }
//            } else {
//                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                    Tr.debug(tc, "We are closing, skipping the call to onError");
//                }
//                synchronized(_srtUpgradeStream){
//                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                        Tr.debug(tc, "Issuing the notify");
//                    }
//                    _srtUpgradeStream.notify();
//                }
//            }
//        }
//        
//    }
    
    
}
