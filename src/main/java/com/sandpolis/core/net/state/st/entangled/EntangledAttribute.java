//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.net.state.st.entangled;

import static com.sandpolis.core.net.stream.StreamStore.StreamStore;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.State.ProtoSTObjectUpdate;
import com.sandpolis.core.instance.state.oid.Oid;
import com.sandpolis.core.instance.state.st.EphemeralAttribute.EphemeralAttributeValue;
import com.sandpolis.core.instance.state.st.STAttribute;
import com.sandpolis.core.instance.state.st.STDocument;
import com.sandpolis.core.net.state.STCmd.STSyncStruct;
import com.sandpolis.core.net.stream.InboundStreamAdapter;
import com.sandpolis.core.net.stream.OutboundStreamAdapter;
import com.sandpolis.core.net.stream.StreamSink;
import com.sandpolis.core.net.stream.StreamSource;

public class EntangledAttribute extends EntangledObject implements STAttribute {

	private static final Logger log = LoggerFactory.getLogger(EntangledAttribute.class);

	private STAttribute container;

	public EntangledAttribute(STAttribute container, Consumer<STSyncStruct> configurator) {
		super(container.parent(), container.oid().last());
		this.container = Objects.requireNonNull(container);

		if (container instanceof EntangledObject)
			throw new IllegalArgumentException("Entanged objects cannot be nested");

		var config = new STSyncStruct();
		configurator.accept(config);

		// Start streams
		switch (config.direction) {
		case BIDIRECTIONAL:
			startSource(config);
			startSink(config);
			break;
		case DOWNSTREAM:
			if (config.initiator) {
				startSink(config);
			} else {
				startSource(config);
			}
			break;
		case UPSTREAM:
			if (config.initiator) {
				startSource(config);
			} else {
				startSink(config);
			}
			break;
		default:
			throw new IllegalArgumentException();
		}
	}

	protected void startSink(STSyncStruct config) {
		sink = new StreamSink<>() {

			@Override
			public void onNext(ProtoSTObjectUpdate item) {
				if (log.isTraceEnabled()) {
					log.trace("Merging snapshot: {}", item);
				}
				container.merge(item);
			};
		};

		StreamStore.add(new InboundStreamAdapter<>(config.streamId, config.connection, ProtoSTObjectUpdate.class),
				getSink());
	}

	protected void startSource(STSyncStruct config) {
		source = new StreamSource<>() {

			@Override
			public void start() {

			}

			@Override
			public void stop() {

			}
		};

		StreamStore.add(getSource(), new OutboundStreamAdapter<>(config.streamId, config.connection));
		getSource().start();
	}

	@Override
	public void addListener(Object listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public Oid oid() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public STDocument parent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeListener(Object listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public List<EphemeralAttributeValue> history() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object get() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void set(Object value) {
		// TODO Auto-generated method stub

	}

	@Override
	public void source(Supplier<?> source) {
		// TODO Auto-generated method stub

	}

	@Override
	public long timestamp() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void merge(ProtoSTObjectUpdate snapshot) {
		// TODO Auto-generated method stub

	}

	@Override
	public ProtoSTObjectUpdate snapshot(Oid... oids) {
		// TODO Auto-generated method stub
		return null;
	}

}
