/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Jan S. Rellermeyer, IBM Research - initial API and implementation
 *******************************************************************************/
package org.eclipse.concierge.service.eventadmin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;
import org.osgi.framework.Filter;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.TopicPermission;

/**
 * <p>
 * encapsulated the mapping between an {@link EventHandler} that has subscribed
 * and the topics it is interested it. Optionally, a {@link Filter} can be
 * provided that will be checked against the properties of incoming events.
 * </p>
 * 
 * @author Jan S. Rellermeyer, ETH Zurich
 */
final class Subscription {
	/**
	 * the <code>EventHandler</code>.
	 */
	private EventHandler handler;

	/**
	 * an array of topics.
	 */
	private String[] topics;

	/**
	 * a filter.
	 */
	private Filter filter;

	/**
	 * hidden default constructor.
	 */
	private Subscription() {
	}

	/**
	 * creates a new EventHandlerSubscription instance.
	 * 
	 * @param handler
	 *            an <code>EventHandler</code> that wants to subscribe.
	 * @param topics
	 *            an array of strings representing the topics.
	 * @param filter
	 *            a <code>Filter</code> for matching event properties.
	 */
	Subscription(final EventHandler eventHandler, final String[] topics,
			final Filter filter) {
		// security check
		if (EventAdminImpl.security != null) {
			ArrayList checkedTopics = new ArrayList(topics.length);
			for (int i = 0; i < topics.length; i++) {
				try {
					EventAdminImpl.security
							.checkPermission(new TopicPermission(topics[i],
									TopicPermission.SUBSCRIBE));
					checkedTopics.add(topics[i]);
				} catch (SecurityException se) {
					System.err
							.println("Bundle does not have permission for subscribing to "
									+ topics[i]);
				}
			}
			this.topics = (String[]) checkedTopics
					.toArray(new String[checkedTopics.size()]);
		} else {
			this.topics = topics;
		}
		this.handler = eventHandler;
		this.filter = filter;
	}

	/**
	 * sends an <code>Event</code> to the <code>EventHandler</code>.
	 * 
	 * @param event
	 *            the <code>Event</code>.
	 */
	void sendEvent(final Event event) {
		try {
			handler.handleEvent(event);
		} catch (Exception shield) {
			shield.printStackTrace();
		}
	}

	/**
	 * get the handler.
	 * 
	 * @return the handler.
	 */
	EventHandler getHandler() {
		return handler;
	}

	/**
	 * checks if an event matches the subscribed topics and the filter, if
	 * present.
	 * 
	 * @param event
	 *            the <code>Event</code>
	 * @return <code>true</code> for the case that the event matches,
	 *         <code>false</code> otherwise.
	 */
	boolean matches(final Event event) {
		if (topics != null && !stringMatch(topics, event.getTopic())) {
			return false;
		}
		if (filter != null && event.matches(filter)) {
			return false;
		}
		return true;
	}

	/**
	 * checks if a string matches a pattern string. Either, the two strings must
	 * be identical or the pattern must contain a wildcard and imply the topic.
	 * 
	 * @param pattern
	 *            a pattern string. E.g. <code>ch/ethz/iks/*</code>.
	 * @param topic
	 *            a topic. E.g. <code>ch/ethz/iks/SAMPLE_TOPIC</code>.
	 * @return <code>true</code> if the topic matches the pattern string,
	 *         <code>false</code> otherwise.
	 */
	private static boolean stringMatch(final String pattern, final String topic) {
		final StringTokenizer strTokens = new StringTokenizer(pattern, "/");
		final StringTokenizer topicTokens = new StringTokenizer(topic, "/");
		while (strTokens.hasMoreTokens()) {
			String current = strTokens.nextToken();
			if (!topicTokens.hasMoreTokens()) {
				return false;
			}
			if (current.equals("*") && !strTokens.hasMoreTokens()) {
				return true;
			}
			if (!current.equals(topicTokens.nextToken())) {
				return false;
			}
		}
		if (topicTokens.hasMoreTokens()) {
			return false;
		}
		return true;
	}

	/**
	 * checks if a string matches one of the pattern strings.
	 * 
	 * @param patterns
	 *            an array of pattern strings.
	 * @param topic
	 *            a topic. E.g. <code>ch.ethz.iks.SampleTopic</code>.
	 * @return <code>true</code> if the topic matches the pattern string,
	 *         <code>false</code> otherwise.
	 */
	private static boolean stringMatch(final String[] patterns,
			final String topic) {
		for (int i = 0; i < patterns.length; i++) {
			if (stringMatch(patterns[i], topic)) {
				return true;
			}
		}
		return false;
	}

	void update(final String[] topics, final Filter filter) {
		this.topics = topics;
		this.filter = filter;
	}

	/**
	 * get a string representation of the <code>EventHandlerSubscription</code>.
	 * 
	 * @return a <code>String</code> representation.
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("[EventHandlerSubscription] ");
		buffer.append(handler.getClass().getName());
		buffer.append(", topics ");
		if (topics != null) {
			buffer.append(Arrays.asList(topics));
		} else {
			buffer.append("*");
		}
		if (filter != null) {
			buffer.append(", filter '");
			buffer.append(filter);
			buffer.append("'");
		}
		return buffer.toString();
	}
}
