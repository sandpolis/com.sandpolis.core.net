//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.net.network;

import static com.sandpolis.core.net.connection.ConnectionStore.ConnectionStore;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.google.common.graph.MutableNetwork;
import com.google.common.graph.Network;
import com.google.common.graph.NetworkBuilder;
import com.sandpolis.core.foundation.ConfigStruct;
import com.sandpolis.core.instance.Entrypoint;
import com.sandpolis.core.instance.Metatypes.InstanceType;
import com.sandpolis.core.instance.state.ConnectionOid;
import com.sandpolis.core.instance.state.st.STDocument;
import com.sandpolis.core.instance.store.ConfigurableStore;
import com.sandpolis.core.instance.store.STCollectionStore;
import com.sandpolis.core.net.Message.MSG;
import com.sandpolis.core.net.config.CfgNet;
import com.sandpolis.core.net.connection.Connection;
import com.sandpolis.core.net.connection.ConnectionStore;
import com.sandpolis.core.net.connection.ConnectionStore.SockEstablishedEvent;
import com.sandpolis.core.net.connection.ConnectionStore.SockLostEvent;
import com.sandpolis.core.net.message.MessageFuture;
import com.sandpolis.core.net.network.NetworkStore.NetworkStoreConfig;
import com.sandpolis.core.net.util.CvidUtil;

/**
 * {@link NetworkStore} manages "logical" connections between any two instances
 * in the network.
 *
 * <p>
 * Internally, this class uses a graph to represent connections and is therefore
 * compatible with standard graph algorithms.
 *
 * @see ConnectionStore
 * @since 5.0.0
 */
public final class NetworkStore extends STCollectionStore<Connection> implements ConfigurableStore<NetworkStoreConfig> {

	@ConfigStruct
	public static final class NetworkStoreConfig {

		public int cvid;
		public int preferredServer;
		public STDocument collection;

	}

	public static final record ServerLostEvent(int cvid) {
	}

	public static final record ServerEstablishedEvent(int cvid) {
	}

	public static final record CvidChangedEvent(int cvid) {
	}

	private static final Logger log = LoggerFactory.getLogger(NetworkStore.class);

	public static final NetworkStore NetworkStore = new NetworkStore();

	/**
	 * The undirected graph which describes the visible connections between nodes on
	 * the network.
	 */
	private MutableNetwork<Integer, Connection> network;

	/**
	 * The CVID of the preferred server on the network.
	 */
	private int preferredServer;

	/**
	 * The CVID of this instance.
	 */
	private int cvid;

	/**
	 * @return This instance's CVID
	 */
	public int cvid() {
		return cvid;
	}

	public void setCvid(int newCvid) {
		if (cvid != 0) {
			network.removeNode(cvid);
		}

		cvid = newCvid;

		network.addNode(newCvid);
		post(new CvidChangedEvent(newCvid));
	}

	public NetworkStore() {
		super(log, Connection::new);
	}

	/**
	 * Transmit a message into the network, taking a path through the preferred
	 * server.
	 *
	 * @param message The message
	 * @return The next hop
	 */
	public int deliver(MSG message) {
		int next = getPreferredServer().orElseThrow();
		ConnectionStore.getByCvid(next).get().send(message);
		return next;
	}

	/**
	 * Transmit a message into the network, taking a path through the preferred
	 * server.
	 *
	 * @param message The message
	 * @return The next hop
	 */
	public int deliver(MSG.Builder message) {
		return deliver(message.build());
	}

	/**
	 * Get the CVIDs of every node directly connected to the given CVID.
	 *
	 * @param cvid The CVID
	 * @return A set of all directly connected CVIDs
	 */
	public synchronized Set<Integer> getDirect(int cvid) {
		return network.adjacentNodes(cvid);
	}

	/**
	 * Get all links involving the given CVID.
	 *
	 * @param cvid The CVID
	 * @return A set of all links involving the CVID
	 */
	public synchronized Set<Connection> getDirectLinks(int cvid) {
		return network.incidentEdges(cvid);
	}

	/**
	 * Update the network tree with the given delta. If the result of an operation
	 * is already present in the network (e.g. a node is already present and the
	 * operation is NodeAdd), then the operation is ignored.
	 *
	 * @param delta The delta event that describes the change
	 */
//	public synchronized void updateNetwork(EV_NetworkDelta delta) {
//		for (NodeAdded na : delta.getNodeAddedList())
//			network.addNode(na.getCvid());
//		for (NodeRemoved nr : delta.getNodeRemovedList())
//			network.removeNode(nr.getCvid());
//
//		for (LinkAdded la : delta.getLinkAddedList())
//			network.addEdge(la.getCvid1(), la.getCvid2(), new SockLink(la.getLink()));
//		for (LinkRemoved lr : delta.getLinkRemovedList())
//			network.removeEdge(network.edgeConnectingOrNull(lr.getCvid1(), lr.getCvid2()));
//	}

	/**
	 * Get all links involving both given CVIDs.
	 *
	 * @param cvid1 The first CVID
	 * @param cvid2 The second CVID
	 * @return A set of all links between the two CVIDs
	 */
	public synchronized Set<Connection> getDirectLinks(int cvid1, int cvid2) {
		return network.edgesConnecting(cvid1, cvid2);
	}

	/**
	 * Get an immutable representation of the network.
	 *
	 * @return The underlying network graph of the store
	 */
	public Network<Integer, Connection> getNetwork() {
		return network;
	}

