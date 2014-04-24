/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jochen Hiller
 *******************************************************************************/
package org.eclipse.concierge.test;

import java.util.HashMap;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.Bundle;

/**
 * Tests for using Equinox service implementations within Concierge framework.
 * 
 * @author Jochen Hiller
 */
public class EquinoxTest extends AbstractConciergeTestCase {

	/**
	 * Just load OSGi services compendium.
	 */
	@Test
	public void testEquinoxOSGiServices() throws Exception {
		try {
			final Map<String, String> launchArgs = new HashMap<String, String>();
			launchArgs.put("org.eclipse.concierge.debug", "true");
			launchArgs.put("org.osgi.framework.storage.clean", "onFirstInit");
			startFramework(launchArgs);

			final String[] bundleNames = new String[] { "org.eclipse.osgi.services_3.3.100.v20130513-1956.jar", };
			final Bundle[] bundles = installAndStartBundles(bundleNames);
			assertBundlesResolved(bundles);
		} finally {
			stopFramework();
		}
	}

	/**
	 * Just load OSGi services compendium but system packages are defined.
	 * 
	 * Does fail as version 1.6 will NOT be resolved.
	 * 
	 * <pre>
	 * COULD NOT RESOLVE REQUIREMENT BundleRequirement{Import-Package org.osgi.framework; version=1.6} CANDIDATES WERE []
	 * </pre>
	 */
	@Test
	public void testEquinoxOSGiServicesWithSystemPackages() throws Exception {
		try {
			final Map<String, String> launchArgs = new HashMap<String, String>();
			// the value of this property does not matter. If the property is
			// NOT set this test will work
			launchArgs.put("org.osgi.framework.system.packages.extras",
					"com.example.some.package");
			launchArgs.put("org.eclipse.concierge.debug", "true");
			launchArgs.put("org.osgi.framework.storage.clean", "onFirstInit");
			startFramework(launchArgs);

			final String[] bundleNames = new String[] { "org.eclipse.osgi.services_3.3.100.v20130513-1956.jar", };
			final Bundle[] bundles = installAndStartBundles(bundleNames);
			assertBundlesResolved(bundles);
		} finally {
			stopFramework();
		}
	}

	/**
	 * Equinox DS requires bundles Equinox-Util, and org.osgi.service.cm (from
	 * org.eclipse.osgi.services).
	 * 
	 * But it fails with missing requirements:
	 * 
	 * <pre>
	 * COULD NOT RESOLVE REQUIREMENT BundleRequirement{Import-Package org.eclipse.osgi.framework.log;version="1.0.0"} CANDIDATES WERE []
	 * COULD NOT RESOLVE REQUIREMENT BundleRequirement{Import-Package org.eclipse.osgi.service.debug;version="1.0"} CANDIDATES WERE []
	 * COULD NOT RESOLVE REQUIREMENT BundleRequirement{Import-Package org.eclipse.osgi.service.environment;version="1.2.0"} CANDIDATES WERE []
	 * COULD NOT RESOLVE REQUIREMENT BundleRequirement{Import-Package org.eclipse.osgi.util} CANDIDATES WERE []
	 * </pre>
	 */
	@Test
	@Ignore
	public void testEquinoxDS() throws Exception {
		final Map<String, String> launchArgs = new HashMap<String, String>();
		launchArgs.put("org.eclipse.concierge.debug", "true");
		launchArgs.put("org.osgi.framework.storage.clean", "onFirstInit");
		try {
			startFramework(launchArgs);
			final String[] bundleNames = new String[] {
					"org.eclipse.osgi.services_3.3.100.v20130513-1956.jar",
					"org.eclipse.equinox.util_1.0.500.v20130404-1337.jar",
					"org.eclipse.equinox.ds_1.4.101.v20130813-1853.jar" };
			final Bundle[] bundles = installAndStartBundles(bundleNames);
			assertBundlesResolved(bundles);
		} finally {
			stopFramework();
		}
	}

	/**
	 * Equinox Event requires bundles Equinox-Util, and org.osgi.service.event
	 * (from org.eclipse.osgi.services).
	 * 
	 * But it fails with missing requirements:
	 * 
	 * <pre>
	 * COULD NOT RESOLVE REQUIREMENT BundleRequirement{Import-Package org.eclipse.osgi.framework.eventmgr;version="1.1.0"} CANDIDATES WERE []
	 * COULD NOT RESOLVE REQUIREMENT BundleRequirement{Import-Package org.eclipse.osgi.util;version="1.1.0"} CANDIDATES WERE []
	 * </pre>
	 */
	@Test
	@Ignore
	public void testEquinoxEvent() throws Exception {
		final Map<String, String> launchArgs = new HashMap<String, String>();
		launchArgs.put("org.eclipse.concierge.debug", "true");
		launchArgs.put("org.osgi.framework.storage.clean", "onFirstInit");
		try {
			startFramework(launchArgs);
			final String[] bundleNames = new String[] {
					"org.eclipse.osgi.services_3.3.100.v20130513-1956.jar",
					"org.eclipse.equinox.util_1.0.500.v20130404-1337.jar",
					"org.eclipse.equinox.event_1.3.0.v20130327-1442.jar" };
			final Bundle[] bundles = installAndStartBundles(bundleNames);
			assertBundlesResolved(bundles);
		} finally {
			stopFramework();
		}
	}
}
