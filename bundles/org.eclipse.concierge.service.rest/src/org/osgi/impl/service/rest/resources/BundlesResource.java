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

import java.util.UUID;
import org.osgi.framework.Bundle;
import org.osgi.impl.service.rest.PojoReflector;
import org.osgi.impl.service.rest.pojos.BundlePojoList;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.representation.Variant;

/**
 * The bundles resource, a list of all bundle paths.
 * 
 * @author Jan S. Rellermeyer, IBM Research
 */
public class BundlesResource extends AbstractOSGiResource<BundlePojoList> {

	private static final MediaType	MEDIA_TYPE	= MediaType.valueOf("application/org.osgi.bundles");

	public BundlesResource() {
		super(PojoReflector.getReflector(BundlePojoList.class), MEDIA_TYPE);
	}

	@Override
	public Representation get(final Variant variant) {
		try {
			final Representation rep = getRepresentation(new BundlePojoList(
					getBundles()), variant);
			return rep;
		} catch (final Exception e) {
			return ERROR(e, variant);
		}
	}

	@Override
	public Representation post(final Representation content,
			final Variant variant) {
		try {
			if (MediaType.TEXT_PLAIN.equals(content.getMediaType())) {
				final String uri = content.getText();
				if (getBundleContext().getBundle(uri) != null) {
					ERROR(Status.CLIENT_ERROR_CONFLICT);
				}

				final Bundle bundle = getBundleContext().installBundle(uri);

				return new StringRepresentation("framework/bundle/"
						+ bundle.getBundleId());
			}

			final Form headers = (Form)
					getRequestAttributes().get("org.restlet.http.headers");
			String location =
					headers.getFirstValue("Content-Location");

			if (location != null) {
				if (getBundleContext().getBundle(location) != null) {
					// conflict detected
					return ERROR(Status.CLIENT_ERROR_CONFLICT);
				}
			} else {
				location = UUID.randomUUID().toString();
			}

			final Bundle bundle = getBundleContext().installBundle(location,
					content.getStream());
			return new StringRepresentation("framework/bundle/"
					+ bundle.getBundleId());
		} catch (final Exception e) {
			return ERROR(e, variant);
		}
	}

}
