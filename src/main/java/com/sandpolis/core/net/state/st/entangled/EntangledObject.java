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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.sandpolis.core.instance.State.ProtoSTObjectUpdate;
import com.sandpolis.core.instance.state.oid.Oid;
import com.sandpolis.core.instance.state.st.AbstractSTObject;
import com.sandpolis.core.instance.state.st.STAttribute;
import com.sandpolis.core.instance.state.st.STDocument;
import com.sandpolis.core.instance.state.st.STObject;
import com.sandpolis.core.net.state.STCmd.STSyncStruct;
import com.sandpolis.core.net.stream.InboundStreamAdapter;
import com.sandpolis.core.net.stream.OutboundStreamAdapter;
import com.sandpolis.core.net.stream.Stream;
import com.sandpolis.core.net.stream.StreamSink;
import com.sandpolis.core.net.stream.StreamSource;

/**
 * An {@link EntangledObject} is synchronized with a remote object on another
 * instance.
 *
 * <p>
 * It uses the {@link Stream} API to efficiently send real-time updates to the
 * remote object.
 *
 * @param <T> The type of the object's protobuf representation
 * @since 7.0.0
 */
public abstract class EntangledObject extends AbstractSTObject {

	public EntangledObject(STDocument parent, String id) {
		super(parent, id);
	}

	private static final Logger log = LoggerFactory.getLogger(EntangledObject.class);

	protected StreamSink<ProtoSTObjectUpdate> sink;

	protected StreamSource<ProtoSTObjectUpdate> source;

	protected STObject container;

	public StreamSink<ProtoSTObjectUpdate> getSink() {
		return sink;
	}

	public StreamSource<ProtoSTObjectUpdate> getSource() {
		return source;
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
				sink);
	}

	protected void startSource(STSyncStruct config) {
		source = new StreamSource<>() {

			@Override
			public void start() {
				container.addListener(EntangledObject.this);
			}

			@Override
			public void stop() {
				container.removeListener(EntangledObject.this);
			}
		};

		StreamStore.add(source, new OutboundStreamAdapter<>(config.streamId, config.connection));

		// Send initial state
		source.submit(container.snapshot());

		source.start();
	}

	@Override
	public Oid oid() {
		return container.oid();
	}

	@Override
	public STDocument parent() {
		return container.parent();
	}

	@Override
	public void merge(ProtoSTObjectUpdate snapshot) {
		container.merge(snapshot);
	}

	@Override
	public ProtoSTObjectUpdate snapshot(Oid... oids) {
		return container.snapshot(oids);
	}

	@Override
	public void addListener(Object listener) {
		container.addListener(listener);
	}

	@Override
	public void removeListener(Object listener) {
		container.removeListener(listener);
	}

	@Subscribe
	void handle(STAttribute.ChangeEvent event) {
		source.submit(event.attribute().snapshot());
	}

	@Subscribe
	void handle(STDocument.DocumentAddedEvent event) {
		source.submit(event.newDocument().snapshot());
	}

	@Subscribe
	void handle(STDocument.DocumentRemovedEvent event) {
		source.submit(ProtoSTObjectUpdate.newBuilder().addRemoved(event.oldDocument().oid().toString()).build());
	}
}
