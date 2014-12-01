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

import java.io.File;

import org.eclipse.concierge.test.util.AbstractConciergeTestCase;
import org.eclipse.concierge.test.util.SyntheticBundleBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * Tests which install bundles which refers to native code.
 * 
 * TODO add more tests and combinations for Bundle-NativeCode, for more OS/ARCH
 * types
 * 
 * @author Jochen Hiller
 */
public class FragmentGetDataFileTest extends AbstractConciergeTestCase {

	private Bundle bundleUnderTest;
	private Bundle fragmentUnderTest;

	@Before
	public void setUp() throws Exception {
		startFramework();
	}

	@After
	public void tearDown() throws Exception {
		stopFramework();
	}

	private void setupDefaultBundles() throws Exception {
		SyntheticBundleBuilder builder = SyntheticBundleBuilder.newBuilder();
		builder.bundleSymbolicName("bundle");
		bundleUnderTest = installBundle(builder);

		builder = SyntheticBundleBuilder.newBuilder();
		builder.bundleSymbolicName("fragment");
		builder.addManifestHeader("Fragment-Host", "bundle");
		fragmentUnderTest = installBundle(builder);
	}

	@Test
	public void testInstallAndStartBundle() throws Exception {
		setupDefaultBundles();
		bundleUnderTest.start();
		assertBundleActive(bundleUnderTest);
		assertBundleResolved(fragmentUnderTest);
	}

	@Test
	public void testInstallAndStartBundlesGetDataFile() throws Exception {
		setupDefaultBundles();
		bundleUnderTest.start();
		assertBundleActive(bundleUnderTest);
		assertBundleResolved(fragmentUnderTest);

		File f1 = bundleUnderTest.getDataFile("");
		Assert.assertNotNull(f1);
		Assert.assertTrue(f1.getAbsolutePath().endsWith(
				"storage/default/1/data"));

		File f2 = fragmentUnderTest.getDataFile("");
		Assert.assertNull(f2);
	}

	@Test
	public void testInstallAndStartBundlesGetDataFileViaBundleContext()
			throws Exception {
		setupDefaultBundles();
		bundleUnderTest.start();
		assertBundleActive(bundleUnderTest);
		assertBundleResolved(fragmentUnderTest);

		File f1 = bundleUnderTest.getBundleContext().getDataFile("");
		Assert.assertNotNull(f1);
		Assert.assertTrue(f1.getAbsolutePath().endsWith(
				"storage/default/1/data"));

		BundleContext context = fragmentUnderTest.getBundleContext();
		Assert.assertNull(context);
	}

}
