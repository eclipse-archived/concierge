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
import org.osgi.impl.service.rest.PojoReflector;
import org.osgi.impl.service.rest.RestService;
import org.osgi.impl.service.rest.pojos.BundleStatePojo;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;

/**
 * The bundle state resource.
 * 
 * @author Jan S. Rellermeyer, IBM Research
 */
public class BundleStateResource extends AbstractOSGiResource<BundleStatePojo> {

	private static final MediaType	MEDIA_TYPE	= MediaType.valueOf("application/org.osgi.bundlestate");

	public BundleStateResource() {
		super(PojoReflector.getReflector(BundleStatePojo.class), MEDIA_TYPE);
	}

	@Override
	public Representation get(final Variant variant) {
		try {
			final Bundle bundle = getBundleFromKeys(RestService.BUNDLE_ID_KEY);
			if (bundle == null) {
				setStatus(Status.CLIENT_ERROR_NOT_FOUND);
				return null;
			}
			return getRepresentation(new BundleStatePojo(bundle.getState()),
					variant);
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
				setStatus(Status.CLIENT_ERROR_NOT_FOUND);
				return null;
			}

			final BundleStatePojo targetState = fromRepresentation(value,
					value.getMediaType());

			if (bundle.getState() == Bundle.UNINSTALLED) {
				return ERROR(Status.CLIENT_ERROR_PRECONDITION_FAILED, "target state "
						+ targetState.getState() + " not reachable from the current state");
			} else if (targetState.getState() == Bundle.ACTIVE) {
				bundle.start(targetState.getOptions());
				return getRepresentation(
						new BundleStatePojo(bundle.getState()), variant);
			} else if (targetState.getState() == Bundle.RESOLVED) {
				bundle.stop(targetState.getOptions());
				return getRepresentation(
						new BundleStatePojo(bundle.getState()), variant);
			} else {
				return ERROR(Status.CLIENT_ERROR_BAD_REQUEST, "target state "
						+ targetState.getState() + " not supported");
			}
		} catch (final Exception e) {
			return ERROR(e, variant);
		}
	}

}
