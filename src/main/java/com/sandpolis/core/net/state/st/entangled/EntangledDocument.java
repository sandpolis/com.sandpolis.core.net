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

import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;

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

public class EntangledDocument extends EntangledObject implements STDocument {

	private static final Logger log = LoggerFactory.getLogger(EntangledDocument.class);

	private STDocument container;

	public EntangledDocument(STDocument container, Consumer<STSyncStruct> configurator) {
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

	private void startSink(STSyncStruct config) {
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

	private void startSource(STSyncStruct config) {
		source = new StreamSource<>() {

			@Override
			public void start() {
				container.parent().set(container.oid().last(), EntangledDocument.this);
			}

			@Override
			public void stop() {
				container.parent().set(container.oid().last(), container);
			}
		};

		StreamStore.add(getSource(), new OutboundStreamAdapter<>(config.streamId, config.connection));
		getSource().start();
	}

	@Override
	protected synchronized void fireAttributeValueChangedEvent(STAttribute attribute, EphemeralAttributeValue oldValue,
			EphemeralAttributeValue newValue) {
		source.submit(attribute.snapshot());

		super.fireAttributeValueChangedEvent(attribute, oldValue, newValue);
	}

	@Override
	protected synchronized void fireDocumentAddedEvent(STDocument document, STDocument newDocument) {
		source.submit(newDocument.snapshot());

		super.fireDocumentAddedEvent(document, newDocument);
	}

	@Override
	protected synchronized void fireDocumentRemovedEvent(STDocument document, STDocument oldDocument) {
		source.submit(ProtoSTObjectUpdate.newBuilder().addRemoved(oldDocument.oid().toString()).build());

		super.fireDocumentRemovedEvent(document, oldDocument);
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
	public STAttribute attribute(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int attributeCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Collection<STAttribute> attributes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public STDocument document(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int documentCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Collection<STDocument> documents() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void remove(STAttribute attribute) {
		// TODO Auto-generated method stub

	}

	@Override
	public void remove(STDocument document) {
		// TODO Auto-generated method stub

	}

	@Override
	public void remove(String id) {
		// TODO Auto-generated method stub

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

	@Override
	public void set(String id, STAttribute attribute) {
		// TODO Auto-generated method stub

	}

	@Override
	public void set(String id, STDocument document) {
		// TODO Auto-generated method stub

	}

}
