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

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * @author Jochen Hiller
 */
public class JavaxLibrariesTest extends AbstractConciergeTestCase {

	/**
	 * Bundle install of javax.activation will fail due to missing javax.imageio
	 * packages, which is OK. We will check for correct exception content.
	 */
	@Test
	public void testJavaxActivationJavaXImageIOMissing() throws Exception {
		startFramework();
		try {
			installAndStartBundle("javax.activation_1.1.0.v201211130549.jar");
		} catch (BundleException ex) {
			// we will expect a resolution failed exception
			Assert.assertTrue("Bundle will not resolve", ex.getMessage()
					.contains("Resolution failed"));
			Assert.assertTrue("Bundle will not resolve", ex.getMessage()
					.contains("javax.imageio"));
			Assert.assertTrue("Bundle will not resolve", ex.getMessage()
					.contains("javax.imageio.metadata"));
		}
		stopFramework();
	}

	/**
	 * Bundle install of javax.activation will work when these packages are
	 * configured as Java system packages when starting OSGi framework.
	 */
	@Test
	public void testJavaxActivationJavaXImageIOAsSystemPackage()
			throws Exception {
		final Map<String, String> launchArgs = new HashMap<String, String>();
		launchArgs.put("org.osgi.framework.system.packages",
				"javax.imageio,javax.imageio.metadata");
		startFramework(launchArgs);
		final Bundle bundle = installAndStartBundle("javax.activation_1.1.0.v201211130549.jar");
		assertBundleResolved(bundle);
		stopFramework();
	}

	/**
	 * Bundle install of javax.activation MUST work when these packages are
	 * configured as boot delegation in OSGi boot classpath.
	 * 
	 * See OSGi R5 spec chapter 3.9.3: Parent delegation
	 * 
	 * @TODO Will actually fail as not supported by Concierge.
	 * JR: I think this is the expected behavior. Boot delegation works
	 * on the classloader level and does not imply package export. 
	 */
	@Test
	public void testJavaxActivationJavaXImageIOAsBootDelegation()
			throws Exception {
		final Map<String, String> launchArgs = new HashMap<String, String>();
		launchArgs.put("org.osgi.framework.bootdelegation",
				"javax.imageio,javax.imageio.metadata");
		startFramework(launchArgs);
		try {
			final Bundle bundle = installAndStartBundle("javax.activation_1.1.0.v201211130549.jar");
			assertBundleResolved(bundle);
		} finally {
			stopFramework();
		}
	}

	@Test
	public void testJavaxXML() throws Exception {
		final Map<String, String> launchArgs = new HashMap<String, String>();
		launchArgs.put("org.osgi.framework.system.packages",
				"javax.imageio,javax.imageio.metadata,javax.net.ssl");
		startFramework(launchArgs);
		final String[] bundleNames = new String[] {
				"javax.xml_1.3.4.v201005080400.jar",
				"javax.activation_1.1.0.v201211130549.jar",
				"javax.xml.stream_1.0.1.v201004272200.jar",
				"javax.xml.bind_2.2.0.v201105210648.jar" };
		final Bundle[] bundles = installAndStartBundles(bundleNames);
		assertBundlesResolved(bundles);
		stopFramework();
	}

	/**
	 * javax.mail does need activation and javax.net.ssl system packages.
	 */
	@Test
	public void testJavaxMail() throws Exception {
		final Map<String, String> launchArgs = new HashMap<String, String>();
		launchArgs.put("org.osgi.framework.system.packages",
				"javax.imageio,javax.imageio.metadata,javax.net.ssl");
		startFramework(launchArgs);
		final String[] bundleNames = new String[] {
				"javax.activation_1.1.0.v201211130549.jar",
				"javax.mail_1.4.0.v201005080615.jar" };
		final Bundle[] bundles = installAndStartBundles(bundleNames);
		assertBundlesResolved(bundles);
		stopFramework();
	}

	@Test
	public void testJavaxJars() throws Exception {
		final Map<String, String> launchArgs = new HashMap<String, String>();
		launchArgs.put("org.osgi.framework.system.packages",
				"javax.imageio,javax.imageio.metadata");
		startFramework(launchArgs);
		final String[] bundleNames = new String[] {
				"javax.xml_1.3.4.v201005080400.jar",
				"javax.activation_1.1.0.v201211130549.jar",
				"javax.xml.stream_1.0.1.v201004272200.jar",
				"javax.xml.bind_2.2.0.v201105210648.jar" };
		final Bundle[] bundles = installAndStartBundles(bundleNames);
		assertBundlesResolved(bundles);
		stopFramework();
	}

}
