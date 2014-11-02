/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jochen Hiller
 *******************************************************************************/
package org.eclipse.concierge;

import org.eclipse.concierge.test.util.AbstractConciergeTestCase;
import org.eclipse.concierge.test.util.SyntheticBundleBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;

/**
 * Test which tries to install some bundles with dependencies to system bundle.
 * 
 * @author Jochen Hiller
 */
public class BundlesWithFrameworkDependenciesTest extends
		AbstractConciergeTestCase {

	@Before
	public void setUp() throws Exception {
		startFramework();
	}

	@After
	public void tearDown() throws Exception {
		stopFramework();
	}

	@Test
	public void testInstallAndStartManifestWithRequireBundleSystemBundle()
			throws Exception {
		final SyntheticBundleBuilder builder = SyntheticBundleBuilder
				.newBuilder();
		builder.bundleSymbolicName(
				"testInstallAndStartManifestWithRequireBundleSystemBundle")
				.bundleVersion("1.0.0")
				.addManifestHeader("Require-Bundle", "system.bundle");
		final Bundle bundleUnderTest = installBundle(builder);
		bundleUnderTest.start();
		assertBundleActive(bundleUnderTest);
	}

	@Test
	public void testInstallAndStartManifestWithImportPackageOSGiNamespae()
			throws Exception {
		final SyntheticBundleBuilder builder = SyntheticBundleBuilder
				.newBuilder();
		builder.bundleSymbolicName(
				"testInstallAndStartManifestWithImportPackageOSGiNamespae")
				.bundleVersion("1.0.0")
				.addManifestHeader("Import-Package",
						"org.osgi.framework.namespace;version=\"[1.0,2.0)\"");
		final Bundle bundleUnderTest = installBundle(builder);
		bundleUnderTest.start();
		assertBundleActive(bundleUnderTest);
	}

}
