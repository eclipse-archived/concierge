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
package org.eclipse.concierge;

import org.eclipse.concierge.test.util.AbstractConciergeTestCase;
import org.eclipse.concierge.test.util.SyntheticBundleBuilder;
import org.junit.Test;
import org.osgi.framework.Bundle;

/**
 * @author Jochen Hiller - Initial Contribution
 */
public class BundlesWithFrameworkExtensionsTest extends
		AbstractConciergeTestCase {

	/**
	 * This test will install a fragment bundle to framework
	 * org.eclipse.concierge.
	 */
	@Test
	public void testFrameworkExtensionFragmentOfConcierge() throws Exception {
		try {
			startFramework();
			SyntheticBundleBuilder builder = SyntheticBundleBuilder
					.newBuilder();
			builder.bundleSymbolicName(
					"testFrameworkExtensionFragmentOfConcierge")
					.bundleVersion("1.0.0")
					.addManifestHeader("Fragment-Host",
							"org.eclipse.concierge; extension:=framework");
			final Bundle bundleUnderTest = installBundle(builder);
			enforceResolveBundle(bundleUnderTest);
			assertBundleResolved(bundleUnderTest);
		} finally {
			stopFramework();
		}
	}

	/**
	 * This test will install a fragment bundle to framework system.bundle.
	 */
	@Test
	public void testFrameworkExtensionFragmentOfSystemBundle() throws Exception {
		try {
			startFramework();
			SyntheticBundleBuilder builder = SyntheticBundleBuilder
					.newBuilder();
			builder.bundleSymbolicName(
					"testFrameworkExtensionFragmentOfSystemBundle")
					.bundleVersion("1.0.0")
					.addManifestHeader("Fragment-Host",
							"system.bundle; extension:=framework");
			final Bundle bundleUnderTest = installBundle(builder);
			enforceResolveBundle(bundleUnderTest);
			assertBundleResolved(bundleUnderTest);
		} finally {
			stopFramework();
		}
	}

}
