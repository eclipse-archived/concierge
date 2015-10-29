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

import org.osgi.framework.ServiceReference;
import org.osgi.impl.service.rest.PojoReflector;
import org.osgi.impl.service.rest.RestService;
import org.osgi.impl.service.rest.pojos.ServiceRepresentationList;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;

/**
 * The service representations resource, a list of the full representations of
 * all services.
 * 
 * @author Jan S. Rellermeyer, IBM Research
 */
public class ServiceRepresentationsResource extends
		AbstractOSGiResource<ServiceRepresentationList> {

	private static final MediaType	MEDIA_TYPE	= MediaType.valueOf("application/org.osgi.services.representations");

	public ServiceRepresentationsResource() {
		super(PojoReflector.getReflector(ServiceRepresentationList.class), MEDIA_TYPE);
	}

	@Override
	public Representation get(final Variant variant) {
		try {
			final String filter = getQuery().getFirstValue(RestService.FILTER_ID_KEY);
			final ServiceReference<?>[] srefs = getBundleContext()
					.getAllServiceReferences(null, filter);
			return getRepresentation(new ServiceRepresentationList(srefs), variant);
		} catch (final Exception e) {
			return ERROR(e, variant);
		}
	}

}