	public synchronized Optional<Integer> getPreferredServer() {

		if (!network.nodes().contains(preferredServer)) {
			// Choose a server at random
			var newServer = network.nodes().stream()
					.filter(cvid -> CvidUtil.extractInstance(cvid) == InstanceType.SERVER).findAny();
			if (newServer.isPresent()) {
				preferredServer = newServer.get();
				return newServer;
			} else {
				return Optional.empty();
			}
		}

		return Optional.of(preferredServer);
	}

	@Override
	public void init(Consumer<NetworkStoreConfig> configurator) {
		var config = new NetworkStoreConfig();
		configurator.accept(config);

		preferredServer = config.preferredServer;
		network = NetworkBuilder.undirected().allowsSelfLoops(false).allowsParallelEdges(true).build();

		if (config.cvid != 0) {
			cvid = config.cvid;
			network.addNode(config.cvid);
		}

		ConnectionStore.register(this);
		post(new CvidChangedEvent(config.cvid));
	}

	@Subscribe
	private synchronized void onSockEstablished(SockEstablishedEvent event) {

		var remote_cvid = event.connection().get(ConnectionOid.REMOTE_CVID).asInt();

		// Add node if not already present
		if (!network.nodes().contains(remote_cvid)) {
			log.debug("Adding node: {} ({})", remote_cvid, CvidUtil.extractInstance(remote_cvid));
			network.addNode(remote_cvid);
		}

		// Add edge representing the new connection
		network.addEdge(cvid(), remote_cvid, event.connection());

		if (Entrypoint.data().instance() != InstanceType.SERVER) {
			// See if that was the first connection to a server
			if (event.connection().get(ConnectionOid.REMOTE_INSTANCE).asInstanceType() == InstanceType.SERVER) {
				// TODO
				postAsync(new ServerEstablishedEvent(remote_cvid));
			}
		}
	}

	@Subscribe
	private synchronized void onSockLost(SockLostEvent event) {
		if (network.nodes().contains(cvid())
				&& network.nodes().contains(event.connection().get(ConnectionOid.REMOTE_CVID).asInt()))
			network.edgeConnecting(cvid(), event.connection().get(ConnectionOid.REMOTE_CVID).asInt())
					.ifPresent(network::removeEdge);

		// Remove nodes that are now disconnected
		network.nodes().stream().filter(cvid -> cvid() != cvid).filter(cvid -> network.degree(cvid) == 0)
				.collect(Collectors.toUnmodifiableList()).forEach(network::removeNode);

		if (Entrypoint.data().instance() != InstanceType.SERVER) {
			// Check whether a server is still reachable after losing the connection
			for (var node : network.nodes()) {
				if (CvidUtil.extractInstance(node) == InstanceType.SERVER) {
					if (network.edgesConnecting(node, event.connection().get(ConnectionOid.REMOTE_CVID).asInt())
							.size() > 0) {
						return;
					}
				}
			}

			// No servers are reachable
			postAsync(new ServerLostEvent(event.connection().get(ConnectionOid.REMOTE_CVID).asInt()));
		}
	}

	/**
	 * Receive a message from the given source.
	 *
	 * @param cvid The message source
	 * @param id   The response ID
	 * @return A MessageFuture
	 */
	public MessageFuture receive(int cvid, int id) {
		var sock = ConnectionStore.getByCvid(cvid);
		if (sock.isEmpty())
			return null;

		return sock.get().read(id);
	}

	/**
	 * Receive a message from the given source.
	 *
	 * @param cvid    The message source
	 * @param id      The message ID
	 * @param timeout The message timeout
	 * @param unit    The timeout unit
	 * @return A MessageFuture
	 */
	public MessageFuture receive(int cvid, int id, int timeout, TimeUnit unit) {
		var sock = ConnectionStore.getByCvid(cvid);
		if (sock.isEmpty())
			return null;

		return sock.get().read(id, timeout, unit);
	}

	/**
	 * Transmit a message into the network, taking the most direct path.
	 *
	 * @param message The message
	 * @return The next hop
	 */
	public synchronized int route(MSG message) {
		if (network.adjacentNodes(cvid()).contains(message.getTo())) {
			ConnectionStore.getByCvid(message.getTo()).get().send(message);
			return message.getTo();
		} else {
			return deliver(message);
		}
	}

	/**
	 * Transmit a message into the network, taking the most direct path.
	 *
	 * @param message The message
	 * @return The next hop
	 */
	public int route(MSG.Builder message) {
		return route(message.build());
	}

	/**
	 * Transmit a message into the network, taking the most direct path.<br>
	 * <br>
	 * Implementation note: this method cannot use {@link #route(MSG)} because it
	 * must place the receive request before actually sending the message. (To avoid
	 * missing a message that is received extremely quickly).
	 *
	 * @param message      The message
	 * @param timeoutClass The message timeout class
	 * @return The next hop
	 */
	public MessageFuture route(MSG.Builder message, String timeoutClass) {
		int next;
		// TODO use timeout class

		// Search adjacent nodes first
		if (network.adjacentNodes(cvid()).contains(message.getTo())) {
			next = message.getTo();
		}

		// Try preferred server
		else {
			next = getPreferredServer().orElseThrow();
		}

		MessageFuture mf = receive(next, message.getId(), CfgNet.MESSAGE_TIMEOUT.value().get(), TimeUnit.MILLISECONDS);
		ConnectionStore.getByCvid(next).get().send(message);
		return mf;
	}

	/**
	 * Explicitly set the preferred server CVID.
	 *
	 * @param cvid The new preferred server
	 */
	public void setPreferredServer(int cvid) {
		preferredServer = cvid;
	}
}
