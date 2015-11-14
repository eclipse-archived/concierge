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

package org.osgi.impl.service.rest.pojos;

import java.util.ArrayList;
import org.osgi.framework.ServiceReference;
import org.osgi.impl.service.rest.PojoReflector.RootNode;

/**
 * List of service representation pojos.
 * 
 * @author Jan S. Rellermeyer, IBM Research
 *
 */
@RootNode(name = "services")
@SuppressWarnings("serial")
public final class ServiceRepresentationList extends ArrayList<ServicePojo> {

	public ServiceRepresentationList(ServiceReference<?>[] srefs) {
		if (srefs != null) {
			for (final ServiceReference<?> sref : srefs) {
				add(new ServicePojo(sref));
			}
		}
	}

}
