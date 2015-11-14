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
package org.eclipse.concierge.test.integration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.concierge.test.util.AbstractConciergeTestCase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * Test Apache WebConsole running in Concierge.
 */
public class Slf4JLibraryV164Test extends AbstractConciergeTestCase {

	@After
	public void tearDown() throws Exception {
		stopFramework();
	}

	/**
	 * This test will install slf4j-API and IMPL. Impl requires apache log4j.
	 * Apache log4j requires some system packages extra. Bundles will be started
	 * manually, as impl is a fragment of API bundle. This impl bundle will be
	 * resolved automatically when starting the API bundle.
	 */
	@Test
	public void testInstallAndStartSlf4j164() throws Exception {
		Map<String, String> launchArgs = new HashMap<String, String>();
		// required for log4j
		launchArgs.put("org.osgi.framework.system.packages.extra",
				"javax.management,javax.naming,"
						+ "javax.xml.parsers,org.w3c.dom,"
						+ "org.xml.sax,org.xml.sax.helpers");
		startFrameworkClean(launchArgs);
		Bundle[] bundles = installBundles(new String[] { "log4j_1.2.17.jar",
				"slf4j.api_1.6.4.jar", "slf4j.log4j12_1.6.0.jar", });
		bundles[0].start();
		bundles[1].start();
		assertBundlesActive(bundles);
	}

	/**
	 * Same test as above plus checks about bundleContext.getDataFile("").
	 */
	@Test
	public void testInstallAndStartSlf4j164GetDataFile() throws Exception {
		Map<String, String> launchArgs = new HashMap<String, String>();
		// required for log4j
		launchArgs.put("org.osgi.framework.system.packages.extra",
				"javax.management,javax.naming,"
						+ "javax.xml.parsers,org.w3c.dom,"
						+ "org.xml.sax,org.xml.sax.helpers");
		startFrameworkClean(launchArgs);
		Bundle[] bundles = installBundles(new String[] { "log4j_1.2.17.jar",
				"slf4j.api_1.6.4.jar", "slf4j.log4j12_1.6.0.jar", });
		bundles[0].start();
		bundles[1].start();
		assertBundlesActive(bundles);

		// now test getDataFile, is bundle #1
		File f0 = bundles[0].getBundleContext().getDataFile("");
		Assert.assertTrue(f0.getAbsolutePath().endsWith(
				"storage/default/1/data"));
		// now test getDataFile, is bundle #2
		File f1 = bundles[1].getBundleContext().getDataFile("");
		Assert.assertTrue(f1.getAbsolutePath().endsWith(
				"storage/default/2/data"));
		// due to OSGi spec (10.1.6.16), framework has to return NULL for a
		// fragment bundle
		// now test getDataFile, is fragment has to be null
		File f2 = bundles[2].getDataFile("");
		Assert.assertNull(f2);
		// due to OSGi spec: bundle context for fragment is also null
		BundleContext context = bundles[2].getBundleContext();
		Assert.assertNull(context);
	}
}
