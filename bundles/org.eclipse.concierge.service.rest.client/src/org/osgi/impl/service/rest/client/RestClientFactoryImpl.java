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

import java.net.URI;
import org.osgi.service.rest.client.RestClient;
import org.osgi.service.rest.client.RestClientFactory;

/**
 * Simple implementation of a REST client factory.
 * 
 * @author Jan S. Rellermeyer, IBM Research
 */
public class RestClientFactoryImpl implements RestClientFactory {

	public static final String	MSG_FORMAT		= "msg.format";
	public static final String	MSG_FORMAT_JSON	= "json";
	public static final String	MSG_FORMAT_XML	= "xml";

	private final boolean		useXml;

	/**
	 * creates a new rest client factory implementation
	 * 
	 * @param useXml use XML as the message format (if false, use JSON)
	 */
	public RestClientFactoryImpl(final boolean useXml) {
		this.useXml = useXml;
	}

	/**
	 * @see org.osgi.service.rest.client.RestClientFactory#createRestClient(java.net.URI)
	 */
	public RestClient createRestClient(final URI uri) {
		return new RestClientImpl(uri, useXml);
	}

}
