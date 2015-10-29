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
import org.osgi.impl.service.rest.pojos.BundleRepresentationsList;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;

/**
 * The bundle representations resource, listing the full representations of all
 * bundles.
 * 
 * @author Jan S. Rellermeyer, IBM Research
 */
public class BundleRepresentationsResource extends
		AbstractOSGiResource<BundleRepresentationsList> {

	private static final MediaType	MEDIA_TYPE	= MediaType.valueOf("application/org.osgi.bundles.representations");

	public BundleRepresentationsResource() {
		super(PojoReflector.getReflector(BundleRepresentationsList.class), MEDIA_TYPE);
	}

	@Override
	public Representation get(final Variant variant) {
		try {
			final Representation rep = getRepresentation(
					new BundleRepresentationsList(getBundles()), variant);
			return rep;
		} catch (final Exception e) {
			return ERROR(e, variant);
		}
	}

}
