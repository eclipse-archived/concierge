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
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

/**
 * Tests parent class loader configurations. We use a javafx class and a
 * com.sun.crypt.provider classes as they will be loaded as an extension from
 * lib/jre/ext folder.
 * 
 * @author Jochen Hiller - Initial contribution
 */
public class ConciergeParentClassLoader extends AbstractConciergeTestCase {

	private Bundle bundleUnderTest;

	@After
	public void tearDown() throws Exception {
		stopFramework();
	}

	private void setupJavaFxBundle() throws BundleException {
		SyntheticBundleBuilder builder = new SyntheticBundleBuilder();
		builder.bundleSymbolicName("bundle").addManifestHeader(
				"Import-Package", "javafx.application");
		bundleUnderTest = installBundle(builder);
		bundleUnderTest.start();
		assertBundleActive(bundleUnderTest);
	}

	private boolean isJavaFxAvailable() {
		try {
			ClassLoader.getSystemClassLoader().loadClass(
					"javafx.application.Application");
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	@Test
	public void testLoadClassJavaFxWithParentClassLoader() throws Exception {
		if (!isJavaFxAvailable()) {
			System.err
					.println("Skipping testLoadClassJavaFxWithParentClassLoader: javafx not available");
			return;
		}
		HashMap<String, String> launchArgs = new HashMap<String, String>();
		// launchArgs.put(Constants.FRAMEWORK_BOOTDELEGATION, "javafx.*");
		launchArgs.put(Constants.FRAMEWORK_BUNDLE_PARENT,
				Constants.FRAMEWORK_BUNDLE_PARENT_EXT);
		launchArgs.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA,
				"javafx.application");
		startFrameworkClean(launchArgs);
		setupJavaFxBundle();

		RunInClassLoader runner = new RunInClassLoader(bundleUnderTest);
		// as javafx is loaded from ext class loader it has to work now
		Class<?> clazz = runner.getClass("javafx.application.Application");

		// and loaded class has to be identical to one loaded from system class
		// loader
		Class<?> clazzFromSystemClassLoader = ClassLoader
				.getSystemClassLoader().loadClass(
						"javafx.application.Application");
		Assert.assertEquals(clazzFromSystemClassLoader, clazz);
		Assert.assertTrue(clazz == clazzFromSystemClassLoader);
	}

	/**
	 * Installs a bundle with an import to a package in an library in lib/ext
	 * (here: sunjce_provider.jar).
	 */
	private void setupSunJceBundle() throws BundleException {
		SyntheticBundleBuilder builder = new SyntheticBundleBuilder();
		builder.bundleSymbolicName("bundle").addManifestHeader(
				"Import-Package", "com.sun.crypto.provider");
		bundleUnderTest = installBundle(builder);
		bundleUnderTest.start();
		assertBundleActive(bundleUnderTest);
	}

	/**
	 * Loads a class from a lib/ext library via BOOT classloader, will NOT work.
	 */
	@Test
	public void testLoadClassFromLibExtWithBootParentClassLoader()
			throws Exception {
		loadClassFailedFromLibExtWithParentClassLoader(Constants.FRAMEWORK_BUNDLE_PARENT_BOOT);
	}

	/**
	 * Loads a class from a lib/ext library via APP classloader, will work, as
	 * app class loader inherits standard class loader which include ext class
	 * loader.
	 */
	@Test
	public void testLoadClassFromLibExtWithAppParentClassLoader()
			throws Exception {
		loadClassSuccessfulFromLibExtWithParentClassLoader(Constants.FRAMEWORK_BUNDLE_PARENT_APP);
	}

	/**
	 * Loads a class from a lib/ext library via FRAMEWORK classloader, will
	 * work, as framework class loader inherits standard class loader which
	 * include ext class loader.
	 */
	@Test
	public void testLoadClassFromLibExtWithFrameworkParentClassLoader()
			throws Exception {
		loadClassSuccessfulFromLibExtWithParentClassLoader(Constants.FRAMEWORK_BUNDLE_PARENT_FRAMEWORK);
	}

	/**
	 * Loads a class from a lib/ext library via EXT classloader, will work, as
	 * lib/ext will found by this class loader.
	 */
	@Test
	// @Ignore("Does not run on Hudson as SunJCE provider not found")
	public void testLoadClassFromLibExtWithExtParentClassLoader()
			throws Exception {
		loadClassSuccessfulFromLibExtWithParentClassLoader(Constants.FRAMEWORK_BUNDLE_PARENT_EXT);
	}

	// helper methods

	public void loadClassFailedFromLibExtWithParentClassLoader(
			String parentClassLoader) throws Exception {
		HashMap<String, String> launchArgs = new HashMap<String, String>();
		launchArgs.put(Constants.FRAMEWORK_BUNDLE_PARENT, parentClassLoader);
		launchArgs.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA,
				"com.sun.crypto.provider");
		startFrameworkClean(launchArgs);
		setupSunJceBundle();

		RunInClassLoader runner = new RunInClassLoader(bundleUnderTest);
		try {
			runner.getClass("com.sun.crypto.provider.HmacSHA1");
			Assert.fail("Uups, ClassNotFoundException expected");
		} catch (ClassNotFoundException ex) {
			// OK, expected
		}
	}

	private void loadClassSuccessfulFromLibExtWithParentClassLoader(
			String parentClassLoader) throws Exception {
		HashMap<String, String> launchArgs = new HashMap<String, String>();
		launchArgs.put(Constants.FRAMEWORK_BUNDLE_PARENT, parentClassLoader);
		launchArgs.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA,
				"com.sun.crypto.provider");
		startFrameworkClean(launchArgs);
		setupSunJceBundle();

		RunInClassLoader runner = new RunInClassLoader(bundleUnderTest);
		Class<?> clazz = runner.getClass("com.sun.crypto.provider.HmacSHA1");

		// loaded class has to be identical to one loaded from system class
		// loader
		Class<?> clazzFromSystemClassLoader = ClassLoader
				.getSystemClassLoader().loadClass(
						"com.sun.crypto.provider.HmacSHA1");
		Assert.assertEquals(clazzFromSystemClassLoader, clazz);
		Assert.assertTrue(clazz == clazzFromSystemClassLoader);
	}
}
