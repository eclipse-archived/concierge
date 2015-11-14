/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Jochen Hiller
 *******************************************************************************/
package org.eclipse.concierge;

import static org.hamcrest.CoreMatchers.notNullValue;

import org.eclipse.concierge.test.util.AbstractConciergeTestCase;
import org.eclipse.concierge.test.util.SyntheticBundleBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.Bundle;

/**
 * @author Jochen Hiller - Initial Contribution
 */
public class BundlesWithFrameworkExtensionsTest
		extends AbstractConciergeTestCase {

	@Override
	protected boolean stayInShell() {
		return false;
	}

	@After
	public void tearDown() throws Exception {
		stopFramework();
	}

	/**
	 * This test will install a fragment bundle to framework
	 * org.eclipse.concierge.
	 */
	@Test
	public void testFrameworkExtensionFragmentOfConcierge() throws Exception {
		startFramework();
		SyntheticBundleBuilder builder = SyntheticBundleBuilder.newBuilder();
		builder.bundleSymbolicName("testFrameworkExtensionFragmentOfConcierge")
				.bundleVersion("1.0.0").addManifestHeader("Fragment-Host",
						"org.eclipse.concierge; extension:=framework");
		Bundle bundleUnderTest = installBundle(builder);
		enforceResolveBundle(bundleUnderTest);
		assertBundleResolved(bundleUnderTest);
	}

	/**
	 * This test will install a fragment bundle to framework system.bundle.
	 */
	@Test
	public void testFrameworkExtensionFragmentOfSystemBundle()
			throws Exception {
		startFramework();
		SyntheticBundleBuilder builder = SyntheticBundleBuilder.newBuilder();
		builder.bundleSymbolicName(
				"testFrameworkExtensionFragmentOfSystemBundle")
				.bundleVersion("1.0.0").addManifestHeader("Fragment-Host",
						"system.bundle; extension:=framework");
		Bundle bundleUnderTest = installBundle(builder);
		enforceResolveBundle(bundleUnderTest);
		assertBundleResolved(bundleUnderTest);
	}

	/**
	 * This test will install a fragment bundle to framework system.bundle, a
	 * second bundle to use the exported package. All is fine. The stop the
	 * framework, restart again from storage. But now the bundle can NOT be
	 * resolved anymore, so something is wrong.
	 */
	@Test
	@Ignore("TODO does not work when restarting from storage")
	public void testFrameworkExtensionFragmentOfSystemBundleRestart()
			throws Exception {
		startFramework();
		SyntheticBundleBuilder builder;

		builder = SyntheticBundleBuilder.newBuilder();
		builder.bundleSymbolicName(
				"testFrameworkExtensionFragmentOfSystemBundle")
				.bundleVersion("1.0.0")
				.addManifestHeader("Fragment-Host",
						"system.bundle; extension:=framework")
				.addManifestHeader("Export-Package", "p1");
		Bundle bundle1UnderTest = installBundle(builder);
		assertBundleResolved(bundle1UnderTest);

		builder = SyntheticBundleBuilder.newBuilder();
		builder.bundleSymbolicName(
				"testBundleUsingAnExtensionFragmentOfSystemBundle")
				.bundleVersion("1.0.0")
				.addManifestHeader("Import-package", "p1");
		Bundle bundle2UnderTest = installBundle(builder);
		bundle2UnderTest.start();
		assertBundleResolved(bundle2UnderTest);

		// restart framework, NO clean start
		stopFramework();
		startFrameworkNonClean();

		Bundle[] bundles = framework.getBundleContext().getBundles();
		Assert.assertThat(bundles, notNullValue());
		Bundle bundle1 = getBundleForBSN(bundles,
				"testFrameworkExtensionFragmentOfSystemBundle");
		Assert.assertThat(bundle1, notNullValue());
		// fragment must be resolved
		assertBundleResolved(bundle1);

		Bundle bundle2 = getBundleForBSN(bundles,
				"testBundleUsingAnExtensionFragmentOfSystemBundle");
		Assert.assertThat(bundle2, notNullValue());
		// bundle must be active, which does not yet work
		assertBundleActive(bundle2);
	}

}
