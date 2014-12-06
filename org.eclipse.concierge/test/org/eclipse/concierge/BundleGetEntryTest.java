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

import java.net.URL;

import org.eclipse.concierge.test.util.AbstractConciergeTestCase;
import org.eclipse.concierge.test.util.SyntheticBundleBuilder;
import org.eclipse.concierge.test.util.TestUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;

/**
 * Tests the getEntry() and getEntryPath() method implementations for Bundle and
 * BundleContext.
 * 
 * @author Jochen Hiller - Initial contribution
 */
public class BundleGetEntryTest extends AbstractConciergeTestCase {

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
		builder.addFile("plugin.properties", "# props");
		builder.addFile("plugin.xml", "<xml>");
		builder.addFile("dir/subdir/file.txt", "# dir/subdir/file.txt");
		bundleUnderTest = installBundle(builder);

		builder = SyntheticBundleBuilder.newBuilder();
		builder.bundleSymbolicName("fragment");
		builder.addManifestHeader("Fragment-Host", "bundle");
		builder.addFile("fragment.properties", "# fragment props");
		builder.addFile("OSGI-INF/i18n/fragment.properties", "# i18 props");
		builder.addFile("dir/subdir/fragment.txt", "# dir/subdir/fragment.txt");
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
	public void testBundleGetEntryActiveState() throws Exception {
		setupDefaultBundles();
		bundleUnderTest.start();
		assertBundleActive(bundleUnderTest);
		checkBundleResources(bundleUnderTest);
	}

	@Test
	public void testBundleGetEntryInstalledState() throws Exception {
		setupDefaultBundles();
		assertBundleInstalled(bundleUnderTest);
		checkBundleResources(bundleUnderTest);
	}

	private void checkBundleResources(Bundle b) {
		// get resources from bundle
		URL res1 = b.getEntry("/plugin.properties");
		Assert.assertNotNull(res1);
		Assert.assertEquals("# props", TestUtils.getContentFromUrl(res1));
		URL res2 = b.getEntry("/plugin.xml");
		Assert.assertNotNull(res2);
		Assert.assertEquals("<xml>", TestUtils.getContentFromUrl(res2));
		URL res3 = b.getEntry("/dir/subdir/file.txt");
		Assert.assertNotNull(res3);
		Assert.assertEquals("# dir/subdir/file.txt",
				TestUtils.getContentFromUrl(res3));
	}

	@Test
	public void testFragmentGetEntryActiveState() throws Exception {
		setupDefaultBundles();
		bundleUnderTest.start();
		checkFragmentResources(fragmentUnderTest);
	}

	@Test
	@Ignore("Does not work, investigate")
	public void testFragmentGetEntryViaBundleActiveState() throws Exception {
		setupDefaultBundles();
		bundleUnderTest.start();
		checkFragmentResources(bundleUnderTest);
	}

	private void checkFragmentResources(Bundle b) {
		// now check of resources from fragment can be resolved too from bundle
		URL res1 = b.getEntry("/fragment.properties");
		Assert.assertNotNull(res1);
		Assert.assertEquals("# fragment props",
				TestUtils.getContentFromUrl(res1));
		URL res2 = b.getEntry("/OSGI-INF/i18n/fragment.properties");
		Assert.assertNotNull(res2);
		Assert.assertEquals("# i18 props", TestUtils.getContentFromUrl(res2));
		URL res3 = b.getEntry("/dir/subdir/fragment.txt");
		Assert.assertNotNull(res3);
		Assert.assertEquals("# dir/subdir/fragment.txt",
				TestUtils.getContentFromUrl(res3));
	}

	@Test
	@Ignore("Does not work due to wrong installBundle() implementation")
	public void testGetEntryFromBundleListenerInstalledEvent() throws Exception {
		try {
			SyntheticBundleBuilder builder = SyntheticBundleBuilder
					.newBuilder();
			builder.bundleSymbolicName("bundle");
			builder.addFile("plugin.properties", "# props");
			// register a bundle listener
			framework.getBundleContext().addBundleListener(
					new BundleListener() {
						public void bundleChanged(BundleEvent event) {
							if (event.getType() == BundleEvent.INSTALLED) {
								// get resources from bundle
								URL res1 = event.getBundle().getEntry(
										"/plugin.properties");
								Assert.assertNotNull(res1);
								Assert.assertEquals("# props",
										TestUtils.getContentFromUrl(res1));

							}
						}
					});
			// now trigger install
			bundleUnderTest = installBundle(builder);
		} finally {
			// TODO wait 1 sec otherwise other tests will fail
			Thread.sleep(1000);
		}
	}

	@Test
	public void testGetEntryFromBundleListenerResolverEvent() throws Exception {
		SyntheticBundleBuilder builder = SyntheticBundleBuilder.newBuilder();
		builder.bundleSymbolicName("bundle");
		builder.addFile("plugin.properties", "# props");
		// register a bundle listener
		framework.getBundleContext().addBundleListener(new BundleListener() {
			public void bundleChanged(BundleEvent event) {
				if (event.getType() == BundleEvent.RESOLVED) {
					// get resources from bundle
					URL res1 = event.getBundle().getEntry("/plugin.properties");
					Assert.assertNotNull(res1);
					Assert.assertEquals("# props",
							TestUtils.getContentFromUrl(res1));

				}
			}
		});
		// now trigger install
		bundleUnderTest = installBundle(builder);
	}
}
