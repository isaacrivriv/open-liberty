package com.ibm.ws.jfap.inbound.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.openliberty.netty.internal.ChannelInitializerWrapper;

public class JmsTCPInitializer extends ChannelInitializerWrapper{

	ChannelInitializerWrapper parent;
	boolean sslEnabled = false;
	
	public JmsTCPInitializer(ChannelInitializerWrapper parent) {
        this.parent = parent;
    }

    public JmsTCPInitializer(ChannelInitializerWrapper parent, boolean sslEnabled) {
        this.parent = parent;
        this.sslEnabled = sslEnabled;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        parent.init(ch);
        ChannelPipeline pipeline = ch.pipeline();
        if(sslEnabled) {
        	//TODO: Add SSL work to pipeline
        }
        // TODO: Add to pipeline
//        pipeline.addLast("decoder", new SipMessageBufferStreamDecoder());
        pipeline.addLast("handler", new JmsChannelHandler());
    }
	
}
