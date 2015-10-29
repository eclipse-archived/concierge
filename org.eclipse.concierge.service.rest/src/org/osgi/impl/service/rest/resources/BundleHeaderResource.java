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

import java.util.List;
import java.util.Map;
import org.osgi.impl.service.rest.RestService;
import org.restlet.data.Language;
import org.restlet.data.MediaType;
import org.restlet.data.Preference;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;

/**
 * The bundle header resource.
 * 
 * @author Jan S. Rellermeyer, IBM Research
 */
public class BundleHeaderResource extends
		AbstractOSGiResource<Map<String, String>> {

	private static final MediaType	MEDIA_TYPE	= MediaType.valueOf("application/org.osgi.bundleheader");

	public BundleHeaderResource() {
		super(null, MEDIA_TYPE);
	}

	@Override
	public Representation get(final Variant variant) {
		try {
			final List<Preference<Language>> acceptedLanguages = getClientInfo()
					.getAcceptedLanguages();

			final String locale = acceptedLanguages == null
					|| acceptedLanguages.isEmpty() ? null : acceptedLanguages
					.get(0).getMetadata().toString();

			final org.osgi.framework.Bundle bundle = getBundleFromKeys(RestService.BUNDLE_ID_KEY);
			if (bundle == null) {
				return ERROR(Status.CLIENT_ERROR_NOT_FOUND);
			}
			return getRepresentation(
					mapFromDict(locale == null ? bundle.getHeaders()
							: bundle.getHeaders(locale)), variant);
		} catch (final Exception e) {
			return ERROR(e, variant);
		}
	}

}
