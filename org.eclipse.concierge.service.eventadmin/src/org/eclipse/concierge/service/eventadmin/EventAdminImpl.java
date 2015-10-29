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
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.TopicPermission;

/**
 * EventAdmin backport for OSGi R3 frameworks.
 * 
 * @author Jan S. Rellermeyer, ETH Zurich
 */
final class EventAdminImpl implements FrameworkListener, BundleListener,
		ServiceListener, EventAdmin {

	/**
	 * strings for framework events.
	 */
	private static final String[] FRAMEWORK_EVENT = { "STARTED", "ERROR",
			"PACKAGES_REFRESHED", "STARTLEVEL_CHANGED", "WARNING" };

	/**
	 * strings for bundle events.
	 */
	private static final String[] BUNDLE_EVENT = { "INSTALLED", "STARTED",
			"STOPPED", "UPDATED", "UNINSTALLED", "RESOLVED", "UNRESOLVED" };

	/**
	 * strings for service events.
	 */
	private static final String[] SERVICE_EVENT = { "REGISTERED", "MODIFIED",
			"UNREGISTERING" };

	/**
	 * queue for asynchronous event delivery.
	 */
	private List eventQueue = new ArrayList(2);

	/**
	 * event handler subscribrions.
	 */
	private HashMap eventHandlerSubscriptions = new HashMap(2);

	/**
	 * the security manager.
	 */
	static SecurityManager security;

	/**
	 * thread variable.
	 */
	boolean running = true;

	/**
	 * create a new EventAdminImpl instance.
	 */
	public EventAdminImpl() {
		security = System.getSecurityManager();

		// bootstrapping: find all registered event handlers
		ServiceReference[] refs = null;
		try {
			refs = EventAdminActivator.context.getServiceReferences(
					EventHandler.class.getName(), null);
		} catch (InvalidSyntaxException e) {
			// does not happen
			e.printStackTrace();
		}

		if (refs != null) {
			for (int i = 0; i < refs.length; i++) {
				try {
					final Object handler = EventAdminActivator.context
							.getService(refs[i]);
					if (handler == this) {
						continue;
					}
					final String[] topics = (String[]) refs[i]
							.getProperty(EventConstants.EVENT_TOPIC);
					final String filter = (String) refs[i]
							.getProperty(EventConstants.EVENT_FILTER);
					final Filter filterObj = filter != null ? EventAdminActivator.context
							.createFilter(filter)
							: null;
					eventHandlerSubscriptions.put(refs[i]
							.getProperty(Constants.SERVICE_ID),
							new Subscription((EventHandler) handler, topics,
									filterObj));
				} catch (InvalidSyntaxException e) {
					e.printStackTrace();
				}
			}
		}

		new EventDispatchingThread().start();
	}

	/**
	 * receive a <code>FrameworkEvent</code>.
	 * 
	 * @param fEvent
	 *            the framework event.
	 * @see org.osgi.framework.FrameworkListener#frameworkEvent(org.osgi.framework.FrameworkEvent)
	 */
	public void frameworkEvent(final FrameworkEvent fEvent) {
		Dictionary props = new Hashtable();
		props.put(EventConstants.EVENT, fEvent);
		props.put(EventConstants.TIMESTAMP,
				new Long(System.currentTimeMillis()));
		final Bundle bundle;
		if ((bundle = fEvent.getBundle()) != null) {
			props.put("bundle.id", new Long(bundle.getBundleId()));
			props.put(EventConstants.BUNDLE_SYMBOLICNAME, "null");
			props.put("bundle", bundle);
		}
		final Throwable throwable;
		if ((throwable = fEvent.getThrowable()) != null) {
			props.put(EventConstants.EXECPTION_CLASS, throwable.getClass()
					.getName());
			props.put(EventConstants.EXCEPTION_MESSAGE, throwable.getMessage());
			props.put(EventConstants.EXCEPTION, throwable);

		}
		int t = log2(fEvent.getType());
		String type = t < 5 ? FRAMEWORK_EVENT[t] : "UNDEFINED";
		Event event = new Event("org/osgi/framework/FrameworkEvent/" + type,
				props);
		postEvent(event);
	}

	/**
	 * receive a <code>BundleEvent</code>.
	 * 
	 * @param bEvent
	 *            the bundle event.
	 * @see org.osgi.framework.BundleListener#bundleChanged(org.osgi.framework.BundleEvent)
	 */
	public void bundleChanged(final BundleEvent bEvent) {
		Dictionary props = new Hashtable();
		props.put(EventConstants.TIMESTAMP,
				new Long(System.currentTimeMillis()));
		Bundle bundle = bEvent.getBundle();
		props.put(EventConstants.EVENT, bEvent);
		props.put("bundle.id", new Long(bundle.getBundleId()));
		props.put("bundle", bundle);
		int t = log2(bEvent.getType());
		String type = t < 7 ? BUNDLE_EVENT[t] : "UNDEFINED";
		Event event = new Event("org/osgi/framework/BundleEvent/" + type, props);
		postEvent(event);
	}

	/**
	 * receive a <code>ServiceEvent</code>.
	 * 
	 * @param sEvent
	 *            the service event.
	 * @see org.osgi.framework.ServiceListener#serviceChanged(org.osgi.framework.ServiceEvent)
	 */
	public void serviceChanged(final ServiceEvent sEvent) {
		/*
		 * check if an event handler is affected, in this case, we have to
		 * update our list of known event handlers
		 */
		try {
			ServiceReference ref = sEvent.getServiceReference();
			final List objClasses = Arrays.asList((String[]) ref
					.getProperty("objectClass"));
			if (objClasses.contains("org.osgi.service.event.EventHandler")) {

				final Long serviceID = (Long) ref
						.getProperty(Constants.SERVICE_ID);

				final Object handler = EventAdminActivator.context
						.getService(ref);

				switch (sEvent.getType()) {
				case ServiceEvent.REGISTERED: {
					final String[] topics = (String[]) ref
							.getProperty(EventConstants.EVENT_TOPIC);
					final String filter = (String) ref
							.getProperty(EventConstants.EVENT_FILTER);
					final Filter filterObj = filter != null ? EventAdminActivator.context
							.createFilter(filter)
							: null;
					Subscription ehandler = new Subscription(
							(EventHandler) handler, topics, filterObj);
					eventHandlerSubscriptions.put(serviceID, ehandler);
					break;
				}
				case ServiceEvent.UNREGISTERING: {
					eventHandlerSubscriptions.remove(serviceID);
					EventAdminActivator.context.ungetService(ref);
					break;
				}
				case ServiceEvent.MODIFIED: {
					final Subscription subscr = (Subscription) eventHandlerSubscriptions
							.get(serviceID);
					if (subscr != null) {
						final String[] topics = (String[]) ref
								.getProperty(EventConstants.EVENT_TOPIC);
						final String filter = (String) ref
								.getProperty(EventConstants.EVENT_FILTER);
						final Filter filterObj = filter != null ? EventAdminActivator.context
								.createFilter(filter)
								: null;
						subscr.update(topics, filterObj);
					}
				}
				default:
				}
			}
			Dictionary props = new Hashtable();
			props.put(EventConstants.EVENT, sEvent);
			props.put(EventConstants.TIMESTAMP, new Long(System
					.currentTimeMillis()));
			props.put(EventConstants.SERVICE, sEvent.getServiceReference());
			int t = log2(sEvent.getType());
			String type = t < 4 ? SERVICE_EVENT[t] : "UNDEFINED";
			Event event = new Event("org/osgi/framework/ServiceEvent/" + type,
					props);
			postEvent(event);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * send an event asynchronously.
	 * 
	 * @param event
	 *            the Event.
	 * 
	 * @see org.osgi.service.event.EventAdmin#postEvent(org.osgi.service.event.Event)
	 */
	public void postEvent(final Event event) {
		if (security != null) {
			// TODO: cache permissions
			security.checkPermission(new TopicPermission(event.getTopic(),
					TopicPermission.PUBLISH));
		}

		final Subscription[] subscriptions = (Subscription[]) eventHandlerSubscriptions.values()
				.toArray(new Subscription[eventHandlerSubscriptions.size()]);
		final ArrayList handlers = new ArrayList(subscriptions.length);
		for (int i = 0; i < subscriptions.length; i++) {
			if (subscriptions[i].matches(event)) {
				handlers.add(subscriptions[i].getHandler());
			}
		}

		synchronized (eventQueue) {
			eventQueue.add(new QueueElement(event, (EventHandler[]) handlers
					.toArray(new EventHandler[handlers.size()])));
			eventQueue.notifyAll();
		}
	}

	/**
	 * send an event synchronously.
	 * 
	 * @param event
	 *            the event.
	 * @see org.osgi.service.event.EventAdmin#sendEvent(org.osgi.service.event.Event)
	 */
	public void sendEvent(final Event event) {
		if (security != null) {
			// TODO: cache permissions
			security.checkPermission(new TopicPermission(event.getTopic(),
					TopicPermission.PUBLISH));
		}

		final Subscription[] subscriptions = (Subscription[]) eventHandlerSubscriptions.values()
				.toArray(new Subscription[eventHandlerSubscriptions.size()]);
		for (int i = 0; i < subscriptions.length; i++) {
			if (subscriptions[i].matches(event)) {
				subscriptions[i].sendEvent(event);
			}
		}
	}

	/**
	 * get the logarithm with base 2.
	 * 
	 * @param num
	 *            the value.
	 * @return the logarithm.
	 */
	private static int log2(final int num) {
		int i = num;
		int j = -1;
		while (i > 0) {
			i = i >> 1;
			j++;
		}
		return j;
	}

	/**
	 * EventDispatchingThread dispatches events on the local framework.
	 */
	private final class EventDispatchingThread extends Thread {
		/**
		 * creates and starts a new EventDispatchingThread.
		 */
		private EventDispatchingThread() {
			setDaemon(true);
		}

		/**
		 * thread loop.
		 * 
		 * @see java.lang.Thread#run()
		 */
		public void run() {
			try {
				while (running) {
					synchronized (eventQueue) {
						if (eventQueue.isEmpty()) {
							// wait until something arrives
							eventQueue.wait();
						}
						// get the element and deliver it
						QueueElement element = (QueueElement) eventQueue
								.remove(0);
						final EventHandler[] handlers = element.handlers;
						final Event event = element.event;
						try {
							for (int i = 0; i < handlers.length; i++) {
								handlers[i].handleEvent(event);
							}
						} catch (Throwable t) {
							t.printStackTrace();
						}
					}
				}
			} catch (InterruptedException ie) {
				ie.printStackTrace();
			}
		}
	}

	/**
	 * Queue element.
	 */
	private static final class QueueElement {

		/**
		 * the event.
		 */
		final Event event;

		/**
		 * the handlers
		 */
		final EventHandler[] handlers;

		/**
		 * create a new QueueElement.
		 * 
		 * @param event
		 *            the event.
		 * @param handlers
		 *            the handlers.
		 */
		private QueueElement(final Event event, final EventHandler[] handlers) {
			this.event = event;
			this.handlers = handlers;
		}
	}
}
