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

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.EventAdmin;

public class EventAdminActivator implements BundleActivator {

	/**
	 * 
	 */
	static BundleContext context;

	/**
	 * 
	 */
	private EventAdminImpl eventAdmin;

	/**
	 * 
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		EventAdminActivator.context = context;
		eventAdmin = new EventAdminImpl();
		context.addBundleListener(eventAdmin);
		context.addServiceListener(eventAdmin);
		context.addFrameworkListener(eventAdmin);
		context.registerService(EventAdmin.class.getName(), eventAdmin, null);
	}

	/**
	 * 
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		context.removeBundleListener(eventAdmin);
		context.removeServiceListener(eventAdmin);
		context.removeFrameworkListener(eventAdmin);
		eventAdmin.running = false;
		EventAdminActivator.context = null;
	}

}
