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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.concierge.test.util.AbstractConciergeTestCase;
import org.eclipse.concierge.test.util.FilteredClassLoader;
import org.junit.Assert;
import org.junit.Test;

/**
 * This class tests in JavaSE 8 the Execution Environments, especially the
 * support of the JavaSE8 compact profiles.
 * 
 * To test if the compact profiles will be handled correct, the framework will
 * be started in an own class loader, which allows to "mock" the existence of
 * some classes. This allows to "fake" the discovery of compact profiles even if
 * running on a full JavaSE 8 installation.
 * 
 * If this test does NOT run with JavaSE 8, it will skip it tests, and report a
 * warning.
 */
public class ConciergeJavaSE8ExecutionEnvironmentTest
		extends AbstractConciergeTestCase {

	@Test
	public void testConciergeJavaSE8Compact1() throws Exception {
		if (!checkForJavaSE8("testConciergeJavaSE8Compact1")) {
			return;
		}
		ClassLoader cl = new FilteredClassLoader(
				this.getClass().getClassLoader(), "org.w3c.dom.Document");
		String eeCaps = startFrameworkInClassLoaderAndReturnEECapabilities(cl);
		Assert.assertTrue(eeCaps.contains(
				"JavaSE, version=[1.8.0, 1.7.0, 1.6.0, 1.5.0, 1.4.0, 1.3.0, 1.2.0, 1.1.0]"));
		Assert.assertTrue(eeCaps.contains("JavaSE/compact1, version=[1.8.0]"));
		Assert.assertFalse(eeCaps.contains("JavaSE/compact2, version=[1.8.0]"));
		Assert.assertFalse(eeCaps.contains("JavaSE/compact3, version=[1.8.0]"));
	}

	@Test
	public void testConciergeJavaSE8Compact2() throws Exception {
		if (!checkForJavaSE8("testConciergeJavaSE8Compact2")) {
			return;
		}
		ClassLoader cl = new FilteredClassLoader(
				this.getClass().getClassLoader(),
				"javax.management.Descriptor");
		String eeCaps = startFrameworkInClassLoaderAndReturnEECapabilities(cl);
		Assert.assertTrue(eeCaps.contains(
				"JavaSE, version=[1.8.0, 1.7.0, 1.6.0, 1.5.0, 1.4.0, 1.3.0, 1.2.0, 1.1.0]"));
		Assert.assertTrue(eeCaps.contains("JavaSE/compact1, version=[1.8.0]"));
		Assert.assertTrue(eeCaps.contains("JavaSE/compact2, version=[1.8.0]"));
		Assert.assertFalse(eeCaps.contains("JavaSE/compact3, version=[1.8.0]"));
	}

	@Test
	public void testConciergeJavaSE8Compact3() throws Exception {
		if (!checkForJavaSE8("testConciergeJavaSE8Compact3")) {
			return;
		}
		ClassLoader cl = new FilteredClassLoader(
				this.getClass().getClassLoader(), "javax.imageio.ImageIO");
		String eeCaps = startFrameworkInClassLoaderAndReturnEECapabilities(cl);
		Assert.assertTrue(eeCaps.contains(
				"JavaSE, version=[1.8.0, 1.7.0, 1.6.0, 1.5.0, 1.4.0, 1.3.0, 1.2.0, 1.1.0]"));
		Assert.assertTrue(eeCaps.contains("JavaSE/compact1, version=[1.8.0]"));
		Assert.assertTrue(eeCaps.contains("JavaSE/compact2, version=[1.8.0]"));
		Assert.assertTrue(eeCaps.contains("JavaSE/compact3, version=[1.8.0]"));
	}

	@Test
	public void testStartConciergeWithFullJRE() throws Exception {
		if (!checkForJavaSE8("testStartConciergeWithFullJRE")) {
			return;
		}
		ClassLoader cl = new FilteredClassLoader(
				this.getClass().getClassLoader()); // no filter
		String eeCaps = startFrameworkInClassLoaderAndReturnEECapabilities(cl);
		Assert.assertTrue(eeCaps.contains(
				"JavaSE, version=[1.8.0, 1.7.0, 1.6.0, 1.5.0, 1.4.0, 1.3.0, 1.2.0, 1.1.0]"));
		Assert.assertTrue(eeCaps.contains("JavaSE/compact1, version=[1.8.0]"));
		Assert.assertTrue(eeCaps.contains("JavaSE/compact2, version=[1.8.0]"));
		Assert.assertTrue(eeCaps.contains("JavaSE/compact3, version=[1.8.0]"));
	}

	// helper methods

	private boolean checkForJavaSE8(String testName) {
		String javaVersion = System.getProperty("java.specification.version");
		boolean isJavaSE8 = "1.8".equals(javaVersion);
		if (!isJavaSE8) {
			System.err.println("Will skip the test '" + testName
					+ "' as not running in JavaSE 8, but in Java " + javaVersion
					+ " instead");
		}
		return isJavaSE8;
	}

	/**
	 * This method will start a Concierge framework in a given ClassLoader. This
	 * will all be done via reflection to avoid any references in test code
	 * here. It will return the execution environment capabilities as a String
	 * for further tests.
	 */
	private String startFrameworkInClassLoaderAndReturnEECapabilities(
			ClassLoader cl) throws Exception {
		RunInClassLoader runner = new AbstractConciergeTestCase.RunInClassLoader(
				cl);

		// Framework framework = new Concierge(emptyMap);
		Map<String, String> launchArgs = new HashMap<String, String>();
		Object framework = runner.createInstance(
				"org.eclipse.concierge.Concierge",
				new String[] { Map.class.getName() },
				new Object[] { launchArgs });
		try {
			// framework.init();
			runner.callMethodNoArgs(framework, "init");
			// framework.start();
			runner.callMethodNoArgs(framework, "start");
			// BundleWiring w = framework.adapt(BundleWiring.class);
			Object w = runner.callMethod(framework, "adapt",
					new Object[] { Class.forName(
							"org.osgi.framework.wiring.BundleWiring", false,
							cl) });
			// List<BundleCapability> caps = w.getCapabilities("osgi.ee");
			Object caps = runner.callMethod(w, "getCapabilities",
					new Object[] { "osgi.ee" });

			return caps.toString();
		} finally {
			// framework.stop();
			runner.callMethodNoArgs(framework, "stop");
		}
	}

}
