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

package org.osgi.impl.service.rest.resources;

import org.osgi.impl.service.rest.PojoReflector;
import org.osgi.impl.service.rest.pojos.ExtensionList;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;

/**
 * The extension resource for extensions to the REST service.
 * 
 * @author Jan S. Rellermeyer, IBM Research
 */
public class ExtensionsResource extends AbstractOSGiResource<ExtensionList> {

	private static final MediaType	MEDIA_TYPE	= MediaType.valueOf("application/org.osgi.extensions");

	public ExtensionsResource() {
		super(PojoReflector.getReflector(ExtensionList.class), MEDIA_TYPE);
	}

	@Override
	public Representation get(final Variant variant) {
		try {
			return getRepresentation(new ExtensionList(getTracker()), variant);
		} catch (final Exception e) {
			return ERROR(e, variant);
		}
	}
}
