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
import org.osgi.framework.Bundle;
import org.osgi.impl.service.rest.PojoReflector.ElementNode;
import org.osgi.impl.service.rest.PojoReflector.RootNode;

/**
 * List of bundle pojos.
 * 
 * @author Jan S. Rellermeyer, IBM Research
 */
@RootNode(name = "bundles")
@ElementNode(name = "uri")
@SuppressWarnings("serial")
public final class BundlePojoList extends ArrayList<String> {

	public BundlePojoList(final Bundle[] bundles) {
		for (int i = 0; i < bundles.length; i++) {
			add("framework/bundle/" + bundles[i].getBundleId());
		}
	}

}
