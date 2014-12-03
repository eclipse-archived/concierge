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
import org.eclipse.concierge.test.util.TestUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * Tests the getDataFile() method implementation for Bundle and BundleContext.
 * 
 * @author Jochen Hiller - Initial contribution
 */
public class BundleGetDataFileTest extends AbstractConciergeTestCase {

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
	public void testInstallAndStartDefaultBundles() throws Exception {
		setupDefaultBundles();
		bundleUnderTest.start();
		assertBundleActive(bundleUnderTest);
		// fragment must be resolved automatically
		assertBundleResolved(fragmentUnderTest);
	}

	@Test
	public void testGetDataFileEmptyString() throws Exception {
		setupDefaultBundles();
		bundleUnderTest.start();

		File f1 = bundleUnderTest.getDataFile("");
		Assert.assertNotNull(f1);
		Assert.assertTrue(f1.getAbsolutePath().endsWith(
				"storage/default/1/data"));
		Assert.assertTrue(f1.exists());
		Assert.assertTrue(f1.isDirectory());

		// fragments do NOT have a data file
		File f2 = fragmentUnderTest.getDataFile("");
		Assert.assertNull(f2);
	}

	@Test
	public void testGetDataFileEmptyStringViaBundleContext() throws Exception {
		setupDefaultBundles();
		bundleUnderTest.start();

		File f1 = bundleUnderTest.getBundleContext().getDataFile("");
		Assert.assertNotNull(f1);
		Assert.assertTrue(f1.getAbsolutePath().endsWith(
				"storage/default/1/data"));
		Assert.assertTrue(f1.exists());
		Assert.assertTrue(f1.isDirectory());

		// fragments do NOT have a bundle context
		BundleContext context = fragmentUnderTest.getBundleContext();
		Assert.assertNull(context);
	}

	@Test
	public void testGetDataFileWithFiles1Level() throws Exception {
		setupDefaultBundles();
		bundleUnderTest.start();

		File f1 = bundleUnderTest.getDataFile("file.txt");
		Assert.assertNotNull(f1);
		Assert.assertTrue(f1.getAbsolutePath().endsWith(
				"storage/default/1/data/file.txt"));
		Assert.assertTrue(f1.getParentFile().exists());
		Assert.assertTrue(f1.getParentFile().isDirectory());
		TestUtils.copyStringToFile("# some text", f1);
		f1.deleteOnExit();
	}

	@Test
	public void testGetDataFileWithFiles3Level() throws Exception {
		setupDefaultBundles();
		bundleUnderTest.start();

		File f1 = bundleUnderTest.getDataFile("a/b/file.txt");
		Assert.assertNotNull(f1);
		Assert.assertTrue(f1.getAbsolutePath().endsWith(
				"storage/default/1/data/a/b/file.txt"));
		Assert.assertTrue(f1.getParentFile().exists());
		Assert.assertTrue(f1.getParentFile().isDirectory());
		TestUtils.copyStringToFile("# some text", f1);
		f1.deleteOnExit();
	}

}
