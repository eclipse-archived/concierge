/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.concierge.test.util.AbstractConciergeTestCase;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.osgi.framework.Bundle;

/**
 * Test which tries to install some bundles with basic assumptions.
 * 
 * TODO add more tests and combinations for Bundle-NativeCode, for more OS/ARCH
 * types
 * 
 * @author Jochen Hiller
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ConciergeActivatorTest extends AbstractConciergeTestCase {

	/**
	 * This test will checks whether Activator.start()/stop() will be called
	 * only once. Does work.
	 */
	@Test
	public void test20TestNoOfCallsOfActivator() throws Exception {
		try {
			final Map<String, String> launchArgs = new HashMap<String, String>();
			// needed as bundle is using these packages from runtime
			launchArgs.put("org.osgi.framework.bootdelegation",
					"javax.xml.parsers,org.xml.sax");
			startFrameworkClean(launchArgs);

			final Bundle[] bundles = installAndStartBundles(new String[] { "org.eclipse.osgi.services_3.4.0.v20140312-2051.jar", });
			assertBundlesResolved(bundles);

			final Bundle bundleUnderTest = installAndStartBundle("org.eclipse.concierge.test.support_1.0.0.jar");
			assertBundleResolved(bundleUnderTest);

			RunInClassLoader runner = new RunInClassLoader(bundleUnderTest);
			Object o;

			// check if 1x started, 0x stopped
			o = runner.getClassField(
					"org.eclipse.concierge.test.support.Monitor",
					"noOfCallsOfStart");
			Assert.assertEquals(1, ((Integer) o).intValue());
			o = runner.getClassField(
					"org.eclipse.concierge.test.support.Monitor",
					"noOfCallsOfStop");
			Assert.assertEquals(0, ((Integer) o).intValue());

			// now stop the bundle
			bundleUnderTest.stop();

			// check if 1x started, 1x stopped
			o = runner.getClassField(
					"org.eclipse.concierge.test.support.Monitor",
					"noOfCallsOfStart");
			Assert.assertEquals(1, ((Integer) o).intValue());
			o = runner.getClassField(
					"org.eclipse.concierge.test.support.Monitor",
					"noOfCallsOfStop");
			Assert.assertEquals(1, ((Integer) o).intValue());

		} finally {
			stopFramework();
		}
	}

}
