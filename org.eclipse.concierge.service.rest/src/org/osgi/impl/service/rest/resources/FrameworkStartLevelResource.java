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

import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.impl.service.rest.PojoReflector;
import org.osgi.impl.service.rest.pojos.FrameworkStartLevelPojo;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;

/**
 * The framework start level resource.
 * 
 * @author Jan S. Rellermeyer, IBM Research
 */
public class FrameworkStartLevelResource extends
		AbstractOSGiResource<FrameworkStartLevelPojo> {

	private static final MediaType	MEDIA_TYPE	= MediaType.valueOf("application/org.osgi.frameworkstartlevel");

	public FrameworkStartLevelResource() {
		super(PojoReflector.getReflector(FrameworkStartLevelPojo.class), MEDIA_TYPE);
	}

	@Override
	public Representation get(final Variant variant) {
		try {
			return getRepresentation(new FrameworkStartLevelPojo(
					getFrameworkStartLevel()), variant);
		} catch (final Exception e) {
			return ERROR(e, variant);
		}
	}

	@Override
	public Representation put(final Representation r,
			final Variant variant) {
		try {
			final FrameworkStartLevelPojo sl = fromRepresentation(r, r.getMediaType());
			final FrameworkStartLevel fsl = getFrameworkStartLevel();

			if (sl.getStartLevel() != 0) {
				fsl.setStartLevel(sl.getStartLevel());
			}
			if (sl.getInitialBundleStartLevel() != 0) {
				fsl.setInitialBundleStartLevel(sl.getInitialBundleStartLevel());
			}

			return SUCCESS(Status.SUCCESS_NO_CONTENT);
		} catch (final Exception e) {
			return ERROR(e, variant);
		}
	}

}
