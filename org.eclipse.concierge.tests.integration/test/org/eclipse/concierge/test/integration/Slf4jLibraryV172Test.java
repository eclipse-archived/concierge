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

import java.io.IOException;

import org.eclipse.concierge.test.util.AbstractConciergeTestCase;
import org.eclipse.concierge.test.util.SyntheticBundleBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * @author Jochen Hiller
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
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
	public void test01InstallManifestOnly() throws InterruptedException,
			BundleException, IOException {

		SyntheticBundleBuilder builder = SyntheticBundleBuilder.newBuilder();
		builder.bundleSymbolicName("concierge.test.testInstallManifestOnly")
				.bundleVersion("1.0.0");
		final Bundle bundleUnderTest = installBundle(builder);
		enforceResolveBundle(bundleUnderTest);
		assertBundleResolved(bundleUnderTest);
		// now start
		bundleUnderTest.start();
		assertBundleActive(bundleUnderTest);
	}

	/**
	 * Install just API bundle. Check whether NOPLogger will be used by default
	 * by a sample bundle.
	 */
	@Test
	public void test02SLf4JGetNOPLogger() throws InterruptedException,
			Exception {
		// install API bundle only
		final String bundleName = "org.slf4j.api_1.7.2.v20121108-1250.jar";
		final Bundle bundle = installAndStartBundle(bundleName);
		assertBundleResolved(bundle);

		// install a bundle which imports org.slf4j package. Must be resolved
		SyntheticBundleBuilder builder = SyntheticBundleBuilder.newBuilder();
		builder.bundleSymbolicName("concierge.test.testSLf4JGetNOPLogger")
				.bundleVersion("1.0.0")
				.addManifestHeader("Import-Package", "org.slf4j");
		final Bundle bundleUnderTest = installBundle(builder);
		// make calls into classloader of installed bundle
		// Logger logger = org.slf4j.LoggerFactory.getLogger("someCategory");
		// logger.info("Logger Test");
		RunInClassLoader runner = new RunInClassLoader(bundleUnderTest);
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
	public void test03SLf4JInstallJars() throws InterruptedException,
			BundleException {
		final Bundle[] bundles = installAndStartBundles(new String[] {
				"org.slf4j.api_1.7.2.v20121108-1250.jar",
				"ch.qos.logback.core_1.0.7.v20121108-1250.jar",
				"ch.qos.logback.classic_1.0.7.v20121108-1250.jar" });
		assertBundlesResolved(bundles);
	}

	/**
	 * Install slf4j AND logback slf4j implementation bundle, which is a
	 * fragment bundle to API bundle. The fragment bundle has required-bundle
	 * directives to logback core and classic.
	 */
	@Test
	public void test04SLf4JGetLogbackLogger() throws Exception {
		final Bundle[] bundles = installBundles(new String[] {
				"org.slf4j.api_1.7.2.v20121108-1250.jar",
				"ch.qos.logback.core_1.0.7.v20121108-1250.jar",
				"ch.qos.logback.classic_1.0.7.v20121108-1250.jar",
				"ch.qos.logback.slf4j_1.0.7.v20121108-1250.jar" });
		bundles[0].start();
		assertBundleActive(bundles[0]);

		// install a test bundle
		SyntheticBundleBuilder builder = SyntheticBundleBuilder.newBuilder();
		builder.bundleSymbolicName("concierge.test.testSLf4JGetLogbackLogger")
				.bundleVersion("1.0.0")
				.addManifestHeader("Import-Package", "org.slf4j");
		final Bundle bundleUnderTest = installBundle(builder);
		bundleUnderTest.start();

		// when fragment will be resolved, the required bundles has also to be
		// resolved by framework
		assertBundleResolved(bundles[1]);
		assertBundleResolved(bundles[2]);

		// Logger logger = LoggerFactory.getLogger(Slf4jLibraryV172Test.class);
		// logger.info("Logger Test");
		RunInClassLoader runner = new RunInClassLoader(bundles[0]);
		Object logger = runner.callClassMethod("org.slf4j.LoggerFactory",
				"getLogger", new Object[] { "someCategory" });
		runner.callMethod(logger, "info", new Object[] { "Logger Test" });
		Assert.assertEquals("ch.qos.logback.classic.Logger", logger.getClass()
				.getName());
	}

}
