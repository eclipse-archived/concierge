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
import org.osgi.impl.service.rest.PojoReflector.RootNode;

/**
 * List of bundle representation pojos.
 * 
 * @author Jan S. Rellermeyer, IBM Research
 */
@SuppressWarnings("serial")
@RootNode(name = "bundles")
public final class BundleRepresentationsList extends ArrayList<BundlePojo> {

	public BundleRepresentationsList(final org.osgi.framework.Bundle[] bundles) {
		for (final org.osgi.framework.Bundle bundle : bundles) {
			add(new BundlePojo(bundle));
		}
	}

}
