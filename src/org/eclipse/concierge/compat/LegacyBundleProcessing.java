/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jan S. Rellermeyer, IBM Research - initial API and implementation
 *******************************************************************************/
package org.eclipse.concierge.compat;

import java.util.List;
import java.util.jar.Manifest;

import org.eclipse.concierge.Concierge;
import org.eclipse.concierge.Tuple;
import org.eclipse.concierge.BundleImpl.Revision;
import org.osgi.framework.BundleException;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;

public interface LegacyBundleProcessing {

	Tuple<List<BundleCapability>, List<BundleRequirement>> processManifest(
			Revision revision, Manifest manifest) throws BundleException;

	List<BundleCapability> translateToCapability(Concierge framework,
			String attributeName, String valueStr);

}
