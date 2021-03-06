//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.net.exelet;

import java.util.Objects;

import com.google.protobuf.MessageLite;
import com.sandpolis.core.net.Message.MSG;
import com.sandpolis.core.net.connection.Connection;

/**
 * An object that can optionally be passed to {@link Exelet} handlers and
 * provides connection-specific utilities.
 *
 * @author cilki
 * @since 5.1.0
 */
public final class ExeletContext {

	public final Connection connector;

	public final MSG request;

	MessageLite.Builder reply;

	Runnable deferAction;

	public ExeletContext(Connection connector, MSG request) {
		this.connector = Objects.requireNonNull(connector);
		this.request = Objects.requireNonNull(request);
	}

	/**
	 * Set the response payload.
	 *
	 * @param msg The response payload
	 */
	public void reply(MessageLite.Builder msg) {
		if (this.reply != null)
			throw new IllegalStateException();

		this.reply = msg;
	}

	/**
	 * Schedule an action to be executed immediately after the response is sent. Do
	 * not call {@link #reply(Message.Builder)} from this block.
	 *
	 * @param action The deferred action
	 */
	public void defer(Runnable action) {
		if (this.deferAction != null)
			throw new IllegalStateException();

		this.deferAction = action;
	}
}
