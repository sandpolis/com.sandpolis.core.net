//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.net.cmdlet;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.sandpolis.core.net.connection.ConnectionStore.ConnectionStore;
import static com.sandpolis.core.net.network.NetworkStore.NetworkStore;

import java.util.concurrent.CompletionStage;

import com.google.protobuf.MessageLite;
import com.google.protobuf.MessageLiteOrBuilder;
import com.sandpolis.core.instance.state.ConnectionOid;
import com.sandpolis.core.net.connection.Connection;
import com.sandpolis.core.net.exelet.Exelet;

/**
 * A {@link Cmdlet} contains commands that can be run against a CVID.
 * {@link Cmdlet}s usually produce requests and the corresponding {@link Exelet}
 * returns a response.
 *
 * @author cilki
 * @since 5.0.0
 */
@SuppressWarnings("unchecked")
public abstract class Cmdlet<E extends Cmdlet<E>> {

	/**
	 * The target CVID. Defaults to the default server CVID.
	 */
	private Integer cvid = NetworkStore.getPreferredServer().orElse(0);

	/**
	 * The target sock which will be used to send and receive messages. Defaults to
	 * the default server.
	 */
	protected Connection target = ConnectionStore.getByCvid(cvid).orElse(null);

	/**
	 * Explicitly set the remote endpoint by {@link Connection}.
	 *
	 * @param sock The target sock
	 * @return {@code this}
	 */
	public E target(Connection sock) {
		this.target = checkNotNull(sock);
		this.cvid = sock.get(ConnectionOid.REMOTE_CVID);
		return (E) this;
	}

	/**
	 * Explicitly set the remote endpoint by CVID.
	 *
	 * @param cvid The target CVID
	 * @return {@code this}
	 */
	public E target(int cvid) {
		this.cvid = cvid;
		this.target = ConnectionStore.getByCvid(cvid).get();
		return (E) this;
	}

	/**
	 * Explicitly set the remote endpoint.
	 *
	 * @param cvid The target CVID
	 * @param sock The target sock
	 * @return {@code this}
	 */
	public E target(int cvid, Connection sock) {
		this.cvid = cvid;
		this.target = checkNotNull(sock);
		return (E) this;
	}

	/**
	 * Alias for {@code target.request(responseType, payload)}.
	 *
	 * @param <T>          The expected response type
	 * @param responseType The expected response type
	 * @param payload      The request payload
	 * @return An asynchronous {@link CompletionStage}
	 */
	protected <T extends MessageLite> CompletionStage<T> request(Class<T> responseType, MessageLiteOrBuilder payload) {
		return target.request(responseType, payload);
	}
}
