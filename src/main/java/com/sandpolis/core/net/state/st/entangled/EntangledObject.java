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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.State.ProtoSTObjectUpdate;
import com.sandpolis.core.instance.state.oid.Oid;
import com.sandpolis.core.instance.state.st.AbstractSTObject;
import com.sandpolis.core.instance.state.st.STDocument;
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

	public StreamSink<ProtoSTObjectUpdate> getSink() {
		return sink;
	}

	public StreamSource<ProtoSTObjectUpdate> getSource() {
		return source;
	}

}
