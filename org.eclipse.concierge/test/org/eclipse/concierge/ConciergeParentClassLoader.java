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
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

/**
 * Tests parent class loader configurations. We use a javafx class as this will
 * be loaded as an extension from lib/jre/ext folder.
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

	@Test
	public void testLoadClassJavaFxWithStandardParentClassLoader()
			throws Exception {
		HashMap<String, String> launchArgs = new HashMap<String, String>();
		launchArgs.put(Constants.FRAMEWORK_BOOTDELEGATION, "javafx.*");
		launchArgs.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA,
				"javafx.application");
		startFrameworkClean(launchArgs);
		setupJavaFxBundle();

		RunInClassLoader runner = new RunInClassLoader(bundleUnderTest);
		try {
			runner.getClass("javafx.application.Application");
			Assert.fail("Uups, ClassNotFoundException expected");
		} catch (ClassNotFoundException ex) {
			// OK, expected
		}
	}

	@Test
	@Ignore("Does not run on Hudson as javafx not installed")
	public void testLoadClassJavaFxWithExtParentClassLoader() throws Exception {
		HashMap<String, String> launchArgs = new HashMap<String, String>();
		launchArgs.put(Constants.FRAMEWORK_BOOTDELEGATION, "javafx.*");
		// define ext as parent class loader
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

	private void setupSunJceBundle() throws BundleException {
		SyntheticBundleBuilder builder = new SyntheticBundleBuilder();
		builder.bundleSymbolicName("bundle").addManifestHeader(
				"Import-Package", "com.sun.crypto.provider");
		bundleUnderTest = installBundle(builder);
		bundleUnderTest.start();
		assertBundleActive(bundleUnderTest);
	}

	@Test
	@Ignore("Does not fail, Sun JCE provider seems not be loaded by Ext ClassLoader")
	public void testLoadClassComSunCryptoProviderHmacSHA1WithStandardParentClassLoader()
			throws Exception {
		HashMap<String, String> launchArgs = new HashMap<String, String>();
		launchArgs.put(Constants.FRAMEWORK_BOOTDELEGATION,
				"com.sun.crypto.provider.*");
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

	@Test
	@Ignore("Does not run on Hudson as SunJCE provider not found")
	public void testLoadClassComSunCryptoProviderHmacSHA1WithExtParentClassLoader()
			throws Exception {
		HashMap<String, String> launchArgs = new HashMap<String, String>();
		launchArgs.put(Constants.FRAMEWORK_BOOTDELEGATION,
				"com.sun.crypto.provider.*");
		// define ext as parent class loader
		launchArgs.put(Constants.FRAMEWORK_BUNDLE_PARENT,
				Constants.FRAMEWORK_BUNDLE_PARENT_EXT);
		launchArgs.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA,
				"com.sun.crypto.provider");
		startFrameworkClean(launchArgs);
		setupSunJceBundle();

		RunInClassLoader runner = new RunInClassLoader(bundleUnderTest);
		// as javafx is loaded from ext class loader it has to work now
		Class<?> clazz = runner.getClass("com.sun.crypto.provider.HmacSHA1");

		// and loaded class has to be identical to one loaded from system class
		// loader
		Class<?> clazzFromSystemClassLoader = ClassLoader
				.getSystemClassLoader().loadClass(
						"com.sun.crypto.provider.HmacSHA1");
		Assert.assertEquals(clazzFromSystemClassLoader, clazz);
		Assert.assertTrue(clazz == clazzFromSystemClassLoader);
	}
}
