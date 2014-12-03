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

	private void setupDefaultBundle() throws BundleException {
		SyntheticBundleBuilder builder = new SyntheticBundleBuilder();
		builder.bundleSymbolicName("bundle");
		bundleUnderTest = installBundle(builder);
		bundleUnderTest.start();
		assertBundleActive(bundleUnderTest);
	}

	@Test
	public void testLoadClassJavaFxWithStandardParentClassLoader()
			throws Exception {
		HashMap<String, String> launchArgs = new HashMap<String, String>();
		launchArgs.put(Constants.FRAMEWORK_BOOTDELEGATION, "javafx.*");
		startFrameworkClean(launchArgs);
		setupDefaultBundle();

		RunInClassLoader runner = new RunInClassLoader(bundleUnderTest);
		try {
			runner.getClass("javafx.application.Application");
			Assert.fail("Uups, ClassNotFoundException expected");
		} catch (ClassNotFoundException ex) {
			// OK, expected
		}
	}

	@Test
	public void testLoadClassJavaFxWithExtParentClassLoader() throws Exception {
		HashMap<String, String> launchArgs = new HashMap<String, String>();
		launchArgs.put(Constants.FRAMEWORK_BOOTDELEGATION, "javafx.*");
		// define ext as parent class loader
		launchArgs.put(Constants.FRAMEWORK_BUNDLE_PARENT,
				Constants.FRAMEWORK_BUNDLE_PARENT_EXT);
		startFrameworkClean(launchArgs);
		setupDefaultBundle();

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

}
