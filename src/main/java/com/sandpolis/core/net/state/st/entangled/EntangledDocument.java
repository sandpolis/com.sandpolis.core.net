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

import java.util.Objects;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.state.st.STAttribute;
import com.sandpolis.core.instance.state.st.STDocument;
import com.sandpolis.core.net.state.STCmd.STSyncStruct;

public class EntangledDocument extends EntangledObject implements STDocument {

	private static final Logger log = LoggerFactory.getLogger(EntangledDocument.class);

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

	@Override
	public STAttribute attribute(String id) {
		return ((STDocument) container).attribute(id);
	}

	@Override
	public int attributeCount() {
		return ((STDocument) container).attributeCount();
	}

	@Override
	public STDocument document(String id) {
		return ((STDocument) container).document(id);
	}

	@Override
	public int documentCount() {
		return ((STDocument) container).documentCount();
	}

	@Override
	public void remove(STAttribute attribute) {
		((STDocument) container).remove(attribute);
	}

	@Override
	public void remove(STDocument document) {
		((STDocument) container).remove(document);
	}

	@Override
	public void remove(String id) {
		((STDocument) container).remove(id);
	}

	@Override
	public void set(String id, STAttribute attribute) {
		((STDocument) container).set(id, attribute);
	}

	@Override
	public void set(String id, STDocument document) {
		((STDocument) container).set(id, document);
	}

	@Override
	public void forEachAttribute(Consumer<STAttribute> consumer) {
		((STDocument) container).forEachAttribute(consumer);
	}

	@Override
	public void forEachDocument(Consumer<STDocument> consumer) {
		((STDocument) container).forEachDocument(consumer);
	}
}
