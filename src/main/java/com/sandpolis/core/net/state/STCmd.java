//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.net.state;

import static com.sandpolis.core.instance.state.STStore.STStore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.foundation.ConfigStruct;
import com.sandpolis.core.foundation.Result.Outcome;
import com.sandpolis.core.foundation.util.IDUtil;
import com.sandpolis.core.foundation.util.RandUtil;
import com.sandpolis.core.instance.State.ProtoSTObjectUpdate;
import com.sandpolis.core.instance.state.oid.Oid;
import com.sandpolis.core.instance.state.st.EphemeralDocument;
import com.sandpolis.core.net.cmdlet.Cmdlet;
import com.sandpolis.core.net.connection.Connection;
import com.sandpolis.core.net.msg.MsgState.RQ_STSnapshot;
import com.sandpolis.core.net.msg.MsgState.RQ_STSync;
import com.sandpolis.core.net.msg.MsgState.RQ_STSync.STSyncDirection;
import com.sandpolis.core.net.state.st.entangled.EntangledDocument;

public class STCmd extends Cmdlet<STCmd> {

	private static final Logger log = LoggerFactory.getLogger(STCmd.class);

	@ConfigStruct
	public static final class STSnapshotStruct {
		public List<Oid> whitelist = new ArrayList<>();
	}

	@ConfigStruct
	public static final class STSyncStruct {
		public Connection connection;

		public STSyncDirection direction = STSyncDirection.DOWNSTREAM;
		public boolean initiator;
		public int streamId = IDUtil.stream();
		public int updatePeriod;
		public List<Oid> whitelist = new ArrayList<>();
	}

	/**
	 * Prepare for an asynchronous command.
	 *
	 * @return A configurable object from which all asynchronous (nonstatic)
	 *         commands in {@link STCmd} can be invoked
	 */
	public static STCmd async() {
		return new STCmd();
	}

	private STCmd() {
	}

	public CompletionStage<EphemeralDocument> snapshot(Oid oid) {
		return snapshot(oid, struct -> {
		});
	}

	public CompletionStage<EphemeralDocument> snapshot(Oid oid, Consumer<STSnapshotStruct> configurator) {
		if (!oid.isConcrete())
			throw new IllegalArgumentException("A concrete OID is required");

		final var config = new STSnapshotStruct();
		configurator.accept(config);

		for (var o : config.whitelist)
			if (!oid.isAncestorOf(o))
				throw new IllegalArgumentException();

		var rq = RQ_STSnapshot.newBuilder() //
				.setOid(oid.toString());

		config.whitelist.stream().map(Oid::toString).forEach(rq::addWhitelist);

		return request(ProtoSTObjectUpdate.class, rq).thenApply(rs -> {
//			return new EphemeralDocument(rs);
			return null;
		});
	}

	public CompletionStage<EntangledDocument> sync(Oid oid) {
		int id = RandUtil.nextInt();

		return sync(oid, config -> {
			config.connection = target;
			config.initiator = true;
			config.streamId = id;
		});
	}

	public CompletionStage<EntangledDocument> sync(Oid oid, Consumer<STSyncStruct> configurator) {
		if (!oid.isConcrete())
			throw new IllegalArgumentException("A concrete OID is required (" + oid + ")");
		if (oid.path().length == 0)
			throw new IllegalArgumentException("Empty OID");

		var config = new STSyncStruct();
		configurator.accept(config);

		if (!config.initiator) {
			throw new IllegalArgumentException();
		}

		for (var o : config.whitelist)
			if (!oid.isAncestorOf(o))
				throw new IllegalArgumentException();

		var rq = RQ_STSync.newBuilder() //
				.setStreamId(config.streamId) //
				.setOid(oid.toString()) //
				.setUpdatePeriod(config.updatePeriod) //
				.setDirection(config.direction);

		config.whitelist.stream().map(Oid::toString).forEach(rq::addWhitelist);

		var document = new EntangledDocument(STStore.get(oid), configurator);

		log.debug("Sending sync command for OID: {}", oid);
		return request(Outcome.class, rq).thenApply(rs -> {
			return document;
		});
	}
}
