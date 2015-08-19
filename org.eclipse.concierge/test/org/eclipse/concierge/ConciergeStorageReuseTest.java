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
import java.util.HashMap;

import org.eclipse.concierge.test.util.AbstractConciergeTestCase;
import org.eclipse.concierge.test.util.SyntheticBundleBuilder;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * Tests the getDataFile() method implementation for Bundle and BundleContext.
 * 
 * @author Jochen Hiller - Initial contribution
 */
public class ConciergeStorageReuseTest extends AbstractConciergeTestCase {

	@Test
	public void testStorageInstallOneBundle() throws Exception {
		// start framework clean
		startFramework();
		// install a bundle into framework
		SyntheticBundleBuilder builder = SyntheticBundleBuilder.newBuilder();
		builder.bundleSymbolicName("bundle");
		installBundle(builder);
		checkStorageStructure();
		stopFramework();
	}

	@Test
	public void testStorageReuseOldStorage() throws Exception {
		// start framework clean
		startFramework();
		// install a bundle into framework
		SyntheticBundleBuilder builder = SyntheticBundleBuilder.newBuilder();
		builder.bundleSymbolicName("bundle");
		installBundle(builder);
		checkStorageStructure();
		stopFramework();

		// check if storage is yet available
		checkStorageStructure();

		// start framework again, do NOT clean storage
		HashMap<String, String> launchArgs = new HashMap<String, String>();
		startFramework(launchArgs);
		// now we assume that previous installed bundles have to be used
		BundleContext bundleContext = framework.getBundleContext();
		Bundle[] bundles = bundleContext.getBundles();
		Assert.assertEquals(2, bundles.length);
	}

	private void checkStorageStructure() {
		// check if storage dir exists, and meta/bundle0 files are there
		File bundleDir = new File("storage/default/1");
		Assert.assertTrue(bundleDir.exists());
		Assert.assertTrue(bundleDir.isDirectory());
		File metaFile = new File("storage/default/1/meta");
		Assert.assertTrue(metaFile.exists());
		Assert.assertTrue(!metaFile.isDirectory());
		File bundleFile = new File("storage/default/1/bundle0");
		Assert.assertTrue(bundleFile.exists());
		Assert.assertTrue(!bundleFile.isDirectory());
	}
}
