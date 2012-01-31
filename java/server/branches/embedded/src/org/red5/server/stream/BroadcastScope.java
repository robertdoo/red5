/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2012 by respective authors (see below). All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.red5.server.stream;


import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.red5.server.BasicScope;
import org.red5.server.api.IScope;
import org.red5.server.messaging.IConsumer;
import org.red5.server.messaging.IMessage;
import org.red5.server.messaging.IPipeConnectionListener;
import org.red5.server.messaging.IProvider;
import org.red5.server.messaging.InMemoryPushPushPipe;
import org.red5.server.messaging.OOBControlMessage;
import org.red5.server.messaging.PipeConnectionEvent;

/**
 * Scope type for publishing that deals with pipe connection events,
 * like async message listening in JMS
 */
public class BroadcastScope extends BasicScope implements IBroadcastScope, IPipeConnectionListener {

	/**
	 *  Simple in memory push pipe, triggered by an active provider to push messages to consumer
	 */
	private InMemoryPushPushPipe pipe;

	/**
	 *  Number of components.
	 */
	private AtomicInteger compCounter;

	/**
	 *  Remove flag
	 */
	private boolean hasRemoved;

	/**
	 * Lock for critical sections, to prevent concurrent modification. 
	 * A "fairness" policy is used wherein the longest waiting thread
	 * will be granted access before others.
	 */
	protected Lock lock = new ReentrantLock(true);

	/**
	 * Creates broadcast scope
	 * @param parent            Parent scope
	 * @param name              Scope name
	 */
	public BroadcastScope(IScope parent, String name) {
		super(parent, TYPE, name, false);
		pipe = new InMemoryPushPushPipe();
		pipe.addPipeConnectionListener(this);
		compCounter = new AtomicInteger(0);
		hasRemoved = false;
		keepOnDisconnect = true;
	}

	/**
	 * Register pipe connection event listener with this scope's pipe.
	 * A listener that wants to listen to events when
	 * provider/consumer connects to or disconnects from
	 * a specific pipe.
	 * @param listener         Pipe connection event listener
	 *
	 * @see org.red5.server.messaging.IPipeConnectionListener
	 */
	public void addPipeConnectionListener(IPipeConnectionListener listener) {
		pipe.addPipeConnectionListener(listener);
	}

	/**
	 * Unregisters pipe connection event listener with this scope's pipe
	 * @param listener         Pipe connection event listener
	 *
	 * @see org.red5.server.messaging.IPipeConnectionListener
	 */
	public void removePipeConnectionListener(IPipeConnectionListener listener) {
		pipe.removePipeConnectionListener(listener);
	}

	/**
	 * Pull message from pipe
	 * @return      Message object
	 *
	 * @see         org.red5.server.messaging.IMessage
	 */
	public IMessage pullMessage() {
		return pipe.pullMessage();
	}

	/**
	 * Pull message with timeout
	 * @param wait  Timeout
	 * @return      Message object
	 *
	 * @see         org.red5.server.messaging.IMessage
	 */
	public IMessage pullMessage(long wait) {
		return pipe.pullMessage(wait);
	}

	/**
	 * Connect scope's pipe to given consumer
	 *
	 * @param consumer       Consumer
	 * @param paramMap       Parameters passed with connection
	 * @return               <code>true</code> on success, <code>false</code> otherwise
	 */
	public boolean subscribe(IConsumer consumer, Map<String, Object> paramMap) {
		lock();
		try {
			return !hasRemoved && pipe.subscribe(consumer, paramMap);
		} finally {
			unlock();
		}
	}

	/**
	 * Disconnects scope's pipe from given consumer
	 * @param consumer       Consumer
	 * @return               <code>true</code> on success, <code>false</code> otherwise
	 */
	public boolean unsubscribe(IConsumer consumer) {
		return pipe.unsubscribe(consumer);
	}

	/**
	 * Getter for pipe consumers
	 * @return    Pipe consumers
	 */
	public List<IConsumer> getConsumers() {
		return pipe.getConsumers();
	}

	/**
	 * Send out-of-band ("special") control message
	 *
	 * @param consumer          Consumer, may be used in concrete implementations
	 * @param oobCtrlMsg        Out-of-band control message
	 */
	public void sendOOBControlMessage(IConsumer consumer, OOBControlMessage oobCtrlMsg) {
		pipe.sendOOBControlMessage(consumer, oobCtrlMsg);
	}

	/**
	 * Push a message to this output endpoint. May block
	 * the pusher when output can't handle the message at
	 * the time.
	 * @param message Message to be pushed.
	 * @throws IOException If message could not be pushed.
	 */
	public void pushMessage(IMessage message) throws IOException {
		pipe.pushMessage(message);
	}

	/**
	 * Connect scope's pipe with given provider
	 * @param provider         Provider
	 * @param paramMap         Parameters passed on connection
	 * @return                 <code>true</code> on success, <code>false</code> otherwise
	 */
	public boolean subscribe(IProvider provider, Map<String, Object> paramMap) {
		lock();
		try {
			return !hasRemoved && pipe.subscribe(provider, paramMap);
		} finally {
			unlock();
		}
	}

	/**
	 * Disconnects scope's pipe from given provider
	 * @param provider         Provider
	 * @return                 <code>true</code> on success, <code>false</code> otherwise
	 */
	public boolean unsubscribe(IProvider provider) {
		lock();
		try {
			return pipe.unsubscribe(provider);
		} finally {
			unlock();
		}
	}

	/**
	 * Getter for providers list
	 * @return    List of providers
	 */
	public List<IProvider> getProviders() {
		return pipe.getProviders();
	}

	/**
	 * Send out-of-band ("special") control message
	 *
	 * @param provider          Provider, may be used in concrete implementations
	 * @param oobCtrlMsg        Out-of-band control message
	 */
	public void sendOOBControlMessage(IProvider provider, OOBControlMessage oobCtrlMsg) {
		pipe.sendOOBControlMessage(provider, oobCtrlMsg);
	}

	/**
	 * Pipe connection event handler
	 * @param event              Pipe connection event
	 */
	public void onPipeConnectionEvent(PipeConnectionEvent event) {
		// Switch event type
		switch (event.getType()) {
			case PipeConnectionEvent.CONSUMER_CONNECT_PULL:
			case PipeConnectionEvent.CONSUMER_CONNECT_PUSH:
			case PipeConnectionEvent.PROVIDER_CONNECT_PULL:
			case PipeConnectionEvent.PROVIDER_CONNECT_PUSH:
				compCounter.incrementAndGet();
				break;

			case PipeConnectionEvent.CONSUMER_DISCONNECT:
			case PipeConnectionEvent.PROVIDER_DISCONNECT:
				if (compCounter.decrementAndGet() <= 0) {
					// XXX should we synchronize parent before removing?
					if (hasParent()) {
						IProviderService providerService = (IProviderService) getParent().getContext().getBean(IProviderService.BEAN_NAME);
						providerService.unregisterBroadcastStream(getParent(), getName());
					}
					hasRemoved = true;
				}
				break;
			default:
				throw new UnsupportedOperationException("Event type not supported: " + event.getType());
		}
	}

	public void lock() {
		lock.lock();
	}

	public void unlock() {
		lock.unlock();
	}

}
