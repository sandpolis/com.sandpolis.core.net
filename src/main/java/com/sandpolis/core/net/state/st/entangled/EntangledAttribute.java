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
import com.sandpolis.core.net.state.STCmd.STSyncStruct;

public class EntangledAttribute extends EntangledObject implements STAttribute {

	private static final Logger log = LoggerFactory.getLogger(EntangledAttribute.class);

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

	@Override
	public List<EphemeralAttributeValue> history() {
		return ((STAttribute) container).history();
	}

	@Override
	public Object get() {
		return ((STAttribute) container).get();
	}

	@Override
	public void set(Object value) {
		((STAttribute) container).set(value);
	}

	@Override
	public void source(Supplier<?> source) {
		((STAttribute) container).source(source);
	}

	@Override
	public long timestamp() {
		return ((STAttribute) container).timestamp();
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
