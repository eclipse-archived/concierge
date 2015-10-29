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

package org.osgi.impl.service.rest.client;

import java.util.Hashtable;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.rest.client.RestClientFactory;

public class Activator implements BundleActivator {

	/**
	 * Bundle activator for the REST client RI
	 * 
	 * @author Jan S. Rellermeyer, IBM Research
	 */
	public void start(final BundleContext context) throws Exception {
		final Hashtable<String, Object> props = new Hashtable<String, Object>();
		props.put(RestClientFactoryImpl.MSG_FORMAT, RestClientFactoryImpl.MSG_FORMAT_JSON);
		context.registerService(RestClientFactory.class, new RestClientFactoryImpl(false), props);

		props.put(RestClientFactoryImpl.MSG_FORMAT, RestClientFactoryImpl.MSG_FORMAT_XML);
		context.registerService(RestClientFactory.class, new RestClientFactoryImpl(true), props);
	}

	public void stop(final BundleContext context) throws Exception {
		// nop
	}

}
