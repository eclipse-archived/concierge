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

import java.util.HashMap;

import org.eclipse.concierge.test.util.AbstractConciergeTestCase;
import org.eclipse.concierge.test.util.SyntheticBundleBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * Tests system packages extra property.
 * 
 * @author Jochen Hiller
 */
public class BundlesWithSystemPackagesExtraTest extends
		AbstractConciergeTestCase {

	private Bundle bundleUnderTest;

	private void setupDefaultBundles() throws Exception {
		SyntheticBundleBuilder builder = SyntheticBundleBuilder.newBuilder();
		builder.bundleSymbolicName("bundle");
		builder.addManifestHeader("Import-Package", "javax.xml.parsers");
		bundleUnderTest = installBundle(builder);
	}

	@Test
	public void testBundleWithGoodImports() throws Exception {
		checkOK("javax.xml.parsers");
		checkOK("javax.xml.parsers,org.w3.dom");
	}

	@Test
	public void testBundleWithBadImports() throws Exception {
		checkFail("bla.blub");
		checkFail("bla.blub,x.y.z");
	}

	private void checkOK(String spe) throws Exception {
		HashMap<String, String> launchArgs = new HashMap<String, String>();
		launchArgs.put("org.osgi.framework.system.packages.extra", spe);
		startFrameworkClean(launchArgs);
		setupDefaultBundles();
		bundleUnderTest.start();
		assertBundleActive(bundleUnderTest);
		Bundle systemBundle = framework.getBundleContext().getBundle(0);
		// check if extra packages will be added to system bundle
		String s = systemBundle.getHeaders().get("Export-Package");
		Assert.assertTrue(s.endsWith(spe));
		stopFramework();
	}

	private void checkFail(String spe) throws Exception {
		HashMap<String, String> launchArgs = new HashMap<String, String>();
		launchArgs.put("org.osgi.framework.system.packages.extra", spe);
		startFrameworkClean(launchArgs);
		setupDefaultBundles();
		try {
			bundleUnderTest.start();
			Assert.fail("Uups, BundleException expected");
		} catch (BundleException ex) {
			// ignore
		}
		assertBundleInstalled(bundleUnderTest);
		stopFramework();
	}
}
