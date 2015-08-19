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
package org.eclipse.concierge;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.concierge.test.util.AbstractConciergeTestCase;
import org.eclipse.concierge.test.util.SyntheticBundleBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Bundle;

/**
 * @author Jochen Hiller - Initial contribution
 */
public class FrameworkLaunchArgsTest extends AbstractConciergeTestCase {

	@After
	public void tearDown() throws Exception {
		stopFramework();
	}

	/**
	 * This test will install a bundle which refers to a class from Java runtime
	 * (<code>javax.imageio</code>). As this <code>javax</code> package is
	 * missing in system packages, it can NOT be used and an exception will be
	 * thrown, which is expected behavior.
	 */
	@Test
	public void testGetClassFromBootdelegationMissing() throws Exception {
		startFramework();
		SyntheticBundleBuilder builder = SyntheticBundleBuilder.newBuilder();
		builder.bundleSymbolicName("testGetClassFromBootdelegationMissing")
				.bundleVersion("1.0.0");
		Bundle bundleUnderTest = installBundle(builder);
		bundleUnderTest.start();
		assertBundleResolved(bundleUnderTest);

		String className = "javax.imageio.ImageTranscoder";
		RunInClassLoader runner = new RunInClassLoader(bundleUnderTest);
		try {
			runner.getClass(className);
			Assert.fail("Oops, ClassNotFoundException expected");
		} catch (ClassNotFoundException ex) {
			// OK expected
		}
	}

	/**
	 * This test will install a bundle which refers to a class from Java runtime
	 * (<code>javax.imageio</code>). As this <code>javax</code> package is added
	 * to boot delegation, the class be used which is checked via reflection.
	 */
	@Test
	public void testGetClassFromBootdelegationOK() throws Exception {
		Map<String, String> launchArgs = new HashMap<String, String>();
		launchArgs.put("org.osgi.framework.bootdelegation", "javax.imageio");
		startFrameworkClean(launchArgs);

		SyntheticBundleBuilder builder = SyntheticBundleBuilder.newBuilder();
		builder.bundleSymbolicName("testGetClassFromBootdelegationOK")
				.bundleVersion("1.0.0");
		Bundle bundleUnderTest = installBundle(builder);
		bundleUnderTest.start();
		assertBundleResolved(bundleUnderTest);

		String className = "javax.imageio.ImageTranscoder";
		RunInClassLoader runner = new RunInClassLoader(bundleUnderTest);
		Class<?> clazz = runner.getClass(className);
		Assert.assertNotNull(clazz);
	}

	/**
	 * This test checks whether <code>system.packages</code> can be specified as
	 * launcher args.
	 */
	@Test
	public void testSystemPackages() throws Exception {
		Map<String, String> launchArgs = new HashMap<String, String>();
		launchArgs.put("org.osgi.framework.system.packages.extra", "p1,p2,p3");
		startFramework(launchArgs);
	}

	/**
	 * This test will fail when property
	 * <code>org.osgi.framework.system.packages</code> will contain a trailing
	 * <code>,</code> (Comma).
	 */
	@Test
	public void testSystemPackagesTrailingComma() throws Exception {
		Map<String, String> launchArgs = new HashMap<String, String>();
		launchArgs.put("org.osgi.framework.system.packages.extra", "p1,p2,p3,");
		startFramework(launchArgs);
	}

	/**
	 * This test checks whether <code>system.packages.extra</code> can be
	 * specified as launcher args.
	 */
	@Test
	public void testSystemPackagesExtra() throws Exception {
		Map<String, String> launchArgs = new HashMap<String, String>();
		launchArgs.put("org.osgi.framework.system.packages.extra", "p1,p2,p3");
		startFramework(launchArgs);
	}

	/**
	 * This test will fail when property
	 * <code>org.osgi.framework.system.packages.extra</code> will contain a
	 * trailing <code>,</code> (Comma).
	 */
	@Test
	public void testSystemPackagesExtraTrailingComma() throws Exception {
		Map<String, String> launchArgs = new HashMap<String, String>();
		launchArgs.put("org.osgi.framework.system.packages.extra", "p1,p2,p3,");
		startFramework(launchArgs);
	}
}
