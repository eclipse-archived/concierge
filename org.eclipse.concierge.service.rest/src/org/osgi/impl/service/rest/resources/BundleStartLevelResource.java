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

import org.osgi.framework.Bundle;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.impl.service.rest.PojoReflector;
import org.osgi.impl.service.rest.RestService;
import org.osgi.impl.service.rest.pojos.BundleStartLevelPojo;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;

/**
 * The bundle start level resource.
 * 
 * @author Jan S. Rellermeyer, IBM Research
 */
public class BundleStartLevelResource extends
		AbstractOSGiResource<BundleStartLevelPojo> {

	private static final MediaType	MEDIA_TYPE	= MediaType.valueOf("application/org.osgi.bundlestartlevel");

	public BundleStartLevelResource() {
		super(PojoReflector.getReflector(BundleStartLevelPojo.class), MEDIA_TYPE);
	}

	@Override
	public Representation get(final Variant variant) {
		try {
			final Bundle bundle = getBundleFromKeys(RestService.BUNDLE_ID_KEY);
			if (bundle == null) {
				return ERROR(Status.CLIENT_ERROR_NOT_FOUND);
			}
			final BundleStartLevel bsl = bundle.adapt(BundleStartLevel.class);
			final BundleStartLevelPojo sl = new BundleStartLevelPojo(bsl);
			return getRepresentation(sl, variant);
		} catch (final Exception e) {
			return ERROR(e, variant);
		}
	}

	@Override
	public Representation put(final Representation value,
			final Variant variant) {
		try {
			final Bundle bundle = getBundleFromKeys(RestService.BUNDLE_ID_KEY);
			if (bundle == null) {
				return ERROR(Status.CLIENT_ERROR_NOT_FOUND);
			}
			final BundleStartLevelPojo sl = fromRepresentation(value, value.getMediaType());
			final BundleStartLevel bsl = bundle.adapt(BundleStartLevel.class);
			bsl.setStartLevel(sl.getStartLevel());

			return getRepresentation(new BundleStartLevelPojo(bsl), variant);
		} catch (final Exception e) {
			return ERROR(e, variant);
		}
	}

}
