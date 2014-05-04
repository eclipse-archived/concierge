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
package org.eclipse.concierge.test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * @author Jochen Hiller
 */
public class Slf4jLibraryV172Test extends AbstractConciergeTestCase {

	@Before
	public void setUp() throws Exception {
		startFramework();
	}

	@After
	public void tearDown() throws Exception {
		stopFramework();
	}

	@Test
	public void testInstallManifestOnly() throws InterruptedException,
			BundleException, IOException {
		final Map<String, String> manifestEntries = new HashMap<String, String>();
		manifestEntries.put("Bundle-Version", "1.0.0");
		final Bundle bundle = installBundle("concierge.test.bundle",
				manifestEntries);
		assertBundleResolved(bundle);
	}

	/**
	 * Install just API bundle. Check whether NOPLogger will be used by default
	 * by a sample bundle.
	 */
	@Test
	public void testSLf4JGetNOPLogger() throws InterruptedException, Exception {
		// install API bundle only
		final String[] bundleNames = new String[] { "org.slf4j.api_1.7.2.v20121108-1250.jar" };
		final Bundle[] bundles = installAndStartBundles(bundleNames);
		assertBundlesResolved(bundles);

		// install a bundle which imports org.slf4j package. Must be resolved
		final Map<String, String> manifestEntries = new HashMap<String, String>();
		manifestEntries.put("Bundle-Version", "1.0.0");
		manifestEntries.put("Import-Package", "org.slf4j");
		final Bundle bundle = installBundle(
				"concierge.test.testSLf4JGetNOPLogger", manifestEntries);
		assertBundleResolved(bundle);

		// make calls into classloader of installed bundle
		// Logger logger = org.slf4j.LoggerFactory.getLogger("someCategory");
		// logger.info("Logger Test");
		RunInClassLoader runner = new RunInClassLoader(bundle);
		Object logger = runner.callClassMethod("org.slf4j.LoggerFactory",
				"getLogger", new Object[] { "someCategory" });
		runner.callMethod(logger, "info", new Object[] { "Logger Test" });
		// the default logger is the NOPLogger
		Assert.assertEquals("org.slf4j.helpers.NOPLogger", logger.getClass()
				.getName());
	}

	/**
	 * Install plain slf4j API and logback implementation bundles.
	 */
	@Test
	public void testSLf4JInstallJars() throws InterruptedException,
			BundleException {
		final String[] bundleNames = new String[] {
				"org.slf4j.api_1.7.2.v20121108-1250.jar",
				"ch.qos.logback.core_1.0.7.v20121108-1250.jar",
				"ch.qos.logback.classic_1.0.7.v20121108-1250.jar" };
		final Bundle[] bundles = installAndStartBundles(bundleNames);
		assertBundlesResolved(bundles);
	}

	/**
	 * Install slf4j AND logback slf4j implementation bundle, which is a
	 * fragment bundle to API bundle.
	 * 
	 * @TODO Will fail due to fragment resolve issues.
	 */
	@Test
	public void testSLf4JGetLogbackLogger() throws Exception {
		final String[] bundleNames = new String[] {
				"org.slf4j.api_1.7.2.v20121108-1250.jar",
				"ch.qos.logback.core_1.0.7.v20121108-1250.jar",
				"ch.qos.logback.classic_1.0.7.v20121108-1250.jar",
				"ch.qos.logback.slf4j_1.0.7.v20121108-1250.jar" };
		//final Bundle[] bundles = installAndStartBundles(bundleNames);
		final Bundle[] bundles = installBundles(bundleNames);

		bundles[0].start();
		
		// install a test bundle
		final Map<String, String> manifestEntries = new HashMap<String, String>();
		manifestEntries.put("Bundle-Version", "1.0.0");
		manifestEntries.put("Import-Package", "org.slf4j");
		final Bundle bundle = installBundle(
				"concierge.test.testSLf4JGetLogbackLogger", manifestEntries);

		bundle.start();

		// Logger logger = LoggerFactory.getLogger(Slf4jLibraryV172Test.class);
		// logger.info("Logger Test");
		RunInClassLoader runner = new RunInClassLoader(bundles[0]);
		Object logger = runner.callClassMethod("org.slf4j.LoggerFactory",
				"getLogger", new Object[] { "someCategory" });
		runner.callMethod(logger, "info", new Object[] { "Logger Test" });
		// TODO add here correct class name when test is working
		Assert.assertEquals("ch.qos.logback.classic.Logger", logger.getClass()
				.getName());
	}

}
