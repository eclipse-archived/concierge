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
package org.eclipse.concierge.example.service.clock.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.concierge.example.service.clock.ClockService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	@Override
	public void start(final BundleContext context) throws Exception {
		final Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(ClockService.IMPLEMENTATION, ClockService.SIMPLE_IMPL);
		context.registerService(ClockService.class, new ClockServiceImpl(), properties);
	}

	@Override
	public void stop(final BundleContext context) throws Exception {

	}

}
