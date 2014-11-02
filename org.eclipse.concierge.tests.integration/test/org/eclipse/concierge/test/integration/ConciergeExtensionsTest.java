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
package org.eclipse.concierge.test.integration;

import org.eclipse.concierge.test.util.AbstractConciergeTestCase;
import org.eclipse.concierge.test.util.SyntheticBundleBuilder;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.osgi.framework.Bundle;

/**
 * @author Jochen Hiller
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ConciergeExtensionsTest extends AbstractConciergeTestCase {

	/**
	 * This test will install the extension for permissions. As this is actually
	 * a normal bundle, no fragment, check for that it is a fragment as reminder
	 * to change the extension bundle to a fragment later.
	 */
	@Test
	@Ignore
	public void test10ConciergeExtensionPermission() throws Exception {
		try {
			startFramework();

			final Bundle frameworkExtensionBundle = installBundle("org.eclipse.concierge.extension.permission_1.0.0.201408052201.jar");
			enforceResolveBundle(frameworkExtensionBundle);
			assertBundleResolved(frameworkExtensionBundle);

			// check for tests: the extension must be a fragment
			Assert.assertTrue(
					"Check code: permission extension is not a fragment",
					isFragmentBundle(frameworkExtensionBundle));

			// install pseudo bundle
			SyntheticBundleBuilder builder = SyntheticBundleBuilder
					.newBuilder();
			builder.bundleSymbolicName(
					"concierge.test.test02FrameworkExtensionFragmentOfSystemBundle")
					.bundleVersion("1.0.0")
					.addManifestHeader("Import-Package",
							"org.osgi.service.condpermadmin, org.osgi.service.permissionadmin");
			final Bundle bundleUnderTest = installBundle(builder);
			enforceResolveBundle(bundleUnderTest);
			assertBundleResolved(bundleUnderTest);
		} finally {
			stopFramework();
		}
	}

}
