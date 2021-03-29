//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.net.config;

import com.sandpolis.core.foundation.config.ConfigProperty;
import com.sandpolis.core.instance.config.DefaultConfigProperty;

public final class CfgNet {

	/**
	 * The default message timeout in milliseconds.
	 */
	public static final ConfigProperty<Integer> MESSAGE_TIMEOUT = new DefaultConfigProperty<>(Integer.class,
			"s7s.net.message_timeout", 1000);

	/**
	 * The maximum number of outgoing connection attempts.
	 */
	public static final ConfigProperty<Integer> OUTGOING_CONCURRENCY = new DefaultConfigProperty<>(Integer.class,
			"s7s.net.connection.max_outgoing");

	/**
	 * Whether TLS will be used for network connections.
	 */
	public static final ConfigProperty<Boolean> TLS_ENABLED = new DefaultConfigProperty<>(Boolean.class,
			"s7s.net.connection.tls");

	/**
	 * Whether decoded network traffic will be logged.
	 */
	public static final ConfigProperty<Boolean> TRAFFIC_DECODED = new DefaultConfigProperty<>(Boolean.class,
			"s7s.net.logging.decoded");

	/**
	 * The traffic statistics interval.
	 */
	public static final ConfigProperty<Integer> TRAFFIC_INTERVAL = new DefaultConfigProperty<>(Integer.class,
			"s7s.net.connection.stat_interval");

	/**
	 * Whether raw network traffic will be logged.
	 */
	public static final ConfigProperty<Boolean> TRAFFIC_RAW = new DefaultConfigProperty<>(Boolean.class,
			"s7s.net.logging.raw");

	private CfgNet() {
	}
}
