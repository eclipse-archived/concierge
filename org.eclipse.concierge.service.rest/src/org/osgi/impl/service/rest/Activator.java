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

package org.osgi.impl.service.rest;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.restlet.Component;
import org.restlet.data.Protocol;

/**
 * Bundle activator for the REST service RI
 * 
 * @author Jan S. Rellermeyer, IBM Research
 */
public class Activator implements BundleActivator {

	private Component	component;

	public void start(final BundleContext context) throws Exception {
		component = new Component();
		component.getServers().add(Protocol.HTTP, 8888);
		component.getClients().add(Protocol.CLAP);
		component.getDefaultHost().attach("", new RestService(context));
		component.start();
	}

	public void stop(final BundleContext context) throws Exception {
		component.stop();
		component = null;
	}

}
