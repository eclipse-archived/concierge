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
import org.eclipse.concierge.ConciergeCollections.Tuple;
import org.eclipse.concierge.BundleImpl.Revision;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;

public interface LegacyBundleProcessing {

	static Version VERSION_ONE = Version.parseVersion("1.0.0");
	static Version VERSION_TWO = Version.parseVersion("2.0.0");
	
	Tuple<List<BundleCapability>, List<BundleRequirement>> processManifest(
			Revision revision, Manifest manifest) throws BundleException;

	List<BundleCapability> translateToCapability(Concierge framework,
			String attributeName, String valueStr);

}
