//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.net.cvid;

import static com.sandpolis.core.net.network.NetworkStore.NetworkStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.Metatypes.InstanceFlavor;
import com.sandpolis.core.instance.Metatypes.InstanceType;
import com.sandpolis.core.instance.state.ConnectionOid;
import com.sandpolis.core.net.Message.MSG;
import com.sandpolis.core.net.channel.ChannelConstant;
import com.sandpolis.core.net.msg.MsgCvid.RQ_Cvid;
import com.sandpolis.core.net.msg.MsgCvid.RS_Cvid;
import com.sandpolis.core.net.network.NetworkEvents.CvidChangedEvent;
import com.sandpolis.core.net.util.MsgUtil;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

/**
 * This handler manages the CVID handshake for the requesting instance. Usually
 * the requesting instance will be the agent or client.
 *
 * @see CvidResponseHandler
 *
 * @since 5.0.0
 */
public class CvidRequestHandler extends AbstractCvidHandler {

	private static final Logger log = LoggerFactory.getLogger(CvidRequestHandler.class);

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, MSG msg) throws Exception {
		var ch = ctx.channel();
		var sock = ch.attr(ChannelConstant.SOCK).get();

		// Autoremove the handler
		ch.pipeline().remove(this);

		RS_Cvid rs = MsgUtil.unpack(msg, RS_Cvid.class);
		if (rs != null && !rs.getServerUuid().isEmpty()) {

			Core.setCvid(rs.getCvid());
			NetworkStore.post(CvidChangedEvent::new, Core.cvid());
			sock.set(ConnectionOid.REMOTE_CVID, rs.getServerCvid());
			sock.set(ConnectionOid.REMOTE_UUID, rs.getServerUuid());

			super.userEventTriggered(ctx, new CvidHandshakeCompletionEvent(rs.getCvid(), rs.getServerCvid()));
			log.debug("CVID handshake succeeded ({})", rs.getCvid());
		} else {
			super.userEventTriggered(ctx, new CvidHandshakeCompletionEvent());
			log.debug("CVID handshake failed");
		}
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		handshake(ctx.channel(), Core.INSTANCE, Core.FLAVOR, Core.UUID);
		super.channelActive(ctx);
	}

	/**
	 * Begin the CVID handshake phase.
	 *
	 * @param channel  The channel
	 * @param instance The instance type
	 * @param flavor   The instance flavor
	 * @param uuid     The instance's UUID
	 */
	void handshake(Channel channel, InstanceType instance, InstanceFlavor flavor, String uuid) {
		log.debug("Initiating CVID handshake");
		channel.writeAndFlush(
				MsgUtil.rq(RQ_Cvid.newBuilder().setInstance(instance).setInstanceFlavor(flavor).setUuid(uuid)).build());
	}
}
